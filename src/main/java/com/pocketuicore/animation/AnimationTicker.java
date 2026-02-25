package com.pocketuicore.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module 4 — Animation Engine
 * <p>
 * A lightweight, <b>client-side</b> easing system driven by
 * {@link System#nanoTime()} so animations are smooth regardless of server
 * tick rate or frame-rate fluctuations.
 * <p>
 * <b>Usage pattern:</b>
 * <pre>{@code
 *     AnimationTicker anim = AnimationTicker.getInstance();
 *
 *     // Start an animation
 *     anim.start("menuSlide", 0f, 1f, 300, EasingType.EASE_IN_OUT);
 *
 *     // In your render() method every frame:
 *     float t = anim.get("menuSlide"); // returns current eased value
 *     int panelX = (int) lerp(-200, 0, t);
 * }</pre>
 * <p>
 * Completed animations remain in the map at their final value until
 * {@link #tick()} cleans them up, or until they are explicitly removed
 * via {@link #cancel(String)}.
 */
public final class AnimationTicker {

    // ── Singleton ────────────────────────────────────────────────────────
    private static final AnimationTicker INSTANCE = new AnimationTicker();

    public static AnimationTicker getInstance() { return INSTANCE; }

    private AnimationTicker() { }

    // ── Active animations, keyed by a caller-chosen ID ───────────────────
    private final Map<String, Animation> animations = new ConcurrentHashMap<>();

    // =====================================================================
    //  Easing types
    // =====================================================================

    public enum EasingType {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        /** Starts fast, overshoots slightly, settles. */
        EASE_OUT_BACK,
        /** Gentle sine-based ease in and out. */
        EASE_IN_OUT_SINE
    }

    // =====================================================================
    //  Animation record
    // =====================================================================

    private static final class Animation {
        final float from;
        final float to;
        final long startNanos;
        final long durationNanos;
        final EasingType easing;
        boolean completed;
        /** Set to true one tick after {@link #completed} becomes true. */
        boolean readyToRemove;

        Animation(float from, float to, long durationMs, EasingType easing) {
            this.from          = from;
            this.to            = to;
            this.startNanos    = System.nanoTime();
            this.durationNanos = durationMs * 1_000_000L;
            this.easing        = easing;
        }

        float evaluate() {
            long elapsed = System.nanoTime() - startNanos;
            if (elapsed >= durationNanos) {
                completed = true;
                return to;
            }
            float t = (float) elapsed / durationNanos; // 0 → 1 linear
            t = applyEasing(t, easing);
            return from + (to - from) * t;
        }
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    /**
     * Start (or restart) a named animation.
     *
     * @param id         unique key — reusing a key replaces the old animation
     * @param from       starting value
     * @param to         ending value
     * @param durationMs duration in <b>milliseconds</b>
     * @param easing     easing curve
     */
    public void start(String id, float from, float to, long durationMs, EasingType easing) {
        animations.put(id, new Animation(from, to, Math.max(1, durationMs), easing));
    }

    /** Convenience — defaults to {@link EasingType#EASE_IN_OUT}. */
    public void start(String id, float from, float to, long durationMs) {
        start(id, from, to, durationMs, EasingType.EASE_IN_OUT);
    }

    /**
     * Get the current animated value.
     *
     * @param id           animation key
     * @param defaultValue returned if no animation with this key exists
     */
    public float get(String id, float defaultValue) {
        Animation a = animations.get(id);
        return a != null ? a.evaluate() : defaultValue;
    }

    /** Shorthand — default value is 0. */
    public float get(String id) {
        return get(id, 0f);
    }

    /**
     * Get the current value and clamp it to [0, 1].
     * Convenient for progress / alpha multipliers.
     */
    public float get01(String id) {
        return Math.clamp(get(id, 0f), 0f, 1f);
    }

    /** @return {@code true} if the animation exists and has not yet finished. */
    public boolean isActive(String id) {
        Animation a = animations.get(id);
        return a != null && !a.completed;
    }

    /** @return {@code true} if the animation exists (running or completed). */
    public boolean exists(String id) {
        return animations.containsKey(id);
    }

    /** Cancel an animation immediately and remove it from the map. */
    public void cancel(String id) {
        animations.remove(id);
    }

    /** Cancel all active animations. */
    public void cancelAll() {
        animations.clear();
    }

    // =====================================================================
    //  Tick — called from PocketUICoreClient on END_CLIENT_TICK
    // =====================================================================

    /**
     * Housekeeping: remove completed animations so the map doesn't grow
     * indefinitely. Safe to call every client tick (~20 Hz).
     * <p>
     * Uses two-phase cleanup: an animation is only removed on the tick
     * <em>after</em> it first completes, guaranteeing that the final
     * value is available for at least one full render cycle.
     */
    public void tick() {
        animations.entrySet().removeIf(e -> {
            Animation a = e.getValue();
            a.evaluate(); // ensure completed flag is set
            if (a.completed && a.readyToRemove) {
                return true;  // second tick after completion → safe to remove
            }
            if (a.completed) {
                a.readyToRemove = true; // mark; will be removed next tick
            }
            return false;
        });
    }

    // =====================================================================
    //  Static easing helpers  (usable standalone)
    // =====================================================================

    /**
     * Linear interpolation: {@code a + (b - a) * t}.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Framerate-independent smoothing ("exponential decay lerp").
     * Call every frame with the current and target value.
     *
     * @param current current value
     * @param target  target value
     * @param speed   smoothing speed (6–12 is typical)
     * @param delta   frame delta time (seconds)
     */
    public static float smoothDamp(float current, float target, float speed, float delta) {
        return current + (target - current) * Math.min(speed * delta, 1f);
    }

    /**
     * Apply an easing curve to a linear {@code t} in [0, 1].
     */
    public static float applyEasing(float t, EasingType type) {
        return switch (type) {
            case LINEAR          -> t;
            case EASE_IN         -> t * t * t;
            case EASE_OUT        -> 1f - (1f - t) * (1f - t) * (1f - t);
            case EASE_IN_OUT     -> t < 0.5f
                                    ? 4f * t * t * t
                                    : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            case EASE_OUT_BACK   -> {
                float c1 = 1.70158f;
                float c3 = c1 + 1f;
                yield 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
            }
            case EASE_IN_OUT_SINE -> -(float) (Math.cos(Math.PI * t) - 1f) / 2f;
        };
    }

    // =====================================================================
    //  Namespaced contexts
    // =====================================================================

    /**
     * Create a namespaced animation context.  All keys passed through
     * the returned {@link AnimationContext} are automatically prefixed
     * with {@code "namespace:"}, preventing collisions between screens
     * or components that share the global AnimationTicker.
     *
     * <pre>{@code
     *     AnimationContext ctx = AnimationTicker.getInstance()
     *             .createContext("shop");
     *     ctx.start("fade", 0, 1, 300);    // stored as "shop:fade"
     *     float f = ctx.get("fade");        // reads "shop:fade"
     *     ctx.cancelAll();                  // cancels "shop:*" only
     * }</pre>
     *
     * @param namespace the prefix (must not contain {@code ':'})
     * @return a new AnimationContext bound to this ticker
     */
    public AnimationContext createContext(String namespace) {
        return new AnimationContext(this, namespace);
    }

    /**
     * A namespaced view over the global {@link AnimationTicker}.
     * All keys are transparently prefixed so different callers
     * never collide.
     */
    public static final class AnimationContext {
        private final AnimationTicker ticker;
        private final String prefix;

        AnimationContext(AnimationTicker ticker, String namespace) {
            this.ticker = ticker;
            this.prefix = namespace + ":";
        }

        private String key(String id) { return prefix + id; }

        /** @see AnimationTicker#start(String, float, float, long, EasingType) */
        public void start(String id, float from, float to, long durationMs, EasingType easing) {
            ticker.start(key(id), from, to, durationMs, easing);
        }

        /** @see AnimationTicker#start(String, float, float, long) */
        public void start(String id, float from, float to, long durationMs) {
            ticker.start(key(id), from, to, durationMs);
        }

        /** @see AnimationTicker#get(String, float) */
        public float get(String id, float defaultValue) {
            return ticker.get(key(id), defaultValue);
        }

        /** @see AnimationTicker#get(String) */
        public float get(String id) { return ticker.get(key(id)); }

        /** @see AnimationTicker#get01(String) */
        public float get01(String id) { return ticker.get01(key(id)); }

        /** @see AnimationTicker#isActive(String) */
        public boolean isActive(String id) { return ticker.isActive(key(id)); }

        /** @see AnimationTicker#exists(String) */
        public boolean exists(String id) { return ticker.exists(key(id)); }

        /** @see AnimationTicker#cancel(String) */
        public void cancel(String id) { ticker.cancel(key(id)); }

        /**
         * Cancel all animations whose key starts with this context's
         * namespace prefix.  Other contexts are unaffected.
         */
        public void cancelAll() {
            ticker.animations.entrySet().removeIf(
                    e -> e.getKey().startsWith(prefix));
        }

        /** @return the namespace prefix (without trailing ':'). */
        public String getNamespace() { return prefix.substring(0, prefix.length() - 1); }
    }
}
