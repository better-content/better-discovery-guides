package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.betterdiscoveryguides.LearningSurfaces;
import com.bettercontent.betterdiscoveryguides.mixin.LevelLoadingScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(modid = LearningSurfaces.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThreadClient {
    static final int ARCHIVE_GOLD = 0xC6A15B;
    static final int ART_TEXTURE_WIDTH = 256;
    static final int ART_TEXTURE_HEIGHT = 384;
    public static final KeyMapping OPEN = new KeyMapping("key.better_discovery_guides.open_reader", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_M, "key.categories.better_discovery_guides");
    private static List<ThreadNetwork.Card> cards = List.of();

    private ThreadClient() {}

    /** Uses the live KeyMapping so remaps are reflected in every reader prompt. */
    static Component readerBinding() { return OPEN.getTranslatedKeyMessage(); }

    public static void receive(ThreadNetwork.Sync sync) {
        cards = sync.cards();
        if (sync.open()) Minecraft.getInstance().setScreen(new ThreadDeckScreen(cards));
        else if (Minecraft.getInstance().screen instanceof ThreadDeckScreen deck) deck.updateCards(cards);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (OPEN.consumeClick()) ThreadNetwork.request("open", "");
        while (OPEN.consumeClick()) {}
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        cards = List.of();
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen) {
            int x = event.getScreen().width - 80;
            int y = 8;
            event.addListener(Button.builder(Component.literal("Threads"), button -> ThreadNetwork.request("open", ""))
                .bounds(x, y, 72, 20).build());
            return;
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        renderUnread(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ (value >>> 16);
    }

    /** Matches the central tease notices: outlined text over the world, no panel behind it. */
    private static void renderUnread(GuiGraphics graphics, int screenWidth, int screenHeight) {
        var pending = UnreadThreadList.select(cards, UnreadBadgeLayout.visibleRows(screenHeight));
        if (pending.empty()) return;
        String binding = readerBinding().getString();
        int keyWidth = Math.min(54, Math.max(14, Minecraft.getInstance().font.width(binding) + 8));
        int lines = pending.visible().size() + (pending.olderCount() > 0 ? 1 : 0);
        var layout = UnreadBadgeLayout.calculate(screenWidth, screenHeight, keyWidth, lines);
        int textWidth = Math.max(1, layout.width() - 12);
        String key = fit(binding, layout.keyWidth());
        drawOutlined(graphics, key, layout.right()-Minecraft.getInstance().font.width(key), layout.y()+3, 0xFFC6A15B);
        drawTeaseLine(graphics, "Threads", layout.right()-Minecraft.getInstance().font.width(key)-5, layout.y()+3,
            Math.max(1, layout.keyX()-layout.x()-9), 0xFFF0E5CE);
        for (int i = 0; i < pending.visible().size(); i++) {
            var card = pending.visible().get(i);
            var definition = ThreadArt.BY_ID.get(card.id());
            String shortName = definition == null ? card.title() : definition.shortTitle();
            drawTeaseLine(graphics, shortName, layout.right(), layout.y()+21+i*12, textWidth, 0xFFE0D4BB);
        }
        if (pending.olderCount() > 0) drawTeaseLine(graphics,
            "+"+pending.olderCount()+" older", layout.right(),
            layout.y()+21+pending.visible().size()*12, textWidth, 0xFFC6A15B);
    }

    private static void drawTeaseLine(GuiGraphics graphics, String text, int right, int y, int maxWidth, int color) {
        String fitted = fit(text, maxWidth);
        drawOutlined(graphics, fitted, right - Minecraft.getInstance().font.width(fitted), y, color);
    }

    private static void drawOutlined(GuiGraphics graphics, String text, int x, int y, int color) {
        var font = Minecraft.getInstance().font;
        int outline = color & 0xFF000000;
        for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) {
            if (ox != 0 || oy != 0) graphics.drawString(font, text, x+ox, y+oy, outline, false);
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    private static String fit(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        if (font.width("…") > maxWidth) return "";
        while (!text.isEmpty() && font.width(text + "…") > maxWidth) text = text.substring(0, text.length()-1);
        return text + "…";
    }

    static void renderSealedPlate(GuiGraphics graphics,int x,int y,int width,int height,int suitColor,int aspectColor,int seed,boolean selected) {
        graphics.fill(x-2,y-2,x+width+2,y+height+2,((selected?0xCC:0x78)<<24)|suitColor);
        graphics.fill(x, y, x + width, y + height, 0xFF111513);
        int traceAlpha = selected ? 0xB0 : 0x78;
        for (int i = 0; i < 5; i++) {
            int mixed = mix(seed + i * 71);
            int tx = x + 2 + Math.floorMod(mixed, Math.max(1, width - 4));
            int ty = y + 2 + i * Math.max(1, (height - 5) / 5);
            graphics.fill(tx, ty, Math.min(x + width - 1, tx + 2), ty + 1, (traceAlpha << 24) | aspectColor);
        }
    }

    static void renderArt(GuiGraphics graphics, String art, int x, int y, int width, int height) {
        var id = ResourceLocation.tryParse(art);
        if (id != null) graphics.blit(id, x, y, width, height, 0.0f, 0.0f,
            ART_TEXTURE_WIDTH, ART_TEXTURE_HEIGHT, ART_TEXTURE_WIDTH, ART_TEXTURE_HEIGHT);
    }

    static void renderArt(GuiGraphics graphics, String art, int x, int y, int width, int height, float alpha) {
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        renderArt(graphics, art, x, y, width, height);
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    static String layer(String art, String layer) {
        return art.endsWith(".png") ? art.substring(0, art.length() - 4) + "_" + layer + ".png" : art + "_" + layer;
    }

    @Mod.EventBusSubscriber(modid = LearningSurfaces.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(ThreadRegistry.FACSIMILE.get(),
                new ResourceLocation(LearningSurfaces.MOD_ID, "thread_index"),
                (stack, level, entity, seed) -> ThreadArt.itemIndex(ThreadFacsimileItem.threadId(stack))));
        }
    }
}
