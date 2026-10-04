package com.bettercontent.betterdiscoveryguides;

import com.bettercontent.betterdeathsdoor.client.ClientRevivalState;
import com.bettercontent.betterdeathsdoor.network.BodyView;
import com.bettercontent.betterdeathsdoor.network.StateSyncPacket;
import com.bettercontent.betterdeathsdoor.state.Region;
import com.bettercontent.betterdeathsdoor.state.MaimType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Native DeathScreen rendering and both production overlays, with inert controls and a supplied recap. */
final class CombinedDeathVisualScreen extends DeathScreen {
    private final DeathHint hint;
    CombinedDeathVisualScreen(DeathHint hint){super(Component.literal("The final fall ended this life."),false);this.hint=hint;}
    @Override protected void init(){
        addRenderableWidget(Button.builder(Component.translatable("deathScreen.respawn"),b->{}).bounds(width/2-100,height/4+72,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"),b->{}).bounds(width/2-100,height/4+96,200,20).build());
        ((com.bettercontent.betterdiscoveryguides.visual.DeathScreenVisualAccess)(Object)this).threads$score(Component.literal("Score: 17"));
        var regions=new ArrayList<BodyView.RegionView>();
        for(var region:Region.values())regions.add(new BodyView.RegionView(region,Map.of(MaimType.CRACKED,1),.1,Map.of()));
        var view=new BodyView(UUID.fromString("00000000-0000-0000-0000-000000000001"),"Layout fixture",0,20,false,0,3,1200,.55,.8,4,regions,false);
        ClientRevivalState.accept(new StateSyncPacket(view,2));
    }
}
