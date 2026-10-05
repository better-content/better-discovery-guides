package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Voice guards for the Learning Surfaces copy. The 2026-10-05 rewrite fixed a corpus
 * that read as one template in one sentence shape; these checks keep later edits from
 * drifting back into it. They measure shape, not wording, so legitimate copy stays free.
 */
final class LearningVoiceTest {

    private static List<String> tipTexts() throws Exception {
        var root = JsonParser.parseString(Files.readString(
            Path.of("src/main/resources/assets/better_discovery_guides/death_hints/catalogue.json"))).getAsJsonObject();
        var out = new ArrayList<String>();
        for (var hint : root.getAsJsonArray("hints")) out.add(hint.getAsJsonObject().get("text").getAsString());
        return out;
    }

    private static List<String> sentences(String text) {
        var out = new ArrayList<String>();
        for (var sentence : text.split("(?<=[.!?])\\s+")) if (!sentence.isBlank()) out.add(sentence);
        return out;
    }

    @Test
    void tipsDoNotShareOneSentenceShape() throws Exception {
        var tips = tipTexts();
        long twoSentence = tips.stream().filter(t -> sentences(t).size() == 2).count();
        assertTrue(twoSentence / (double) tips.size() <= 0.55,
            "two-sentence tips dominate the catalogue again: " + twoSentence + "/" + tips.size());
        long oneSentence = tips.stream().filter(t -> sentences(t).size() == 1).count();
        assertTrue(oneSentence >= 10, "one-sentence tips have disappeared: " + oneSentence);
        var words = tips.stream().flatMap(t -> sentences(t).stream())
            .map(s -> s.split("\\s+").length).mapToDouble(Integer::doubleValue).toArray();
        double mean = Arrays.stream(words).average().orElse(0);
        double variance = Arrays.stream(words).map(w -> (w - mean) * (w - mean)).sum() / words.length;
        assertTrue(Math.sqrt(variance) >= 3.0,
            "tip sentence lengths have flattened into one rhythm again: stdev " + Math.sqrt(variance));
    }

    @Test
    void tipsDoNotFallBackOnOneVerbAndOneRegister() throws Exception {
        var tips = tipTexts();
        long checkOpeners = tips.stream().filter(t -> t.startsWith("Check")).count();
        assertTrue(checkOpeners / (double) tips.size() <= 0.10,
            "'Check' openers dominate the catalogue again: " + checkOpeners + "/" + tips.size());
        long contractions = tips.stream().filter(t -> t.matches(".*\\w+(n't|'re|'ll|'ve|'d)\\b.*")).count();
        assertTrue(contractions / (double) tips.size() >= 0.20,
            "the copy has gone uniformly formal again: " + contractions + "/" + tips.size());
        long openers = tips.stream().map(t -> t.split("\\s+")[0]).distinct().count();
        assertTrue(openers / (double) tips.size() >= 0.40,
            "tips keep opening with the same few words: " + openers + "/" + tips.size());
    }
}
