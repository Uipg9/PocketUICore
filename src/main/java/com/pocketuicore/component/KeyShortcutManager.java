package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Context-based keyboard shortcut manager with modifier-key support,
 * conflict detection, and a built-in help overlay.
 * <p>
 * Contexts are stacked (like {@link com.pocketuicore.focus.FocusManager}),
 * so opening a dialog can push a new context with its own shortcuts,
 * and popping it restores the previous set.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     KeyShortcutManager keys = KeyShortcutManager.getInstance();
 *     keys.pushContext("main");
 *     keys.register("main", GLFW.GLFW_KEY_E, Modifier.NONE,
 *                   "Open Inventory", () -> openInventory());
 *     keys.register("main", GLFW.GLFW_KEY_H, Modifier.CTRL,
 *                   "Toggle Help", () -> keys.toggleHelp());
 *
 *     // in keyPressed():
 *     if (keys.handleKey(keyCode, modifiers)) return true;
 *
 *     // in render():
 *     keys.renderHelp(ctx, screenW, screenH);
 * }</pre>
 */
public final class KeyShortcutManager {

    // ── Singleton ────────────────────────────────────────────────────────
    private static final KeyShortcutManager INSTANCE = new KeyShortcutManager();
    public static KeyShortcutManager getInstance() { return INSTANCE; }

    // ── Modifier flags ───────────────────────────────────────────────────
    public static final int NONE  = 0;
    public static final int CTRL  = 1;
    public static final int SHIFT = 2;
    public static final int ALT   = 4;

    // ── Data structures ──────────────────────────────────────────────────

    /**
     * A registered shortcut binding.
     */
    public record Shortcut(int keyCode, int modifiers, String description,
                           Runnable action) {}

    /**
     * Context: named group of shortcuts.
     */
    private static final class ShortcutContext {
        final String name;
        final List<Shortcut> shortcuts = new ArrayList<>();

        ShortcutContext(String name) { this.name = name; }
    }

    private final Deque<ShortcutContext> contextStack = new ArrayDeque<>();
    private boolean helpVisible = false;

    // =====================================================================
    //  Context management
    // =====================================================================

    /**
     * Push a new named context onto the stack. Shortcuts registered to
     * this context will take priority.
     */
    public void pushContext(String name) {
        contextStack.push(new ShortcutContext(name));
    }

    /**
     * Pop the top context, removing all its shortcuts.
     */
    public void popContext() {
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
    }

    /**
     * Pop all contexts until we find one with the matching name (inclusive).
     */
    public void popContextTo(String name) {
        while (!contextStack.isEmpty()) {
            if (contextStack.peek().name.equals(name)) {
                contextStack.pop();
                break;
            }
            contextStack.pop();
        }
    }

    /** @return the name of the current top context, or {@code null}. */
    public String currentContextName() {
        return contextStack.isEmpty() ? null : contextStack.peek().name;
    }

    /** @return total number of stacked contexts. */
    public int getContextDepth() { return contextStack.size(); }

    /** Remove all contexts and shortcuts. */
    public void clearAll() {
        contextStack.clear();
        helpVisible = false;
    }

    // =====================================================================
    //  Registration
    // =====================================================================

    /**
     * Register a shortcut in the named context. If the context doesn't
     * exist in the stack, a new one is pushed.
     *
     * @param context     context name
     * @param keyCode     GLFW key code
     * @param modifiers   modifier flags (use {@link #CTRL}, {@link #SHIFT}, {@link #ALT}, OR them together)
     * @param description human-readable description shown in help overlay
     * @param action      callback to execute
     * @return conflict description if a duplicate binding already exists in
     *         the context, otherwise {@code null}
     */
    public String register(String context, int keyCode, int modifiers,
                           String description, Runnable action) {
        ShortcutContext ctx = findOrCreateContext(context);
        // Conflict check
        String conflict = null;
        for (Shortcut s : ctx.shortcuts) {
            if (s.keyCode == keyCode && s.modifiers == modifiers) {
                conflict = "Key conflict: " + formatKey(keyCode, modifiers)
                        + " already bound to \"" + s.description + "\"";
                break;
            }
        }
        ctx.shortcuts.add(new Shortcut(keyCode, modifiers, description, action));
        return conflict;
    }

