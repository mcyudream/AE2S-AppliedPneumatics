package com.wintercogs.appliedpneumatics.compat.ponder;

import com.wintercogs.appliedpneumatics.common.init.APContent;
import net.createmod.ponder.api.registration.*;
import net.createmod.ponder.api.scene.*;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.catnip.math.Pointing;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/** Reached only through guarded client IMC; Ponder is never needed on the common classpath. */
public final class AppliedPneumaticsPonderPlugin implements PonderPlugin {
    @Override public String getModId(){return "ae2s-appliedpneumatics";}
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper){
        // One logical scene can have many subjects; reuse its board across distinct registration sites.
        PonderStoryBoard airNetwork=AppliedPneumaticsPonderPlugin::air;
        for(var item:APContent.CELLS)helper.addStoryBoard(item.getRegistryName(),APContent.id("air_network"),airNetwork,APContent.id("pneumatics")).identifiedBy(APContent.id("air_network"));
        for(var item:APContent.PORTABLE_CELLS)helper.addStoryBoard(item.getRegistryName(),APContent.id("air_network"),airNetwork,APContent.id("pneumatics")).identifiedBy(APContent.id("air_network"));
        for(var item:new net.minecraft.item.Item[]{net.minecraft.item.Item.getItemFromBlock(APContent.PRESSURE),APContent.AIR_P2P,APContent.SECURITY_CARD,APContent.VACUUM_CARD,APContent.CHARGING_CARD,APContent.VOLUME_CARD})
            helper.addStoryBoard(item.getRegistryName(),APContent.id("air_network"),airNetwork,APContent.id("pneumatics")).identifiedBy(APContent.id("air_network"));
        for(var item:new net.minecraft.item.Item[]{net.minecraft.item.Item.getItemFromBlock(APContent.TEMPERATURE),APContent.HEAT_P2P})
            helper.addStoryBoard(item.getRegistryName(),APContent.id("temperature"),AppliedPneumaticsPonderPlugin::heat,APContent.id("pneumatics")).identifiedBy(APContent.id("temperature"));
        for(var item:new net.minecraft.item.Item[]{net.minecraft.item.Item.getItemFromBlock(APContent.STATION),net.minecraft.item.Item.getItemFromBlock(APContent.EXTENDED_STATION),APContent.WIRELESS,APContent.AMADRON_PATTERN,APContent.PROCESS_UPGRADE})
            helper.addStoryBoard(item.getRegistryName(),APContent.id("amadron"),AppliedPneumaticsPonderPlugin::trade,APContent.id("pneumatics")).identifiedBy(APContent.id("amadron"));
    }
    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper){
        helper.registerTag(APContent.id("pneumatics")).title("Applied Pneumatics").description("Air, temperature and Amadron automation with AE2S.")
            .item(new ItemStack(APContent.PRESSURE),true,true).addToIndex().register();
    }
    @Override public void registerSharedText(SharedTextRegistrationHelper helper){
        helper.registerSharedText("air_power","Power the ME network and provide channels. Put an air storage cell in the drive.");
        helper.registerSharedText("air_import","An import bus facing a compressor imports compressed air. Select the air resource type in the bus.");
        helper.registerSharedText("air_pressure","Connect the pressure interface to tubes and set its target in bar. It exchanges air with ME storage to hold that pressure.");
        helper.registerSharedText("air_safety","Fit security cards to protect full cells. Vacuum cards void overflow in cells and unlock negative pressure in interfaces.");
        helper.registerSharedText("air_portable","Portable cells are 20 bar when full. Charge them in a PNC charging station or the interface's item slot; a charging card supplies carried pneumatic tools.");
        helper.registerSharedText("air_p2p","Attune an ME P2P with a pressure tube, then link its input and outputs with an AE2S memory card. Air follows pressure differences.");
        helper.registerSharedText("heat_target","Connect the temperature interface to your machine. Use the original +/- buttons to set the target in Celsius; hold Shift for tenfold steps.");
        helper.registerSharedText("heat_cost","Supply stored air and AE energy. Temperature control consumes both; speed cards increase the rate and volume cards increase heat capacity.");
        helper.registerSharedText("heat_p2p","Attune a P2P with a blaze rod and link it with a memory card to distribute heat to distant machines.");
        helper.registerSharedText("trade_station","Connect the processing station to a powered ME network with item/fluid storage and a crafting CPU. Leave the sky above it clear for drones.");
        helper.registerSharedText("trade_link","Charge the wireless terminal, then sneak-use it on the station to bind it. Open it to browse and search the server's real Amadron offers.");
        helper.registerSharedText("trade_order","Right-click offer cards to add trades to the basket, then press Submit. Payment comes from ME storage; PNC drones collect and deliver it.");
        helper.registerSharedText("trade_pattern","Put AE2S blank patterns into the terminal slot. Select an unlimited offer and click the arrow to encode it, then put that pattern in the station.");
        helper.registerSharedText("trade_cpu","Request the traded output through AE2S crafting. The station offers its patterns to the CPU, orders the trade, and returns deliveries to the same ME network.");
    }
    private static void start(SceneBuilder s,String id,String title){s.title(id,title);s.configureBasePlate(0,0,7);s.showBasePlate();s.idle(10);}
    private static void step(SceneBuilder s,SceneBuildingUtil u,String key,int x,int y,int z,ItemStack icon){
        s.overlay().showOutline(PonderPalette.WHITE,key,u.select().position(x,y,z),120);
        s.overlay().showControls(u.vector().topOf(x,y,z),Pointing.DOWN,75).rightClick().withItem(icon);
        s.overlay().showText(120).sharedText(key).independent(45).attachKeyFrame();s.idle(130);
    }
    private static void air(SceneBuilder s,SceneBuildingUtil u){
        start(s,"air_network","Store and regulate compressed air");s.world().showSection(u.select().layersFrom(1),EnumFacing.DOWN);
        step(s,u,"air_power",2,1,3,new ItemStack(APContent.CELLS.getFirst()));
        step(s,u,"air_import",1,1,3,new ItemStack(APContent.AIR_P2P));
        step(s,u,"air_pressure",4,1,3,new ItemStack(APContent.PRESSURE));
        step(s,u,"air_safety",2,1,3,new ItemStack(APContent.SECURITY_CARD));
        step(s,u,"air_portable",4,1,3,new ItemStack(APContent.PORTABLE_CELLS.getFirst()));
        step(s,u,"air_p2p",3,1,3,new ItemStack(APContent.AIR_P2P));s.markAsFinished();
    }
    private static void heat(SceneBuilder s,SceneBuildingUtil u){
        start(s,"temperature","Control and distribute temperature");s.world().showSection(u.select().layersFrom(1),EnumFacing.DOWN);
        step(s,u,"heat_target",4,1,3,new ItemStack(APContent.TEMPERATURE));
        step(s,u,"heat_cost",2,1,3,new ItemStack(APContent.CELLS.getFirst()));
        step(s,u,"heat_p2p",3,1,3,new ItemStack(APContent.HEAT_P2P));s.markAsFinished();
    }
    private static void trade(SceneBuilder s,SceneBuildingUtil u){
        start(s,"amadron","Automate Amadron trades");s.world().showSection(u.select().layersFrom(1),EnumFacing.DOWN);
        step(s,u,"trade_station",4,1,3,new ItemStack(APContent.STATION));
        step(s,u,"trade_link",4,1,3,new ItemStack(APContent.WIRELESS));
        step(s,u,"trade_order",4,1,3,new ItemStack(net.minecraft.init.Items.EMERALD));
        step(s,u,"trade_pattern",4,1,3,ae2.core.definitions.AEItems.BLANK_PATTERN.stack());
        step(s,u,"trade_cpu",3,1,2,new ItemStack(APContent.AMADRON_PATTERN));s.markAsFinished();
    }
}
