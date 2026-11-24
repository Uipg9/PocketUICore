package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pocketuicore.render.RenderContext;
import com.pocketuicore.input.PocketKeys;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Component Input Component — Single-line editable Component field.
 * <p>
 * Supports cursor navigation, Component selection via keyboard, character
 * filtering, a placeholder string, and a submit callback.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     TextInputComponent input = new TextInputComponent(10, 10, 200, 20);
 *     input.setPlaceholder("Enter name...");
 *     input.setMaxLength(32);
 *     input.setOnSubmit(Component -> System.out.println("Submitted: " + Component));
 * }</pre>
 */
public class TextInputComponent extends UIComponent {

    // ── State ────────────────────────────────────────────────────────────
    private StringBuilder Component = new StringBuilder();
    private int cursorPos = 0;
    private int selectionStart = -1;
    private boolean focused = false;
    private int maxLength = 256;
    private String placeholder = "";

    // ── Appearance ───────────────────────────────────────────────────────
    private int backgroundColor  = ProceduralRenderer.COL_BG_PRIMARY;
    private int borderColor      = ProceduralRenderer.COL_BORDER;
    private int focusBorderColor = ProceduralRenderer.COL_ACCENT_TEAL;
    private int textColor        = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int placeholderColor = ProceduralRenderer.COL_TEXT_MUTED;
    private int cursorColor      = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int cornerRadius     = 4;
    private int selectionColor   = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_ACCENT_TEAL, 120);

    // ── Cursor blink ─────────────────────────────────────────────────────
    private long lastCursorToggle = 0;
    private boolean cursorVisible = true;
    private static final long CURSOR_BLINK_MS = 530;

    // ── Scroll offset for long Component ──────────────────────────────────────
    private int scrollOffset = 0;

    // ── Callbacks ────────────────────────────────────────────────────────
    private Consumer<String> onSubmit;
    private Consumer<String> onChanged;
    private Predicate<Character> charFilter;

    // ── Numeric mode ─────────────────────────────────────────────────────
    private boolean numericMode = false;
    private double numericMin = -Double.MAX_VALUE;
    private double numericMax = Double.MAX_VALUE;
    private boolean integerOnly = false;

    /** Built-in numeric character filter. */
    private static final Predicate<Character> NUMERIC_FILTER = c ->
            Character.isDigit(c) || c == '.' || c == '-';
    private static final Predicate<Character> INTEGER_FILTER = c ->
            Character.isDigit(c) || c == '-';

    // =====================================================================
    //  Construction
    // =====================================================================

    public TextInputComponent(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(RenderContext ctx, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        Font tr = client.font;

        // Background + border
        int border = focused ? focusBorderColor : borderColor;
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, backgroundColor);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, border);

        int pad = 4;
        int innerX = x + pad;
        int innerW = width - pad * 2;
        int textY  = y + (height - tr.lineHeight) / 2;

        // Enable scissor to clip Component within bounds
        ctx.enableScissor(innerX, y, x + width - pad, y + height);

        String displayText = Component.toString();

        if (displayText.isEmpty() && !focused) {
            // Placeholder
            ProceduralRenderer.drawText(ctx, tr, placeholder, innerX, textY, placeholderColor);
        } else {
            // Compute scroll offset so cursor is always visible
            int cursorPixel = tr.width(displayText.substring(0, cursorPos));
            if (cursorPixel - scrollOffset > innerW - 2) {
                scrollOffset = cursorPixel - innerW + 2;
            }
            if (cursorPixel - scrollOffset < 0) {
                scrollOffset = cursorPixel;
            }

            // Selection highlight
            if (selectionStart >= 0 && selectionStart != cursorPos) {
                int selMin = Math.min(selectionStart, cursorPos);
                int selMax = Math.max(selectionStart, cursorPos);
                int selX1 = innerX + tr.width(displayText.substring(0, selMin)) - scrollOffset;
                int selX2 = innerX + tr.width(displayText.substring(0, selMax)) - scrollOffset;
                ProceduralRenderer.fillRect(ctx, selX1, y + 2, selX2 - selX1, height - 4,
                        selectionColor);
            }

            // Component
            ProceduralRenderer.drawText(ctx, tr, displayText, innerX - scrollOffset, textY, textColor);

            // Cursor
            if (focused) {
                long now = System.currentTimeMillis();
                if (now - lastCursorToggle >= CURSOR_BLINK_MS) {
                    cursorVisible = !cursorVisible;
                    lastCursorToggle = now;
                }
                if (cursorVisible) {
                    int cursorX = innerX + cursorPixel - scrollOffset;
                    ProceduralRenderer.fillRect(ctx, cursorX, textY - 1, 1, tr.lineHeight + 2, cursorColor);
                }
            }
        }

        ctx.disableScissor();
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        if (isHovered(mouseX, mouseY)) {
            focused = true;
            cursorVisible = true;
            lastCursorToggle = System.currentTimeMillis();

            // Place cursor at MouseButtonEvent position
            Minecraft client = Minecraft.getInstance();
            Font tr = client.font;
            int pad = 4;
            int relativeX = (int) mouseX - (x + pad) + scrollOffset;
            String str = Component.toString();
            int best = 0;
            for (int i = 0; i <= str.length(); i++) {
                if (tr.width(str.substring(0, i)) <= relativeX) best = i;
            }
            cursorPos = best;
            selectionStart = -1;
            return true;
        } else {
            focused = false;
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled || !focused) return false;

        boolean shift = (modifiers & PocketKeys.MOD_SHIFT) != 0;
        boolean ctrl  = (modifiers & PocketKeys.MOD_CONTROL) != 0;

        switch (keyCode) {
            case PocketKeys.KEY_LEFT -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                if (ctrl) cursorPos = findWordBoundary(-1);
                else if (cursorPos > 0) cursorPos--;
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_RIGHT -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                if (ctrl) cursorPos = findWordBoundary(1);
                else if (cursorPos < Component.length()) cursorPos++;
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_HOME -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                cursorPos = 0;
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_END -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                cursorPos = Component.length();
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_BACKSPACE -> {
                if (deleteSelection()) { resetBlink(); return true; }
                if (cursorPos > 0) {
                    if (ctrl) {
                        int target = findWordBoundary(-1);
                        Component.delete(target, cursorPos);
                        cursorPos = target;
                    } else {
                        Component.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                    fireChanged();
                }
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_DELETE -> {
                if (deleteSelection()) { resetBlink(); return true; }
                if (cursorPos < Component.length()) {
                    if (ctrl) {
                        int target = findWordBoundary(1);
                        Component.delete(cursorPos, target);
                    } else {
                        Component.deleteCharAt(cursorPos);
                    }
                    fireChanged();
                }
                resetBlink();
                return true;
            }
            case PocketKeys.KEY_ENTER, PocketKeys.KEY_KP_ENTER -> {
                if (onSubmit != null) onSubmit.accept(Component.toString());
                return true;
            }
            case PocketKeys.KEY_A -> {
                if (ctrl) {
                    selectionStart = 0;
                    cursorPos = Component.length();
                    return true;
                }
            }
            case PocketKeys.KEY_C -> {
                if (ctrl) { copySelection(); return true; }
            }
            case PocketKeys.KEY_X -> {
                if (ctrl) { copySelection(); deleteSelection(); return true; }
            }
            case PocketKeys.KEY_V -> {
                if (ctrl) { pasteClipboard(); return true; }
            }
            case PocketKeys.KEY_ESCAPE -> {
                focused = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !enabled || !focused) return false;
        if (chr < 32) return false; // control chars

        // Numeric mode filter
        if (numericMode) {
            Predicate<Character> nf = integerOnly ? INTEGER_FILTER : NUMERIC_FILTER;
            if (!nf.test(chr)) return false;
        }
        if (charFilter != null && !charFilter.test(chr)) return false;

        deleteSelection();
        if (Component.length() < maxLength) {
            Component.insert(cursorPos, chr);
            cursorPos++;
            fireChanged();
        }
        resetBlink();
        return true;
    }

    // =====================================================================
    //  Clipboard + selection helpers
    // =====================================================================

    private boolean deleteSelection() {
        if (selectionStart < 0 || selectionStart == cursorPos) return false;
        int min = Math.min(selectionStart, cursorPos);
        int max = Math.max(selectionStart, cursorPos);
        Component.delete(min, max);
        cursorPos = min;
        selectionStart = -1;
        fireChanged();
        return true;
    }

    private String getSelectedText() {
        if (selectionStart < 0 || selectionStart == cursorPos) return "";
        int min = Math.min(selectionStart, cursorPos);
        int max = Math.max(selectionStart, cursorPos);
        return Component.substring(min, max);
    }

    private void copySelection() {
        String sel = getSelectedText();
        if (!sel.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(sel);
        }
    }

    private void pasteClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        deleteSelection();
        // Filter pasted Component
        StringBuilder filtered = new StringBuilder();
        for (char c : clip.toCharArray()) {
            if (c >= 32 && (charFilter == null || charFilter.test(c))) {
                filtered.append(c);
            }
        }
        int room = maxLength - Component.length();
        String toInsert = filtered.substring(0, Math.min(filtered.length(), room));
        Component.insert(cursorPos, toInsert);
        cursorPos += toInsert.length();
        fireChanged();
    }

    private int findWordBoundary(int direction) {
        int pos = cursorPos;
        if (direction < 0) {
            if (pos <= 0) return 0;
            pos--;
            while (pos > 0 && Character.isWhitespace(Component.charAt(pos))) pos--;
            while (pos > 0 && !Character.isWhitespace(Component.charAt(pos - 1))) pos--;
        } else {
            if (pos >= Component.length()) return Component.length();
            while (pos < Component.length() && !Character.isWhitespace(Component.charAt(pos))) pos++;
            while (pos < Component.length() && Character.isWhitespace(Component.charAt(pos))) pos++;
        }
        return pos;
    }

    private void resetBlink() {
        cursorVisible = true;
        lastCursorToggle = System.currentTimeMillis();
    }

    private void fireChanged() {
        if (onChanged != null) onChanged.accept(Component.toString());
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public String getText()                     { return Component.toString(); }
    public TextInputComponent setText(String t) {
        Component = new StringBuilder(t != null ? t : "");
        cursorPos = Component.length();
        selectionStart = -1;
        scrollOffset = 0;
        fireChanged();
        return this;
    }
    public boolean isFocused()                  { return focused; }
    public TextInputComponent    setFocused(boolean f)        { this.focused = f; if (f) resetBlink(); return this; }
    public int     getMaxLength()               { return maxLength; }
    public TextInputComponent    setMaxLength(int m)          { this.maxLength = Math.max(1, m); return this; }
    public String  getPlaceholder()             { return placeholder; }
    public TextInputComponent    setPlaceholder(String p)     { this.placeholder = p != null ? p : ""; return this; }
    public TextInputComponent    setOnSubmit(Consumer<String> cb) { this.onSubmit = cb; return this; }
    public TextInputComponent    setOnChanged(Consumer<String> cb) { this.onChanged = cb; return this; }
    public TextInputComponent    setCharFilter(Predicate<Character> f) { this.charFilter = f; return this; }
    public int     getCursorPos()               { return cursorPos; }
    public TextInputComponent    setCursorPos(int pos)        { this.cursorPos = Math.clamp(pos, 0, Component.length()); return this; }

    // ── Appearance setters ───────────────────────────────────────────────
    public TextInputComponent setBackgroundColor(int c)       { this.backgroundColor = c; return this; }
    public TextInputComponent setBorderColor(int c)           { this.borderColor = c; return this; }
    public TextInputComponent setFocusBorderColor(int c)      { this.focusBorderColor = c; return this; }
    public TextInputComponent setTextColor(int c)             { this.textColor = c; return this; }
    public TextInputComponent setPlaceholderColor(int c)      { this.placeholderColor = c; return this; }
    public TextInputComponent setCursorColor(int c)           { this.cursorColor = c; return this; }
    public TextInputComponent setCornerRadius(int r)          { this.cornerRadius = r; return this; }
    public TextInputComponent setSelectionColor(int c)        { this.selectionColor = c; return this; }

    // ── Numeric mode ─────────────────────────────────────────────────────

    /**
     * Enable numeric-only input mode. Only digits, minus sign, and
     * decimal point (if not integer-only) are accepted.
     *
     * @param numeric    whether numeric mode is enabled
     * @param integerOnly if true, only integers (no decimal point)
     * @return this component for chaining
     * @since 1.15.0
     */
    public TextInputComponent setNumericMode(boolean numeric, boolean integerOnly) {
        this.numericMode = numeric;
        this.integerOnly = integerOnly;
        return this;
    }

    /**
     * Set the numeric range for validation.
     * When numeric mode is active, {@link #getNumericValue()} clamps to this range.
     *
     * @param min minimum value
     * @param max maximum value
     * @return this component for chaining
     * @since 1.15.0
     */
    public TextInputComponent setNumericRange(double min, double max) {
        this.numericMin = min;
        this.numericMax = max;
        return this;
    }

    /**
     * Parse the Component content as a numeric value, clamped to the configured range.
     *
     * @return the numeric value, or 0 if parsing fails
     * @since 1.15.0
     */
    public double getNumericValue() {
        try {
            double val = Double.parseDouble(Component.toString());
            return Math.max(numericMin, Math.min(numericMax, val));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Set the numeric value (sets the Component to the string representation).
     *
     * @param value the value to set
     * @return this component for chaining
     * @since 1.15.0
     */
    public TextInputComponent setNumericValue(double value) {
        value = Math.max(numericMin, Math.min(numericMax, value));
        if (integerOnly) {
            setText(String.valueOf((int) value));
        } else {
            setText(String.valueOf(value));
        }
        return this;
    }

    public boolean isNumericMode()  { return numericMode; }
    public boolean isIntegerOnly()  { return integerOnly; }
    public double getNumericMin()   { return numericMin; }
    public double getNumericMax()   { return numericMax; }

    // ── Focus integration ────────────────────────────────────────────────

    /**
     * Programmatically focus this Component input.
     * Sets the internal focus flag and registers with the FocusManager.
     *
     * @return this component for chaining
     * @since 1.15.0
     */
    @Override
    public TextInputComponent requestFocus() {
        this.focused = true;
        resetBlink();
        FocusManager fm = FocusManager.getInstance();
        fm.register(this);
        fm.focus(this);
        return this;
    }
}
