package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Paginated container — multi-page component with page indicator dots,
 * left/right arrow buttons, arrow-key navigation, optional slide
 * animation, and a page-change callback.
 * <p>
 * Each page is a {@link UIComponent} that is shown/hidden as the user
 * navigates.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     PaginatedContainer pages = new PaginatedContainer(10, 10, 300, 200);
 *     pages.addPage(overviewPanel);
 *     pages.addPage(detailsPanel);
 *     pages.addPage(settingsPanel);
 *     pages.setOnPageChange(idx -> refreshData(idx));
 * }</pre>
 */
public final class PaginatedContainer extends UIComponent {

    // ── Layout constants ─────────────────────────────────────────────────
    private static final int DOT_RADIUS    = 3;
    private static final int DOT_GAP       = 8;
    private static final int DOT_AREA_H    = 16;
    private static final int ARROW_SIZE    = 16;
    private static final int ARROW_PAD     = 4;

    // ── Pages ────────────────────────────────────────────────────────────
    private final List<UIComponent> pages = new ArrayList<>();
    private int currentPage = 0;
    private IntConsumer onPageChange;

    // ── Slide animation ──────────────────────────────────────────────────
    private boolean animateSlide = true;
    private long slideStartMs;
    private long slideDurationMs = 250;
    private int slideFromX;
    private int slideDirection; // -1 left, +1 right
    private boolean sliding = false;

    // ── Appearance ───────────────────────────────────────────────────────
    private int dotActiveColor   = ProceduralRenderer.COL_ACCENT;
    private int dotInactiveColor = ProceduralRenderer.COL_TEXT_MUTED;
    private int arrowColor       = ProceduralRenderer.COL_TEXT_MUTED;
    private int arrowHoverColor  = ProceduralRenderer.COL_TEXT_PRIMARY;
    private boolean showDots   = true;
    private boolean showArrows = true;

    // ── Arrow hover state ────────────────────────────────────────────────
    private boolean leftArrowHovered;
    private boolean rightArrowHovered;

    // =====================================================================
    //  Constructor
    // =====================================================================

    public PaginatedContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // =====================================================================
    //  Page management
    // =====================================================================

    /**
     * Add a page. Its bounds are set to fill the content area
     * (above the dot indicator).
     */
    public void addPage(UIComponent page) {
        int contentH = height - (showDots ? DOT_AREA_H : 0);
        page.setPosition(x, y);
        page.setBounds(x, y, width, contentH);
        pages.add(page);
        if (pages.size() == 1) {
            addChild(page);
        }
    }

    /** Remove a page by index. */
    public void removePage(int index) {
        if (index < 0 || index >= pages.size()) return;
        UIComponent removed = pages.remove(index);
        removeChild(removed);
        if (currentPage >= pages.size()) {
            setPage(Math.max(0, pages.size() - 1), false);
        } else if (currentPage == index) {
            setPage(currentPage, false);
        }
    }

    /** @return number of pages. */
    public int getPageCount() { return pages.size(); }

    /** @return the current page index. */
    public int getCurrentPage() { return currentPage; }

    /** @return the current page component, or {@code null}. */
    public UIComponent getCurrentPageComponent() {
        return (currentPage >= 0 && currentPage < pages.size())
                ? pages.get(currentPage) : null;
    }

    // =====================================================================
    //  Navigation
    // =====================================================================

    /** Go to a specific page. */
    public void setPage(int index, boolean animate) {
        if (index < 0 || index >= pages.size() || index == currentPage) return;

        int direction = (index > currentPage) ? 1 : -1;

        clearChildren();
        currentPage = index;
        UIComponent page = pages.get(currentPage);
        int contentH = height - (showDots ? DOT_AREA_H : 0);
        page.setPosition(x, y);
        page.setBounds(x, y, width, contentH);
        addChild(page);

        if (animate && animateSlide) {
            sliding = true;
            slideStartMs = System.currentTimeMillis();
            slideDirection = direction;
            slideFromX = x + direction * width;
        }

        if (onPageChange != null) onPageChange.accept(currentPage);
    }

    /** Go to the next page (wraps). */
    public void nextPage() {
        if (pages.isEmpty()) return;
        setPage((currentPage + 1) % pages.size(), animateSlide);
        UISoundManager.playClick();
    }

