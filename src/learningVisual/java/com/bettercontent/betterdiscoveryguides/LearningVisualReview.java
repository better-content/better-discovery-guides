package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Review fixture using production screens, font and artwork; no world or pointer input. */
@Mod.EventBusSubscriber(modid = LearningSurfaces.MOD_ID, value = Dist.CLIENT)
public final class LearningVisualReview {
    private record Frame(String name, int scale, Supplier<Screen> screen) {}
    private static final List<Frame> frames = new ArrayList<>();
    private static int frame = -1;
    private static int ticks;
    private static boolean capturing;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (Boolean.getBoolean("bc.learningVisual.tipsOnly") || Boolean.getBoolean("bc.learningVisual.journalOnly") || Boolean.getBoolean("bc.learningVisual.combinedDeathOnly") || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || mc.screen == null) return;
        if (frame < 0) {
            if (++ticks < 30) return;
            prepare();
            next();
            return;
        }
        if (capturing || ++ticks < 12) return;
        capturing = true;
        String name = frames.get(frame).name();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {
            System.out.println("LEARNING_VISUAL captured " + name + " " + message.getString());
            mc.execute(LearningVisualReview::next);
        });
    }

    private static void next() {
        var mc = Minecraft.getInstance();
        if (++frame >= frames.size()) {
            System.out.println("LEARNING_VISUAL complete frames=" + frames.size());
            mc.stop();
            return;
        }
        var item = frames.get(frame);
        mc.options.guiScale().set(item.scale());
        mc.resizeDisplay();
        mc.setScreen(item.screen().get());
        if (item.name().startsWith("catalogue")) {
            mc.screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
            mc.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
            if (!(mc.screen instanceof LearningLibraryScreen)) throw new IllegalStateException("Keyboard cannot open Lessons");
            mc.setScreen(item.screen().get());
            mc.screen.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            mc.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
            mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            if (!(mc.screen instanceof ThreadDeckScreen)) throw new IllegalStateException("Keyboard did not enter and return from a card");
            mc.setScreen(item.screen().get());
        }
        ticks = 0;
        capturing = false;
    }

    private static void prepare() throws Exception {
        for (int scale : new int[]{4, 3, 2}) {
            for (String id : List.of("purity_three", "door_prepare", "body_and_air", "orbit_possible")) {
                var hint = DeathHints.INSTANCE.all().stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
                frames.add(new Frame("death-" + id + "-scale-" + scale, scale, () -> new DeathHintVisualScreen(hint, false)));
            }
            frames.add(new Frame("death-hardcore-scale-" + scale, scale,
                () -> new DeathHintVisualScreen(DeathHints.FALLBACK, true)));
        }
        if (Boolean.getBoolean("bc.learningVisual.deathHintsOnly")) return;
        var json = JsonParser.parseString(Files.readString(Path.of("../../src/main/resources/data/better_discovery_guides/threads/catalogue.json")));
        var cards = new ArrayList<ThreadNetwork.Card>();
        for (var row : json.getAsJsonObject().getAsJsonArray("threads")) {
            var d = ThreadDefinition.parse(row.getAsJsonObject());
            var door = d.doorway();
            cards.add(new ThreadNetwork.Card(d.id(), d.conceptId(), d.title(), d.topic().id(), d.order(), d.aspect()==null?"":d.aspect().id(), d.art().toString(),
                true, false, true, d.event(), d.cause(), d.action(), d.recipeItems(),
                door == null ? "" : door.type(), door == null ? "" : door.target(), 1, 0, 0, d.order(), ""));
        }
        for (int scale : new int[]{4, 3, 2}) {
            String suffix = "-scale-" + scale;
            frames.add(new Frame("catalogue" + suffix, scale, () -> new ThreadDeckScreen(cards)));
            frames.add(new Frame("lesson-library" + suffix, scale, () -> new LearningLibraryScreen(cards)));
            for (var brief : LoadingBriefs.INSTANCE.all()) {
                frames.add(new Frame("loading-" + brief.id() + suffix, scale, () -> new Screen(Component.literal("Loading review")) {
                    private final LoadingBriefSession session = new LoadingBriefSession(List.of(brief), new LoadingBriefRotation.State(Set.of(), ""));
                    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        LoadingLessonClient.renderWorldGenerationBrief(graphics, session, 57, width, height);
                        var layout = LoadingBriefBackdropLayout.calculate(width, height, true);
                        var font = Minecraft.getInstance().font;
                        int lines = font.split(Component.literal(brief.headline()), layout.textWidth()).size()
                            + font.split(Component.literal(brief.body()), layout.textWidth()).size()
                            + font.split(Component.literal("TRY THIS: " + brief.action()), layout.textWidth()).size();
                        if (34 + lines * 10 > layout.captionY() + layout.captionHeight() - layout.barY() - 28)
                            throw new IllegalStateException("Loading copy does not fit: " + brief.id() + " " + width + "x" + height);
                    }
                }));
            }
            for (String id : List.of("first_hand_axe", "make_backpack", "deaths_door", "frozen_food", "train_fuel_cost", "heavy_blow")) {
                frames.add(new Frame("card-" + id + suffix, scale, () -> new ThreadDeckScreen(cards, id)));
                frames.add(new Frame("card-bottom-" + id + suffix, scale, () -> new ThreadDeckScreen(cards, id) {
                    private int rendered;
                    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        super.render(graphics, mouseX, mouseY, partialTick);
                        if (rendered++ == 0) for (int i = 0; i < 20; i++) keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0);
                    }
                }));
            }
            frames.add(new Frame("lesson-detail" + suffix, scale, () -> new LearningLibraryScreen(cards, "movement")));
            frames.add(new Frame("arrival" + suffix, scale, () -> new LoadingBriefScreen(
                new LoadingBriefSession(LoadingBriefs.INSTANCE.all(), new LoadingBriefRotation.State(Set.of(), "")), () -> {})));
            frames.add(new Frame("generation" + suffix, scale, () -> new LearningLevelLoadingScreen(
                new net.minecraft.server.level.progress.StoringChunkProgressListener(0),
                new LoadingBriefSession(LoadingBriefs.INSTANCE.all(), new LoadingBriefRotation.State(Set.of(), "")))));
            frames.add(new Frame("terrain" + suffix, scale,
                net.minecraft.client.gui.screens.ReceivingLevelScreen::new));

        }
    }
}
