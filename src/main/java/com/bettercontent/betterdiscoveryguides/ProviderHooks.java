package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.betterdiscoveryguides.compat.ArsNouveauThreads;
import com.bettercontent.betterdiscoveryguides.compat.Ae2Threads;
import com.bettercontent.betterdiscoveryguides.compat.BloodMagicThreads;
import com.bettercontent.betterdiscoveryguides.compat.CreatingSpaceThreads;
import com.bettercontent.betterdiscoveryguides.compat.DynamicTreeThreads;
import com.bettercontent.betterdiscoveryguides.compat.GoetyThreads;
import com.bettercontent.betterdiscoveryguides.compat.PneumaticThreads;
import com.bettercontent.betterdiscoveryguides.compat.PowerGridThreads;
import com.bettercontent.betterdiscoveryguides.compat.RelicsThreads;
import com.bettercontent.betterdiscoveryguides.compat.SereneSeasonsThreads;
import com.bettercontent.betterdiscoveryguides.compat.TConstructThreads;
import com.bettercontent.betterdiscoveryguides.compat.WeatherTwoThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.DimensionDrinkThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.ArcaneChunkLoaderThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.EconomyThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.HeatSyncThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.RpgStatsThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.RealisticOresThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.SettlementRoadThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.WaterSurvivalThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.WorldLifecycleThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.BetterContentFixesThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.PlayerTraceThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.SystemicSalienceThreads;
import com.bettercontent.betterdiscoveryguides.compat.bettercontent.DownedRevivalThreads;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/** Optional provider adapters; gameplay authority stays with each provider. */
final class ProviderHooks {
    private ProviderHooks() {}

    static void register() {
        if (ModList.get().isLoaded("better_cave_encounters")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.DepthDirectorThreads.class);
        if (ModList.get().isLoaded("better_bumblezone_crops")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.CultivarThreads.class);
        if (ModList.get().isLoaded("better_tinkers_loot_affixes")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.RuinousFluxThreads.class);
        if (ModList.get().isLoaded("better_world_management")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.SchematicSubstitutionThreads.class);
        if (ModList.get().isLoaded("better_oc2r_create_controls")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.ComputerOperationThreads.class);
        if (ModList.get().isLoaded("better_oc2r_wireless_messaging")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.WirelessReceiptThreads.class);
        if (ModList.get().isLoaded("better_create_kinetic_loss")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.TransmissionLossThreads.class);
        if (ModList.get().isLoaded("better_create_train_fuel")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.TrainFuelThreads.class);
        if (ModList.get().isLoaded("better_rail_beetle")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.RailBeetleThreads.class);
        if (ModList.get().isLoaded("better_pillager_campaigns")) MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.PillagerCampaignThreads.class);
        MinecraftForge.EVENT_BUS.register(PackActionThreads.class);
        if (ModList.get().isLoaded("ae2")) {
            MinecraftForge.EVENT_BUS.register(Ae2Threads.class);
        }
        if (ModList.get().isLoaded("better_magic_chunk_anchors")) {
            MinecraftForge.EVENT_BUS.register(ArcaneChunkLoaderThreads.class);
        }
        if (ModList.get().isLoaded("better_spirit_commerce")) {
            MinecraftForge.EVENT_BUS.register(EconomyThreads.class);
        }
        if (ModList.get().isLoaded("better_industrial_heat")) {
            MinecraftForge.EVENT_BUS.register(HeatSyncThreads.class);
        }
        if (ModList.get().isLoaded("better_settlement_roads")) {
            MinecraftForge.EVENT_BUS.register(SettlementRoadThreads.class);
        }
        if (ModList.get().isLoaded("better_drinking_water")) {
            MinecraftForge.EVENT_BUS.register(WaterSurvivalThreads.class);
        }
        if (ModList.get().isLoaded("better_world_management")) {
            MinecraftForge.EVENT_BUS.register(WorldLifecycleThreads.class);
        }
        if (ModList.get().isLoaded("better_compat_fixes")) {
            MinecraftForge.EVENT_BUS.register(BetterContentFixesThreads.class);
        }
        if (ModList.get().isLoaded("better_player_traces")) {
            MinecraftForge.EVENT_BUS.register(PlayerTraceThreads.class);
        }
        if (ModList.get().isLoaded("better_survival_physiology")) {
            MinecraftForge.EVENT_BUS.register(SystemicSalienceThreads.class);
            MinecraftForge.EVENT_BUS.register(com.bettercontent.betterdiscoveryguides.compat.bettercontent.MetabolicDiscoveryThreads.class);
        }
        if (ModList.get().isLoaded("better_deaths_door")) {
            MinecraftForge.EVENT_BUS.register(DownedRevivalThreads.class);
        }
        if (ModList.get().isLoaded("better_dimension_fonts")) {
            MinecraftForge.EVENT_BUS.register(DimensionDrinkThreads.class);
        }
        if (ModList.get().isLoaded("better_rpg_progression")) {
            MinecraftForge.EVENT_BUS.register(RpgStatsThreads.class);
        }
        if (ModList.get().isLoaded("better_ore_geology")) {
            MinecraftForge.EVENT_BUS.register(RealisticOresThreads.class);
        }
        if (ModList.get().isLoaded("dynamictrees")) {
            MinecraftForge.EVENT_BUS.register(DynamicTreeThreads.class);
        }
        if (ModList.get().isLoaded("sereneseasons")) {
            MinecraftForge.EVENT_BUS.register(SereneSeasonsThreads.class);
        }
        if (ModList.get().isLoaded("weather2")) {
            MinecraftForge.EVENT_BUS.register(WeatherTwoThreads.class);
        }
        if (ModList.get().isLoaded("pneumaticcraft")) {
            MinecraftForge.EVENT_BUS.register(PneumaticThreads.class);
        }
        if (ModList.get().isLoaded("powergrid")) {
            MinecraftForge.EVENT_BUS.register(PowerGridThreads.class);
        }
        if (ModList.get().isLoaded("creatingspace")) {
            MinecraftForge.EVENT_BUS.register(CreatingSpaceThreads.class);
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            MinecraftForge.EVENT_BUS.register(ArsNouveauThreads.class);
        }
        if (ModList.get().isLoaded("bloodmagic")) {
            MinecraftForge.EVENT_BUS.register(BloodMagicThreads.class);
        }
        if (ModList.get().isLoaded("goety")) {
            MinecraftForge.EVENT_BUS.register(GoetyThreads.class);
        }
        if (ModList.get().isLoaded("tconstruct")) {
            MinecraftForge.EVENT_BUS.register(TConstructThreads.class);
        }
        if (ModList.get().isLoaded("relics")) {
            MinecraftForge.EVENT_BUS.register(RelicsThreads.class);
        }
    }
}
