package com.pocketuicore.render;

import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.UIComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Debug Overlay — renders component bounds, hierarchy, and focus state
 * over the current screen. Toggle with F3+P.
 * <p>
 * Call {@link #handleKeyPress(int, int)} from your screen's keyPressed
 * handler, and {@link #render(DrawContext, List, int, int)} after rendering
 * your components.
 * <p>
 * <b>Usage in a PocketScreen:</b>
 * <pre>{@code
 *     @Override
 *     public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
 *         DebugOverlay.handleKeyPress(keyCode, scanCode);
 *         return super.keyPressed(keyCode, scanCode, modifiers);
 *     }
 *
 *     @Override
 *     public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
 *         super.render(ctx, mouseX, mouseY, delta);
 *         DebugOverlay.render(ctx, rootComponents, mouseX, mouseY);
 *     }
 * }</pre>
 *
 * @since 1.12.0
 */
public final class DebugOverlay {

    private DebugOverlay() {}

    private static boolean enabled = false;
    private static boolean f3Held = false;

    /** @return whether the debug overlay is currently enabled. */
    public static boolean isEnabled() { return enabled; }

    /** Programmatically enable or disable the debug overlay. */
    public static void setEnabled(boolean on) { enabled = on; }

    /**
     * Handle F3+P toggle. Call from your screen's keyPressed handler.
     * F3 (keyCode 292) must be held, then P (keyCode 80) toggles the overlay.
     *
     * @param keyCode  GLFW key code
     * @param scanCode GLFW scan code (ignored)
     */
    public static void handleKeyPress(int keyCode, int scanCode) {
        if (keyCode == 292) { // F3
            f3Held = true;
        } else if (keyCode == 80 && f3Held) { // P while F3 held
            enabled = !enabled;
            f3Held = false;
        } else {
            f3Held = false;
        }
    }

    /**
     * Render the debug overlay for a list of root components.
     * Draws coloured borders around each component, labels with class name,
     * position, and size, and highlights the focused component.
     *
     * @param ctx        draw context
     * @param roots      root components of the screen
     * @param mouseX     current mouse X
     * @param mouseY     current mouse Y
     */
    public static void render(DrawContext ctx, List<? extends UIComponent> roots,
                               int mouseX, int mouseY) {
        if (!enabled || roots == null) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        FocusManager fm = FocusManager.getInstance();

        for (UIComponent root : roots) {
            renderComponent(ctx, tr, fm, root, 0);
        }

        // Show hovered component info at cursor
        for (UIComponent root : roots) {
            UIComponent hovered = findHovered(root, mouseX, mouseY);
            if (hovered != null) {
                String info = hovered.getClass().getSimpleName()
                        + " [" + hovered.getX() + "," + hovered.getY()
                        + " " + hovered.getWidth() + "x" + hovered.getHeight() + "]";
                int tw = tr.getWidth(info);
                int tooltipX = mouseX + 12;
                int tooltipY = mouseY - 12;
                ctx.fill(tooltipX - 2, tooltipY - 2, tooltipX + tw + 2, tooltipY + tr.fontHeight + 2, 0xCC000000);
                ProceduralRenderer.drawText(ctx, tr, info, tooltipX, tooltipY, 0xFFFFFF00);
                break;
            }
        }
    }

    private static void renderComponent(DrawContext ctx, TextRenderer tr,
                                         FocusManager fm, UIComponent comp, int depth) {
        if (!comp.isVisible()) return;

        int x = comp.getX();
        int y = comp.getY();
        int w = comp.getWidth();
        int h = comp.getHeight();

        // Colour based on depth (cycle through hues)
        int[] depthColors = {0x80FF0000, 0x8000FF00, 0x800000FF, 0x80FFFF00, 0x80FF00FF, 0x8000FFFF};
        int borderColor = depthColors[depth % depthColors.length];

        // Focused component gets a bright highlight
        if (fm.isFocused(comp)) {
            borderColor = 0xFFFFD700; // gold
            ctx.fill(x, y, x + w, y + h, 0x20FFD700);
        }

        ProceduralRenderer.drawBorder(ctx, x, y, w, h, borderColor);

        // Label (only for leaf / interactive components to avoid clutter)
        if (comp.getChildren().isEmpty() || fm.isFocused(comp)) {
            String label = comp.getClass().getSimpleName();
            if (!comp.isEnabled()) label += " [disabled]";
            ProceduralRenderer.drawText(ctx, tr, label, x + 1, y + 1,
                    comp.isEnabled() ? 0xFFCCCCCC : 0xFFFF6666);
        }

        // Recurse into children
        for (UIComponent child : comp.getChildren()) {
            renderComponent(ctx, tr, fm, child, depth + 1);
        }
    }

    private static UIComponent findHovered(UIComponent comp, int mouseX, int mouseY) {
        if (!comp.isVisible()) return null;
        // Check children first (innermost component wins)
        for (int i = comp.getChildren().size() - 1; i >= 0; i--) {
            UIComponent found = findHovered(comp.getChildren().get(i), mouseX, mouseY);
            if (found != null) return found;
        }
        if (mouseX >= comp.getX() && mouseX < comp.getX() + comp.getWidth()
                && mouseY >= comp.getY() && mouseY < comp.getY() + comp.getHeight()) {
            return comp;
        }
        return null;
    }
}
