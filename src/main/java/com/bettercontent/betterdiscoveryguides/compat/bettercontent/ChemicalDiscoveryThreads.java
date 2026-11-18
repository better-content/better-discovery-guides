package com.bettercontent.betterdiscoveryguides.compat.bettercontent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Historical ownership codec retained for existing persistence regression tests.
 * No Hazards event hook is registered or delivered in the active pack. */
public final class ChemicalDiscoveryThreads {
    public static final class Sources extends SavedData {
        private final Map<Long, Source> entries = new HashMap<>();
        static Sources get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(Sources::load, Sources::new, "better_discovery_guides_chemical_sources");
        }
        void record(ServerLevel level, BlockPos pos, UUID owner) {
            record(pos, owner, ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()).toString());
        }
        void record(BlockPos pos, UUID owner, String blockId) {
            entries.put(pos.asLong(), new Source(owner, blockId));
            setDirty();
        }
        UUID owner(ServerLevel level, BlockPos pos) {
            return owner(pos, ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()).toString());
        }
        UUID owner(BlockPos pos, String blockId) {
            Source source = entries.get(pos.asLong());
            if (source == null) return null;
            if (!source.block().equals(blockId)) {
                remove(pos); return null;
            }
            return source.owner();
        }
        void remove(BlockPos pos) { if (entries.remove(pos.asLong()) != null) setDirty(); }
        @Override public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            entries.forEach((pos, source) -> { CompoundTag item = new CompoundTag(); item.putLong("pos", pos);
                item.putUUID("owner", source.owner()); item.putString("block", source.block()); list.add(item); });
            tag.put("sources", list); return tag;
        }
        static Sources load(CompoundTag tag) {
            Sources data = new Sources();
            for (Tag entry : tag.getList("sources", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) entry;
                if (item.hasUUID("owner")) data.entries.put(item.getLong("pos"), new Source(item.getUUID("owner"), item.getString("block")));
            }
            return data;
        }
        private record Source(UUID owner, String block) {}
    }
}
