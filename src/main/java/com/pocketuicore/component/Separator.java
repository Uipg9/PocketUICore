package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.render.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * A simple horizontal or vertical line separator.
 * <p>
 * Use in panels between groups of components to visually divide
 * sections. Defaults to a horizontal line 1 px thick.
 * <p>
 * Respects the active {@link Theme} for its default colour.
 *
 * @since 1.12.0
 */
public class Separator extends UIComponent {

    public enum Orientation { HORIZONTAL, VERTICAL }

    private Orientation orientation = Orientation.HORIZONTAL;
    private int color = -1; // -1 = use theme
    private boolean useThemeColor = true;
    private int thickness = 1;

    /**
     * Create a horizontal separator spanning the given width.
     */
    public Separator(int x, int y, int length) {
        super(x, y, length, 1);
    }

    /**
     * Create a separator with explicit orientation.
     */
    public Separator(int x, int y, int length, Orientation orientation) {
        super(x, y,
              orientation == Orientation.HORIZONTAL ? length : 1,
              orientation == Orientation.HORIZONTAL ? 1 : length);
        this.orientation = orientation;
    }

    /**
     * Create a horizontal separator with default length (stretches when
     * placed inside a layout panel).
     *
     * @since 1.14.0
     */
    public Separator() {
        this(0, 0, 1);
    }

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w, h;
        if (orientation == Orientation.HORIZONTAL) {
            w = width;
            h = thickness;
        } else {
            w = thickness;
            h = height;
        }
        int renderColor = useThemeColor ? Theme.current().border() : color;
        ProceduralRenderer.fillRect(ctx, x, y, w, h, renderColor);
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public Separator setColor(int c)             { this.color = c; this.useThemeColor = false; return this; }
    public int getColor()                        { return color; }
    public Separator setThickness(int t)         { this.thickness = Math.max(1, t); return this; }
    public int getThickness()                    { return thickness; }
    public Separator setOrientation(Orientation o) { this.orientation = o; return this; }
    public Orientation getOrientation()          { return orientation; }
}
