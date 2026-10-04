package com.bettercontent.betterdiscoveryguides;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

/** Wrapped, scrollable copy. No sentence is discarded to make room for decoration. */
final class ReadingText {
    private record Line(FormattedCharSequence text, int y, int color) {}
    private final Font font;
    private final int width;
    private final boolean rightAligned;
    private final List<Line> lines = new ArrayList<>();
    private int height;

    ReadingText(Font font, int width) { this(font, width, false); }
    ReadingText(Font font, int width, boolean rightAligned) { this.font = font; this.width = Math.max(1, width); this.rightAligned = rightAligned; }
    void add(String text, int color) {
        for (var line : font.split(Component.literal(text), width)) {
            lines.add(new Line(line, height, color));
            height += 12;
        }
    }
    void gap() { height += 6; }
    int height() { return height; }
    int maximumScroll(int viewportHeight) { return maximumScroll(viewportHeight, 0); }
    int maximumScroll(int viewportHeight, int extraHeight) { return Math.max(0, height + extraHeight - viewportHeight); }
    void render(GuiGraphics graphics, int x, int y, int panelWidth, int viewportHeight, int scroll) {
        render(graphics, x, y, panelWidth, viewportHeight, scroll, 0);
    }
    void render(GuiGraphics graphics, int x, int y, int panelWidth, int viewportHeight, int scroll, int extraHeight) {
        int offset = Math.max(0, Math.min(scroll, maximumScroll(viewportHeight, extraHeight)));
        graphics.enableScissor(x, y, x + panelWidth, y + viewportHeight);
        for (var line : lines) {
            int lineY = y + line.y() - offset;
            if (lineY >= y && lineY + font.lineHeight <= y + viewportHeight)
                graphics.drawString(font, line.text(), rightAligned ? x + panelWidth - 4 - font.width(line.text()) : x, lineY, line.color(), false);
        }
        graphics.disableScissor();
        if (height + extraHeight > viewportHeight) {
            int contentHeight = height + extraHeight;
            int thumb = Math.max(8, viewportHeight * viewportHeight / contentHeight);
            int thumbY = y + offset * (viewportHeight - thumb) / maximumScroll(viewportHeight, extraHeight);
            graphics.fill(x + panelWidth - 3, y, x + panelWidth - 1, y + viewportHeight, 0xFF343C36);
            graphics.fill(x + panelWidth - 3, thumbY, x + panelWidth - 1, thumbY + thumb, 0xFFC6A15B);
        }
    }
}
