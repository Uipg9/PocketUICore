package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model for list / selectable-list components.
 * <p>
 * Manages an observable list of string items plus the selected index.
 * Views can subscribe to changes and re-render automatically.
 *
 * @since 1.13.0
 */
public class ListModel {

    private final List<String> items = new ArrayList<>();
    private final ObservableState<Integer> selectedIndex;
    private final ObservableState<Integer> itemCount;

    public ListModel() {
        this.selectedIndex = new ObservableState<>(-1);
        this.itemCount     = new ObservableState<>(0);
    }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<Integer> selectedIndexState() { return selectedIndex; }
    public ObservableState<Integer> itemCountState()     { return itemCount; }

    // ── Items ────────────────────────────────────────────────────────────

    public List<String> getItems() { return Collections.unmodifiableList(items); }

    public void addItem(String item) {
        items.add(item);
        itemCount.set(items.size());
    }

    public void removeItem(int index) {
        if (index < 0 || index >= items.size()) return;
        items.remove(index);
        itemCount.set(items.size());
        if (selectedIndex.get() >= items.size()) {
            selectedIndex.set(items.isEmpty() ? -1 : items.size() - 1);
        }
    }

    public void clearItems() {
        items.clear();
        itemCount.set(0);
        selectedIndex.set(-1);
    }

    public void setItems(List<String> newItems) {
        items.clear();
        items.addAll(newItems);
        itemCount.set(items.size());
        if (selectedIndex.get() >= items.size()) {
            selectedIndex.set(items.isEmpty() ? -1 : 0);
        }
    }

    // ── Selection ────────────────────────────────────────────────────────

    public int getSelectedIndex() { return selectedIndex.get(); }

    public void setSelectedIndex(int idx) {
        if (idx < -1 || idx >= items.size()) return;
        selectedIndex.set(idx);
    }

    /** @return the selected item, or {@code null} if nothing is selected. */
    public String getSelectedItem() {
        int idx = selectedIndex.get();
        return (idx >= 0 && idx < items.size()) ? items.get(idx) : null;
    }

    public int size() { return items.size(); }
}
