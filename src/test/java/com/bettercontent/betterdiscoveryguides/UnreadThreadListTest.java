package com.bettercontent.betterdiscoveryguides;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnreadThreadListTest {
    private ThreadNetwork.Card card(ThreadDefinition d, long order, boolean unread) {
        return new ThreadNetwork.Card(d.id(), d.conceptId(), d.title(), d.topic().id(), d.order(), "",
            d.art().toString(), true, unread, true, d.event(), d.cause(), d.action(), d.recipeItems(), "", "",
            1, 0, 0, order, "");
    }

    @Test
    void displaysFiveNewestUnreadAndCountsOlderOnes() {
        var definitions = new ArrayList<>(ThreadArt.BY_ID.values());
        var cards = new ArrayList<ThreadNetwork.Card>();
        for (int i = 0; i < 7; i++) cards.add(card(definitions.get(i), i + 1, true));
        cards.add(card(definitions.get(7), 8, false));
        var selected = UnreadThreadList.select(cards, 5);
        assertEquals(List.of(7L, 6L, 5L, 4L, 3L),
            selected.visible().stream().map(ThreadNetwork.Card::discoveryOrder).toList());
        assertEquals(2, selected.olderCount());
        assertEquals(definitions.get(6).shortTitle(), ThreadArt.BY_ID.get(selected.visible().get(0).id()).shortTitle());
        cards.set(6, card(definitions.get(6), 7, false));
        assertEquals(1, UnreadThreadList.select(cards, 5).olderCount());
    }

    @Test
    void hidesPromptWhenNoDiscoveryRemainsUnread() {
        var definition = ThreadArt.BY_ID.values().iterator().next();
        assertTrue(UnreadThreadList.select(List.of(card(definition, 1, false)), 5).empty());
    }
}
