package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Button Group — Mutually-exclusive mode selector.
 * <p>
 * Manages a set of {@link HoverButton}s where only one can be "active"
 * at a time, like a radio-button group or tab bar. When a button is
 * clicked the previously active button is deselected automatically.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ButtonGroup tabs = new ButtonGroup();
 *     tabs.add(btnOverview);
 *     tabs.add(btnEstate);
 *     tabs.add(btnPrestige);
 *     tabs.setOnChanged(index -> openTab(index));
 *     tabs.select(0); // activate first
 * }</pre>
 */
public final class ButtonGroup {

    /** A tracked button and its original colours. */
    private static final class Entry {
        final HoverButton button;
        final int originalNormalColor;
        final int originalHoverColor;

        Entry(HoverButton button) {
            this.button = button;
            this.originalNormalColor = button.getNormalColor();
            this.originalHoverColor = button.getHoverColor();
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int selectedIndex = -1;

    private int activeColor      = ProceduralRenderer.COL_ACCENT;
    private int activeHoverColor = ProceduralRenderer.COL_ACCENT;
    private Consumer<Integer> onChanged;

    // =====================================================================
    //  Build
    // =====================================================================

    /**
     * Add a button to the group. Its click handler is wrapped so that
     * clicking it selects it in the group.
     *
     * @param button the HoverButton to add
     */
    public void add(HoverButton button) {
        int idx = entries.size();
        Entry entry = new Entry(button);
        entries.add(entry);

        // Wrap the onClick so selecting this button updates the group
        Runnable originalClick = button.getOnClick() instanceof Runnable r ? r : null;
        button.setOnClick(() -> {
            select(idx);
            if (originalClick != null) originalClick.run();
        });
    }

    /**
     * Select the button at the given index as active.
     *
     * @param index 0-based index in insertion order
     */
    public void select(int index) {
        if (index < 0 || index >= entries.size()) return;
        if (index == selectedIndex) return;

        // Deselect previous
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            Entry prev = entries.get(selectedIndex);
            prev.button.setNormalColor(prev.originalNormalColor);
            prev.button.setHoverColor(prev.originalHoverColor);
        }

        // Select new
        selectedIndex = index;
        Entry current = entries.get(index);
        current.button.setNormalColor(activeColor);
        current.button.setHoverColor(activeHoverColor);

        if (onChanged != null) onChanged.accept(index);
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    /** @return the currently selected index, or -1 if none. */
    public int getSelectedIndex() { return selectedIndex; }

    /** @return the currently selected button, or {@code null}. */
    public HoverButton getSelectedButton() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return null;
        return entries.get(selectedIndex).button;
    }

    /** @return total number of buttons in the group. */
    public int size() { return entries.size(); }

    /**
     * Set the colour used for the active/selected button.
     */
    public void setActiveColor(int normalColor, int hoverColor) {
        this.activeColor = normalColor;
        this.activeHoverColor = hoverColor;
        // Re-apply to current selection
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            entries.get(selectedIndex).button.setNormalColor(normalColor);
            entries.get(selectedIndex).button.setHoverColor(hoverColor);
        }
    }

    /**
     * Set a callback invoked when the selection changes.
     *
     * @param listener receives the newly selected index
     */
    public void setOnChanged(Consumer<Integer> listener) {
        this.onChanged = listener;
    }

    /**
     * Remove all buttons from the group.
     */
    public void clear() {
        // Restore colours
        for (Entry e : entries) {
            e.button.setNormalColor(e.originalNormalColor);
            e.button.setHoverColor(e.originalHoverColor);
        }
        entries.clear();
        selectedIndex = -1;
    }
}
