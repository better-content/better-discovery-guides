package com.bettercontent.betterdiscoveryguides;

import java.util.Collection;

/** Fixed packaged card catalogue. Its authoring source is validated before packaging. */
public final class ThreadDefinitions {
    public static final ThreadDefinitions INSTANCE = new ThreadDefinitions();

    private ThreadDefinitions() {}

    public Collection<ThreadDefinition> all() { return ThreadArt.BY_ID.values(); }
    public java.util.List<ThreadDefinition> known(ThreadPlayerState state) {
        return all().stream().filter(d -> state.known.contains(d.id()))
            .sorted(java.util.Comparator.comparingLong(d -> state.discoveryOrder.getOrDefault(d.id(), Long.MAX_VALUE))).toList();
    }
    public ThreadDefinition get(String id) { return ThreadArt.BY_ID.get(id); }
    public boolean contains(String id) { return ThreadArt.BY_ID.containsKey(id); }
}
