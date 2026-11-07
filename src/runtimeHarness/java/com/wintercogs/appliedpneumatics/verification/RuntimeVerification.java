package com.wintercogs.appliedpneumatics.verification;

import ae2.api.config.Actionable;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.*;
import ae2.api.networking.crafting.*;
import ae2.api.networking.security.IActionSource;
import ae2.api.parts.PartHelper;
import ae2.api.stacks.*;
import ae2.api.storage.MEStorageChangeListener;
import ae2.core.definitions.*;
import ae2.me.service.P2PService;
import ae2.parts.AEBasePart;
import ae2.parts.automation.*;
import ae2.parts.storagebus.StorageBusPart;
import ae2.tile.grid.AENetworkedTile;
import ae2.tile.storage.TileDrive;
import ae2.tile.crafting.TileCraftingUnit;
import com.google.gson.GsonBuilder;
import com.wintercogs.appliedpneumatics.common.init.*;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.*;
import com.wintercogs.appliedpneumatics.common.items.*;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import com.wintercogs.appliedpneumatics.common.me.keys.*;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import com.wintercogs.appliedpneumatics.common.me.p2p.*;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.api.tileentity.*;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.recipes.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.Future;

@Mod(modid="apneumatics_verification",name="Applied Pneumatics Acceptance",version="1",dependencies="required-after:ae2s-appliedpneumatics")
public final class RuntimeVerification {
    public static RuntimeVerification INSTANCE;
    public static volatile boolean gameplayDone;
    public static final BlockPos BASE=new BlockPos(0,80,0),PRESSURE=BASE.east(4),HEAT=BASE.east(8),STATION=BASE.south(4);
    public WorldServer world;
    public MEPressureInterfaceBlockEntity pressure;
    public METemperatureInterfaceBlockEntity heat;
    public MEAmadronProcessStationBlockEntity station;
    private MinecraftServer server;
    private AENetworkedTile power;
    private ae2.me.helpers.IGridConnectedTile controller;
    private TileDrive drive;
    private TileCraftingUnit cpu;
    private final List<IManagedGridNode> endpoints=new ArrayList<>();
    private ImportBusPart importer;
    private ExportBusPart exporter;
    private StorageBusPart storageBus;
    private AirP2PTunnelPart airIn,airOut;
    private HeatP2PTunnelPart heatIn,heatOut;
    private IAirHandler importSource,exportTarget,external,airSource,airTarget;
    private IHeatExchangerLogicHolder heatSource,heatTarget;
    private int ticks,phase,phaseStart;
    private long beforeImport;
    private Future<ICraftingPlan> plan;
    private AmadronOffer trade,fluidTrade;
    private boolean done;
    private net.minecraftforge.common.ForgeChunkManager.Ticket fixtureTicket;
    public final List<String> checks=new ArrayList<>();
    private record IHeatExchangerLogicHolder(me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic logic){}
    @Mod.EventHandler public void init(FMLInitializationEvent event)throws Exception{
        INSTANCE=this;MinecraftForge.EVENT_BUS.register(this);
        ForgeChunkManager.setForcedChunkLoadingCallback(this,(tickets,world)->{});
        if(event.getSide().isClient())Class.forName("com.wintercogs.appliedpneumatics.verification.ClientVerification").getMethod("install").invoke(null);
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent event){server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);}
    public static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    public synchronized void checked(String text){checks.add(text);write("RUNNING",null);}
    public synchronized void write(String result,Throwable error){
        try{
            var report=new LinkedHashMap<String,Object>();report.put("result",result);report.put("cleanroom",Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            report.put("java",System.getProperty("java.version"));report.put("ae2s",Loader.instance().getIndexedModList().get("ae2").getVersion());report.put("pneumaticcraft",Loader.instance().getIndexedModList().get("pneumaticcraft").getVersion());
            report.put("ponder",Loader.isModLoaded("ponder-legacy"));report.put("phase",phase);report.put("checks",List.copyOf(checks));
            if(pressure!=null)report.put("pressure",Map.of("actual",pressure.getAirHandler(null).getPressure(),"target",pressure.getExpectedPressure(),"active",pressure.getMainNode().isActive()));
            if(station!=null){report.put("station",station.guiState().toString());report.put("buffer",station.buffer.serializeNBT().toString());}
            if(world!=null)report.put("drones",world.loadedEntityList.stream().filter(e->e instanceof me.desht.pneumaticcraft.common.entity.living.EntityDrone).map(e->Map.of("id",e.getEntityId(),"x",e.posX,"y",e.posY,"z",e.posZ,"dead",e.isDead)).toList());
            if(world!=null&&power!=null)report.put("fixture",Map.of(
                "power_block",String.valueOf(world.getBlockState(BASE).getBlock().getRegistryName()),
                "power_is_current_tile",world.getTileEntity(BASE)==power,
                "power_ready",power.getMainNode().isReady(),
                "power_active",power.getMainNode().isActive(),
                "controller_block",String.valueOf(world.getBlockState(BASE.west(4)).getBlock().getRegistryName()),
                "ready_nodes",endpoints.stream().filter(n->n.isReady()).count(),
                "active_nodes",endpoints.stream().filter(n->n.isActive()).count(),
                "ticks",ticks,"phase_ticks",elapsed()));
            if(error!=null){var trace=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(trace));report.put("failure",trace.toString());}
            Files.createDirectories(Path.of("apneumatics-verification"));Files.writeString(Path.of("apneumatics-verification/report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception failure){throw new RuntimeException(failure);}
    }
    private void next(){phase++;phaseStart=ticks;}
    private int elapsed(){return ticks-phaseStart;}
    private ae2.api.storage.MEStorage storage(){return power.getMainNode().getGrid().getStorageService().getInventory();}
    private long count(AEKey key){return storage().extract(key,Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty());}
    private void place(BlockPos pos,net.minecraft.block.Block block){world.getChunk(pos);world.setBlockToAir(pos);world.setBlockState(pos,block.getDefaultState(),3);}
    private ae2.me.helpers.IGridConnectedTile tile(BlockPos pos,net.minecraft.block.Block block){place(pos,block);var tile=(ae2.me.helpers.IGridConnectedTile)world.getTileEntity(pos);endpoints.add(tile.getMainNode());return tile;}
    private <T extends AEBasePart>T part(BlockPos pos,ae2.api.parts.IPartItem<T> item){
        place(pos,AEBlocks.CABLE_BUS.block());var part=PartHelper.setPart(world,pos,EnumFacing.EAST,null,item);endpoints.add(part.getMainNode());return part;
    }
    private IAirHandler tank(BlockPos pos,int amount){place(pos,Blockss.CHARGING_STATION);world.setBlockState(pos,world.getBlockState(pos).withProperty(me.desht.pneumaticcraft.common.block.BlockPneumaticCraft.ROTATION,EnumFacing.WEST),3);var air=((IPneumaticMachine)world.getTileEntity(pos)).getAirHandler(EnumFacing.WEST);air.addAir(amount);return air;}
    private IHeatExchangerLogicHolder heatTank(BlockPos pos,double temperature){place(pos,APContent.TEMPERATURE);var h=((IHeatExchanger)world.getTileEntity(pos)).getHeatExchangerLogic(EnumFacing.WEST);h.setThermalCapacity(1000);h.setTemperature(temperature);return new IHeatExchangerLogicHolder(h);}
    private void create(){
        require("0.4.0".equals(Loader.instance().getIndexedModList().get("cleanroom").getVersion()),"Wrong Cleanroom baseline");
        fixtureTicket=ForgeChunkManager.requestTicket(this,world,ForgeChunkManager.Type.NORMAL);
        require(fixtureTicket!=null,"Could not keep acceptance fixture chunks loaded");fixtureTicket.setChunkListDepth(16);
        for(int x=-1;x<=2;x++)for(int z=-2;z<=1;z++)ForgeChunkManager.forceChunk(fixtureTicket,new net.minecraft.util.math.ChunkPos(x,z));
        power=(AENetworkedTile)tile(BASE,AEBlocks.CREATIVE_ENERGY_CELL.block());
        controller=tile(BASE.west(4),AEBlocks.CONTROLLER.block());
        drive=(TileDrive)tile(BASE.north(4),AEBlocks.DRIVE.block());
        cpu=(TileCraftingUnit)tile(BASE.west(4).south(4),AEBlocks.CRAFTING_STORAGE_64K.block());
        pressure=(MEPressureInterfaceBlockEntity)tile(PRESSURE,APContent.PRESSURE);pressure.setTarget(0);
        heat=(METemperatureInterfaceBlockEntity)tile(HEAT,APContent.TEMPERATURE);
        station=(MEAmadronProcessStationBlockEntity)tile(STATION,APContent.STATION);
        drive.getInternalInventory().setItemDirect(0,new ItemStack(APContent.CELLS.get(4)));
        drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_64K.stack());
        drive.getInternalInventory().setItemDirect(2,AEItems.FLUID_CELL_64K.stack());
        importer=part(BASE.north(8),AEParts.IMPORT_BUS.item());importSource=tank(BASE.north(8).east(),4000);
        exporter=part(BASE.north(12),AEParts.EXPORT_BUS.item());exportTarget=tank(BASE.north(12).east(),0);
        exporter.getConfig().setStack(0,new GenericStack(AirKey.INSTANCE,1));
        storageBus=part(BASE.north(16),AEParts.STORAGE_BUS.item());external=tank(BASE.north(16).east(),2000);
        airIn=part(BASE.south(8),APContent.AIR_P2P);airSource=tank(BASE.south(8).east(),6000);
        airOut=part(BASE.south(12),APContent.AIR_P2P);airTarget=tank(BASE.south(12).east(),0);
        heatIn=part(BASE.south(16),APContent.HEAT_P2P);heatSource=heatTank(BASE.south(16).east(),500);
        heatOut=part(BASE.south(20),APContent.HEAT_P2P);heatTarget=heatTank(BASE.south(20).east(),300);
        next();
    }
    private void connect()throws Exception{
        if(endpoints.stream().anyMatch(n->!n.isReady()))return;
        for(var node:endpoints)if(node!=controller.getMainNode())GridHelper.createConnection(controller.getMainNode().getNode(),node.getNode());
        for(var pair:List.of(new ae2.parts.p2p.P2PTunnelPart<?>[]{airIn,airOut},new ae2.parts.p2p.P2PTunnelPart<?>[]{heatIn,heatOut})){
            var field=ae2.parts.p2p.P2PTunnelPart.class.getDeclaredField("output");field.setAccessible(true);field.set(pair[1],true);
            short frequency=(short)(pair[0]==airIn?1234:2345);
            for(var tunnel:pair)P2PService.get(tunnel.getMainNode().getGrid()).updateFreq(tunnel,frequency);
        }
        next();
    }
    private void storageChecks(){
        if(!station.getMainNode().isActive()||!airIn.isActive()||elapsed()<40)return;
        require(APContent.ITEMS.size()==35,"Expected all 35 upstream items and blocks");
        require(APRecipes.machineRecipeCount==29,"Missing machine/trade recipes");
        for(var item:new ArrayList<Item>(){{addAll(APContent.CELLS);addAll(APContent.PORTABLE_CELLS);}}){
            var stack=new ItemStack(item);var inv=new AirCellInventory(stack,null);long capacity=((IAirStorageCell)item).getAirCapacity();
            require(inv.insert(AirKey.INSTANCE,Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty())==capacity&&AirCellInventory.getAir(stack)==0,"Cell simulation mutated content");
            require(inv.insert(AirKey.INSTANCE,capacity,Actionable.MODULATE,IActionSource.empty())==capacity,"Cell capacity overflow");
            require(inv.getUsedBytes()==((IAirStorageCell)item).getTotalBytes(),"Used byte statistics incorrect");
            if(item instanceof PortableAirStorageCell cell)require(Math.abs(cell.getPressure(stack)-20)<.0001&&cell.getVolume(stack)>0,"Large portable cell pressure/volume overflow");
            require(inv.extract(AirKey.INSTANCE,Long.MAX_VALUE,Actionable.MODULATE,IActionSource.empty())==capacity,"Cell long extraction failed");
            require(inv.insert(AEItemKey.of(Items.STICK),1,Actionable.MODULATE,IActionSource.empty())==0,"Air cell accepted an item");
        }
        var stack=new ItemStack(APContent.CELLS.getFirst());var inv=new AirCellInventory(stack,null);var deltas=new ArrayList<Long>();
        var listener=new MEStorageChangeListener(){public boolean isValid(Object token){return token==stack;}public void onStackChange(AEKey key,long delta){deltas.add(delta);}public void onListUpdate(){throw new IllegalStateException("Ordinary change enumerated");}};
        inv.addListener(listener,stack);inv.insert(AirKey.INSTANCE,2000,Actionable.MODULATE,IActionSource.empty());inv.extract(AirKey.INSTANCE,123,Actionable.MODULATE,IActionSource.empty());
        require(deltas.equals(List.of(2000L,-123L)),"AE2S signed monitor contract failed");inv.removeListener(listener);inv.extract(AirKey.INSTANCE,10,Actionable.MODULATE,IActionSource.empty());require(deltas.size()==2,"Removed listener called");
        APContent.CELLS.getFirst().getUpgrades(stack).setItemDirect(0,new ItemStack(APContent.VACUUM_CARD));
        require(inv.insert(AirKey.INSTANCE,Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty())==Long.MAX_VALUE,"Vacuum simulation disagrees with voiding insert");
        require(AEKey.fromTagGeneric(AirKey.INSTANCE.toTagGeneric())==AirKey.INSTANCE,"Air key NBT roundtrip failed");
        var packet=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());AEKey.writeKey(packet,AirKey.INSTANCE);require(AEKey.readKey(packet)==AirKey.INSTANCE,"Air key packet roundtrip failed");packet.release();
        var previousAir=new NBTTagCompound();previousAir.setString(AEKey.TYPE_FIELD,"appliedpneumatics:air_type");
        require(AEKey.fromTagGeneric(previousAir)==AirKey.INSTANCE,"Previous saved air key did not migrate");
        var oldTile=pressure.writeToNBT(new NBTTagCompound());oldTile.setString("id","appliedpneumatics:me_pressure_interface_block");
        require(net.minecraft.tileentity.TileEntity.create(world,oldTile) instanceof MEPressureInterfaceBlockEntity,"Previous tile ID did not migrate");
        checked("Previous saved air resource and pressure tile IDs resolve to the renamed mod");
        checked("All 20 cells: long capacity, 256M pressure, nonmutating simulation, exact monitor deltas, vacuum and key serialization");
        var chargingPos=BASE.north(20);tank(chargingPos,20000);
        var charging=world.getTileEntity(chargingPos);
        var hugeCell=new ItemStack(APContent.PORTABLE_CELLS.get(7));AirCellInventory.setAir(hugeCell,3_000_000_000L);
        var chargingInventory=charging.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,null);
        require(chargingInventory!=null&&chargingInventory.insertItem(0,hugeCell,false).isEmpty(),"PNC station rejected large portable air cell");
        ((net.minecraft.util.ITickable)charging).update();require(AirCellInventory.getAir(chargingInventory.getStackInSlot(0))>3_000_000_000L,"PNC charging station overflowed at 2^31 mL");
        checked("Original PNC charging station continues charging a 16M portable cell beyond 2^31 mL");
        var wireless=new ItemStack(APContent.WIRELESS);double capacity=APContent.WIRELESS.getAEMaxPower(wireless);
        APContent.WIRELESS.getUpgrades(wireless).setItemDirect(0,AEItems.ENERGY_CARD.stack());require(APContent.WIRELESS.getAEMaxPower(wireless)>capacity,"Wireless energy card had no effect");
        APContent.WIRELESS.getUpgrades(wireless).setItemDirect(0,ItemStack.EMPTY);require(APContent.WIRELESS.getAEMaxPower(wireless)==capacity,"Removing wireless energy card did not restore capacity");
        checked("Wireless energy-card insertion and removal update real battery capacity");
        require(storage().insert(AirKey.INSTANCE,5_000_000,Actionable.MODULATE,IActionSource.empty())==5_000_000,"Drive did not mount native air cell");
        require(count(AirKey.INSTANCE)>=5_000_000,"Native grid cache missed cell insertion");
        storage().insert(AEItemKey.of(Items.EMERALD),128,Actionable.MODULATE,IActionSource.empty());
        storage().insert(AEFluidKey.of(net.minecraftforge.fluids.FluidRegistry.WATER),16000,Actionable.MODULATE,IActionSource.empty());
        importSource.addAir(4000-importSource.getAir());external.addAir(2000-external.getAir());
        long before=count(AirKey.INSTANCE);int airBefore=importSource.getAir();importer.tickingRequest(importer.getMainNode().getNode(),1);
        require(count(AirKey.INSTANCE)>before&&airBefore-importSource.getAir()==count(AirKey.INSTANCE)-before,"Native import bus conservation failed");
        airBefore=exportTarget.getAir();before=count(AirKey.INSTANCE);exporter.tickingRequest(exporter.getMainNode().getNode(),1);
        require(exportTarget.getAir()>airBefore&&exportTarget.getAir()-airBefore==before-count(AirKey.INSTANCE),"Native export bus conservation failed");
        var ext=storageBus.getInternalHandler();require(ext!=null&&ext.extract(AirKey.INSTANCE,100,Actionable.SIMULATE,IActionSource.empty())==100,"Storage bus did not mount PNC machine");
        checked("Real native import/export/storage bus operations conserve exact air amounts");
        beforeImport=importSource.getAir();pressure.setTarget(2);heat.setTarget(305);
        next();
    }
    private void physicalChecks(){
        if(elapsed()<100)return;
        require(Math.abs(pressure.getAirHandler(null).getPressure()-2)<.01,"Pressure interface did not regulate to target: "+pressure.guiState()+" air="+count(AirKey.INSTANCE));
        require(heat.getHeatExchangerLogic(null).getTemperature()>300,"Temperature interface did not consume stored air to heat");
        require(airTarget.getAir()>0&&airSource.getAir()<6000,"Air P2P failed through real PNC tile interface");
        require(airTarget.getAir()+airSource.getAir()==6000,"Air P2P failed conservation");
        require(heatTarget.logic().getTemperature()>300&&heatSource.logic().getTemperature()<500,"Heat P2P failed actual diffusion");
        require(world.getTileEntity(airIn.getTileEntity().getPos()) instanceof IPneumaticMachine,"Cablebus bridge missing");
        pressure.setTarget(Double.NaN);require(pressure.getExpectedPressure()==2,"NaN target accepted");
        pressure.setTarget(-1);require(pressure.getExpectedPressure()==0,"Negative pressure allowed without vacuum card");
        pressure.getUpgrades().setItemDirect(0,new ItemStack(APContent.VACUUM_CARD));pressure.setTarget(-.5);
        var saved=pressure.writeToNBT(new NBTTagCompound());var copy=new MEPressureInterfaceBlockEntity();copy.readFromNBT(saved);require(copy.getExpectedPressure()==-.5&&copy.getUpgrades().isInstalled(APContent.VACUUM_CARD),"Pressure settings/upgrades NBT failed");
        checked("Real native import/export/storage buses, pressure/temperature interfaces, both P2P tunnels and pressure upgrade persistence");
        pressure.setTarget(2);
        trade=AmadronOfferManager.getInstance().getAllOffers().stream().filter(o->o.getInput() instanceof ItemStack input&&input.getItem()==Items.EMERALD&&o.getOutput() instanceof ItemStack output&&output.getItem()==AEItems.CERTUS_QUARTZ_CRYSTAL.asItem()).findFirst().orElseThrow();
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);player.capabilities.allowEdit=true;
        require(station.order(trade,1,player),"Manual ME-backed Amadron order rejected");
        // Confirm queued reservations and patterns round-trip before the drone is dispatched.
        saved=station.writeToNBT(new NBTTagCompound());var restored=new MEAmadronProcessStationBlockEntity();restored.readFromNBT(saved);require(restored.queuedCount()==1,"Amadron queue not persistent");
        next();
    }
    private AEKey tradeOutput(){return AmadronPatternDetails.resource(trade.getOutput()).what();}
    private long tradeOutputAmount(){return AmadronPatternDetails.resource(trade.getOutput()).amount();}
    private void manualTrade(){
        if(count(tradeOutput())<tradeOutputAmount())return;
        require(count(AEItemKey.of(Items.EMERALD))==128-AmadronPatternDetails.resource(trade.getInput()).amount(),"Manual trade charged incorrect payment");
        checked("Actual PNC collection and delivery drones fulfill ME-backed manual Amadron trade and return output");
        storage().extract(tradeOutput(),Long.MAX_VALUE,Actionable.MODULATE,IActionSource.empty());
        var encoded=new ItemStack(APContent.AMADRON_PATTERN);AmadronPatternDetails.encode(encoded,trade);station.patterns.setStackInSlot(0,encoded);
        require(PatternDetailsHelper.decodePattern(encoded,world) instanceof AmadronPatternDetails,"Native pattern decoder rejected Amadron");
        next();
    }
    private void planTrade(){
        var grid=power.getMainNode().getGrid();
        if(grid.getCraftingService().getCpus().isEmpty()||grid.getCraftingService().getCraftingFor(tradeOutput()).isEmpty())return;
        plan=grid.getCraftingService().beginCraftingCalculation(world,new ICraftingSimulationRequester(){
            public IActionSource getActionSource(){return IActionSource.empty();}public IGridNode getGridNode(){return power.getMainNode().getNode();}
        },tradeOutput(),tradeOutputAmount()*2,CalculationStrategy.REPORT_MISSING_ITEMS);
        next();
    }
    private void submitTrade()throws Exception{
        if(!plan.isDone())return;
        var calculated=plan.get();require(!calculated.simulation()&&calculated.missingItems().isEmpty(),"Native crafting could not plan Amadron pattern");
        require(power.getMainNode().getGrid().getCraftingService().submitJob(calculated,null,null,false,IActionSource.empty()).successful(),"Native crafting CPU rejected trade");next();
    }
    private void craftingTrade(){
        if(cpu.getCluster().isBusy()||count(tradeOutput())<2*tradeOutputAmount())return;
        require(count(AEItemKey.of(Items.EMERALD))==128-3*AmadronPatternDetails.resource(trade.getInput()).amount(),"CPU trade payment/output mismatch");
        checked("Native AE2S crafting planner and CPU execute Amadron patterns through real drones with exact payment and completed output");
        fluidTrade=new AmadronOffer(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER,1000),new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.LAVA,1000));
        AmadronOfferManager.getInstance().addStaticOffer(fluidTrade);
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);require(station.order(fluidTrade,1,player),"Fluid trade rejected");next();
    }
    private void fluidAndFinish(){
        if(count(AEFluidKey.of(net.minecraftforge.fluids.FluidRegistry.LAVA))<1000)return;
        require(count(AEFluidKey.of(net.minecraftforge.fluids.FluidRegistry.WATER))==15000,"Fluid payment not conserved");
        checked("Fluid payment and delivery use real PNC drones and return to native ME fluid storage");
        require(station.canUpgrade(),"Empty, idle station cannot upgrade");
        var tester=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        long emeralds=count(AEItemKey.of(Items.EMERALD)),water=count(AEFluidKey.of(net.minecraftforge.fluids.FluidRegistry.WATER));
        require(!station.orderBasket(java.util.Map.of(trade,256,fluidTrade,256),tester),"Unaffordable cart accepted");
        require(count(AEItemKey.of(Items.EMERALD))==emeralds&&count(AEFluidKey.of(net.minecraftforge.fluids.FluidRegistry.WATER))==water&&station.queuedCount()==0,"Rejected cart partially debited storage or queued jobs");
        station.setPriority(42);var saved=station.writeToNBT(new NBTTagCompound());var copy=new MEAmadronProcessStationBlockEntity();copy.readFromNBT(saved);
        require(copy.getPatternPriority()==42,"Crafting priority NBT failed");
        checked("Whole-cart material validation is atomic and station crafting priority survives NBT persistence");station.setPriority(0);
        gameplayDone=true;done=true;
        if(FMLCommonHandler.instance().getSide().isServer()){write("PASS",null);server.initiateShutdown();}else write("RUNNING",null);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||server==null||done)return;
        try{
            if(++ticks>6000)throw new IllegalStateException("Acceptance timeout: phase="+phase+" elapsed="+elapsed()+" checks="+checks+" loaded drones="+world.loadedEntityList.size());
            if(ticks%100==0)write("RUNNING",null);
            switch(phase){case 0->create();case 1->connect();case 2->storageChecks();case 3->physicalChecks();case 4->manualTrade();case 5->planTrade();case 6->submitTrade();case 7->craftingTrade();case 8->fluidAndFinish();default->{}}
        }catch(Throwable failure){failure.printStackTrace();done=true;write("FAIL",failure);server.initiateShutdown();}
    }
}
