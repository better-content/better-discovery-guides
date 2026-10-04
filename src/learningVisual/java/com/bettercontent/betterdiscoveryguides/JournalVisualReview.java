package com.bettercontent.betterdiscoveryguides;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Production-screen review with direct screen input only; no world or operating-system pointer input. */
@Mod.EventBusSubscriber(modid=LearningSurfaces.MOD_ID,value=Dist.CLIENT)
public final class JournalVisualReview {
 private record Frame(String name,int scale,Supplier<Screen> screen,Consumer<Screen> action){}
 private static final List<Frame> frames=new ArrayList<>();
 private static int frame=-1,ticks;private static boolean capturing;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(!Boolean.getBoolean("bc.learningVisual.journalOnly")||event.phase!=TickEvent.Phase.END)return;
  var mc=Minecraft.getInstance();if(mc.getOverlay()!=null||mc.screen==null)return;
  if(frame<0){if(++ticks<30)return;prepare();next();return;}
  if(capturing)return;
  if(++ticks==2)frames.get(frame).action().accept(mc.screen);
  if(ticks<32)return;capturing=true;
  String name=frames.get(frame).name();
  System.out.println("JOURNAL_VISUAL screen="+mc.screen.width+"x"+mc.screen.height+" "+name);
  Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),message->{System.out.println("JOURNAL_VISUAL captured "+name+" "+message.getString());mc.execute(JournalVisualReview::next);});
 }
 private static void next(){
  var mc=Minecraft.getInstance();if(++frame>=frames.size()){System.out.println("JOURNAL_VISUAL complete frames="+frames.size());mc.stop();return;}
  var f=frames.get(frame);mc.options.guiScale().set(f.scale());mc.resizeDisplay();mc.setScreen(f.screen().get());ticks=0;capturing=false;
 }
 private static ThreadNetwork.Card card(ThreadDefinition d,boolean known,boolean unread,long order){var door=d.doorway();return new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),d.aspect()==null?"":d.aspect().id(),d.art().toString(),known,known&&unread,known&&unread,known?d.event():"",known?d.cause():"",known?d.action():"",known?d.recipeItems():List.of(),!known||door==null?"":door.type(),!known||door==null?"":door.target(),known?1:0,known?0:-1,known?0:-1,order,"");}
 private static void prepare(){
  var definitions=ThreadArt.BY_ID.values().stream().sorted(Comparator.comparingInt(ThreadDefinition::order)).toList();
  String body=definitions.stream().filter(d->d.topic()==ThreadTopic.BODY&&d.id().contains("frozen")).findFirst().orElseThrow().id();
  String industry=definitions.stream().filter(d->d.topic()==ThreadTopic.INDUSTRY).findFirst().orElseThrow().id();
  String travel=definitions.stream().filter(d->d.topic()==ThreadTopic.TRAVEL).findFirst().orElseThrow().id();
  String remembered=definitions.get(0).id();
  var sequence=new LinkedHashMap<String,Long>();sequence.put(body,1L);sequence.put(industry,2L);sequence.put(travel,3L);sequence.put(remembered,0L);
  var cards=definitions.stream().map(d->card(d,sequence.containsKey(d.id()),!d.id().equals(remembered),sequence.getOrDefault(d.id(),0L))).toList();
  var history=definitions.stream().map(d->card(d,sequence.containsKey(d.id()),false,sequence.getOrDefault(d.id(),0L))).toList();
  for(int scale:new int[]{4,3,2}){
   String suffix="-scale-"+scale;var reader=new ThreadDeckScreen(cards);
   add("01-oldest-unread-"+body+suffix,scale,()->reader,s->{});
   add("02-continue-cross-topic-"+industry+suffix,scale,()->reader,s->{
    // Set the body filter through the production tab click handler, reopen its read card,
    // then Continue must still select the next unread industry discovery.
    int total=Math.min(s.width-16,560),cell=total/4,start=(s.width-cell*4)/2;
    s.mouseClicked(start+cell*2+cell/2.0,33,0);s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);s.keyPressed(GLFW.GLFW_KEY_SPACE,0,0);
   });
   add("03-next-unread-"+travel+suffix,scale,()->reader,s->s.keyPressed(GLFW.GLFW_KEY_SPACE,0,0));
   add("04-last-returns-journal"+suffix,scale,()->reader,JournalVisualReview::continueButton);
   add("05-partial-journal-four-known"+suffix,scale,()->new ThreadDeckScreen(history),s->{});
   add("06-empty-journal"+suffix,scale,()->new ThreadDeckScreen(List.of()),s->{});
   add("07-immediate-next-card-"+industry+suffix,scale,()->new ThreadDeckScreen(cards),s->s.keyPressed(GLFW.GLFW_KEY_SPACE,0,0));
   add("08-explanation-bottom"+suffix,scale,()->new ThreadDeckScreen(history,body),s->{for(int i=0;i<30;i++)s.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);});
   add("09-lessons"+suffix,scale,()->new LearningLibraryScreen(history),s->{});
   add("10-lesson-detail"+suffix,scale,()->new LearningLibraryScreen(history,"movement"),s->{});
  }
 }
 private static void continueButton(Screen s){
  var button=s.children().stream().filter(w->w instanceof Button).map(w->(Button)w).filter(b->b.getMessage().getString().equals("Journal")||b.getMessage().getString().equals("Continue")).findFirst().orElseThrow();button.onPress();
 }
 private static void add(String name,int scale,Supplier<Screen> screen,Consumer<Screen> action){frames.add(new Frame(name,scale,screen,action));}
}
