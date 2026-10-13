package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.gameplaynotices.BetterUiTheme;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = LearningSurfaces.MOD_ID, value = Dist.CLIENT)
public final class DeathHintClient {
    private static final Map<String, String> MENU_ART_ALIASES = Map.ofEntries(
        Map.entry("survival.context_alcohol", "survival_hunger_and_nutrition"),
        Map.entry("survival.context_campaign", "survival_hazard_preparation"),
        Map.entry("survival.context_crash", "survival_hunger_and_nutrition"),
        Map.entry("survival.context_frozen", "world_seasons_turn_work"),
        Map.entry("survival.context_low_health", "survival_injury_treatment_prepare"),
        Map.entry("survival.context_nutrition", "survival_hunger_and_nutrition"),
        Map.entry("survival.door_healing_locked", "survival_door_prepare"),
        Map.entry("survival.door_restore_hp", "survival_door_prepare"),
        Map.entry("survival.injury_find_medicine", "survival_injury_treatment_prepare"),
        Map.entry("survival.injury_medicine_ready", "survival_injury_treatment_prepare"),
        Map.entry("survival.trauma_penalties", "survival_injury_treatment_prepare"),
        Map.entry("works.better_rail_beetle", "works_rail_beetle"),
        Map.entry("works.better_ratlantis_logistics", "works_ratlantis_logistics"),
        Map.entry("world.better_bumblezone_crops", "world_bumblezone_cultivars")
    );
    private static final Random RANDOM = new Random();
    private static HintLifecycle lifecycle;
    private static Screen layoutScreen;
    private static List<WidgetPosition> positions = List.of();
    private record WidgetPosition(AbstractWidget widget, int y) {}

