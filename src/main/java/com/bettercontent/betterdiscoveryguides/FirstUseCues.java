package com.bettercontent.betterdiscoveryguides;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/** One-time personal teaching cues; item and movement work follows relevant changes. */
public final class FirstUseCues {
    private static final String TIMER_KEY = "better_discovery_guides_onboarding_remaining";
    private static final TagKey<net.minecraft.world.item.Item> FRUITS = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "fruits"));
    private static final TagKey<net.minecraft.world.item.Item> VEGETABLES = TagKey.create(Registries.ITEM, new ResourceLocation("diet", "vegetables"));
    private static final Set<String> DRYABLE = Set.of("minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
        "minecraft:mutton", "minecraft:rabbit", "minecraft:cod", "minecraft:salmon");
    private record Timer(UUID player, long dueTick) {}
    private static final Map<UUID, Timer> ACTIVE_TIMERS = new HashMap<>();
    private static final PriorityQueue<Timer> DUE_TIMERS = new PriorityQueue<>(Comparator.comparingLong(Timer::dueTick));
    private static final Map<UUID, BlockPos> LAST_PROXIMITY_CHECK = new HashMap<>();

    private FirstUseCues() {}

    private static void once(ServerPlayer player, String id, String type, String value) {
        if (!ThreadDefinitions.INSTANCE.contains(id) || ThreadPlayerState.get(player).known.contains(id)) return;
        ThreadSignals.emit(player, type, value, player.getUUID() + ":" + id);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) once(player, "choose_return_point", "first_spectator_spawn", "entered");
        inspectInventory(player);
        inspectProximity(player, true);
        if (!ThreadPlayerState.get(player).known.contains("first_hand_axe")) {
            CompoundTag saved = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            int remaining = saved.contains(TIMER_KEY) ? saved.getInt(TIMER_KEY) : 600;
            if (remaining > 0) schedule(player, remaining);
        }
    }

    private static void schedule(ServerPlayer player, int remaining) {
        Timer timer = new Timer(player.getUUID(), player.server.getTickCount() + remaining);
        Timer previous = ACTIVE_TIMERS.put(player.getUUID(), timer);
        if (previous != null) DUE_TIMERS.remove(previous);
        DUE_TIMERS.add(timer);
    }

    private static void saveRemaining(ServerPlayer player, int remaining) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(TIMER_KEY, remaining);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = event.getServer().getTickCount();
        while (!DUE_TIMERS.isEmpty() && DUE_TIMERS.peek().dueTick() <= now) {
            Timer timer = DUE_TIMERS.remove();
            if (ACTIVE_TIMERS.get(timer.player()) != timer) continue;
            ACTIVE_TIMERS.remove(timer.player());
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(timer.player());
            if (player == null) continue;
            once(player, "first_hand_axe", "onboarding_elapsed", "active_30_seconds");
            saveRemaining(player, 0);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Timer timer = ACTIVE_TIMERS.remove(player.getUUID());
        if (timer != null) {
            DUE_TIMERS.remove(timer);
            saveRemaining(player, (int) Math.max(1, timer.dueTick() - player.server.getTickCount()));
        }
        LAST_PROXIMITY_CHECK.remove(player.getUUID());
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        ACTIVE_TIMERS.clear();
        DUE_TIMERS.clear();
        LAST_PROXIMITY_CHECK.clear();
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_PROXIMITY_CHECK.remove(player.getUUID());
            inspectProximity(player, true);
        }
    }

    /** Called after the server accepts a movement packet, not from a player tick. */
    public static void moved(ServerPlayer player) {
        BlockPos last = LAST_PROXIMITY_CHECK.get(player.getUUID());
        BlockPos now = player.blockPosition();
        if (last == null || last.distManhattan(now) >= 3) inspectProximity(player, false);
    }

    private static void inspectProximity(ServerPlayer player, boolean force) {
        BlockPos center = player.blockPosition();
        BlockPos last = LAST_PROXIMITY_CHECK.get(player.getUUID());
        if (!force && last != null && last.distManhattan(center) < 3) return;
        LAST_PROXIMITY_CHECK.put(player.getUUID(), center.immutable());
        boolean font = !ThreadPlayerState.get(player).known.contains("font_trip_time_budget");
        boolean hotstone = !ThreadPlayerState.get(player).known.contains("hotstone_dangerous_ground");
        if (!font && !hotstone) return;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(player.serverLevel().getBlockState(pos).getBlock());
            if (key == null) continue;
            String block = key.toString();
            if (font && block.equals("better_dimension_fonts:dimensional_font")) {
                once(player, "font_trip_time_budget", "font_approach", "first");
                font = false;
            }
            if (hotstone && (block.equals("better_ore_geology:hotstone") || block.equals("better_ore_geology:deepslate_hotstone")
                    || block.startsWith("excavated_variants:") && block.endsWith("_hotstone"))) {
                once(player, "hotstone_dangerous_ground", "hotstone_approach", "first");
                hotstone = false;
            }
            if (!font && !hotstone) break;
        }
    }

    private static void inspectInventory(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) inspectItem(player, stack);
        inspectItem(player, player.getOffhandItem());
    }

    private static void inspectItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        String item = key.toString();
        if (item.equals("farmersdelight:straw")) once(player, "make_backpack", "straw_acquired", "first");
        if (DRYABLE.contains(item)) once(player, "dry_food_for_journey", "dryable_food_acquired", "first");
        if (item.equals("tconstruct:pickaxe")) once(player, "find_dimensional_font", "first_tinkers_tool", "assembled");
        if (item.startsWith("tconstruct:") && stack.isDamageableItem() && stack.getDamageValue() * 5 >= stack.getMaxDamage() * 4)
            once(player, "maintain_tinkers_tool", "tinkers_tool_low", "first");
    }

    @SubscribeEvent public static void pickedUp(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) inspectItem(player, event.getStack());
    }
    @SubscribeEvent public static void containerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) inspectInventory(player);
    }
    @SubscribeEvent public static void containerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof AbstractFurnaceMenu)
            once(player, "lit_furnace_pollutes", "furnace_opened", "first");
    }
    @SubscribeEvent public static void equipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot() == EquipmentSlot.MAINHAND)
            inspectItem(player, event.getTo());
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        inspectItem(player, event.getCrafting());
        ResourceLocation item = ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem());
        if (item != null && item.toString().equals("tconstruct:hand_axe"))
            once(player, "start_tinkers_tools", "first_hand_axe_crafted", "completed");
    }
    @SubscribeEvent public static void ate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack food = event.getItem();
        if (food.is(FRUITS)) once(player, "fruit_fuels_a_sprint", "first_food_category", "fruit");
        if (food.is(VEGETABLES)) once(player, "vegetables_buffer_exposure", "first_food_category", "vegetable");
    }
    @SubscribeEvent public static void boarded(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof ServerPlayer player)
                || !(event.getEntityBeingMounted() instanceof Boat boat)) return;
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(boat.getType());
        if (type != null && type.getNamespace().equals("minecraft"))
            once(player, "boats_break_into_parts", "vanilla_boat_boarded", "first");
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation block = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        if (block != null && Set.of("create:millstone", "create:mechanical_press").contains(block.toString()))
            once(player, "plan_sustained_rotation", "root_machine_placed", "first");
    }
    @SubscribeEvent public static void used(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation block = ForgeRegistries.BLOCKS.getKey(player.serverLevel().getBlockState(event.getPos()).getBlock());
        if (block == null) return;
        String id = block.toString();
        if (id.equals("tconstruct:melter") || id.equals("tconstruct:smeltery_controller"))
            once(player, "alloy_in_smeltery", "tinkers_melter_use", "first");
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LAST_PROXIMITY_CHECK.remove(player.getUUID());
        inspectProximity(player, true);
        if (event.getTo().location().toString().equals("rats:ratlantis"))
            once(player, "ratlantis_below_islands", "ratlantis_arrival", "first");
    }
}
