package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/** Packaged lessons are fixed for a release; only their textures reload. */
final class LoadingBriefs {
    static final LoadingBrief FALLBACK = new LoadingBrief("threads", "learning.threads", "better-discovery-guides", "", "",
        "Orientation", "Using Threads",
        "Threads is your journal of the rules you uncover through play, and it opens with the shown key - M by default. The Lessons tab carries survival instruction from your first minute, long before any discovery.",
        "Press your Threads key, then select Lessons.",
        new ResourceLocation(LearningSurfaces.MOD_ID, "textures/gui/loading_briefs/threads.png"));
    static final LoadingBriefs INSTANCE = new LoadingBriefs();
    private final List<LoadingBrief> all;

    private LoadingBriefs() {
        try (var stream = LoadingBriefs.class.getResourceAsStream(
                "/assets/better_discovery_guides/loading_briefs/catalogue.json")) {
            if (stream == null) throw new IllegalStateException("Missing packaged lessons");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"bc.better_discovery_guides.lessons.v1".equals(root.get("schema").getAsString()))
                throw new IllegalArgumentException("Invalid packaged lesson schema");
            var parsed = new ArrayList<LoadingBrief>();
            var ids = new HashSet<String>();
            for (var element : root.getAsJsonArray("briefs")) {
                var brief = LoadingBrief.parse(element.getAsJsonObject());
                if (!ids.add(brief.id())) throw new IllegalArgumentException("Duplicate lesson: " + brief.id());
                if (!brief.relatedThread().isEmpty() && !ThreadArt.BY_ID.containsKey(brief.relatedThread()))
                    throw new IllegalArgumentException("Unknown related card: " + brief.relatedThread());
                parsed.add(brief);
            }
            if (parsed.size() != 18 || !parsed.get(0).id().equals("threads"))
                throw new IllegalArgumentException("Expected 18 lessons with the card introduction first");
            all = List.copyOf(parsed);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read packaged lessons", failure);
        }
    }

    List<LoadingBrief> all() {
        var mods = net.minecraftforge.fml.ModList.get();
        return mods == null ? all : available(all, mods::isLoaded);
    }

    static List<LoadingBrief> available(List<LoadingBrief> briefs, Predicate<String> loaded) {
        return briefs.stream().filter(brief -> brief.requiresMod().isEmpty() || loaded.test(brief.requiresMod())).toList();
    }
}
