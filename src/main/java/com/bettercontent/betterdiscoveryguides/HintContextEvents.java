package com.bettercontent.betterdiscoveryguides;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Read-only advice inputs. No snapshot observation awards a Thread. */
@Mod.EventBusSubscriber(modid=LearningSurfaces.MOD_ID)
public final class HintContextEvents {
    private record Recent(String context,long tick){}
    private static final Map<UUID,Recent> DAMAGE=new HashMap<>();
    @SubscribeEvent public static void damage(LivingDamageEvent event){
        if(event.getEntity() instanceof ServerPlayer player && event.getAmount()>0)
            DAMAGE.put(player.getUUID(),new Recent(DeathHintEvents.classify(event.getSource()),player.server.getTickCount()));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount%20==0)
            ThreadNetwork.hintContext(player,context(player));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){DAMAGE.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){DAMAGE.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){DAMAGE.clear();}
    static String context(ServerPlayer player){
        if(!player.isAlive()||player.isCreative()||player.isSpectator())return "general";
        String injury=loaded("better_deaths_door")?Injury.context(player):"general";
        if(injury.startsWith("door_"))return injury;
        if(player.isOnFire())return "fire";
        if(player.getAirSupply()<player.getMaxAirSupply()/4)return "drowning";
        if(loaded("better_survival_physiology")&&Metabolism.thirst(player))return "thirst";
        if(loaded("cold_sweat")){String thermal=Thermal.context(player);if(!thermal.equals("general"))return thermal;}
        if(player.getFoodData().needsFood()&&player.getFoodData().getFoodLevel()<=6)return "hunger";
        if(player.getHealth()<=player.getMaxHealth()/4)return "low_health";
        if(!injury.equals("general"))return injury;
        if(player.hasEffect(MobEffects.POISON)||player.hasEffect(MobEffects.WITHER))return "magic";
        if(loaded("better_survival_physiology")&&loaded("diet")){String metabolic=Metabolism.context(player);if(!metabolic.equals("general"))return metabolic;}
        if(loaded("better_industrial_heat")&&Food.frozen(player))return "frozen_food";
        var recent=DAMAGE.get(player.getUUID());
        if(recent!=null&&player.server.getTickCount()-recent.tick()<200&&!recent.context().equals("general"))return recent.context();
        if(loaded("better_pillager_campaigns"))return Campaign.context(player);
        return "general";
    }
    private static boolean loaded(String id){return ModList.get().isLoaded(id);}
    private static final class Injury {
        static String context(ServerPlayer player){
            var body=com.bettercontent.betterdeathsdoor.api.InjuryApi.snapshot(player);
            if(body.atDoor())return body.healingLockTicks()>0?"door_locked":"door_healable";
            if(body.activeMaims().isEmpty())return "general";
            // Treatment is now server-owned care rather than a consumable item. The
            // authoritative active-treatment state is the only cure cue.
            if(com.bettercontent.betterdeathsdoor.RevivalManager.isTreating(player, player))return "injury_cure";
            if(body.traumaCount()>0&&body.activeMaims().stream().anyMatch(m->m.region()!=com.bettercontent.betterdeathsdoor.state.Region.HEAD)
                &&body.functionalMultiplier()>body.tuning().restingFunctionalMultiplier())return "injury_trauma";
            return "injury_uncured";
        }
    }
    private static final class Metabolism {
        static boolean thirst(ServerPlayer player){return com.bettercontent.bettersurvivalphysiology.compat.ThirstCompat.isLow(player);}
        static String context(ServerPlayer player){
            if(com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge.tracker(player).isEmpty())return "general";
            var nutrition=com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge.snapshot(player);
            var state=com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore.get(player);
            var presentation=com.bettercontent.bettersurvivalphysiology.presentation.PresentationSnapshot.create(player,nutrition,state);
            if((presentation.flags()&com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags.TEMPO_CRASH)!=0)return "sugar_crash";
            if((presentation.flags()&com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags.CONTROL_IMPAIRED)!=0)return "alcohol";
            for(var group:com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot.Group.values())
                if(group!=com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot.Group.ALCOHOL
                    && nutrition.actual(group)<presentation.ordinary())return "nutrition";
            return "general";
        }
    }
    private static final class Thermal {
        static String context(ServerPlayer player){
            double body=com.momosoftworks.coldsweat.api.util.Temperature.get(player,com.momosoftworks.coldsweat.api.util.Temperature.Trait.BODY);
            // Heat Sync's published body-stress boundary (BodyTemperatureEpisodes.STRESS).
            return body<=-75?"cold":body>=75?"heat":"general";
        }
    }
    private static final class Food {
        static boolean frozen(ServerPlayer player){return player.getMainHandItem().isEdible()&&com.bettercontent.betterindustrialheat.food.FoodThermalService.INSTANCE.isFrozen(player.getMainHandItem());}
    }
    private static final class Campaign {
        static String context(ServerPlayer player){return switch(com.bettercontent.betterpillagercampaigns.api.CampaignStatusApi.state(player)) {
            case APPROACHING -> "campaign_approaching";
            case MATERIALIZED -> "campaign_active";
            case SURVIVED -> "campaign_recovery";
            default -> "general";
        };}
    }
}
