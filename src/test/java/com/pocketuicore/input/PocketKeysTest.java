package com.pocketuicore.input;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PocketKeysTest {

    @Test
    void navigationAndEditingKeysAreDistinct() {
        int[] keys = {
            PocketKeys.KEY_UP, PocketKeys.KEY_DOWN, PocketKeys.KEY_LEFT, PocketKeys.KEY_RIGHT,
            PocketKeys.KEY_ENTER, PocketKeys.KEY_KP_ENTER, PocketKeys.KEY_TAB, PocketKeys.KEY_ESCAPE,
            PocketKeys.KEY_SPACE, PocketKeys.KEY_HOME, PocketKeys.KEY_END, PocketKeys.KEY_DELETE,
            PocketKeys.KEY_BACKSPACE, PocketKeys.KEY_PAGE_UP, PocketKeys.KEY_PAGE_DOWN,
            PocketKeys.KEY_LEFT_SHIFT, PocketKeys.KEY_RIGHT_SHIFT,
            PocketKeys.KEY_LEFT_CONTROL, PocketKeys.KEY_RIGHT_CONTROL
        };
        assertAllDistinct(keys);
    }

    @Test
    void letterDigitAndFunctionKeysAreDistinct() {
        int[] keys = {
            PocketKeys.KEY_A, PocketKeys.KEY_C, PocketKeys.KEY_E, PocketKeys.KEY_H, PocketKeys.KEY_V,
            PocketKeys.KEY_X, PocketKeys.KEY_Z,
            PocketKeys.KEY_0, PocketKeys.KEY_1, PocketKeys.KEY_9,
            PocketKeys.KEY_F1, PocketKeys.KEY_F2, PocketKeys.KEY_F3, PocketKeys.KEY_F4,
            PocketKeys.KEY_F5, PocketKeys.KEY_F6, PocketKeys.KEY_F7, PocketKeys.KEY_F8,
            PocketKeys.KEY_F9, PocketKeys.KEY_F10, PocketKeys.KEY_F11, PocketKeys.KEY_F12
        };
        assertAllDistinct(keys);
    }

    @Test
    void functionKeysAreConsecutive() {
        assertEquals(PocketKeys.KEY_F1 + 11, PocketKeys.KEY_F12);
        assertEquals(PocketKeys.KEY_F1 + 4, PocketKeys.KEY_F5);
    }

    @Test
    void digitsAreOrderedForTabHotkeys() {
        // TabbedPanel maps KEY_1..KEY_9 to tab indices 0..8.
        assertEquals(PocketKeys.KEY_1 + 8, PocketKeys.KEY_9);
        assertEquals(0, PocketKeys.digitIndex(PocketKeys.KEY_1));
        assertEquals(8, PocketKeys.digitIndex(PocketKeys.KEY_9));
        assertEquals(-1, PocketKeys.digitIndex(PocketKeys.KEY_0));
        assertEquals(-1, PocketKeys.digitIndex(PocketKeys.KEY_A));
    }

    @Test
    void enterAndConfirmHelpers() {
        assertTrue(PocketKeys.isEnter(PocketKeys.KEY_ENTER));
        assertTrue(PocketKeys.isEnter(PocketKeys.KEY_KP_ENTER));
        assertFalse(PocketKeys.isEnter(PocketKeys.KEY_SPACE));
    }

    @Test
    void mouseButtonsAreDistinctAndLeftIsRecognised() {
        assertAllDistinct(new int[] {PocketKeys.MOUSE_LEFT, PocketKeys.MOUSE_RIGHT, PocketKeys.MOUSE_MIDDLE});
        assertTrue(PocketKeys.isLeftClick(PocketKeys.MOUSE_LEFT));
        assertFalse(PocketKeys.isLeftClick(PocketKeys.MOUSE_RIGHT));
        assertFalse(PocketKeys.isLeftClick(PocketKeys.MOUSE_MIDDLE));
    }

    @Test
    void modifierFlagsAreDistinctBitsOrMasks() {
        assertNotEquals(0, PocketKeys.MOD_SHIFT);
        assertNotEquals(0, PocketKeys.MOD_CONTROL);
        assertNotEquals(0, PocketKeys.MOD_ALT);
        assertEquals(0, PocketKeys.MOD_SHIFT & PocketKeys.MOD_CONTROL);
        assertEquals(0, PocketKeys.MOD_SHIFT & PocketKeys.MOD_ALT);
        assertEquals(0, PocketKeys.MOD_CONTROL & PocketKeys.MOD_ALT);
    }

    @Test
    void modifierHelpersDetectEachFlagIndependently() {
        assertFalse(PocketKeys.hasShift(0));
        assertFalse(PocketKeys.hasControl(0));
        assertFalse(PocketKeys.hasAlt(0));

        assertTrue(PocketKeys.hasShift(PocketKeys.MOD_SHIFT));
        assertFalse(PocketKeys.hasControl(PocketKeys.MOD_SHIFT));
        assertFalse(PocketKeys.hasAlt(PocketKeys.MOD_SHIFT));

        int combo = PocketKeys.MOD_CONTROL | PocketKeys.MOD_ALT;
        assertTrue(PocketKeys.hasControl(combo));
        assertTrue(PocketKeys.hasAlt(combo));
        assertFalse(PocketKeys.hasShift(combo));
    }

    @Test
    void modifierHelpersAcceptEitherSideBit() {
        // SDL reports left/right modifiers as separate bits; the aggregate masks cover both.
        // Whatever the platform sets, at least one bit of the mask must trigger the helper.
        for (int bit = 0; bit < 32; bit++) {
            int b = 1 << bit;
            assertEquals((b & PocketKeys.MOD_SHIFT) != 0, PocketKeys.hasShift(b));
            assertEquals((b & PocketKeys.MOD_CONTROL) != 0, PocketKeys.hasControl(b));
            assertEquals((b & PocketKeys.MOD_ALT) != 0, PocketKeys.hasAlt(b));
        }
    }

    @Test
    void allPublicIntKeyConstantsAreUniqueWithinTheirFamily() throws Exception {
        Map<Integer, String> seen = new HashMap<>();
        for (Field f : PocketKeys.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) continue;
            if (!f.getName().startsWith("KEY_")) continue;
            int v = f.getInt(null);
            String prev = seen.put(v, f.getName());
            assertNull(prev, f.getName() + " collides with " + prev);
        }
        assertTrue(seen.size() >= 40);
    }

    @Test
    void alphanumericCharMapsLettersAndDigits() {
        assertEquals('A', PocketKeys.alphanumericChar(PocketKeys.KEY_A));
        assertEquals('Z', PocketKeys.alphanumericChar(PocketKeys.KEY_Z));
        assertEquals('H', PocketKeys.alphanumericChar(PocketKeys.KEY_H));
        assertEquals('1', PocketKeys.alphanumericChar(PocketKeys.KEY_1));
        assertEquals('9', PocketKeys.alphanumericChar(PocketKeys.KEY_9));
        assertEquals('0', PocketKeys.alphanumericChar(PocketKeys.KEY_0));
        assertEquals((char) 0, PocketKeys.alphanumericChar(PocketKeys.KEY_SPACE));
        assertEquals((char) 0, PocketKeys.alphanumericChar(PocketKeys.KEY_F1));
    }

    @Test
    void isDigitHandlesSdlOrdering() {
        assertTrue(PocketKeys.isDigit(PocketKeys.KEY_0));
        assertTrue(PocketKeys.isDigit(PocketKeys.KEY_9));
        assertFalse(PocketKeys.isDigit(PocketKeys.KEY_A));
    }

    private static void assertAllDistinct(int[] values) {
        for (int i = 0; i < values.length; i++)
            for (int j = i + 1; j < values.length; j++)
                assertNotEquals(values[i], values[j], "constants at index " + i + " and " + j + " collide");
    }
}
