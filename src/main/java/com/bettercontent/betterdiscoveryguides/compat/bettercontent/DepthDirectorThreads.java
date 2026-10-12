package com.bettercontent.betterdiscoveryguides.compat.bettercontent;
import com.bettercontent.bettercaveencounters.api.event.CaveWarningEvent;
import com.bettercontent.betterdiscoveryguides.ThreadSignals;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class DepthDirectorThreads {
 @SubscribeEvent public static void warning(CaveWarningEvent event){ThreadSignals.emit(event.player,"depth_warning","committed",event.encounter.toString(),"Approach "+event.approach.toShortString());}
}
