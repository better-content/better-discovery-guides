package com.bettercontent.betterdiscoveryguides.compat.bettercontent;

import com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MetabolicDiscoveryThreads {
    @SubscribeEvent public static void committed(MetabolicDiscoveryEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // The old producer keeps WORK_RHYTHM as an ABI enum value, but the mining
        // sequence is retired. Never credit an impossible lesson from that signal.
        switch (event.kind()) {
            case SUGAR_CRASH -> ThreadSignals.emit(player, "sugar_crash", "entered", event.episode(), event.detail());
            case HEAVY_BLOW -> ThreadSignals.emit(player, "heavy_blow", "executed", event.episode(), event.detail());
            case WORK_RHYTHM -> { }
            case DEEP_RESERVE -> ThreadSignals.emit(player, "deep_reserve", "activated", event.episode(), event.detail());
            case CLEANSE -> ThreadSignals.emit(player, "nutrition_cleanse", "removed", event.episode(), event.detail());
        }
    }
}
