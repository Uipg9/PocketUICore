package com.pocketuicore.controller;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLStdinc;

import java.nio.IntBuffer;

/**
 * {@link GamepadBackend} on top of the SDL3 gamepad API (LWJGL {@code lwjgl-sdl}).
 * <p>
 * Every native call is guarded: if the SDL natives are missing, the gamepad
 * subsystem cannot be initialised, or no controller is attached, this behaves
 * as "no controller". The first failure is logged once; nothing ever throws.
 */
public final class SdlGamepadBackend implements GamepadBackend {

    /** Re-scan for devices every this many updates while none is connected (about 1 s). */
    private static final int RESCAN_INTERVAL = 20;

    private boolean failed;          // permanent: SDL unusable
    private boolean subsystemUp;
    private long handle;             // SDL_Gamepad pointer, 0 = none
    private int sinceScan = RESCAN_INTERVAL;
    private boolean warned;

    public SdlGamepadBackend() { }

    // -- Pure helpers (unit tested) ---------------------------------------

    /** Map an SDL axis value (-32768..32767) to [-1, 1]. */
    static float normalizeAxis(short raw) {
        float f = raw >= 0 ? raw / 32767f : raw / 32768f;
        return Math.max(-1f, Math.min(1f, f));
    }

    /** Convert a 0-1 intensity to the unsigned 16-bit rumble magnitude SDL expects (as a short bit pattern). */
    static short toRumbleMagnitude(float intensity) {
        float c = Math.max(0f, Math.min(1f, intensity));
        return (short) Math.round(c * 65535f);
    }

    private static int sdlButton(GamepadButton b) {
        return switch (b) {
            case A            -> SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH;
            case B            -> SDLGamepad.SDL_GAMEPAD_BUTTON_EAST;
            case LEFT_BUMPER  -> SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
            case RIGHT_BUMPER -> SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
            case DPAD_UP      -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP;
            case DPAD_DOWN    -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
            case DPAD_LEFT    -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
            case DPAD_RIGHT   -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
        };
    }

    private static int sdlAxis(GamepadAxis a) {
        return switch (a) {
            case LEFT_X  -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX;
            case LEFT_Y  -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY;
            case RIGHT_X -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX;
            case RIGHT_Y -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY;
        };
    }

    // -- GamepadBackend -----------------------------------------------------

    @Override
    public void update() {
        if (failed) return;
        try {
            if (!subsystemUp) {
                if (!SDLInit.SDL_InitSubSystem(SDLInit.SDL_INIT_GAMEPAD)) {
                    fail("SDL_InitSubSystem(GAMEPAD) failed: " + sdlError());
                    return;
                }
                subsystemUp = true;
            }

            if (handle != 0L && !SDLGamepad.SDL_GamepadConnected(handle)) {
                SDLGamepad.SDL_CloseGamepad(handle);
                handle = 0L;
                sinceScan = RESCAN_INTERVAL;
            }
            if (handle == 0L && ++sinceScan >= RESCAN_INTERVAL) {
                sinceScan = 0;
                scanForGamepad();
            }
            if (handle != 0L) {
                SDLGamepad.SDL_UpdateGamepads();
            }
        } catch (Throwable t) {
            fail("SDL gamepad polling failed: " + t);
        }
    }

    private void scanForGamepad() {
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        if (ids == null) return;
        try {
            for (int i = 0; i < ids.remaining(); i++) {
                long g = SDLGamepad.SDL_OpenGamepad(ids.get(ids.position() + i));
                if (g != 0L) {
                    handle = g;
                    return;
                }
            }
        } finally {
            SDLStdinc.SDL_free(ids);
        }
    }

    @Override
    public boolean isConnected() {
        return !failed && handle != 0L;
    }

    @Override
    public boolean isPressed(GamepadButton button) {
        if (!isConnected()) return false;
        try {
            return SDLGamepad.SDL_GetGamepadButton(handle, sdlButton(button));
        } catch (Throwable t) {
            fail("SDL gamepad read failed: " + t);
            return false;
        }
    }

    @Override
    public float axis(GamepadAxis axis) {
        if (!isConnected()) return 0f;
        try {
            return normalizeAxis(SDLGamepad.SDL_GetGamepadAxis(handle, sdlAxis(axis)));
        } catch (Throwable t) {
            fail("SDL gamepad read failed: " + t);
            return 0f;
        }
    }

    @Override
    public void rumble(float strong, float weak, int durationMs) {
        if (!isConnected()) return;
        try {
            SDLGamepad.SDL_RumbleGamepad(handle, toRumbleMagnitude(strong), toRumbleMagnitude(weak),
                    Math.max(0, durationMs));
        } catch (Throwable ignored) {
            // rumble is best-effort
        }
    }

    @Override
    public void close() {
        try {
            if (handle != 0L) SDLGamepad.SDL_CloseGamepad(handle);
        } catch (Throwable ignored) {
            // nothing to do
        }
        handle = 0L;
        try {
            if (subsystemUp) SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_GAMEPAD);
        } catch (Throwable ignored) {
            // nothing to do
        }
        subsystemUp = false;
    }

    // -- Internals ----------------------------------------------------------

    private static String sdlError() {
        try {
            return SDLError.SDL_GetError();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private void fail(String message) {
        failed = true;
        handle = 0L;
        if (!warned) {
            warned = true;
            System.err.println("[PocketUICore] Controller support disabled: " + message);
        }
    }
}
