package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Tabbed panel — a tab bar at the top with auto-sized buttons and a
 * content area below that swaps based on the active tab.
 * <p>
 * <ul>
 *   <li>Tab key cycles through tabs.</li>
 *   <li>Optional Ctrl+1–9 numbered hotkeys.</li>
 *   <li>Active tab gets accent underline highlight.</li>
 *   <li>Tab change callback.</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     TabbedPanel tabs = new TabbedPanel(10, 10, 300, 200);
 *     tabs.addTab("Stats",  statsPanel);
 *     tabs.addTab("Crops",  cropsPanel);
 *     tabs.addTab("Config", configPanel);
 *     tabs.setOnTabChange(idx -> refreshContent(idx));
 * }</pre>
 */
public final class TabbedPanel extends UIComponent {

    // ── Tab definition ───────────────────────────────────────────────────
    private record Tab(String label, UIComponent content) {}

    // ── State ────────────────────────────────────────────────────────────
    private final List<Tab> tabs = new ArrayList<>();
    private int activeIndex = 0;
    private IntConsumer onTabChange;
    private boolean numberHotkeys = true; // Ctrl+1-9

    // ── Appearance ───────────────────────────────────────────────────────
    private static final int TAB_BAR_HEIGHT  = 24;
    private static final int TAB_PAD_H       = 10;  // horizontal padding per tab
    private static final int UNDERLINE_H     = 2;
    private int tabBarBg      = ProceduralRenderer.COL_BG_PRIMARY;
    private int tabNormalColor = ProceduralRenderer.COL_BG_SURFACE;
    private int tabActiveColor = ProceduralRenderer.COL_BG_ELEVATED;
    private int tabTextColor  = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int accentColor   = ProceduralRenderer.COL_ACCENT;

    // =====================================================================
    //  Constructor
    // =====================================================================

    public TabbedPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // =====================================================================
    //  Tab management
    // =====================================================================

    /**
     * Add a tab with the given label and content panel.
     * The content's bounds are automatically set to the content area.
     */
    public void addTab(String label, UIComponent content) {
        content.setPosition(x, y + TAB_BAR_HEIGHT);
        content.setBounds(x, y + TAB_BAR_HEIGHT, width, height - TAB_BAR_HEIGHT);
        tabs.add(new Tab(label, content));
        if (tabs.size() == 1) {
            // First tab is auto-selected
            addChild(content);
        }
    }

