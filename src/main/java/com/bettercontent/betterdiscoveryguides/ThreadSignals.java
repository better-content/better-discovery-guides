package com.bettercontent.betterdiscoveryguides;
import com.bettercontent.betterdiscoveryguides.diagnostics.PlaytestDiagnostics;
import com.bettercontent.betterdiscoveryguides.diagnostics.PlaytestLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.bettercontent.gameplaynotices.GameplayNotice;
import com.bettercontent.gameplaynotices.GameplayNotices;
import com.bettercontent.gameplaynotices.NoticeTheme;
import java.util.*;
/** Authoritative outcome ingress. A route matches one completed native effect, never setup. */
public final class ThreadSignals {
 private ThreadSignals(){}
 public static void emit(ServerPlayer player,String type,String value){emit(player,type,value,null,"");}
 public static void emit(ServerPlayer player,String type,String value,String correlation){emit(player,type,value,correlation,"");}
 public static void emit(ServerPlayer player,String type,String value,String correlation,String context){if(player!=null)emit(player.server,player.getUUID(),type,value,correlation,context);}
 public static void emit(MinecraftServer server,UUID owner,String type,String value,String correlation,String context){
  if(server==null||owner==null||type==null||!type.matches("[a-z0-9_]{1,32}")||value==null||value.length()>160||!ThreadPlayerState.validCorrelation(correlation) ){
   if(PlaytestDiagnostics.LOG.enabled("signals"))PlaytestDiagnostics.LOG.event("signals","thread.signal","server","rejected","invalid_envelope",null);return;
  }
  if(!server.isSameThread()){server.execute(()->emit(server,owner,type,value,correlation,context));
   if(PlaytestDiagnostics.LOG.enabled("signals"))PlaytestDiagnostics.LOG.event("signals","thread.signal","server","queued","server_dispatch_submitted",null);return;}
  String detail=context==null?"":context.substring(0,Math.min(256,context.length()));
  var state=ThreadPlayerState.get(server,owner);state.enterGeneration(ThreadPlayerState.currentGeneration(server));boolean changed=false;boolean matched=false;
  for(var d:ThreadDefinitions.INSTANCE.all())for(var route:d.discoveryRoutes())if(route.type().equals(type)&&matches(route.value(),value)){
   matched=true;changed|=state.discover(d.id(),route.id(),correlation,detail,state.generation);break;
  }
  if(!changed){if(PlaytestDiagnostics.LOG.enabled("signals"))PlaytestDiagnostics.LOG.event("signals","thread.signal","server","unchanged",SignalObservation.unchangedReason(matched),null);return;}
  state.save(server,owner);
  if(PlaytestDiagnostics.LOG.enabled("signals"))PlaytestDiagnostics.LOG.event("signals","thread.credit","server","saved","state_backend_updated",()->PlaytestLog.fields("type",type,"generation",state.generation));
  var player=server.getPlayerList().getPlayer(owner);if(player!=null)deliver(player);
 }
 public static void login(ServerPlayer player){var state=ThreadPlayerState.get(player);state.enterGeneration(ThreadPlayerState.currentGeneration(player.server));state.save(player);deliver(player);}
 public static void deliver(ServerPlayer player){
  var state=ThreadPlayerState.get(player);
  int pending=state.pendingNotices.size();int notices=0;
  for(String id:state.pendingNotices){
   var d=ThreadDefinitions.INSTANCE.get(id);
   if(d==null)continue;
   boolean reminder=state.generationCounts.getOrDefault(id,0)>1;
   long generation=state.lastGeneration.getOrDefault(id,state.generation);
   GameplayNotices.send(player,new GameplayNotice("thread:"+id+":"+generation,NoticeTheme.THREADS,
    Component.translatable(reminder?"message.better_discovery_guides.thread_remembered":"message.better_discovery_guides.thread_discovered",d.title()),
    Component.translatable("message.better_discovery_guides.thread_reader_hint",Component.keybind("key.better_discovery_guides.open_reader")),
    d.aspect()==null?d.topic().color():d.aspect().color()));notices++;
  }
  ThreadNetwork.sync(player,false);
  state.pendingNotices.clear();state.save(player);
  final int submitted=notices;
  if(pending>0&&PlaytestDiagnostics.LOG.enabled("delivery"))PlaytestDiagnostics.LOG.event("delivery","thread.delivery","server","submitted",SignalObservation.deliveryReason(submitted),()->PlaytestLog.fields("pending_processed",pending,"notices_submitted",submitted,"generation",state.generation));
 }
 private static boolean matches(String expected,String actual){if(expected.equals("*"))return true;return Arrays.asList(expected.split("\\|",-1)).contains(actual);}
}
