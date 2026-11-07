package com.wintercogs.appliedpneumatics.common.init;

import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.features.P2PTunnelAttunement;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.*;
import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import ae2.items.parts.*;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.MachineBlock;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.*;
import com.wintercogs.appliedpneumatics.common.items.*;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import com.wintercogs.appliedpneumatics.common.me.p2p.*;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import com.wintercogs.appliedpneumatics.common.me.strategies.AirBusStrategies;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import java.util.*;

@Mod.EventBusSubscriber(modid=AppliedPneumatics.MODID)
public final class APContent {
    public static final List<Item> ITEMS=new ArrayList<>();
    public static final List<Block> BLOCKS=new ArrayList<>();
    public static final String[] TIERS={"1k","4k","16k","64k","256k","1m","4m","16m","64m","256m"};
    public static final List<AirStorageCell> CELLS=new ArrayList<>();
    public static final List<PortableAirStorageCell> PORTABLE_CELLS=new ArrayList<>();
    public static MachineBlock PRESSURE,TEMPERATURE,STATION,EXTENDED_STATION;
    public static Item AIR_CELL_SHELL,MEGA_AIR_CELL_SHELL,VOLUME_CARD,SECURITY_CARD,VACUUM_CARD,CHARGING_CARD,AMADRON_PATTERN,PROCESS_UPGRADE;
    public static AmadronWirelessTerminalItem WIRELESS;
    public static PartItem<AirP2PTunnelPart> AIR_P2P;
    public static PartItem<HeatP2PTunnelPart> HEAT_P2P;
    public static final CreativeTabs TAB=new CreativeTabs("ae2s-appliedpneumatics.items") {
        @Override public ItemStack createIcon(){return CELLS.isEmpty()?new ItemStack(net.minecraft.init.Items.IRON_INGOT):new ItemStack(CELLS.getFirst());}
    };
    public static ResourceLocation id(String path){return new ResourceLocation(AppliedPneumatics.MODID,path);}
    private static <T extends Item>T item(T item,String name){item.setRegistryName(id(name));item.setTranslationKey(AppliedPneumatics.MODID+"."+name);item.setCreativeTab(TAB);ITEMS.add(item);return item;}
    private static MachineBlock block(Class<? extends MachineTile> tile,String name){var b=new MachineBlock(tile);b.setRegistryName(id(name));b.setTranslationKey(AppliedPneumatics.MODID+"."+name);b.setCreativeTab(TAB);BLOCKS.add(b);GameRegistry.registerTileEntity(tile,id(name));return b;}
    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event){
        PRESSURE=block(MEPressureInterfaceBlockEntity.class,"me_pressure_interface_block");
        TEMPERATURE=block(METemperatureInterfaceBlockEntity.class,"me_temperature_interface");
        STATION=block(MEAmadronProcessStationBlockEntity.class,"me_amadron_process_station");
        EXTENDED_STATION=block(MEAmadronProcessStationBlockEntity.Extended.class,"me_amadron_extended_process_station");
        event.getRegistry().registerAll(BLOCKS.toArray(Block[]::new));
        com.wintercogs.appliedpneumatics.compat.PreviousTileIds.register();
    }
    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event){
        for(var block:BLOCKS){var item=new ItemBlock(block);item.setRegistryName(block.getRegistryName());ITEMS.add(item);}
        AIR_CELL_SHELL=item(new Item(),"air_cell_shell");MEGA_AIR_CELL_SHELL=item(new Item(),"mega_air_cell_shell");
        for(int i=0,k=1;i<TIERS.length;i++,k*=4){CELLS.add(item(new AirStorageCell(k,.5*(i+1)),"air_cell_"+TIERS[i]));PORTABLE_CELLS.add(item(new PortableAirStorageCell(k,.5*(i+1)),"portable_air_cell_"+TIERS[i]));}
        VOLUME_CARD=item(Upgrades.createUpgradeCardItem(),"volume_card");SECURITY_CARD=item(Upgrades.createUpgradeCardItem(),"security_card");
        VACUUM_CARD=item(Upgrades.createUpgradeCardItem(),"vacuum_card");CHARGING_CARD=item(Upgrades.createUpgradeCardItem(),"charging_card");
        AIR_P2P=item(new PartItem<>(AirP2PTunnelPart.class,AirP2PTunnelPart::new),"air_p2p_tunel");
        HEAT_P2P=item(new PartItem<>(HeatP2PTunnelPart.class,HeatP2PTunnelPart::new),"heat_p2p_tunel");
        WIRELESS=item(new AmadronWirelessTerminalItem(),"amadron_wireless_terminal");
        AMADRON_PATTERN=item(PatternDetailsHelper.encodedPatternItemBuilder(AmadronPatternDetails::new).build(),"amadron_pattern");
        PROCESS_UPGRADE=item(new AmadronProcessUpgradeItem(),"amadron_process_upgrade");
        event.getRegistry().registerAll(ITEMS.toArray(Item[]::new));
        ae2.api.parts.PartModels.registerModels(PartModelsHelper.createModels(AirP2PTunnelPart.class));
        ae2.api.parts.PartModels.registerModels(PartModelsHelper.createModels(HeatP2PTunnelPart.class));
    }
    @SubscribeEvent public static void missingItems(RegistryEvent.MissingMappings<Item> event){
        for(var mapping:event.getAllMappings())if(mapping.key.getNamespace().equals("appliedpneumatics")){
            var replacement=Item.REGISTRY.getObject(id(mapping.key.getPath()));
            if(replacement!=null&&replacement!=net.minecraft.init.Items.AIR)mapping.remap(replacement);
        }
    }
    @SubscribeEvent public static void missingBlocks(RegistryEvent.MissingMappings<Block> event){
        for(var mapping:event.getAllMappings())if(mapping.key.getNamespace().equals("appliedpneumatics")){
            var replacement=Block.REGISTRY.getObject(id(mapping.key.getPath()));
            if(replacement!=null&&replacement!=net.minecraft.init.Blocks.AIR)mapping.remap(replacement);
        }
    }
    public static void integrate(){
        StorageCells.addCellHandler(new ICellHandler(){
            @Override public boolean isCell(ItemStack s){return s.getItem() instanceof IAirStorageCell;}
            @Override public StorageCell getCellInventory(ItemStack s,ISaveProvider host){return isCell(s)?new AirCellInventory(s,host):null;}
        });
        AirBusStrategies.register();
        for(var cell:CELLS){Upgrades.add(SECURITY_CARD,cell,1);Upgrades.add(VACUUM_CARD,cell,1);}
        for(var cell:PORTABLE_CELLS){Upgrades.add(SECURITY_CARD,cell,1);Upgrades.add(VACUUM_CARD,cell,1);Upgrades.add(CHARGING_CARD,cell,1);Upgrades.add(AEItems.ENERGY_CARD.asItem(),cell,2);}
        Upgrades.add(VOLUME_CARD,Item.getItemFromBlock(PRESSURE),4);Upgrades.add(SECURITY_CARD,Item.getItemFromBlock(PRESSURE),1);Upgrades.add(VACUUM_CARD,Item.getItemFromBlock(PRESSURE),1);
        Upgrades.add(VOLUME_CARD,Item.getItemFromBlock(TEMPERATURE),4);Upgrades.add(AEItems.SPEED_CARD.asItem(),Item.getItemFromBlock(TEMPERATURE),4);
        for(var block:List.of(STATION,EXTENDED_STATION))Upgrades.add(AEItems.SPEED_CARD.asItem(),Item.getItemFromBlock(block),4);
        Upgrades.add(AEItems.ENERGY_CARD.asItem(),WIRELESS,2);
        P2PTunnelAttunement.registerAttunementTag(AIR_P2P.getRegistryName());P2PTunnelAttunement.registerAttunementTag(HEAT_P2P.getRegistryName());
        OreDictionary.registerOre(P2PTunnelAttunement.getAttunementTag(AIR_P2P.getRegistryName()),new ItemStack(me.desht.pneumaticcraft.common.block.Blockss.PRESSURE_TUBE));
        OreDictionary.registerOre(P2PTunnelAttunement.getAttunementTag(HEAT_P2P.getRegistryName()),new ItemStack(net.minecraft.init.Items.BLAZE_ROD));
        APRecipes.integrate();
    }
    private static final Map<TileEntity,Long> OVERPRESSURE=new IdentityHashMap<>();
    public static void scheduleOverpressure(TileEntity tile){if(tile.getWorld()!=null&&!tile.getWorld().isRemote)OVERPRESSURE.putIfAbsent(tile,tile.getWorld().getTotalWorldTime()+1);}
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var e:new IdentityHashMap<>(OVERPRESSURE).entrySet()){
            var tile=e.getKey();var world=tile.getWorld();if(world==null||tile.isInvalid()){OVERPRESSURE.remove(tile);continue;}
            if(world.getTotalWorldTime()<e.getValue())continue;
            OVERPRESSURE.remove(tile);if(world.getTileEntity(tile.getPos())==tile)world.destroyBlock(tile.getPos(),true);
        }
    }
}
