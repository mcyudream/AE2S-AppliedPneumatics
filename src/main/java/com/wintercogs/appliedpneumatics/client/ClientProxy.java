package com.wintercogs.appliedpneumatics.client;

import ae2.api.client.*;
import com.wintercogs.appliedpneumatics.*;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.common.menu.ContainerAP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid=AppliedPneumatics.MODID,value=Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @SubscribeEvent public static void models(ModelRegistryEvent event){
        for(var item:APContent.ITEMS)ModelLoader.setCustomModelResourceLocation(item,0,new ModelResourceLocation(item.getRegistryName(),"inventory"));
        for(int i=0;i<APContent.TIERS.length;i++){
            var model=APContent.id("block/drive/cells/air_cell_"+APContent.TIERS[i]);
            StorageCellModels.registerModel(APContent.CELLS.get(i),model);StorageCellModels.registerModel(APContent.PORTABLE_CELLS.get(i),model);
        }
    }
    @SubscribeEvent public static void portableScreen(net.minecraftforge.client.event.GuiOpenEvent event){
        if(event.getGui() instanceof ae2.client.gui.me.common.GuiMEStorage<?> screen
            &&screen.inventorySlots instanceof ae2.container.me.common.ContainerMEStorage menu
            &&menu.getTarget() instanceof ae2.items.contents.PortableCellGuiHost<?> host
            &&host.getItemStack().getItem() instanceof com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell){
            event.setGui(new ae2.client.gui.me.common.GuiMEStorage<>(menu,Minecraft.getMinecraft().player.inventory,null,
                ae2.client.gui.style.GuiStyleManager.loadStyleDoc("/screens/terminals/portable_air_cell.json")));
        }
    }
    @Override public void init(){
        if(net.minecraftforge.fml.common.Loader.isModLoaded("ponder-legacy"))
            net.minecraftforge.fml.common.event.FMLInterModComms.sendMessage("ponder-legacy","register_ponder_plugin","com.wintercogs.appliedpneumatics.compat.ponder.AppliedPneumaticsPonderPlugin");
        AEKeyRendering.register(AirKeyType.INSTANCE,AirKey.class,new AEKeyRenderHandler<AirKey>(){
            @Override public void drawInGui(Minecraft mc,int x,int y,AirKey key){
                GlStateManager.color(1,1,1,1);mc.getTextureManager().bindTexture(APContent.id("textures/item/air_cell_shell.png"));
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x,y,0,0,16,16,16,16);
            }
            @Override public void drawOnBlockFace(AirKey key,float scale,int light,World world){
                GlStateManager.pushMatrix();GlStateManager.scale(scale/16,scale/16,scale/16);drawInGui(Minecraft.getMinecraft(),-8,-8,key);GlStateManager.popMatrix();
            }
            @Override public ITextComponent getDisplayName(AirKey key){return AirKeyType.NAME;}
        });
    }
    @Override public Object createScreen(ContainerAP menu){return menu==null?null:menu.isWireless()?new com.wintercogs.appliedpneumatics.client.gui.AmadronWirelessTerminalGUI(menu):menu.machine instanceof com.wintercogs.appliedpneumatics.common.blocks.entitis.MEPressureInterfaceBlockEntity?new com.wintercogs.appliedpneumatics.client.gui.MEPressureInterfaceGUI(menu):menu.machine instanceof com.wintercogs.appliedpneumatics.common.blocks.entitis.METemperatureInterfaceBlockEntity?new com.wintercogs.appliedpneumatics.client.gui.METemperatureInterfaceGUI(menu):new com.wintercogs.appliedpneumatics.client.gui.MEAmadronProcessStationGUI(menu);}
    @Override public void handleState(int window,net.minecraft.nbt.NBTTagCompound state){
        Minecraft.getMinecraft().addScheduledTask(()->{var p=Minecraft.getMinecraft().player;if(p!=null&&p.openContainer instanceof ContainerAP menu&&menu.windowId==window)menu.state=state;});
    }
}
