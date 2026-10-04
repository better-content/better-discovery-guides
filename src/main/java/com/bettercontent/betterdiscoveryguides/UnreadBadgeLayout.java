package com.bettercontent.betterdiscoveryguides;

/** Geometry for the text-only in-world unread Threads prompt. */
record UnreadBadgeLayout(int x, int y, int width, int height, int keyX, int keyWidth) {
    static int visibleRows(int screenHeight) {
        return Math.min(5, Math.max(1, (screenHeight - 38) / 12));
    }

    static UnreadBadgeLayout calculate(int screenWidth, int screenHeight, int keyWidth, int lines) {
        int width = Math.min(180, Math.max(1, screenWidth - 12));
        int height = 20 + Math.max(1, lines) * 12 + 4;
        int x = Math.max(0, screenWidth - width - 6);
        int y = Math.max(0, Math.min(screenHeight - height - 6, (screenHeight - height) / 2));
        int actualKeyWidth = Math.min(keyWidth, Math.max(1, width - 12));
        return new UnreadBadgeLayout(x, y, width, height, x + width - actualKeyWidth - 5, actualKeyWidth);
    }

    int right() { return x + width; }

    int bottom() { return y + height; }
}
