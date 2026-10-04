package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class ThreadContractsTest {
 private String id(){return ThreadArt.IDS.get(0);}
 @Test void catalogueDefines53CompleteExplanationsWithUniqueGlobalOrder(){
  assertEquals(53,ThreadArt.IDS.size());var orders=new HashSet<Integer>();var topics=new HashSet<ThreadTopic>();
  for(var d:ThreadArt.BY_ID.values()){assertTrue(orders.add(d.order()));topics.add(d.topic());assertFalse(d.shortTitle().isBlank());assertTrue(d.shortTitle().length()<=24);assertFalse(d.event().isBlank());assertFalse(d.cause().isBlank());assertFalse(d.action().isBlank());assertFalse(d.discoveryRoutes().isEmpty());assertTrue(d.discoveryRoutes().stream().allMatch(r->!r.producer().isBlank()));}
  assertEquals(java.util.stream.IntStream.rangeClosed(1,53).boxed().collect(java.util.stream.Collectors.toSet()),orders);assertEquals(Set.of(ThreadTopic.values()),topics);
 }
 @Test void definitionRejectsEmptyExplanationOrNoOutcomeRoutes()throws Exception{
  var root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/better_discovery_guides/threads/catalogue.json"))).getAsJsonObject();
  var row=root.getAsJsonArray("threads").get(0).getAsJsonObject().deepCopy();row.addProperty("cause","");assertThrows(IllegalArgumentException.class,()->ThreadDefinition.parse(row));
  var missing=root.getAsJsonArray("threads").get(0).getAsJsonObject().deepCopy();missing.getAsJsonArray("discovery_routes").remove(0);assertThrows(IllegalArgumentException.class,()->ThreadDefinition.parse(missing));
 }
 @Test void oneOutcomeCompletesTeachingAndCountsOnlyOncePerGeneration(){
  var s=new ThreadPlayerState();assertTrue(s.discover(id(),"native","job:1","Iron ingot",0));
  assertTrue(s.known.contains(id()));assertTrue(s.unread.contains(id()));assertTrue(s.discovered.contains(id()));assertEquals(1,s.generationCounts.get(id()));
  assertFalse(s.discover(id(),"native","job:1","Iron ingot",0));assertFalse(s.discover(id(),"native","job:2","Gold ingot",0));assertEquals(1,s.generationCounts.get(id()));
 }
 @Test void successorCreditsKnownCardWithoutMakingItUnread(){
  var s=new ThreadPlayerState();s.discover(id(),"native","job:1","",0);s.markRead(id());s.enterGeneration(1);
  assertTrue(s.known.contains(id()));assertFalse(s.discovered.contains(id()));assertTrue(s.discover(id(),"native","job:2","",1));
  assertFalse(s.unread.contains(id()));assertEquals(2,s.generationCounts.get(id()));assertEquals(0L,s.firstGeneration.get(id()));assertEquals(1L,s.lastGeneration.get(id()));assertEquals(1,s.known.size());
 }
 @Test void discoveryHistoryAndPendingOfflineNoticeSurviveReload(){
  var s=new ThreadPlayerState();s.discover(id(),"native","job:1","Machine finished",0);s.markRead(id());
  var restored=ThreadPlayerState.fromTag(s.toTag());assertEquals(s.known,restored.known);assertEquals(s.unread,restored.unread);assertEquals(s.pendingNotices,restored.pendingNotices);assertEquals(s.contexts,restored.contexts);assertEquals(s.discoveryOrder,restored.discoveryOrder);assertFalse(restored.discover(id(),"native","job:1","",0));
 }
 @Test void generationsDoNotResetOnSameGenerationOrAcceptEarlierEvents(){
  var s=new ThreadPlayerState();s.enterGeneration(4);s.discover(id(),"native","job:1","",4);s.enterGeneration(4);assertTrue(s.discovered.contains(id()));assertFalse(s.discover(ThreadArt.IDS.get(1),"native","job:2","",3));assertThrows(IllegalArgumentException.class,()->s.enterGeneration(3));
 }
 @Test void oldSchemaIsNotMigratedAndIndependentStateSharesNothing(){
  var legacy=new CompoundTag();legacy.putInt("schema",4);assertTrue(ThreadPlayerState.fromTag(legacy).known.isEmpty());var a=new ThreadPlayerState();var b=new ThreadPlayerState();a.discover(id(),"native","job:1","",0);assertTrue(b.known.isEmpty());
 }
 @Test void oldestDiscoveryOrderSurvivesLaterGenerationRepeat(){
  var s=new ThreadPlayerState();String first=ThreadArt.IDS.get(3),second=ThreadArt.IDS.get(0);s.discover(first,"native","job:1","",0);s.discover(second,"native","job:2","",0);s.enterGeneration(1);s.discover(first,"native","job:3","",1);assertEquals(1L,s.discoveryOrder.get(first));assertEquals(2L,s.discoveryOrder.get(second));
 }
 @Test void revealNeedsNoReadingDeadlineAndOneSkipOnlyCompletesAnimation(){
  var r=new ThreadRevealState();r.select(true);assertEquals(ThreadRevealState.Activation.STARTED,r.activate());assertFalse(r.advance(100));assertEquals(ThreadRevealState.Activation.COMPLETED,r.activate());assertEquals(ThreadRevealState.Phase.COMPLETE,r.phase());assertFalse(r.advance(999999));assertEquals(ThreadRevealState.Activation.IGNORED,r.activate());
 }
 @Test void detailLayoutRemainsInsideCompactAndWideScreens(){for(var size:List.of(new int[]{160,160},new int[]{320,240},new int[]{854,480})){var l=ThreadDeckScreen.detailLayout(size[0],size[1]);assertTrue(l.cardX()>=0);assertTrue(l.detailsX()>=0);assertTrue(l.detailsX()+l.panelWidth()<=size[0]);assertTrue(l.cardY()+l.cardHeight()<=size[1]-34);assertTrue(l.panelWidth()>=Math.min(128,Math.max(80,(size[0]-24)/2)));assertTrue(Math.abs(l.cardWidth()*3-l.cardHeight()*2)<=2);}}
}
