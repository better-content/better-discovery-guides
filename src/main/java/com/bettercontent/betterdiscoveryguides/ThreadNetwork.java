package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.betterdiscoveryguides.LearningSurfaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.*;
import java.util.function.Supplier;

public final class ThreadNetwork {
    private static final String VERSION="2";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(LearningSurfaces.MOD_ID,"threads"),()->VERSION,VERSION::equals,VERSION::equals);
    private static int messageId;
    private ThreadNetwork(){}
    public static void register(){CHANNEL.messageBuilder(HintContext.class,messageId++,NetworkDirection.PLAY_TO_CLIENT).encoder(HintContext::encode).decoder(HintContext::decode).consumerMainThread(HintContext::handle).add();CHANNEL.messageBuilder(Sync.class,messageId++,NetworkDirection.PLAY_TO_CLIENT).encoder(Sync::encode).decoder(Sync::decode).consumerMainThread(Sync::handle).add();CHANNEL.messageBuilder(Action.class,messageId++,NetworkDirection.PLAY_TO_SERVER).encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();CHANNEL.messageBuilder(DeathContext.class,messageId++,NetworkDirection.PLAY_TO_CLIENT).encoder(DeathContext::encode).decoder(DeathContext::decode).consumerMainThread(DeathContext::handle).add();}
    public static void deathHint(ServerPlayer player, String context) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DeathContext(player.getUUID(), context));
    }
    public record DeathContext(UUID player, String context) {
        public DeathContext {
            Objects.requireNonNull(player);
            if (!DeathHintContext.CATEGORIES.contains(context)) throw new IllegalArgumentException("invalid death context");
        }
        void encode(FriendlyByteBuf buffer) { buffer.writeUUID(player); buffer.writeUtf(context, 16); }
        static DeathContext decode(FriendlyByteBuf buffer) { return new DeathContext(buffer.readUUID(), buffer.readUtf(16)); }
        static void handle(DeathContext message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DeathHintClient.receive(message.player(), message.context()));
            context.get().setPacketHandled(true);
        }
    }
    public static void hintContext(ServerPlayer player,String context){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new HintContext(player.getUUID(),context));}
    public record HintContext(UUID player,String context){
        public HintContext{Objects.requireNonNull(player);if(context==null||!context.matches("[a-z0-9_:-]{0,64}"))throw new IllegalArgumentException("invalid hint context");}
        void encode(FriendlyByteBuf b){b.writeUUID(player);b.writeUtf(context,64);}
        static HintContext decode(FriendlyByteBuf b){return new HintContext(b.readUUID(),b.readUtf(64));}
        static void handle(HintContext m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->ContextHintClient.receive(m.player(),m.context()));c.get().setPacketHandled(true);}
    }
    public static void sync(ServerPlayer player,boolean open){var state=ThreadPlayerState.get(player);var cards=ThreadDefinitions.INSTANCE.all().stream().filter(d->state.known.contains(d.id())).sorted(Comparator.comparingLong(d->state.discoveryOrder.getOrDefault(d.id(),Long.MAX_VALUE))).map(d->card(d,state)).toList();CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Sync(open,cards));}
    private static Card card(ThreadDefinition d,ThreadPlayerState s){var doorway=d.doorway();return new Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),d.aspect()==null?"":d.aspect().id().toString(),d.art().toString(),true,s.unread.contains(d.id()),s.discovered.contains(d.id()),d.event(),d.cause(),d.action(),d.recipeItems(),doorway==null?"":doorway.type(),doorway==null?"":doorway.target(),s.generationCounts.getOrDefault(d.id(),0),s.firstGeneration.getOrDefault(d.id(),-1L),s.lastGeneration.getOrDefault(d.id(),-1L),s.discoveryOrder.getOrDefault(d.id(),0L),s.contexts.getOrDefault(d.id(),""));}
    public static void request(String action,String thread){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientAccess.send(action,thread));}

    private static final class ClientAccess{
        private static void send(String action,String thread){if(net.minecraft.client.Minecraft.getInstance().getConnection()!=null)CHANNEL.sendToServer(new Action(action,thread));}
    }

    public record Card(String id,String conceptId,String title,String topic,int order,String aspect,String art,boolean known,boolean unread,boolean discovered,String event,String cause,String action,List<String> recipeItems,String doorwayType,String doorwayTarget,int generationCount,long firstGeneration,long lastGeneration,long discoveryOrder,String context){
        public Card{ThreadPacketValidation.id(id);if(!conceptId.matches("[a-z0-9_.]{3,80}"))throw new IllegalArgumentException("invalid concept");ThreadPacketValidation.title(title);ThreadTopic.parse(topic);if(order<1||order>53)throw new IllegalArgumentException("invalid global order");if(!aspect.isEmpty())ThreadAspect.parse(aspect);ThreadPacketValidation.resource(art,"art");if(event.length()>ThreadDefinition.MAX_TEXT||cause.length()>ThreadDefinition.MAX_TEXT||action.length()>ThreadDefinition.MAX_TEXT||context.length()>256)throw new IllegalArgumentException("oversized text");recipeItems=List.copyOf(recipeItems);if(recipeItems.size()>8||recipeItems.stream().anyMatch(i->i.length()>128||ResourceLocation.tryParse(i)==null))throw new IllegalArgumentException("invalid recipe items");if(!doorwayType.isEmpty()&&!ThreadDefinition.Doorway.validType(doorwayType))throw new IllegalArgumentException("invalid doorway");if(doorwayTarget.length()>128||generationCount<0||firstGeneration< -1||lastGeneration< -1||discoveryOrder<0)throw new IllegalArgumentException("invalid history");if(!known&&(!event.isEmpty()||!cause.isEmpty()||!action.isEmpty()||!context.isEmpty()||!doorwayType.isEmpty()||!doorwayTarget.isEmpty()||!recipeItems.isEmpty()))throw new IllegalArgumentException("unknown card leaked teaching copy");}
        void encode(FriendlyByteBuf b){b.writeUtf(id,48);b.writeUtf(conceptId,80);b.writeUtf(title,64);b.writeUtf(topic,16);b.writeVarInt(order);b.writeUtf(aspect,16);b.writeUtf(art,128);b.writeBoolean(known);b.writeBoolean(unread);b.writeBoolean(discovered);b.writeUtf(event,ThreadDefinition.MAX_TEXT);b.writeUtf(cause,ThreadDefinition.MAX_TEXT);b.writeUtf(action,ThreadDefinition.MAX_TEXT);b.writeCollection(recipeItems,(buffer,item)->buffer.writeUtf(item,128));b.writeUtf(doorwayType,24);b.writeUtf(doorwayTarget,128);b.writeVarInt(generationCount);b.writeLong(firstGeneration);b.writeLong(lastGeneration);b.writeLong(discoveryOrder);b.writeUtf(context,256);}
        static Card decode(FriendlyByteBuf b){String id=b.readUtf(48),conceptId=b.readUtf(80),title=b.readUtf(64),topic=b.readUtf(16);int order=b.readVarInt();String aspect=b.readUtf(16),art=b.readUtf(128);boolean known=b.readBoolean(),unread=b.readBoolean(),discovered=b.readBoolean();String event=b.readUtf(ThreadDefinition.MAX_TEXT),cause=b.readUtf(ThreadDefinition.MAX_TEXT),action=b.readUtf(ThreadDefinition.MAX_TEXT);var items=b.readCollection(capacity->{if(capacity>8)throw new IllegalArgumentException("too many recipe items");return new ArrayList<String>(capacity);},buffer->buffer.readUtf(128));return new Card(id,conceptId,title,topic,order,aspect,art,known,unread,discovered,event,cause,action,items,b.readUtf(24),b.readUtf(128),b.readVarInt(),b.readLong(),b.readLong(),b.readLong(),b.readUtf(256));}
    }
    public record Sync(boolean open,List<Card>cards){
        public Sync{if(cards.size()>53)throw new IllegalArgumentException("too many thread entries");}
        void encode(FriendlyByteBuf b){b.writeBoolean(open);writeCards(b,cards);}
        static Sync decode(FriendlyByteBuf b){return new Sync(b.readBoolean(),readCards(b));}
        static void handle(Sync m,Supplier<NetworkEvent.Context>c){c.get().enqueueWork(()->ThreadClient.receive(m));c.get().setPacketHandled(true);}
        private static void writeCards(FriendlyByteBuf b,List<Card>cards){if(cards.size()>53)throw new IllegalArgumentException("too many thread cards");b.writeVarInt(cards.size());cards.forEach(c->c.encode(b));}
        private static List<Card>readCards(FriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>53)throw new IllegalArgumentException("invalid thread packet");var out=new ArrayList<Card>(n);var ids=new HashSet<String>();for(int i=0;i<n;i++){var card=Card.decode(b);if(!ids.add(card.id()))throw new IllegalArgumentException("duplicate thread card");out.add(card);}return List.copyOf(out);}
    }
    public record Action(String action,String thread){
        void encode(FriendlyByteBuf b){b.writeUtf(action,16);b.writeUtf(thread,48);}static Action decode(FriendlyByteBuf b){return new Action(b.readUtf(16),b.readUtf(48));}
        static void handle(Action m,Supplier<NetworkEvent.Context>c){var player=c.get().getSender();c.get().enqueueWork(()->handle(player,m));c.get().setPacketHandled(true);}
        private static void handle(ServerPlayer player,Action action){
            if(player==null)return;
            if(action.action.equals("emi")){
                if(!action.thread.matches("[a-z0-9_:./-]{1,48}"))return;
                String token=player.getUUID()+":emi:"+player.server.getTickCount();
                if(action.thread.equals("create:millstone")||action.thread.equals("create:mechanical_press"))
                    ThreadSignals.emit(player,"emi_recipe_closed","root_machine",token);
                if(action.thread.equals("better_ratlantis_logistics:courier_lattice")||action.thread.equals("prettypipes:pipe"))
                    ThreadSignals.emit(player,"emi_recipe_closed","better_ratlantis_logistics",token);
                return;
            }
            if(!isReaderAction(action.action))return;
            if(action.action.equals("open")){sync(player,true);return;}
            if(!ThreadDefinitions.INSTANCE.contains(action.thread))return;
            var state=ThreadPlayerState.get(player);if(!state.known.contains(action.thread))return;
            if(state.markRead(action.thread)){state.save(player);sync(player,false);}
        }
        static boolean isReaderAction(String action) { return action.equals("open") || action.equals("read"); }
    }
}
