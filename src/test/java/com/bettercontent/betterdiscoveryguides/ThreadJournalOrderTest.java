package com.bettercontent.betterdiscoveryguides;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class ThreadJournalOrderTest {
 private ThreadNetwork.Card card(ThreadDefinition d,long order,boolean unread){return new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),true,unread,true,d.event(),d.cause(),d.action(),d.recipeItems(),"","",1,0,0,order,"");}
 @Test void continueCrossesFiltersAndReturnsNoCardAfterLastUnread(){
  var world=ThreadArt.BY_ID.values().stream().filter(d->d.topic()==ThreadTopic.WORLD).findFirst().orElseThrow();
  var body=ThreadArt.BY_ID.values().stream().filter(d->d.topic()==ThreadTopic.BODY).findFirst().orElseThrow();
  var a=card(world,8,true);var b=card(body,2,true);var cards=List.of(a,b);var read=new HashSet<String>();
  assertEquals(List.of(a),ThreadJournalOrder.visible(cards,read,ThreadTopic.WORLD));assertEquals(b,ThreadJournalOrder.nextUnread(cards,read));
  read.add(b.id());assertEquals(a,ThreadJournalOrder.nextUnread(cards,read));read.add(a.id());assertNull(ThreadJournalOrder.nextUnread(cards,read));
 }
 @Test void journalShowsUnreadBeforeOlderReadHistory(){var defs=ThreadArt.BY_ID.values().stream().filter(d->d.topic()==ThreadTopic.WORLD).toList();var older=card(defs.get(0),1,false);var unread=card(defs.get(1),2,true);assertEquals(List.of(unread,older),ThreadJournalOrder.visible(List.of(older,unread),Set.of(),null));}
}
