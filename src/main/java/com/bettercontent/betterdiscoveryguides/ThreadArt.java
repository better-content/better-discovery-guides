package com.bettercontent.betterdiscoveryguides;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Packaged identity order is also the stable item-model predicate order on clients. */
final class ThreadArt {
 static final Map<String,ThreadDefinition> BY_ID=read();
 static final List<String> IDS=BY_ID.values().stream().sorted(Comparator.comparingInt(ThreadDefinition::order)).map(ThreadDefinition::id).toList();
 private static Map<String,ThreadDefinition> read(){
  try(var stream=ThreadArt.class.getResourceAsStream("/data/better_discovery_guides/threads/catalogue.json")){
   if(stream==null)throw new IllegalStateException("Missing packaged learning cards");
   var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
   if(!"bc.better_discovery_guides.cards.v2".equals(root.get("schema").getAsString()))throw new IllegalStateException("Unsupported learning card catalogue");
   var out=new LinkedHashMap<String,ThreadDefinition>();
   root.getAsJsonArray("threads").forEach(e->{var d=ThreadDefinition.parse(e.getAsJsonObject());if(out.putIfAbsent(d.id(),d)!=null)throw new IllegalStateException("Duplicate card "+d.id());});
   if(out.size()!=53||out.values().stream().map(ThreadDefinition::order).distinct().count()!=53)throw new IllegalStateException("Learning cards require 53 distinct orders");
   return Collections.unmodifiableMap(out);
  }catch(java.io.IOException e){throw new IllegalStateException("Cannot read packaged card identities",e);}
 }
 static float itemIndex(String id){int index=IDS.indexOf(id);return index<0?0:index+1;}
 private ThreadArt(){}
}
