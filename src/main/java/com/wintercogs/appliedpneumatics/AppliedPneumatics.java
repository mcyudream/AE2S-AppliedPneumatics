package com.wintercogs.appliedpneumatics;

import ae2.api.stacks.AEKeyTypes;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.common.menu.APNetwork;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.common.config.Configuration;

@Mod(modid=AppliedPneumatics.MODID,name="AE2S Applied Pneumatics",version=AppliedPneumatics.VERSION,
    acceptedMinecraftVersions="[1.12.2]",dependencies="required-after:cleanroom@[0.4.0,);required-after:ae2@[1.1.0,1.2.0);required-after:pneumaticcraft@[1.12.2-0.11.15,);after:ponder-legacy")
public final class AppliedPneumatics {
    public static final String MODID="ae2s-appliedpneumatics",VERSION="1.0.9-cleanroom.4";
    /** Render paths remain readable across the rename; content identities use MODID. */
    public static final String PREVIOUS_RESOURCE_NAMESPACE="appliedpneumatics";
    @Mod.Instance(MODID) public static AppliedPneumatics INSTANCE;
    @SidedProxy(clientSide="com.wintercogs.appliedpneumatics.client.ClientProxy",serverSide="com.wintercogs.appliedpneumatics.CommonProxy")
    public static CommonProxy proxy;
    public static boolean enableLargeCells=true;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        var config=new Configuration(event.getSuggestedConfigurationFile());config.load();
        enableLargeCells=config.getBoolean("enableLargeCells","general",true,"Enable 1M..256M cell recipes using the recursive recipe chain.");
        if(config.hasChanged())config.save();
        AEKeyTypes.register(AirKeyType.INSTANCE);ae2.api.behaviors.GenericSlotCapacities.register(AirKeyType.INSTANCE,64000L);APNetwork.init();proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { APContent.integrate();proxy.init(); }
}
