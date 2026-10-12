package com.bettercontent.betterdiscoveryguides.gametest;

import com.bettercontent.betterdiscoveryguides.ThreadDefinitions;
import com.bettercontent.betterdiscoveryguides.ThreadPlayerState;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Core persistence and duplicate contracts against the promoted card roster. */
@GameTestHolder("better_discovery_guides")
@PrefixGameTestTemplate(false)
public final class ThreadGameTests {
    private static final String CARD = "manual_work";
    private static final String EPISODE = "gametest:manual-work-a";

    @GameTest(template = "empty") public static void committed_outcome_immediately_discovers(GameTestHelper h) {
        withPlayer(h, p -> {
            ThreadSignals.emit(p, "create_manual_complete", "hand_crank", EPISODE);
            var state = ThreadPlayerState.get(p);
            h.assertTrue(state.known.contains(CARD) && state.unread.contains(CARD) && state.discovered.contains(CARD),
                "One committed manual work must immediately teach the complete card");
            h.assertTrue(state.generationCounts.getOrDefault(CARD, 0) == 1, "Discovery must credit once");
        });
    }

    @GameTest(template = "empty") public static void setup_and_invalid_evidence_do_not_discover(GameTestHelper h) {
        withPlayer(h, p -> {
            ThreadSignals.emit(p, "create_manual_complete", "wrong_machine", EPISODE);
            ThreadSignals.emit(p, "create_manual_complete", "hand_crank", null);
            h.assertFalse(ThreadPlayerState.get(p).known.contains(CARD), "Wrong operation and uncorrelated claims cannot discover");
        });
    }

    @GameTest(template = "empty") public static void duplicate_delivery_counts_once(GameTestHelper h) {
        withPlayer(h, p -> {
            ThreadSignals.emit(p, "create_manual_complete", "hand_crank", EPISODE);
            ThreadSignals.emit(p, "create_manual_complete", "hand_crank", EPISODE);
            ThreadSignals.emit(p, "create_manual_complete", "hand_crank", "gametest:manual-work-b");
            h.assertTrue(ThreadPlayerState.get(p).generationCounts.getOrDefault(CARD, 0) == 1,
                "Repeated delivery in one generation must not inflate history");
        });
    }

    @GameTest(template = "empty") public static void offline_success_survives_reload(GameTestHelper h) {
        withPlayer(h, p -> {
            ThreadSignals.emit(p.server, p.getUUID(), "create_manual_complete", "hand_crank", EPISODE, "Hand crank work completed");
            var state = ThreadPlayerState.get(p);
            h.assertTrue(state.pendingNotices.contains(CARD), "Offline notice must be durable");
            state.markRead(CARD);
            state.save(p);
            ThreadPlayerState.forget(p);
            var restored = ThreadPlayerState.get(p);
            h.assertTrue(restored.known.contains(CARD) && !restored.unread.contains(CARD)
                && restored.pendingNotices.contains(CARD), "History and pending notice must survive cache eviction");
            h.assertTrue("Hand crank work completed".equals(restored.contexts.get(CARD)), "Context must survive reload");
        });
    }

    @GameTest(template = "empty") public static void removed_signals_and_straw_do_not_award(GameTestHelper h) {
        withPlayer(h, p -> {
            for (String[] signal : new String[][]{{"spirit_acquired","first"},{"depth_pressure","started"},{"trace_return","own_old_trace"},{"deaths_door","entered"},{"straw_acquired","first"}})
                ThreadSignals.emit(p,signal[0],signal[1],"gametest:cut:"+signal[0]);
            h.assertTrue(ThreadPlayerState.get(p).known.isEmpty(),"Removed signals cannot discover");
            h.assertTrue(ThreadPlayerState.get(p).pendingNotices.isEmpty(),"Removed signals cannot queue notices");
            ThreadSignals.emit(p,"depth_warning","committed","gametest:warning");
            ThreadSignals.emit(p,"spirit_market_contact","first","gametest:market");
            h.assertTrue(ThreadPlayerState.get(p).known.containsAll(java.util.Set.of("cave_warning","trading_spirits")),"Surviving adapters still teach");
        });
    }

    @GameTest(template = "empty") public static void only_table_craft_discovers_backpack(GameTestHelper h) {
        withPlayer(h, p -> {
            var grid=new net.minecraft.world.SimpleContainer(4);
            p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE));
            com.bettercontent.betterdiscoveryguides.FirstUseCues.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
            h.assertFalse(ThreadPlayerState.get(p).known.contains("make_backpack"),"Table possession does not count");
            com.bettercontent.betterdiscoveryguides.FirstUseCues.crafted(new net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE),grid));
            com.bettercontent.betterdiscoveryguides.FirstUseCues.crafted(new net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE),grid));
            h.assertTrue(ThreadPlayerState.get(p).generationCounts.getOrDefault("make_backpack",0)==1,"Successful table craft awards once");
            com.bettercontent.betterdiscoveryguides.FirstUseCues.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
        });
    }

    @GameTest(template = "empty") public static void spectator_timer_reconnect_respawn_and_known_card(GameTestHelper h) {
        withPlayer(h, p -> {
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            com.bettercontent.betterdiscoveryguides.FirstUseCues.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
            for(int i=0;i<600;i++) tick(p);
            h.assertFalse(ThreadPlayerState.get(p).known.contains("first_hand_axe"),"Spectator ticks must not progress onboarding");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for(int i=0;i<300;i++) tick(p);
            com.bettercontent.betterdiscoveryguides.FirstUseCues.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            com.bettercontent.betterdiscoveryguides.FirstUseCues.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
            var replacement=new net.minecraftforge.common.util.FakePlayer(p.serverLevel(),p.getGameProfile());
            com.bettercontent.betterdiscoveryguides.FirstUseCues.respawn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(replacement,false));
            for(int i=0;i<299;i++) tick(replacement);
            h.assertFalse(ThreadPlayerState.get(p).known.contains("first_hand_axe"),"599 active ticks must not award after player replacement");
            tick(replacement);
            h.assertTrue(ThreadPlayerState.get(p).generationCounts.getOrDefault("first_hand_axe",0)==1,"600th active tick must award once");
            com.bettercontent.betterdiscoveryguides.FirstUseCues.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            com.bettercontent.betterdiscoveryguides.FirstUseCues.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
            for(int i=0;i<600;i++) tick(p);
            h.assertTrue(ThreadPlayerState.get(p).generationCounts.getOrDefault("first_hand_axe",0)==1,"Known card must not restart timer");
            com.bettercontent.betterdiscoveryguides.FirstUseCues.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
        });
    }

    private static void tick(ServerPlayer player) {
        com.bettercontent.betterdiscoveryguides.FirstUseCues.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
            net.minecraftforge.event.TickEvent.Phase.END,player));
    }

    private static void withPlayer(GameTestHelper h, Consumer<ServerPlayer> scenario) {
        h.assertTrue(ThreadDefinitions.INSTANCE.contains(CARD), "Packaged card catalogue must load");
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "surface-test"));
        try { scenario.accept(player); h.succeed(); }
        finally {
            com.bettercontent.betterdiscoveryguides.FirstUseCues.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            ThreadPlayerState.forget(player);
        }
    }
}
