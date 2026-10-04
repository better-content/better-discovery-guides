package com.bettercontent.betterdiscoveryguides;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnreadBadgeLayoutTest {
    @Test
    void showsFiveRowsOnSupportedHeights() {
        assertEquals(5, UnreadBadgeLayout.visibleRows(160));
        assertEquals(5, UnreadBadgeLayout.visibleRows(480));
        assertEquals(2, UnreadBadgeLayout.visibleRows(64));
    }

    @Test
    void textPanelAndRemappedKeyStayInsideSupportedScreens() {
        for (var size : List.of(new int[]{160, 160}, new int[]{320, 240}, new int[]{427, 240}, new int[]{854, 480})) {
            for (int keyWidth : List.of(14, 54)) {
                var layout = UnreadBadgeLayout.calculate(size[0], size[1], keyWidth, 6);
                assertTrue(layout.x() >= 0);
                assertTrue(layout.y() >= 0);
                assertTrue(layout.right() <= size[0]);
                assertTrue(layout.bottom() <= size[1]);
                assertTrue(layout.keyX() >= layout.x());
                assertTrue(layout.keyX() + layout.keyWidth() <= layout.right());
            }
        }
    }
}
