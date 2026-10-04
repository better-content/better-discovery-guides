package com.bettercontent.betterdiscoveryguides;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Set;

/** One committed outcome teaches one complete explanation. */
public record ThreadDefinition(String id,String conceptId,String owner,String title,String shortTitle,ThreadTopic topic,int order,
 ThreadAspect aspect,ResourceLocation art,String event,String cause,String action,List<String> recipeItems,List<Route> discoveryRoutes,Doorway doorway){
 public static final int MAX_TEXT=512;
 public ThreadDefinition{
  if(id==null||!id.matches("[a-z0-9_]{1,48}"))throw new IllegalArgumentException("invalid thread id");
  bounded(title,64,"title");
  bounded(shortTitle,24,"short title");
  if(!conceptId.matches("[a-z0-9_.]{3,80}")||!owner.matches("[a-z0-9_.-]{1,80}"))throw new IllegalArgumentException("invalid thread ownership");
  if(topic==null||art==null||order<1||order>53)throw new IllegalArgumentException("invalid thread identity");
  bounded(event,MAX_TEXT,"event");bounded(cause,MAX_TEXT,"cause");bounded(action,MAX_TEXT,"action");
  recipeItems=List.copyOf(recipeItems);
  if(recipeItems.size()>8||recipeItems.stream().distinct().count()!=recipeItems.size()||recipeItems.stream().anyMatch(i->ResourceLocation.tryParse(i)==null))throw new IllegalArgumentException("invalid recipe items");
  discoveryRoutes=List.copyOf(discoveryRoutes);
  if(discoveryRoutes.isEmpty()||discoveryRoutes.stream().map(Route::id).distinct().count()!=discoveryRoutes.size())throw new IllegalArgumentException("invalid discovery routes");
 }
 private static void bounded(String value,int max,String field){if(value==null||value.isBlank()||value.length()>max)throw new IllegalArgumentException("invalid thread "+field);}
 public record Route(String id,String type,String value,String producer){
  public Route{if(!id.matches("[a-z0-9_]{1,48}")||!type.matches("[a-z0-9_]{1,32}")||value==null||value.length()>160||!producer.matches("[a-z0-9_.-]{1,80}"))throw new IllegalArgumentException("invalid discovery route");}
 }
 public record Doorway(String type,String target){
  private static final Set<String>TYPES=Set.of("trace_sight","rpg","diet","tconstruct","emi","ponder","font","campaign","guideme","powers","lifecycle","body");
  public Doorway{if(!TYPES.contains(type)||target==null||target.isBlank()||target.length()>128)throw new IllegalArgumentException("unknown doorway action "+type);if((type.equals("emi")||type.equals("ponder"))&&ResourceLocation.tryParse(target)==null)throw new IllegalArgumentException("invalid item doorway target");}
  static boolean validType(String type){return TYPES.contains(type);}
 }
 public static ThreadDefinition parse(JsonObject json){
  var doorway=json.has("doorway")?json.getAsJsonObject("doorway"):null;
  var routes=new java.util.ArrayList<Route>();
  json.getAsJsonArray("discovery_routes").forEach(e->{var r=e.getAsJsonObject();routes.add(new Route(string(r,"id"),string(r,"type"),string(r,"value"),string(r,"producer")));});
  String aspect=string(json,"aspect");
  var items=new java.util.ArrayList<String>();
  if(json.has("recipe_items"))json.getAsJsonArray("recipe_items").forEach(e->items.add(e.getAsString()));
  return new ThreadDefinition(string(json,"id"),string(json,"concept_id"),string(json,"owner"),string(json,"title"),string(json,"short_title"),ThreadTopic.parse(string(json,"topic")),json.get("order").getAsInt(),aspect.isEmpty()?null:ThreadAspect.parse(aspect),new ResourceLocation(string(json,"art")),string(json,"event"),string(json,"cause"),string(json,"action"),items,routes,doorway==null?null:new Doorway(string(doorway,"type"),string(doorway,"target")));
 }
 private static String string(JsonObject json,String key){return json.has(key)?json.get(key).getAsString():"";}
}