    private static HintLifecycle lifecycle() {
        if (lifecycle == null) lifecycle = new HintLifecycle(DeathHintStore.load("death"),DeathHintStore.load("pause"),DeathHintStore.load("menu"));
        return lifecycle;
    }
    public static void receive(UUID player, String context) {
        var local = Minecraft.getInstance().player;
        if (local != null && local.getUUID().equals(player) && lifecycle().death(context, Util.getMillis()))
            DeathHintStore.save(lifecycle().state());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (lifecycle != null) lifecycle.disconnect();
        ContextHintClient.clear();
        layoutScreen = null;
        positions = List.of();
    }
    @SubscribeEvent public static void clone(ClientPlayerNetworkEvent.Clone event) {
        if (lifecycle != null) lifecycle.respawn();
    }
    private static boolean normalPause(Screen screen) {
        return screen instanceof PauseScreen && screen.children().stream().filter(c -> c instanceof Button).count() > 1;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void init(ScreenEvent.Init.Post event) {
        if (normalPause(event.getScreen())) { lifecycle().openPause(ContextHintClient.context()); rememberPauseLayout(event.getScreen()); }
    }
    @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event) {
        if(event.getScreen() instanceof DeathScreen screen && ModList.get().isLoaded("better_deaths_door")) {
            var selected=lifecycle().deathTip(DeathHints.INSTANCE.all(), id->ModList.get().isLoaded(id),RANDOM,Util.getMillis());
            if(selected!=null)InjuryLayout.reserve(screen,Minecraft.getInstance().font.split(text(selected.hint()),InjuryLayout.textWidth(screen)).size(),Minecraft.getInstance().font.lineHeight);
        }
        if (normalPause(event.getScreen())) {
            var selected = lifecycle().pauseTip(DeathHints.INSTANCE.all(), id -> ModList.get().isLoaded(id), RANDOM);
            if (selected != null) layoutPause(event.getScreen(), selected.hint());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void render(ScreenEvent.Render.Post event) {
        var screen = event.getScreen();
        boolean menu = screen instanceof TitleScreen;
        boolean pause = normalPause(screen);
        if (!pause && !menu && !(screen instanceof DeathScreen)) return;
        String surface=menu?"menu":pause?"pause":"death";
        var before = lifecycle().state(surface);
        var selected = menu ? lifecycle().menuTip(DeathHints.INSTANCE.all(), id -> ModList.get().isLoaded(id), RANDOM) : pause
            ? lifecycle().pauseTip(DeathHints.INSTANCE.all(), id -> ModList.get().isLoaded(id), RANDOM)
            : lifecycle().deathTip(DeathHints.INSTANCE.all(), id -> ModList.get().isLoaded(id), RANDOM, Util.getMillis());
        if (selected != null && renderHint(event.getGuiGraphics(), screen, selected.hint())) lifecycle().displayed(selected, surface);
        if (!before.equals(lifecycle().state(surface))) DeathHintStore.save(surface,lifecycle().state(surface));
    }

    private static final class InjuryLayout {
        static int textWidth(Screen screen) {
            return Math.max(1,com.bettercontent.betterdeathsdoor.client.DeathRecapOverlay.contentViewport(screen).width()-16);
        }
        static DeathHintLayout reserve(Screen screen,int lines,int lineHeight) {
            int height=8+lineHeight*lines;
            var box=com.bettercontent.betterdeathsdoor.client.DeathRecapOverlay.reserveHint(screen,height);
            boolean visible=com.bettercontent.betterdeathsdoor.client.DeathRecapOverlay.hintVisible(screen);
            return new DeathHintLayout(box.x(),box.y(),box.width(),height,visible);
        }
    }
    static Component text(DeathHint hint) {
        var mc = Minecraft.getInstance();
        return Component.literal(hint.text().replace("{sneak}", mc.options.keyShift.getTranslatedKeyMessage().getString())
            .replace("{use}", mc.options.keyUse.getTranslatedKeyMessage().getString())
            .replace("{threads}", ThreadClient.OPEN.getTranslatedKeyMessage().getString()));
    }
    static void rememberPauseLayout(Screen screen) {
        layoutScreen = screen;
        positions = screen.children().stream().filter(c -> c instanceof AbstractWidget)
            .map(c -> (AbstractWidget) c).map(w -> new WidgetPosition(w, w.getY())).toList();
    }
    static void layoutPause(Screen screen, DeathHint hint) {
        if (layoutScreen != screen) rememberPauseLayout(screen);
        var controls = positions.stream().filter(p -> p.widget() instanceof Button && p.y() >= 28).toList();
        if (controls.isEmpty()) return;
        int first = controls.stream().mapToInt(WidgetPosition::y).min().orElse(36);
        int bottom = controls.stream().mapToInt(p -> p.y() + p.widget().getHeight()).max().orElse(36);
        int lines = Minecraft.getInstance().font.split(text(hint), DeathHintLayout.textWidth(screen.width)).size();
        var layout = PauseHintLayout.calculate(screen.height, first, bottom, lines, Minecraft.getInstance().font.lineHeight);
        for (var p : positions) {
            int y = p.y();
            if (layout.fits()) {
                if (p.widget() instanceof StringWidget && layout.titleY() >= 0) y = layout.titleY();
                else if (p.widget() instanceof Button && p.y() >= 28) y -= layout.shift();
            }
            p.widget().setY(y);
        }
    }

    /** Layout measures native widgets and is shared by the isolated visual fixtures. */
    static boolean renderHint(GuiGraphics graphics, Screen screen, DeathHint hint) {
        if (screen instanceof TitleScreen) return renderMenuEntry(graphics, screen, hint);
        var font = Minecraft.getInstance().font;
        boolean injuryDeath=screen instanceof DeathScreen && ModList.get().isLoaded("better_deaths_door");
        var lines = font.split(text(hint), injuryDeath?InjuryLayout.textWidth(screen):DeathHintLayout.textWidth(screen.width));
        int bottom = screen.children().stream().filter(c -> screen instanceof TitleScreen ? c instanceof AbstractButton b && b.getHeight()>=20 : c instanceof AbstractWidget)
            .map(c -> (AbstractWidget) c).filter(w -> w.visible).mapToInt(w -> w.getY() + w.getHeight())
            .max().orElse(screen.height / 4 + 116);
        var layout = DeathHintLayout.calculate(screen.width, screen.height, bottom, lines.size(), font.lineHeight);
        if(screen instanceof DeathScreen && ModList.get().isLoaded("better_deaths_door"))
            layout = InjuryLayout.reserve(screen,lines.size(),font.lineHeight);
        if (!layout.visible()) return false;
        if (injuryDeath) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 600);
            var viewport=com.bettercontent.betterdeathsdoor.client.DeathRecapOverlay.contentViewport(screen);
            graphics.enableScissor(viewport.x(),viewport.y(),viewport.x()+viewport.width(),viewport.y()+viewport.height());
        }
        graphics.fill(layout.x(),layout.y(),layout.x()+layout.width(),layout.y()+layout.height(),injuryDeath?BetterUiTheme.color(0xFFEDE0C1,0xFF263A30):0xCE151310);
        if(injuryDeath){
            graphics.fill(layout.x(),layout.y(),layout.x()+layout.width(),layout.y()+2,0xFFAC9163);
            graphics.fill(layout.x(),layout.y()+layout.height()-2,layout.x()+layout.width(),layout.y()+layout.height(),0xFFAC9163);
        }
        if(!injuryDeath) graphics.drawString(font, Component.translatable("screen.better_discovery_guides.death_hint"),
            layout.x() + 8, layout.y() + 6, 0xC6A15B, false);
        int y = layout.y() + (injuryDeath?4:9 + font.lineHeight);
        for (var line : lines) {
            graphics.drawString(font, line, layout.x() + 8, y, injuryDeath?BetterUiTheme.color(0xFF254637,0xFFF4E6C7):0xEEE8DB, false);
            y += font.lineHeight;
        }
        if (injuryDeath) { graphics.disableScissor(); graphics.pose().popPose(); }
        // A clipped fragment is drawable, but is not a completed teaching exposure.
        return !injuryDeath || com.bettercontent.betterdeathsdoor.client.DeathRecapOverlay.hintFullyVisible(screen);
    }

