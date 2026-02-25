package com.pocketuicore.data;

import com.pocketuicore.component.PercentageBar;
import com.pocketuicore.component.TextLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Observable State — Reactive Data Binding
 * <p>
 * A lightweight observable value wrapper.  Listeners are notified whenever
 * the value changes, enabling automatic UI updates without manual polling.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ObservableState<Float> health = new ObservableState<>(1.0f);
 *     health.addListener(val -> myBar.setProgress(val));
 *
 *     // Later — the bar updates automatically:
 *     health.set(0.5f);
 *
 *     // Derived / mapped state:
 *     ObservableState<String> label =
 *             health.map(h -> "HP: " + Math.round(h * 100) + "%");
 * }</pre>
 *
 * @param <T> the value type
 */
public class ObservableState<T> {

    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    // =====================================================================
    //  Construction
    // =====================================================================

    public ObservableState(T initialValue) {
        this.value = initialValue;
    }

    /** Create an ObservableState with a {@code null} initial value. */
    public static <T> ObservableState<T> empty() {
        return new ObservableState<>(null);
    }

    /** Create an ObservableState with the given initial value. */
    public static <T> ObservableState<T> of(T value) {
        return new ObservableState<>(value);
    }

    // =====================================================================
    //  Value access
    // =====================================================================

    /** Get the current value. */
    public T get() { return value; }

    /**
     * Set a new value and notify all listeners if the value actually changed
     * (compared via {@link Objects#equals}).
     *
     * @param newValue the new value
     */
    public void set(T newValue) {
        if (Objects.equals(this.value, newValue)) return;
        this.value = newValue;
        notifyListeners(newValue);
    }

    /**
     * Force-set the value and notify listeners even if the value is
     * equal to the current one.
     */
    public void forceSet(T newValue) {
        this.value = newValue;
        notifyListeners(newValue);
    }

    private void notifyListeners(T val) {
        for (Consumer<T> listener : listeners) {
            listener.accept(val);
        }
    }

    // =====================================================================
    //  Listeners
    // =====================================================================

    /**
     * Add a listener that is called whenever the value changes.
     * The listener receives the new value.
     */
    public void addListener(Consumer<T> listener) {
        if (listener != null) listeners.add(listener);
    }

    /** Remove a previously added listener. */
    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    /** Remove all listeners. */
    public void clearListeners() {
        listeners.clear();
    }

    /** @return the number of currently registered listeners. */
    public int getListenerCount() { return listeners.size(); }

    // =====================================================================
    //  Derived state
    // =====================================================================

    /**
     * Create a new ObservableState that is derived from this one via a
     * mapping function.  When this state changes, the derived state
     * automatically updates.
     * <p>
     * The returned {@link MappedState} extends {@code ObservableState<R>}
     * and provides a {@link MappedState#dispose()} method to unsubscribe
     * from the source, preventing memory leaks when the derived state
     * is no longer needed.
     *
     * @param mapper transformation function
     * @param <R>    result type
     * @return a new MappedState that tracks this one
     */
    public <R> MappedState<R> map(Function<T, R> mapper) {
        Consumer<T> bridge = val -> {}; // placeholder — replaced below
        MappedState<R> derived = new MappedState<>(mapper.apply(value), this, bridge);
        // Build the real bridge that updates the derived state
        Consumer<T> realBridge = val -> derived.set(mapper.apply(val));
        derived.sourceBridge = realBridge;
        addListener(realBridge);
        return derived;
    }

    /**
     * A derived observable state returned by {@link #map(Function)}.
     * <p>
     * Call {@link #dispose()} when this mapped state is no longer needed
     * to remove the listener from the source and avoid memory leaks.
     *
     * @param <R> the derived value type
     */
    public static class MappedState<R> extends ObservableState<R> {
        private final List<ObservableState<?>> sources = new ArrayList<>();
        private final List<Consumer<?>> bridges = new ArrayList<>();
        Consumer<?> sourceBridge;
        private boolean disposed = false;

