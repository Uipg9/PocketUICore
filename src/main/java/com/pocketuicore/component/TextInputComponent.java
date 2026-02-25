package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Text Input Component — Single-line editable text field.
 * <p>
 * Supports cursor navigation, text selection via keyboard, character
 * filtering, a placeholder string, and a submit callback.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     TextInputComponent input = new TextInputComponent(10, 10, 200, 20);
 *     input.setPlaceholder("Enter name...");
 *     input.setMaxLength(32);
 *     input.setOnSubmit(text -> System.out.println("Submitted: " + text));
 * }</pre>
 */
public class TextInputComponent extends UIComponent {

    // ── State ────────────────────────────────────────────────────────────
    private StringBuilder text = new StringBuilder();
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

    // ── Cursor blink ─────────────────────────────────────────────────────
    private long lastCursorToggle = 0;
    private boolean cursorVisible = true;
    private static final long CURSOR_BLINK_MS = 530;

    // ── Scroll offset for long text ──────────────────────────────────────
    private int scrollOffset = 0;

    // ── Callbacks ────────────────────────────────────────────────────────
    private Consumer<String> onSubmit;
    private Consumer<String> onChanged;
    private Predicate<Character> charFilter;

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
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Background + border
        int border = focused ? focusBorderColor : borderColor;
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, backgroundColor);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, border);

        int pad = 4;
        int innerX = x + pad;
        int innerW = width - pad * 2;
        int textY  = y + (height - tr.fontHeight) / 2;

        // Enable scissor to clip text within bounds
        ctx.enableScissor(innerX, y, x + width - pad, y + height);

        String displayText = text.toString();

        if (displayText.isEmpty() && !focused) {
            // Placeholder
            ProceduralRenderer.drawText(ctx, tr, placeholder, innerX, textY, placeholderColor);
        } else {
            // Compute scroll offset so cursor is always visible
            int cursorPixel = tr.getWidth(displayText.substring(0, cursorPos));
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
                int selX1 = innerX + tr.getWidth(displayText.substring(0, selMin)) - scrollOffset;
                int selX2 = innerX + tr.getWidth(displayText.substring(0, selMax)) - scrollOffset;
                ProceduralRenderer.fillRect(ctx, selX1, y + 2, selX2 - selX1, height - 4,
                        ProceduralRenderer.withAlpha(ProceduralRenderer.COL_ACCENT_TEAL, 80));
            }

            // Text
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
                    ProceduralRenderer.fillRect(ctx, cursorX, textY - 1, 1, tr.fontHeight + 2, cursorColor);
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

            // Place cursor at click position
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = client.textRenderer;
            int pad = 4;
            int relativeX = (int) mouseX - (x + pad) + scrollOffset;
            String str = text.toString();
            int best = 0;
            for (int i = 0; i <= str.length(); i++) {
                if (tr.getWidth(str.substring(0, i)) <= relativeX) best = i;
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

        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl  = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                if (ctrl) cursorPos = findWordBoundary(-1);
                else if (cursorPos > 0) cursorPos--;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                if (ctrl) cursorPos = findWordBoundary(1);
                else if (cursorPos < text.length()) cursorPos++;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                cursorPos = 0;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                if (!shift) selectionStart = -1;
                cursorPos = text.length();
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) { resetBlink(); return true; }
                if (cursorPos > 0) {
                    if (ctrl) {
                        int target = findWordBoundary(-1);
                        text.delete(target, cursorPos);
                        cursorPos = target;
                    } else {
                        text.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                    fireChanged();
                }
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) { resetBlink(); return true; }
                if (cursorPos < text.length()) {
                    if (ctrl) {
                        int target = findWordBoundary(1);
                        text.delete(cursorPos, target);
                    } else {
                        text.deleteCharAt(cursorPos);
                    }
                    fireChanged();
                }
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (onSubmit != null) onSubmit.accept(text.toString());
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    selectionStart = 0;
                    cursorPos = text.length();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl) { copySelection(); return true; }
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl) { copySelection(); deleteSelection(); return true; }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) { pasteClipboard(); return true; }
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
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
        if (charFilter != null && !charFilter.test(chr)) return false;

        deleteSelection();
        if (text.length() < maxLength) {
            text.insert(cursorPos, chr);
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
        text.delete(min, max);
        cursorPos = min;
        selectionStart = -1;
        fireChanged();
        return true;
    }

    private String getSelectedText() {
        if (selectionStart < 0 || selectionStart == cursorPos) return "";
        int min = Math.min(selectionStart, cursorPos);
        int max = Math.max(selectionStart, cursorPos);
        return text.substring(min, max);
    }

    private void copySelection() {
        String sel = getSelectedText();
        if (!sel.isEmpty()) {
            MinecraftClient.getInstance().keyboard.setClipboard(sel);
        }
    }

    private void pasteClipboard() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        deleteSelection();
        // Filter pasted text
        StringBuilder filtered = new StringBuilder();
        for (char c : clip.toCharArray()) {
            if (c >= 32 && (charFilter == null || charFilter.test(c))) {
                filtered.append(c);
            }
        }
        int room = maxLength - text.length();
        String toInsert = filtered.substring(0, Math.min(filtered.length(), room));
        text.insert(cursorPos, toInsert);
        cursorPos += toInsert.length();
        fireChanged();
    }

    private int findWordBoundary(int direction) {
        int pos = cursorPos;
        if (direction < 0) {
            if (pos <= 0) return 0;
            pos--;
            while (pos > 0 && Character.isWhitespace(text.charAt(pos))) pos--;
            while (pos > 0 && !Character.isWhitespace(text.charAt(pos - 1))) pos--;
        } else {
            if (pos >= text.length()) return text.length();
            while (pos < text.length() && !Character.isWhitespace(text.charAt(pos))) pos++;
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }
        return pos;
    }

    private void resetBlink() {
        cursorVisible = true;
        lastCursorToggle = System.currentTimeMillis();
    }

    private void fireChanged() {
        if (onChanged != null) onChanged.accept(text.toString());
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public String getText()                     { return text.toString(); }
    public void   setText(String t) {
        text = new StringBuilder(t != null ? t : "");
        cursorPos = text.length();
        selectionStart = -1;
        scrollOffset = 0;
        fireChanged();
    }
    public boolean isFocused()                  { return focused; }
    public void    setFocused(boolean f)        { this.focused = f; if (f) resetBlink(); }
    public int     getMaxLength()               { return maxLength; }
    public void    setMaxLength(int m)          { this.maxLength = Math.max(1, m); }
    public String  getPlaceholder()             { return placeholder; }
    public void    setPlaceholder(String p)     { this.placeholder = p != null ? p : ""; }
    public void    setOnSubmit(Consumer<String> cb) { this.onSubmit = cb; }
    public void    setOnChanged(Consumer<String> cb) { this.onChanged = cb; }
    public void    setCharFilter(Predicate<Character> f) { this.charFilter = f; }
    public int     getCursorPos()               { return cursorPos; }
    public void    setCursorPos(int pos)        { this.cursorPos = Math.clamp(pos, 0, text.length()); }

    // ── Appearance setters ───────────────────────────────────────────────
    public void setBackgroundColor(int c)       { this.backgroundColor = c; }
    public void setBorderColor(int c)           { this.borderColor = c; }
    public void setFocusBorderColor(int c)      { this.focusBorderColor = c; }
    public void setTextColor(int c)             { this.textColor = c; }
    public void setPlaceholderColor(int c)      { this.placeholderColor = c; }
    public void setCursorColor(int c)           { this.cursorColor = c; }
    public void setCornerRadius(int r)          { this.cornerRadius = r; }
}
