package com.bettercontent.betterdiscoveryguides;

import java.util.Comparator;
import java.util.List;

/** The newest unread discoveries appear first in the in-world reader prompt. */
record UnreadThreadList(List<ThreadNetwork.Card> visible, int olderCount) {
    static UnreadThreadList select(List<ThreadNetwork.Card> cards, int limit) {
        var pending = cards.stream().filter(c -> c.known() && c.unread())
            .sorted(Comparator.comparingLong(ThreadNetwork.Card::discoveryOrder).reversed()
                .thenComparing(ThreadNetwork.Card::id))
            .toList();
        int shown = Math.min(Math.max(0, limit), pending.size());
        return new UnreadThreadList(List.copyOf(pending.subList(0, shown)), pending.size() - shown);
    }

    boolean empty() { return visible.isEmpty() && olderCount == 0; }
}
