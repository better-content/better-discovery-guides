package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DeathHintsTest {
    private List<DeathHint> catalogue() throws Exception{return DeathHints.parse(JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/better_discovery_guides/death_hints/catalogue.json"))).getAsJsonObject());}
    @Test void allTipsHaveExplicitSurfaceAndAuthoritativeRequirements() throws Exception {
        var all=catalogue();assertFalse(all.isEmpty());
        assertEquals(all.size(),all.stream().map(DeathHint::id).distinct().count());
        for(var hint:all){assertFalse(hint.sources().isEmpty());assertFalse(hint.surfaces().isEmpty());
            if(hint.pool().equals("teaser"))assertEquals(Set.of("menu"),hint.surfaces());
            assertFalse(hint.text().toLowerCase().contains("bleed out"));assertFalse(hint.text().toLowerCase().contains("revive a downed"));}
    }
    @Test void everyMenuHintResolvesToPackagedSketchArt() throws Exception {
        for (var concept : catalogue().stream().map(DeathHint::conceptId).distinct().toList()) {
            var resource = "/assets/better_discovery_guides/textures/gui/menu_sketches/"
                + DeathHintClient.menuArtName(concept) + ".png";
            try (var stream = DeathHintsTest.class.getResourceAsStream(resource)) {
                assertNotNull(stream, "missing menu sketch for " + concept + " at " + resource);
            }
        }
    }
    @Test void unavailableModsAndMissingEvidenceNeverInventADiagnosis() throws Exception {
        var locked=catalogue().stream().filter(h->h.id().equals("door_healing_locked")).findFirst().orElseThrow();
        assertEquals(DeathHints.FALLBACK,DeathHintRotation.select(List.of(locked),"general","pause",DeathHintRotation.State.empty(),m->true,new Random(),Set.of()).hint());
        assertEquals(DeathHints.FALLBACK,DeathHintRotation.select(List.of(locked),"door_locked","pause",DeathHintRotation.State.empty(),m->false,new Random(),Set.of()).hint());
    }
    @Test void terminalDamageNeverUsesTheOriginalDoorHit(){
        var context=new DeathHintContext();var player=UUID.randomUUID();context.downed(player,"fire");
        assertEquals("fall",context.death(player,"fall"));
        assertEquals("general",DeathHintContext.classify("better_deaths_door:bled_out",false,false,false,false,false,false));
    }
    @Test void savedSurfaceHistoryRoundTripsAtomically(@TempDir Path directory) throws Exception {
        var state=new DeathHintRotation.State(Set.of("some_tip"),0,1,"","some_tip","");
        var file=directory.resolve("history.json");DeathHintStore.write(file,state);
        assertEquals(state,DeathHintStore.decode(JsonParser.parseString(Files.readString(file)).getAsJsonObject()));
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void malformedBindingsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new DeathHint("bad","test.tip","survival","Press {unknown}.",Set.of(),Set.of(),List.of("source")));
    }
    @Test void injuryHintWrapUsesTheSharedViewportBudget() throws Exception {
        var source=Files.readString(Path.of("src/main/java/com/bettercontent/betterdiscoveryguides/DeathHintClient.java"));
        assertTrue(source.contains("font.split(text(selected.hint()),InjuryLayout.textWidth(screen))"));
        assertTrue(source.contains("injuryDeath?InjuryLayout.textWidth(screen)"));
        assertTrue(source.contains("DeathRecapOverlay.contentViewport(screen).width()-16"));
        assertEquals(280,com.bettercontent.betterdeathsdoor.client.DeathRecapLayout.fit(320,240,1,3,0,false).width()-16);
        assertEquals(544,com.bettercontent.betterdeathsdoor.client.DeathRecapLayout.fit(640,480,1,3,0,false).width()-16);
    }
    @Test void staleDeathContextsFallBack(){var session=new DeathHintSession();session.receive("fire",100);assertEquals("general",session.freeze(6000));}
}
