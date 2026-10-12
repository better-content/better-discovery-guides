package com.bettercontent.betterdiscoveryguides.compat.bettercontent;

import com.bettercontent.betterdeathsdoor.api.event.InjuryEvent;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import com.bettercontent.betterdiscoveryguides.DeathHintEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Committed injury outcomes and final-death advice; no entry-state discovery. */
public final class DownedRevivalThreads {
    private DownedRevivalThreads() {}
    private static void emit(InjuryEvent event, String type, String value) {
        if (event.getEntity() instanceof ServerPlayer player)
            ThreadSignals.emit(player, type, value, player.getUUID()+":injury:"+event.snapshot().serverTick()+":"+type, context(event));
    }
    private static String context(InjuryEvent event) {
        if(event instanceof InjuryEvent.Treated treated) {
            var record=treated.record();
            return record.type().name()+" "+record.region().name()+"; medicine "+record.itemId();
        }
        return "Active injuries: "+event.snapshot().activeMaims().size()+"; trauma: "+event.snapshot().traumaCount();
    }
    @SubscribeEvent public static void healed(InjuryEvent.Healed event) {
        if (event.snapshot().health()>0 && !event.snapshot().activeMaims().isEmpty()) emit(event,"injury_healing","persistent");
    }
    @SubscribeEvent public static void trauma(InjuryEvent.TraumaIncreased event) {
        if (event.amplifiedExistingInjury()) emit(event,"injury_trauma","amplified");
    }
    @SubscribeEvent public static void treated(InjuryEvent.Treated event) {
        if (event.healer().getUUID().equals(event.getEntity().getUUID())) emit(event,"injury_treatment","self");
    }
    @SubscribeEvent public static void died(InjuryEvent.FinalDeath event) {
        if (event.getEntity() instanceof ServerPlayer player) DeathHintEvents.finalDeath(player,event.source());
    }
}
