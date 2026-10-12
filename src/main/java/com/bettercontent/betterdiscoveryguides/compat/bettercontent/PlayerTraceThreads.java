package com.bettercontent.betterdiscoveryguides.compat.bettercontent;

import com.bettercontent.betterplayertraces.api.event.TraceEpisodeEvent;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Committed trace work remains independent of the retired return discovery. */
public final class PlayerTraceThreads {
    private PlayerTraceThreads() {}
    @SubscribeEvent public static void traceProgressed(TraceEpisodeEvent event) {
        if (event.getKind() == TraceEpisodeEvent.Kind.COMMITTED)
            ThreadSignals.emit(event.getPlayer(), "trace_commit", "footprint", event.getEpisodeId());
    }
}