        MappedState(R initialValue, ObservableState<?> source, Consumer<?> bridge) {
            super(initialValue);
            this.sources.add(source);
            this.sourceBridge = bridge;
        }

        MappedState(R initialValue) {
            super(initialValue);
        }

        void addSource(ObservableState<?> source, Consumer<?> bridge) {
            this.sources.add(source);
            this.bridges.add(bridge);
        }

        /**
         * Unsubscribe from the source observable(s). After calling this,
         * this mapped state will no longer receive updates.
         * <p>
         * Safe to call multiple times.
         */
        @SuppressWarnings("unchecked")
        public void dispose() {
            if (!disposed) {
                // Legacy single-source bridge
                if (sourceBridge != null && !sources.isEmpty()) {
                    ((ObservableState<Object>) sources.get(0))
                            .removeListener((Consumer<Object>) sourceBridge);
                }
                // Multi-source bridges (from combine)
                for (int i = 0; i < bridges.size(); i++) {
                    if (i < sources.size()) {
                        ((ObservableState<Object>) sources.get(i))
                                .removeListener((Consumer<Object>) bridges.get(i));
                    }
                }
                disposed = true;
            }
        }

        /** @return {@code true} if this mapped state has been disposed. */
        public boolean isDisposed() { return disposed; }
    }

    // =====================================================================
    //  Combining multiple observables
    // =====================================================================

    /**
     * Create a new derived ObservableState that combines two source
     * observables using a combining function.  The derived state updates
     * whenever <em>either</em> source changes.
     * <p>
     * Call {@link MappedState#dispose()} on the result when it is no
     * longer needed.
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     *     ObservableState<Integer> a = new ObservableState<>(3);
     *     ObservableState<Integer> b = new ObservableState<>(4);
     *     var sum = ObservableState.combine(a, b, Integer::sum);
     *     // sum.get() == 7
     *     a.set(10);
     *     // sum.get() == 14
     * }</pre>
     *
     * @param stateA   first source
     * @param stateB   second source
     * @param combiner function that merges the two values
     * @param <A>      type of first source
     * @param <B>      type of second source
     * @param <R>      type of the combined result
     * @return a new MappedState tracking both sources
     */
    public static <A, B, R> MappedState<R> combine(
            ObservableState<A> stateA,
            ObservableState<B> stateB,
            BiFunction<A, B, R> combiner) {

        R initial = combiner.apply(stateA.get(), stateB.get());
        MappedState<R> derived = new MappedState<>(initial);

        Consumer<A> bridgeA = val -> derived.set(combiner.apply(val, stateB.get()));
        Consumer<B> bridgeB = val -> derived.set(combiner.apply(stateA.get(), val));

        derived.addSource(stateA, bridgeA);
        derived.addSource(stateB, bridgeB);

        stateA.addListener(bridgeA);
        stateB.addListener(bridgeB);

        return derived;
    }

    // =====================================================================
    //  Convenience bindings for common UI components
    // =====================================================================

    /**
     * Bind a Float observable to a {@link PercentageBar}'s progress.
     * The bar updates automatically whenever the observable changes.
     *
     * @param state the observable float (0.0–1.0)
     * @param bar   the target PercentageBar
     */
    public static void bindProgress(ObservableState<Float> state, PercentageBar bar) {
        bar.setProgress(state.get() != null ? state.get() : 0f);
        state.addListener(val -> bar.setProgress(val != null ? val : 0f));
    }

    /**
     * Bind a String observable to a {@link TextLabel}'s text.
     * The label updates automatically whenever the observable changes.
     *
     * @param state the observable string
     * @param label the target TextLabel
     */
    public static void bindText(ObservableState<String> state, TextLabel label) {
        label.setText(state.get() != null ? state.get() : "");
        state.addListener(val -> label.setText(val != null ? val : ""));
    }
}