    /** Go to the previous page (wraps). */
    public void prevPage() {
        if (pages.isEmpty()) return;
        setPage((currentPage - 1 + pages.size()) % pages.size(), animateSlide);
        UISoundManager.playClick();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        if (pages.isEmpty()) return;

        // ── Slide animation offset ───────────────────────────────────────
        if (sliding) {
            long elapsed = System.currentTimeMillis() - slideStartMs;
            if (elapsed >= slideDurationMs) {
                sliding = false;
            } else {
                // Ease-out
                float t = (float) elapsed / slideDurationMs;
                float eased = 1f - (1f - t) * (1f - t);
                int offset = (int) ((1f - eased) * slideDirection * width);
                // The current page content will be shifted — we handle
                // this via the child's position temporarily
                UIComponent page = pages.get(currentPage);
                page.setPosition(x - offset * slideDirection, y);
            }
        }

        // ── Page dots ────────────────────────────────────────────────────
        if (showDots && pages.size() > 1) {
            int dotsY = y + height - DOT_AREA_H + DOT_AREA_H / 2;
            int totalDotsW = pages.size() * (DOT_RADIUS * 2) + (pages.size() - 1) * DOT_GAP;
            int dotsX = x + (width - totalDotsW) / 2;

            for (int i = 0; i < pages.size(); i++) {
                int cx = dotsX + i * (DOT_RADIUS * 2 + DOT_GAP) + DOT_RADIUS;
                int color = (i == currentPage) ? dotActiveColor : dotInactiveColor;
                // Draw filled circle (approximated with small rounded rect)
                ProceduralRenderer.fillRoundedRect(ctx,
                        cx - DOT_RADIUS, dotsY - DOT_RADIUS,
                        DOT_RADIUS * 2, DOT_RADIUS * 2,
                        DOT_RADIUS, color);
            }
        }

        // ── Arrow buttons ────────────────────────────────────────────────
        if (showArrows && pages.size() > 1) {
            int arrowY = y + (height - (showDots ? DOT_AREA_H : 0)) / 2 - ARROW_SIZE / 2;

            // Left arrow
            int lx = x + ARROW_PAD;
            leftArrowHovered = mouseX >= lx && mouseX <= lx + ARROW_SIZE
                    && mouseY >= arrowY && mouseY <= arrowY + ARROW_SIZE;
            int lColor = leftArrowHovered ? arrowHoverColor : arrowColor;
            drawArrowLeft(ctx, lx, arrowY, ARROW_SIZE, lColor);

            // Right arrow
            int rx = x + width - ARROW_SIZE - ARROW_PAD;
            rightArrowHovered = mouseX >= rx && mouseX <= rx + ARROW_SIZE
                    && mouseY >= arrowY && mouseY <= arrowY + ARROW_SIZE;
            int rColor = rightArrowHovered ? arrowHoverColor : arrowColor;
            drawArrowRight(ctx, rx, arrowY, ARROW_SIZE, rColor);
        }
    }

    // ── Arrow drawing helpers ────────────────────────────────────────────

    private void drawArrowLeft(DrawContext ctx, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 3;
        // Simple < shape using thin rects
        for (int i = 0; i < half; i++) {
            ctx.fill(cx - half + i, cy - i, cx - half + i + 1, cy + i + 1, color);
        }
    }

    private void drawArrowRight(DrawContext ctx, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 3;
        for (int i = 0; i < half; i++) {
            ctx.fill(cx + half - i, cy - i, cx + half - i + 1, cy + i + 1, color);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        if (button == 0 && showArrows && pages.size() > 1) {
            if (leftArrowHovered)  { prevPage(); return true; }
            if (rightArrowHovered) { nextPage(); return true; }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            prevPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            nextPage();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    public PaginatedContainer setOnPageChange(IntConsumer callback) {
        this.onPageChange = callback;
        return this;
    }

    public PaginatedContainer setAnimateSlide(boolean animate) {
        this.animateSlide = animate;
        return this;
    }

    public PaginatedContainer setSlideDuration(long durationMs) {
        this.slideDurationMs = durationMs;
        return this;
    }

    public PaginatedContainer setShowDots(boolean show) {
        this.showDots = show;
        return this;
    }

    public PaginatedContainer setShowArrows(boolean show) {
        this.showArrows = show;
        return this;
    }

    public PaginatedContainer setDotActiveColor(int color) {
        this.dotActiveColor = color;
        return this;
    }

    public PaginatedContainer setDotInactiveColor(int color) {
        this.dotInactiveColor = color;
        return this;
    }

    public PaginatedContainer setArrowColor(int color) {
        this.arrowColor = color;
        return this;
    }

    public PaginatedContainer setArrowHoverColor(int color) {
        this.arrowHoverColor = color;
        return this;
    }
}
