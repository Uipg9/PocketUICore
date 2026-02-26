package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Interactive grid — generic 2D grid with per-cell rendering callback,
 * click detection, selection state (single or multi-select),
 * keyboard shortcuts, and hover highlight.
 * <p>
 * Unlike the existing auto-layout {@code GridPanel} (which just arranges
 * child components), this grid provides <b>cell-level interaction</b>
 * with row/column indices and typed data.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     InteractiveGrid<Item> grid = new InteractiveGrid<>(10, 10, 200, 200, 4, 4);
 *     grid.setCellRenderer((ctx, item, cx, cy, cw, ch, hov, sel) ->
 *          ProceduralRenderer.fillRoundedRect(ctx, cx+1, cy+1, cw-2, ch-2, 2,
 *              sel ? ProceduralRenderer.COL_ACCENT : ProceduralRenderer.COL_BG_SURFACE));
 *     grid.setData(itemGrid); // List of lists, or flat list
 *     grid.setOnCellClick((row, col) -> selectCell(row, col));
 * }</pre>
 *
 * @param <T> the cell data type
 */
public final class InteractiveGrid<T> extends UIComponent {

    // ── Cell renderer ────────────────────────────────────────────────────
    @FunctionalInterface
    public interface CellRenderer<T> {
        /**
         * @param ctx      draw context
         * @param item     cell data (may be null for empty cells)
         * @param x        cell left x
         * @param y        cell top y
         * @param width    cell width
         * @param height   cell height
         * @param hovered  true if mouse is over this cell
         * @param selected true if this cell is selected
         */
        void render(DrawContext ctx, T item,
                    int x, int y, int width, int height,
                    boolean hovered, boolean selected);
    }

    // ── Configuration ────────────────────────────────────────────────────
    private int columns;
    private int rows;
    private final List<T> data = new ArrayList<>();
    private boolean multiSelect = false;
    private int cellGap = 1; // pixels between cells

    // ── State ────────────────────────────────────────────────────────────
    private int hoveredRow = -1, hoveredCol = -1;
    private int selectedRow = -1, selectedCol = -1;
    private final Set<Long> selectedSet = new HashSet<>();  // for multi-select: (row << 32 | col)

    // ── Callbacks ────────────────────────────────────────────────────────
    private BiConsumer<Integer, Integer> onCellClick; // (row, col)
    private CellRenderer<T> cellRenderer;

    // ── Colours ──────────────────────────────────────────────────────────
    private int bgColor     = ProceduralRenderer.COL_BG_PRIMARY;
    private int hoverColor  = ProceduralRenderer.COL_HOVER;
    private int selectColor = ProceduralRenderer.setAlpha(ProceduralRenderer.COL_ACCENT, 0.3f);
    private int gridLineColor = ProceduralRenderer.COL_BORDER;

    // =====================================================================
    //  Constructor
    // =====================================================================

    /**
     * @param x       component x position
     * @param y       component y position
     * @param width   total grid width
     * @param height  total grid height
     * @param columns number of columns
     * @param rows    number of rows
     */
    public InteractiveGrid(int x, int y, int width, int height,
                           int columns, int rows) {
        super(x, y, width, height);
        this.columns = columns;
        this.rows    = rows;
    }

    // =====================================================================
    //  Data
    // =====================================================================

    /**
     * Set grid data as a flat list. Cells are filled left-to-right,
     * top-to-bottom. Excess cells will be {@code null}.
     */
    public void setData(List<T> flatData) {
        data.clear();
        data.addAll(flatData);
    }

    /** Get data at (row, col), or {@code null}. */
    public T getCell(int row, int col) {
        int idx = row * columns + col;
        return (idx >= 0 && idx < data.size()) ? data.get(idx) : null;
    }

    /** Set data at (row, col). Expands the internal list as needed. */
    public void setCell(int row, int col, T value) {
        int idx = row * columns + col;
        while (data.size() <= idx) data.add(null);
        data.set(idx, value);
    }

    /** Clear all data. */
    public void clearData() {
        data.clear();
        selectedSet.clear();
        selectedRow = -1;
        selectedCol = -1;
    }

    // =====================================================================
    //  Selection
    // =====================================================================

    /** @return selected row index or -1. */
    public int getSelectedRow() { return selectedRow; }
    /** @return selected column index or -1. */
    public int getSelectedCol() { return selectedCol; }

    /** @return data at the selected cell, or {@code null}. */
    public T getSelected() {
        return (selectedRow >= 0 && selectedCol >= 0)
                ? getCell(selectedRow, selectedCol) : null;
    }

    /** @return true if the cell at (row, col) is selected (multi-select). */
    public boolean isSelected(int row, int col) {
        if (multiSelect) return selectedSet.contains(packCell(row, col));
        return row == selectedRow && col == selectedCol;
    }