    private static boolean renderMenuEntry(GuiGraphics graphics, Screen screen, DeathHint hint) {
        if (screen.width < 320 || screen.height < 200) return false;
        var font = Minecraft.getInstance().font;
        int pageX = Math.round(screen.width * .065f);
        int pageY = Math.round(screen.height * .11f);
        int pageWidth = Math.round(screen.width * .405f);
        int pageBottom = Math.round(screen.height * .90f);
        int textWidth = pageWidth - 12;
        var lines = font.split(text(hint), textWidth);
        int textHeight = 17 + lines.size() * font.lineHeight;
        int textY = pageBottom - textHeight;
        int artY = pageY + 6;
        int artHeight = Math.max(34, Math.min(textY - artY - 9,
                Math.round((pageWidth - 6) * 342f / 512f * 1.3f)));
        if (artY + artHeight + 9 > textY || textY < pageY + 44) return false;
        String artName = menuArtName(hint.conceptId());
        var art = new ResourceLocation("better_discovery_guides", "textures/gui/menu_sketches/" + artName + ".png");
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.fill(pageX - 3, pageY - 3, pageX + pageWidth + 3, pageBottom + 5, BetterUiTheme.color(0xFFF0E4C6,0xFF182B26));
        graphics.fill(pageX, pageY, pageX + pageWidth, pageY + 1, 0xFFAA8E62);
        int imageWidth = pageWidth - 6;
        int sourceWidth = Math.min(512, Math.max(1, Math.round(342f * imageWidth / artHeight)));
        int sourceX = (512 - sourceWidth) / 2;
        graphics.blit(art, pageX + 3, artY, imageWidth, artHeight, sourceX, 0, sourceWidth, 342, 512, 342);
        graphics.fill(pageX + 3, textY - 5, pageX + pageWidth - 3, textY - 4, 0xFFAD9467);
        graphics.drawString(font, "FIELD NOTE / " + hint.pool().toUpperCase(java.util.Locale.ROOT),
                pageX + 3, textY, BetterUiTheme.color(0xFF77634B,0xFFC4B99E), false);
        int rowY = textY + 13;
        for (var line : lines) {
            graphics.drawString(font, line, pageX + 3, rowY, BetterUiTheme.color(0xFF30483E,0xFFF4E6C7), false);
            rowY += font.lineHeight;
        }
        graphics.pose().popPose();
        return true;
    }

    static String menuArtName(String conceptId) {
        return MENU_ART_ALIASES.getOrDefault(conceptId, conceptId.replace('.', '_'));
    }
}
