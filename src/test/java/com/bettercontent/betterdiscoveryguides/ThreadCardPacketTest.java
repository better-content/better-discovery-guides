package com.bettercontent.betterdiscoveryguides;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ThreadCardPacketTest {
 private ThreadNetwork.Card card(boolean known,String event,String cause,String action){var d=ThreadArt.BY_ID.values().iterator().next();return new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),known,known,known,event,cause,action,java.util.List.of(),"","",2,1,3,9,"");}
 @Test void completeExplanationAndGenerationHistoryRoundTrip(){var c=card(true,"A machine finished.","It received power.","Collect its output.");var b=new FriendlyByteBuf(Unpooled.buffer());try{c.encode(b);assertEquals(c,ThreadNetwork.Card.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}}
 @Test void unknownCardsCannotLeakAnyTeachingPart(){assertThrows(IllegalArgumentException.class,()->card(false,"Event","",""));assertThrows(IllegalArgumentException.class,()->card(false,"","Cause",""));assertThrows(IllegalArgumentException.class,()->card(false,"","","Action"));}
 @Test void readerSyncCarriesCardsWithoutVisualNoticeData(){var n=new ThreadNetwork.Sync(false,java.util.List.of(card(true,"Event","Cause","Action")));var b=new FriendlyByteBuf(Unpooled.buffer());try{n.encode(b);assertEquals(n,ThreadNetwork.Sync.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}}
 @Test void handAxeRecipeItemSurvivesTheCardPacket(){var d=ThreadArt.BY_ID.get("first_hand_axe");var c=new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),true,true,true,d.event(),d.cause(),d.action(),d.recipeItems(),"","",1,0,0,1,"");var b=new FriendlyByteBuf(Unpooled.buffer());try{c.encode(b);assertEquals(java.util.List.of("tconstruct:hand_axe"),ThreadNetwork.Card.decode(b).recipeItems());}finally{b.release();}}
}