    /**
     * Register without modifiers.
     */
    public String register(String context, int keyCode,
                           String description, Runnable action) {
        return register(context, keyCode, NONE, description, action);
    }

    /**
     * Unregister all shortcuts with the given key+modifier in the context.
     */
    public void unregister(String context, int keyCode, int modifiers) {
        ShortcutContext ctx = findContext(context);
        if (ctx != null) {
            ctx.shortcuts.removeIf(s -> s.keyCode == keyCode && s.modifiers == modifiers);
        }
    }

    // =====================================================================
    //  Key handling
    // =====================================================================

    /**
     * Process a key press. Searches the context stack top-down (newest
     * first) and executes the first matching shortcut.
     *
     * @param keyCode    GLFW key code
     * @param glfwMods   GLFW modifier bitmask (from keyPressed)
     * @return {@code true} if a shortcut was triggered
     */
    public boolean handleKey(int keyCode, int glfwMods) {
        int mods = translateMods(glfwMods);

        for (ShortcutContext ctx : contextStack) {
            for (Shortcut s : ctx.shortcuts) {
                if (s.keyCode == keyCode && s.modifiers == mods) {
                    s.action.run();
                    return true;
                }
            }
        }
        return false;
    }

    // =====================================================================
    //  Help overlay
    // =====================================================================

    /** Toggle the help overlay visibility. */
    public void toggleHelp() { helpVisible = !helpVisible; }

    /** Show the help overlay. */
    public void showHelp() { helpVisible = true; }

    /** Hide the help overlay. */
    public void hideHelp() { helpVisible = false; }

    /** @return {@code true} if the help overlay is visible. */
    public boolean isHelpVisible() { return helpVisible; }

    /**
     * Render the help overlay (semi-transparent background with a list
     * of all active shortcuts).
     */
    public void renderHelp(DrawContext ctx, int screenW, int screenH) {
        if (!helpVisible) return;

        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // ── Gather all active shortcuts ──────────────────────────────────
        List<String[]> entries = new ArrayList<>();  // [key, description]
        Set<String> seen = new HashSet<>();
        for (ShortcutContext sc : contextStack) {
            for (Shortcut s : sc.shortcuts) {
                String key = formatKey(s.keyCode, s.modifiers);
                String id = key + "|" + s.description;
                if (seen.add(id)) {
                    entries.add(new String[]{key, s.description});
                }
            }
        }

        if (entries.isEmpty()) return;

        // ── Layout ───────────────────────────────────────────────────────
        int lineH = 14;
        int pad   = 10;
        int keyColW = 0;
        for (String[] e : entries) {
            int w = textRenderer.getWidth(e[0]);
            if (w > keyColW) keyColW = w;
        }
        int panelW = keyColW + 20 + 160 + pad * 2;
        int panelH = entries.size() * lineH + 30 + pad * 2;
        int px = (screenW - panelW) / 2;
        int py = (screenH - panelH) / 2;

        // ── Background ──────────────────────────────────────────────────
        ProceduralRenderer.drawFullScreenOverlay(ctx, screenW, screenH, ProceduralRenderer.COL_OVERLAY);
        ProceduralRenderer.fillRoundedRectWithBorder(
                ctx, px, py, panelW, panelH,
                6, ProceduralRenderer.COL_BG_PRIMARY, ProceduralRenderer.COL_BORDER
        );

        // ── Title ────────────────────────────────────────────────────────
        ProceduralRenderer.drawCenteredText(
                ctx, textRenderer, "Keyboard Shortcuts",
                px + panelW / 2, py + pad,
                ProceduralRenderer.COL_ACCENT
        );
        ProceduralRenderer.drawDivider(
                ctx, px + pad, py + pad + 14, panelW - pad * 2,
                ProceduralRenderer.COL_BORDER
        );

        // ── Entries ──────────────────────────────────────────────────────
        int ey = py + pad + 24;
        for (String[] e : entries) {
            // Key badge
            int kw = textRenderer.getWidth(e[0]);
            int bx = px + pad + keyColW - kw;
            ctx.fill(bx - 3, ey - 2, bx + kw + 3, ey + 11, ProceduralRenderer.COL_BG_ELEVATED);
            ProceduralRenderer.drawText(ctx, textRenderer, e[0], bx, ey, ProceduralRenderer.COL_ACCENT);
            // Description
            ProceduralRenderer.drawText(ctx, textRenderer, e[1],
                    px + pad + keyColW + 20, ey,
                    ProceduralRenderer.COL_TEXT_PRIMARY);
            ey += lineH;
        }
    }