    /** Programmatic select. */
    public void select(int row, int col) {
        if (multiSelect) {
            long key = packCell(row, col);
            if (selectedSet.contains(key)) {
                selectedSet.remove(key);
            } else {
                selectedSet.add(key);
            }
        }
        selectedRow = row;
        selectedCol = col;
    }

    /** Clear selection. */
    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        selectedSet.clear();
    }

    private static long packCell(int row, int col) {
        return ((long) row << 32) | (col & 0xFFFFFFFFL);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        // ── Background ──────────────────────────────────────────────────
        ctx.fill(x, y, x + width, y + height, bgColor);

        int cellW = (width - cellGap * (columns - 1)) / columns;
        int cellH = (height - cellGap * (rows - 1)) / rows;

        // ── Determine hover ──────────────────────────────────────────────
        hoveredRow = -1;
        hoveredCol = -1;
        if (isHovered(mouseX, mouseY)) {
            int relX = mouseX - x;
            int relY = mouseY - y;
            int col = relX / (cellW + cellGap);
            int row = relY / (cellH + cellGap);
            if (col >= 0 && col < columns && row >= 0 && row < rows) {
                hoveredCol = col;
                hoveredRow = row;
            }
        }

        // ── Draw cells ───────────────────────────────────────────────────
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int cx = x + c * (cellW + cellGap);
                int cy = y + r * (cellH + cellGap);
                boolean hov = (r == hoveredRow && c == hoveredCol);
                boolean sel = isSelected(r, c);

                // Fill background
                if (sel) {
                    ctx.fill(cx, cy, cx + cellW, cy + cellH, selectColor);
                } else if (hov) {
                    ctx.fill(cx, cy, cx + cellW, cy + cellH, hoverColor);
                }

                // Custom renderer
                if (cellRenderer != null) {
                    T item = getCell(r, c);
                    cellRenderer.render(ctx, item, cx, cy, cellW, cellH, hov, sel);
                }
            }
        }

        // ── Grid lines ───────────────────────────────────────────────────
        if (cellGap > 0) {
            for (int c = 1; c < columns; c++) {
                int lx = x + c * (cellW + cellGap) - cellGap;
                ctx.fill(lx, y, lx + cellGap, y + height, gridLineColor);
            }
            for (int r = 1; r < rows; r++) {
                int ly = y + r * (cellH + cellGap) - cellGap;
                ctx.fill(x, ly, x + width, ly + cellGap, gridLineColor);
            }
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && hoveredRow >= 0 && hoveredCol >= 0) {
            select(hoveredRow, hoveredCol);
            UISoundManager.playClick();
            if (onCellClick != null) onCellClick.accept(hoveredRow, hoveredCol);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;

        // Arrow key navigation
        boolean moved = false;
        if (keyCode == GLFW.GLFW_KEY_UP && selectedRow > 0) {
            selectedRow--;
            moved = true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN && selectedRow < rows - 1) {
            selectedRow++;
            moved = true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT && selectedCol > 0) {
            selectedCol--;
            moved = true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && selectedCol < columns - 1) {
            selectedCol++;
            moved = true;
        }

        if (moved) {
            if (selectedRow < 0) selectedRow = 0;
            if (selectedCol < 0) selectedCol = 0;
            UISoundManager.playClick();
            return true;
        }

        // Enter to confirm selection
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (selectedRow >= 0 && selectedCol >= 0 && onCellClick != null) {
                onCellClick.accept(selectedRow, selectedCol);
                return true;
            }
        }

        // Number shortcuts 1-9 (flat index)
        int num = keyCode - GLFW.GLFW_KEY_1;
        if (num >= 0 && num < 9 && num < rows * columns) {
            int r = num / columns;
            int c = num % columns;
            select(r, c);
            UISoundManager.playClick();
            if (onCellClick != null) onCellClick.accept(r, c);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    public InteractiveGrid<T> setCellRenderer(CellRenderer<T> renderer) {
        this.cellRenderer = renderer;
        return this;
    }

    public InteractiveGrid<T> setOnCellClick(BiConsumer<Integer, Integer> callback) {
        this.onCellClick = callback;
        return this;
    }

    public InteractiveGrid<T> setMultiSelect(boolean multi) {
        this.multiSelect = multi;
        return this;
    }

    public InteractiveGrid<T> setCellGap(int gap) {
        this.cellGap = gap;
        return this;
    }

    public InteractiveGrid<T> setGridDimensions(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        return this;
    }

    public InteractiveGrid<T> setBackgroundColor(int color) {
        this.bgColor = color;
        return this;
    }

    public InteractiveGrid<T> setHoverColor(int color) {
        this.hoverColor = color;
        return this;
    }

    public InteractiveGrid<T> setSelectColor(int color) {
        this.selectColor = color;
        return this;
    }

    public InteractiveGrid<T> setGridLineColor(int color) {
        this.gridLineColor = color;
        return this;
    }

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
}