    /** Remove a tab by index. */
    public void removeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        Tab removed = tabs.remove(index);
        removeChild(removed.content);
        if (activeIndex >= tabs.size()) {
            setActiveTab(Math.max(0, tabs.size() - 1));
        } else if (activeIndex == index) {
            setActiveTab(activeIndex);
        }
    }

    /** @return number of tabs. */
    public int getTabCount() { return tabs.size(); }

    /** @return the active tab index. */
    public int getActiveIndex() { return activeIndex; }

    /** @return the content component of the active tab, or {@code null}. */
    public UIComponent getActiveContent() {
        return (activeIndex >= 0 && activeIndex < tabs.size())
                ? tabs.get(activeIndex).content() : null;
    }

    // =====================================================================
    //  Tab switching
    // =====================================================================

    /** Set the active tab by index. */
    public void setActiveTab(int index) {
        if (index < 0 || index >= tabs.size() || index == activeIndex && !children.isEmpty()) return;
        // Remove current content
        clearChildren();
        activeIndex = index;
        // Add new content
        Tab t = tabs.get(activeIndex);
        t.content.setPosition(x, y + TAB_BAR_HEIGHT);
        t.content.setBounds(x, y + TAB_BAR_HEIGHT, width, height - TAB_BAR_HEIGHT);
        addChild(t.content);
        if (onTabChange != null) onTabChange.accept(activeIndex);
    }

    /** Cycle to the next tab (wraps). */
    public void nextTab() {
        if (tabs.size() <= 1) return;
        setActiveTab((activeIndex + 1) % tabs.size());
        UISoundManager.playClick();
    }

    /** Cycle to the previous tab (wraps). */
    public void prevTab() {
        if (tabs.size() <= 1) return;
        setActiveTab((activeIndex - 1 + tabs.size()) % tabs.size());
        UISoundManager.playClick();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        if (tabs.isEmpty()) return;

        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // ── Tab bar background ───────────────────────────────────────────
        ctx.fill(x, y, x + width, y + TAB_BAR_HEIGHT, tabBarBg);

        // ── Compute tab widths (auto-size to text) ───────────────────────
        int totalW = 0;
        int[] tabWidths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            tabWidths[i] = textRenderer.getWidth(tabs.get(i).label()) + TAB_PAD_H * 2;
            totalW += tabWidths[i];
        }

        // ── Draw tabs ────────────────────────────────────────────────────
        int tx = x;
        for (int i = 0; i < tabs.size(); i++) {
            int tw = tabWidths[i];
            boolean active = (i == activeIndex);
            boolean hovered = mouseX >= tx && mouseX < tx + tw
                    && mouseY >= y && mouseY < y + TAB_BAR_HEIGHT;

            // Tab background
            int bg = active ? tabActiveColor : (hovered ? ProceduralRenderer.COL_HOVER : tabNormalColor);
            ctx.fill(tx, y, tx + tw, y + TAB_BAR_HEIGHT, bg);

            // Tab label
            int textX = tx + (tw - textRenderer.getWidth(tabs.get(i).label())) / 2;
            int textY = y + (TAB_BAR_HEIGHT - 8) / 2;
            ProceduralRenderer.drawText(ctx, textRenderer, tabs.get(i).label(), textX, textY,
                    active ? accentColor : tabTextColor);

            // Active underline
            if (active) {
                ctx.fill(tx, y + TAB_BAR_HEIGHT - UNDERLINE_H,
                        tx + tw, y + TAB_BAR_HEIGHT, accentColor);
            }

            // Divider between tabs
            if (i < tabs.size() - 1) {
                ctx.fill(tx + tw - 1, y + 4, tx + tw, y + TAB_BAR_HEIGHT - 4,
                        ProceduralRenderer.COL_BORDER);
            }

            tx += tw;
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        // Check if click is in tab bar
        if (button == 0 && mouseY >= y && mouseY < y + TAB_BAR_HEIGHT) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            int tx = x;
            for (int i = 0; i < tabs.size(); i++) {
                int tw = textRenderer.getWidth(tabs.get(i).label()) + TAB_PAD_H * 2;
                if (mouseX >= tx && mouseX < tx + tw) {
                    if (i != activeIndex) {
                        setActiveTab(i);
                        UISoundManager.playClick();
                    }
                    return true;
                }
                tx += tw;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;

        // Tab key cycles
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
                prevTab();
            } else {
                nextTab();
            }
            return true;
        }

        // Ctrl+1-9 hotkeys
        if (numberHotkeys && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            int num = keyCode - GLFW.GLFW_KEY_1;
            if (num >= 0 && num < 9 && num < tabs.size()) {
                setActiveTab(num);
                UISoundManager.playClick();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    public TabbedPanel setOnTabChange(IntConsumer callback) {
        this.onTabChange = callback;
        return this;
    }

    public TabbedPanel setNumberHotkeys(boolean enabled) {
        this.numberHotkeys = enabled;
        return this;
    }

    public TabbedPanel setTabBarBackground(int color) {
        this.tabBarBg = color;
        return this;
    }

    public TabbedPanel setTabNormalColor(int color) {
        this.tabNormalColor = color;
        return this;
    }

    public TabbedPanel setTabActiveColor(int color) {
        this.tabActiveColor = color;
        return this;
    }

    public TabbedPanel setTabTextColor(int color) {
        this.tabTextColor = color;
        return this;
    }

    public TabbedPanel setAccentColor(int color) {
        this.accentColor = color;
        return this;
    }
}