    // =====================================================================
    //  Internals
    // =====================================================================

    private ShortcutContext findContext(String name) {
        for (ShortcutContext ctx : contextStack) {
            if (ctx.name.equals(name)) return ctx;
        }
        return null;
    }

    private ShortcutContext findOrCreateContext(String name) {
        ShortcutContext ctx = findContext(name);
        if (ctx == null) {
            ctx = new ShortcutContext(name);
            contextStack.push(ctx);
        }
        return ctx;
    }

    /** Translate GLFW modifier bitmask to our compact flags. */
    private static int translateMods(int glfwMods) {
        int m = 0;
        if ((glfwMods & GLFW.GLFW_MOD_CONTROL) != 0) m |= CTRL;
        if ((glfwMods & GLFW.GLFW_MOD_SHIFT)   != 0) m |= SHIFT;
        if ((glfwMods & GLFW.GLFW_MOD_ALT)     != 0) m |= ALT;
        return m;
    }

    /** Format a key+modifier combo as a human-readable string. */
    public static String formatKey(int keyCode, int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & CTRL)  != 0) sb.append("Ctrl+");
        if ((modifiers & SHIFT) != 0) sb.append("Shift+");
        if ((modifiers & ALT)   != 0) sb.append("Alt+");
        sb.append(getKeyName(keyCode));
        return sb.toString();
    }

    /** Get a readable name for a GLFW key code. */
    private static String getKeyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE       -> "Space";
            case GLFW.GLFW_KEY_ESCAPE      -> "Esc";
            case GLFW.GLFW_KEY_ENTER       -> "Enter";
            case GLFW.GLFW_KEY_TAB         -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE   -> "Backspace";
            case GLFW.GLFW_KEY_DELETE      -> "Delete";
            case GLFW.GLFW_KEY_UP          -> "↑";
            case GLFW.GLFW_KEY_DOWN        -> "↓";
            case GLFW.GLFW_KEY_LEFT        -> "←";
            case GLFW.GLFW_KEY_RIGHT       -> "→";
            case GLFW.GLFW_KEY_HOME        -> "Home";
            case GLFW.GLFW_KEY_END         -> "End";
            case GLFW.GLFW_KEY_PAGE_UP     -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN   -> "PgDn";
            case GLFW.GLFW_KEY_F1          -> "F1";
            case GLFW.GLFW_KEY_F2          -> "F2";
            case GLFW.GLFW_KEY_F3          -> "F3";
            case GLFW.GLFW_KEY_F4          -> "F4";
            case GLFW.GLFW_KEY_F5          -> "F5";
            case GLFW.GLFW_KEY_F6          -> "F6";
            case GLFW.GLFW_KEY_F7          -> "F7";
            case GLFW.GLFW_KEY_F8          -> "F8";
            case GLFW.GLFW_KEY_F9          -> "F9";
            case GLFW.GLFW_KEY_F10         -> "F10";
            case GLFW.GLFW_KEY_F11         -> "F11";
            case GLFW.GLFW_KEY_F12         -> "F12";
            default -> {
                if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                    yield String.valueOf((char) keyCode);
                } else if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                    yield String.valueOf((char) keyCode);
                } else {
                    yield "Key" + keyCode;
                }
            }
        };
    }
}
