package com.bettercontent.betterdiscoveryguides;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OptionalOreDependencyContractTest {
    @Test void oreDependencyAcceptsCurrentReleaseWhileRemainingOptional() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        String ore = java.util.Arrays.stream(metadata.split("(?=\\[\\[dependencies\\.)"))
            .filter(section -> section.contains("modId=\"better_ore_geology\""))
            .findFirst().orElseThrow();
        assertEquals(1, java.util.Arrays.stream(metadata.split("modId=\"better_ore_geology\"", -1)).count() - 1);
        assertEquals("false", value(ore, "mandatory"));
        assertEquals("[0.3.0,)", value(ore, "versionRange"));
        assertEquals("AFTER", value(ore, "ordering"));
        assertEquals("BOTH", value(ore, "side"));
    }

    private static String value(String section, String key) {
        return section.lines().map(String::trim).filter(line -> line.startsWith(key + "="))
            .map(line -> line.substring(key.length() + 1).replace("\"", ""))
            .findFirst().orElseThrow();
    }
}
