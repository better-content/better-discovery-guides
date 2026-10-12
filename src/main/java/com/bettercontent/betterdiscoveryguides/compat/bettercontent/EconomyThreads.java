package com.bettercontent.betterdiscoveryguides.compat.bettercontent;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class EconomyThreads {
    private EconomyThreads() {}
    @SubscribeEvent public static void marketContact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof AbstractVillager villager)) return;
        boolean spiritOffer = villager.getOffers().stream().anyMatch(offer -> {
            var currency = ForgeRegistries.ITEMS.getKey(offer.getCostA().getItem());
            return currency != null && currency.getNamespace().equals("malum") && currency.getPath().contains("spirit");
        });
        if (spiritOffer) ThreadSignals.emit(player, "spirit_market_contact", "first", player.getUUID() + ":market:" + villager.getUUID());
    }
}
