package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

final class EarlyTeachingTest {
    @Test void timerPausesAndAwardsExactlyOnce() {
        var timer = new OnboardingTimer(600);
        for (int i=0;i<600;i++) assertFalse(timer.tick(true));
        assertEquals(600,timer.remaining());
        for (int i=0;i<599;i++) assertFalse(timer.tick(false));
        assertEquals(1,timer.remaining());
        assertFalse(timer.tick(true));
        assertTrue(timer.tick(false));
        assertFalse(timer.tick(false));
    }
    @Test void persistedRemainderAndSpectatorTransitionsResumeWithoutReset() {
        var timer = new OnboardingTimer(17);
        assertFalse(timer.tick(false));
        assertFalse(timer.tick(true));
        var reconnected = new OnboardingTimer(timer.remaining());
        for(int i=0;i<15;i++) assertFalse(reconnected.tick(false));
        assertTrue(reconnected.tick(false));
        var zero = new OnboardingTimer(0);
        assertFalse(zero.tick(true)); assertTrue(zero.tick(false)); assertFalse(zero.tick(false));
    }
    @Test void backpackMatchesOnlyCraftedTableAndRouteIsConsistent() {
        var route = ThreadArt.BY_ID.get("make_backpack").discoveryRoutes().get(0);
        assertEquals("crafting_table_crafted",route.type()); assertEquals("first",route.value());
    }
    @Test void authoredAndPackagedIdsMatchAndCutsHaveNoRoutes() throws Exception {
        var authored=JsonParser.parseString(Files.readString(Path.of("authoring/discoveries.json")))
            .getAsJsonObject().getAsJsonArray("cards");
        var ids = new java.util.HashSet<String>();
        authored.forEach(row -> ids.add(row.getAsJsonObject().get("id").getAsString()));
        assertEquals(Set.copyOf(ThreadArt.IDS),ids); assertEquals(48,ids.size());
        assertTrue(java.util.Collections.disjoint(ids,Set.of("spirits_are_materials","underground_pressure_builds","returned_traces","deaths_door")));
        var types=ThreadArt.BY_ID.values().stream().flatMap(d->d.discoveryRoutes().stream()).map(ThreadDefinition.Route::type).collect(Collectors.toSet());
        assertTrue(java.util.Collections.disjoint(types,Set.of("spirit_acquired","depth_pressure","trace_return","deaths_door","straw_acquired")));
        assertTrue(types.containsAll(Set.of("depth_warning","spirit_market_contact")));
        assertEquals("Race the Font’s Clock",ThreadArt.BY_ID.get("font_trip_time_budget").title());
        assertEquals("Font Clock",ThreadArt.BY_ID.get("font_trip_time_budget").shortTitle());
    }
    @Test void removingDefinitionsDoesNotEraseReadHistory() {
        var state=new ThreadPlayerState();
        // Simulate a prior release's saved, already-read retired card.
        state.known.add("deaths_door"); state.discovered.add("deaths_door");
        state.generationCounts.put("deaths_door",1); state.discoveryOrder.put("deaths_door",1L);
        state.discover("first_hand_axe","native","keep:1","",0); state.markRead("first_hand_axe");
        var restored=ThreadPlayerState.fromTag(state.toTag());
        assertEquals(state.known,restored.known); assertEquals(state.unread,restored.unread);
        assertTrue(restored.known.contains("deaths_door")); assertFalse(restored.unread.contains("deaths_door"));
        assertFalse(ThreadDefinitions.INSTANCE.contains("deaths_door"));
        assertTrue(ThreadDefinitions.INSTANCE.contains("first_hand_axe"));
        assertEquals(java.util.List.of("first_hand_axe"),ThreadDefinitions.INSTANCE.known(restored).stream().map(ThreadDefinition::id).toList());
    }
}
