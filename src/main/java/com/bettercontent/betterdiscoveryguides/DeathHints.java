package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fixed advice catalogue shared by menu, pause, and death surfaces. */
final class DeathHints {
    static final DeathHint FALLBACK = new DeathHint("review_controls", "controls.pack_specific_actions", "discovery",
        "Options > Controls lists your current bindings. Practice the unfamiliar ones somewhere safe.",
        Set.of(), Set.of(), List.of("minecraft: OptionsScreen / KeyBindsScreen"));
    static final DeathHints INSTANCE = new DeathHints();
    private final List<DeathHint> all;

    private DeathHints() {
        try (var stream = DeathHints.class.getResourceAsStream(
                "/assets/better_discovery_guides/death_hints/catalogue.json")) {
            if (stream == null) throw new IllegalStateException("Missing packaged advice catalogue");
            all = parse(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read packaged advice catalogue", failure);
        }
    }

    List<DeathHint> all() { return all; }

    static List<DeathHint> parse(JsonObject root) {
        if (!"bc.better_discovery_guides.tips.v1".equals(root.get("schema").getAsString()))
            throw new IllegalArgumentException("Invalid packaged advice schema");
        var hints = new ArrayList<DeathHint>();
        var ids = new HashSet<String>();
        for (var entry : root.getAsJsonArray("hints")) {
            var hint = DeathHint.parse(entry.getAsJsonObject());
            if (!ids.add(hint.id())) throw new IllegalArgumentException("Duplicate advice: " + hint.id());
            hints.add(hint);
        }
        if (hints.size() != 135) throw new IllegalArgumentException("Expected 135 packaged tips");
        return List.copyOf(hints);
    }
}
