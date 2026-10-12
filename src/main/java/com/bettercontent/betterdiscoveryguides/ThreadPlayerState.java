package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.betterdiscoveryguides.compat.bettercontent.WorldLifecycleThreads;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.ModList;
import java.io.IOException;
import java.util.*;

/** Discovery history is scoped to a server/lineage, never to a process-global player UUID. */
public final class ThreadPlayerState {
 private static final ResourceLocation LINEAGE_KEY=new ResourceLocation("better_discovery_guides","threads");
 private static final Map<MinecraftServer,Map<UUID,ThreadPlayerState>> CACHE=new WeakHashMap<>();
 public final Set<String> known=new LinkedHashSet<>(),unread=new LinkedHashSet<>(),discovered=new LinkedHashSet<>(),pendingNotices=new LinkedHashSet<>();
 public final Map<String,Integer> generationCounts=new HashMap<>();
 public final Map<String,Long> firstGeneration=new HashMap<>(),lastGeneration=new HashMap<>(),discoveryOrder=new HashMap<>();
 public final Map<String,String> contexts=new HashMap<>();
 private final Set<String> outcomes=new LinkedHashSet<>();
 public long generation;
 private long nextOrder;

 public static ThreadPlayerState get(ServerPlayer player){return get(player.server,player.getUUID());}
 public static ThreadPlayerState get(MinecraftServer server,UUID player){return CACHE.computeIfAbsent(server,s->new HashMap<>()).computeIfAbsent(player,id->fromTag(read(server,id)));}
 public static void forget(ServerPlayer player){var cache=CACHE.get(player.server);if(cache!=null)cache.remove(player.getUUID());}
 public static long currentGeneration(MinecraftServer server){if(!ModList.get().isLoaded("better_world_management"))return 0;try{return WorldLifecycleThreads.generation(server);}catch(IOException e){throw new IllegalStateException("Cannot read lineage generation",e);}}
 public boolean discover(String id,String route,String correlation,String context,long atGeneration){
  if(!ThreadArt.BY_ID.containsKey(id)||!validCorrelation(correlation)||atGeneration!=generation||discovered.contains(id))return false;
  String outcome=id+":"+correlation;
  if(!outcomes.add(outcome))return false;
  boolean first=known.add(id);discovered.add(id);pendingNotices.add(id);
  if(first){unread.add(id);discoveryOrder.put(id,++nextOrder);}
  generationCounts.merge(id,1,Integer::sum);firstGeneration.putIfAbsent(id,generation);lastGeneration.put(id,generation);
  if(context!=null&&!context.isBlank())contexts.put(id,context.substring(0,Math.min(256,context.length())));
  return true;
 }
 public boolean markRead(String id){return known.contains(id)&&unread.remove(id);}
 public void enterGeneration(long next){if(next==generation)return;if(next<generation)throw new IllegalArgumentException("Cannot rewind lineage generation");generation=next;discovered.clear();outcomes.clear();}
 static ThreadPlayerState fromTag(CompoundTag root){
  var s=new ThreadPlayerState();if(root.getInt("schema")!=1)return s;
  s.generation=Math.max(0,root.getLong("generation"));s.nextOrder=Math.max(0,root.getLong("nextOrder"));
  readSet(root,"known",s.known);readSet(root,"unread",s.unread);s.unread.retainAll(s.known);
  readSet(root,"discovered",s.discovered);s.discovered.retainAll(s.known);readSet(root,"pendingNotices",s.pendingNotices);s.pendingNotices.retainAll(s.known);
  for(String id:s.known){s.generationCounts.put(id,Math.max(1,root.getCompound("generationCounts").getInt(id)));s.firstGeneration.put(id,Math.max(0,root.getCompound("firstGeneration").getLong(id)));s.lastGeneration.put(id,Math.max(0,root.getCompound("lastGeneration").getLong(id)));long order=Math.max(1,root.getCompound("discoveryOrder").getLong(id));s.discoveryOrder.put(id,order);s.nextOrder=Math.max(s.nextOrder,order);String context=root.getCompound("contexts").getString(id);if(context.length()<=256)s.contexts.put(id,context);}
  root.getList("outcomes",Tag.TAG_STRING).stream().limit(53).map(Tag::getAsString).filter(v->v.length()<=177).forEach(s.outcomes::add);
  return s;
 }
 CompoundTag toTag(){var root=new CompoundTag();root.putInt("schema",1);root.putLong("generation",generation);root.putLong("nextOrder",nextOrder);root.put("known",strings(known));root.put("unread",strings(unread));root.put("discovered",strings(discovered));root.put("pendingNotices",strings(pendingNotices));root.put("outcomes",strings(outcomes));var counts=new CompoundTag();generationCounts.forEach(counts::putInt);root.put("generationCounts",counts);root.put("firstGeneration",longs(firstGeneration));root.put("lastGeneration",longs(lastGeneration));root.put("discoveryOrder",longs(discoveryOrder));var context=new CompoundTag();contexts.forEach(context::putString);root.put("contexts",context);return root;}
 public void save(ServerPlayer player){save(player.server,player.getUUID());}
 public void save(MinecraftServer server,UUID player){var root=toTag();if(ModList.get().isLoaded("better_world_management")){try{WorldLifecycleThreads.writePlayerData(server,LINEAGE_KEY,player,root);return;}catch(IOException e){throw new IllegalStateException("Cannot save lineage discoveries",e);}}var data=worldData(server);data.players.put(player.toString(),root);data.setDirty();}
 private static CompoundTag read(MinecraftServer server,UUID player){if(ModList.get().isLoaded("better_world_management")){try{return WorldLifecycleThreads.readPlayerData(server,LINEAGE_KEY,player);}catch(IOException e){throw new IllegalStateException("Cannot read lineage discoveries",e);}}return worldData(server).players.getCompound(player.toString());}
 private static WorldData worldData(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(WorldData::new,WorldData::new,"better_discovery_guides_discoveries");}
 private static final class WorldData extends SavedData {private final CompoundTag players;WorldData(){players=new CompoundTag();}WorldData(CompoundTag tag){players=tag.getCompound("players");}@Override public CompoundTag save(CompoundTag tag){tag.put("players",players);return tag;}}
 private static void readSet(CompoundTag tag,String key,Set<String> out){tag.getList(key,Tag.TAG_STRING).stream().map(Tag::getAsString).filter(id->id.matches("[a-z0-9_]{1,48}")).limit(53).forEach(out::add);}
 private static ListTag strings(Collection<String> values){var list=new ListTag();values.forEach(v->list.add(StringTag.valueOf(v)));return list;}
 private static CompoundTag longs(Map<String,Long> values){var out=new CompoundTag();values.forEach(out::putLong);return out;}
 public static boolean validCorrelation(String value){return value!=null&&!value.isBlank()&&value.length()<=128&&value.chars().allMatch(c->c>=0x21&&c<=0x7e);}
}
