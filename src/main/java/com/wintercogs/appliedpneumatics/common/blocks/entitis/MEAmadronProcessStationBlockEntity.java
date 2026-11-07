package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import ae2.api.config.Actionable;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import ae2.helpers.IPriorityHost;
import ae2.container.ISubGui;
import ae2.util.SettingsFrom;
import net.minecraftforge.items.wrapper.RangedWrapper;
import ae2.api.crafting.*;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.*;
import ae2.core.definitions.AEItems;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import me.desht.pneumaticcraft.common.recipes.*;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadron;
import me.desht.pneumaticcraft.common.item.ItemAmadronTablet;
import me.desht.pneumaticcraft.common.item.Itemss;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import java.util.*;

/** Real PNC retrieval/delivery drones handle payment and output. Queues and reservations survive saves. */
public class MEAmadronProcessStationBlockEntity extends MachineTile implements ICraftingProvider, IPriorityHost {
    private int priority;
    private final int patternSlots;
    public final ItemStackHandler patterns;
    private final List<Order> queued = new ArrayList<>();
    private final KeyCounter reserved = new KeyCounter();
    private boolean loading;
    public final ItemStackHandler buffer = new ItemStackHandler(18) {
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            var result = super.extractItem(slot, count, simulate);
            if (!simulate && !result.isEmpty()) {
                var key = AEItemKey.of(result); reserved.set(key, Math.max(0,reserved.get(key)-result.getCount()));
            }
            return result;
        }
        @Override protected void onContentsChanged(int slot) { saveChanges(); }
    };
    // Collection drones extract payments; delivery drones insert only into the output grid.
    private final IItemHandler deliveryAccess=new RangedWrapper(buffer,0,18){
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return slot<9?stack:super.insertItem(slot,stack,simulate);}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot>=9&&super.isItemValid(slot,stack);}
    };
    public final FluidTank tank = new FluidTank(64000) {
        @Override protected void onContentsChanged() { saveChanges(); }
        @Override public FluidStack drain(int amount, boolean doDrain) {
            var result = super.drain(amount, doDrain);
            if (doDrain && result != null) { var key = AEFluidKey.of(result); reserved.set(key, Math.max(0,reserved.get(key)-result.amount)); }
            return result;
        }
    };
    private record Order(AmadronOffer offer, int count, String player) {}
    public MEAmadronProcessStationBlockEntity() { this(9); }
    protected MEAmadronProcessStationBlockEntity(int slots) {
        patternSlots = slots;
        patterns = new ItemStackHandler(slots) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.getItem() == APContent.AMADRON_PATTERN; }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override protected void onContentsChanged(int slot) { if (!loading) { saveChanges(); ICraftingProvider.requestUpdate(getMainNode()); } }
        };
        getMainNode().addService(ICraftingProvider.class, this);
    }
    @Override protected int upgradeSlotCount(){return 4;}
    @Override protected Item machineItem() { return Item.getItemFromBlock(patternSlots > 9 ? APContent.EXTENDED_STATION : APContent.STATION); }
    public boolean skyClear() { return world != null && world.canSeeSky(pos.up()); }
    @Override public boolean isBusy() { return !getMainNode().isActive() || !skyClear() || queuedCount() >= 512; }
    @Override public List<AmadronPatternDetails> getAvailablePatterns() {
        var result = new ArrayList<AmadronPatternDetails>();
        for (int i=0;i<patterns.getSlots();i++) {
            var p = PatternDetailsHelper.decodePattern(patterns.getStackInSlot(i), world);
            if (p instanceof AmadronPatternDetails pattern) result.add(pattern);
        }
        return List.copyOf(result);
    }
    @Override public boolean pushPattern(IPatternDetails details, KeyCounter[] holders, int multiplier) {
        if (!(details instanceof AmadronPatternDetails pattern) || multiplier != 1 || isBusy() || !getAvailablePatterns().contains(pattern)) return false;
        var actual = new KeyCounter(); for (var holder : holders) actual.addAll(holder);
        var expected = pattern.input();
        if (actual.size() != 1 || actual.get(expected.what()) != expected.amount()) return false;
        if (!stage(expected.what(), expected.amount(), true)) return false;
        stage(expected.what(), expected.amount(), false);
        reserved.add(expected.what(), expected.amount());
        queued.add(new Order(pattern.offer(), 1, "AE2S")); saveChanges(); return true;
    }
    private boolean stage(AEKey key, long count, boolean simulate) {
        if (count < 1 || count > Integer.MAX_VALUE) return false;
        if (key instanceof AEItemKey item) return ItemHandlerHelper.insertItemStacked(new RangedWrapper(buffer,0,9), item.toStack((int) count), simulate).isEmpty();
        if (key instanceof AEFluidKey fluid) return tank.fill(fluid.toStack((int) count), !simulate) == count;
        return false;
    }
    public boolean order(AmadronOffer selected,int times,EntityPlayer player){return orderBasket(Map.of(selected,times),player);}
    /** Validate the entire cart, including shared buffer space, before debiting ME storage. */
    public boolean orderBasket(Map<AmadronOffer,Integer> basket,EntityPlayer player){
        if(basket.isEmpty()||isBusy()||!mayUse(player)||network()==null)return false;
        var manager=AmadronOfferManager.getInstance();var orders=new ArrayList<Order>();var needed=new KeyCounter();int units=0;
        for(var entry:basket.entrySet()){
            var offer=manager.get(entry.getKey());int times=entry.getValue();
            if(offer==null||times<1||times>256||(offer.getStock()>=0&&times>offer.getStock()))return false;
            units+=times;if(units+queuedCount()>512)return false;
            var input=AmadronPatternDetails.resource(offer.getInput());needed.add(input.what(),input.amount()*times);
            orders.add(new Order(offer,times,player.getName()));
        }
        var trial=new ItemStackHandler();trial.deserializeNBT(buffer.serializeNBT());
        var trialTank=new FluidTank(64000);trialTank.readFromNBT(tank.writeToNBT(new NBTTagCompound()));
        var src=ae2.api.networking.security.IActionSource.ofPlayer(player,this);
        for(var entry:needed){
            var key=entry.getKey();long amount=entry.getLongValue();
            if(amount<1||amount>Integer.MAX_VALUE||network().extract(key,amount,Actionable.SIMULATE,src)<amount)return false;
            if(key instanceof AEItemKey item&&!ItemHandlerHelper.insertItemStacked(new RangedWrapper(trial,0,9),item.toStack((int)amount),false).isEmpty())return false;
            if(key instanceof AEFluidKey fluid&&trialTank.fill(fluid.toStack((int)amount),true)!=amount)return false;
        }
        var pulled=new KeyCounter();
        for(var entry:needed){
            long got=network().extract(entry.getKey(),entry.getLongValue(),Actionable.MODULATE,src);pulled.add(entry.getKey(),got);
            if(got!=entry.getLongValue()){
                for(var refund:pulled){long returned=network().insert(refund.getKey(),refund.getLongValue(),Actionable.MODULATE,src);if(returned<refund.getLongValue())stage(refund.getKey(),refund.getLongValue()-returned,false);}
                return false;
            }
        }
        for(var entry:needed){stage(entry.getKey(),entry.getLongValue(),false);reserved.add(entry.getKey(),entry.getLongValue());}
        queued.addAll(orders);saveChanges();return true;
    }
    public int queuedCount(){return queued.stream().mapToInt(Order::count).sum();}
    @Override public int getPriority(){return priority;}
    @Override public int getPatternPriority(){return priority;}
    @Override public void setPriority(int value){priority=value;saveChanges();ICraftingProvider.requestUpdate(getMainNode());}
    @Override public void returnToMainContainer(EntityPlayer player,ISubGui subGui){
        if(!world.isRemote&&!isInvalid()&&mayUse(player))player.openGui(AppliedPneumatics.INSTANCE,0,world,pos.getX(),pos.getY(),pos.getZ());
    }
    @Override public ItemStack getMainContainerIcon(){return getItemFromTile();}
    @Override public void exportSettings(SettingsFrom mode,NBTTagCompound tag){super.exportSettings(mode,tag);tag.setInteger("ap_priority",priority);}
    @Override public void importSettings(SettingsFrom mode,NBTTagCompound tag,EntityPlayer player){super.importSettings(mode,tag,player);if(tag.hasKey("ap_priority"))setPriority(tag.getInteger("ap_priority"));}
    public boolean canUpgrade() { reserved.removeZeros(); return patternSlots == 9 && queued.isEmpty() && reserved.isEmpty() && tank.getFluidAmount() == 0 && java.util.stream.IntStream.range(0,buffer.getSlots()).allMatch(i -> buffer.getStackInSlot(i).isEmpty()); }
    public void cancelQueued() {
        for(var order:queued){var input=AmadronPatternDetails.resource(order.offer().getInput());reserved.set(input.what(),Math.max(0,reserved.get(input.what())-input.amount()*order.count()));}
        queued.clear();saveChanges();
    }
    @Override public void serverTick() {
        if (!getMainNode().isActive()) return;
        returnOutputs();
        int interval = Math.max(10,200 >> getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD.asItem()));
        if (world.getTotalWorldTime() % interval != 0 || queued.isEmpty() || !skyClear()) return;
        var order = queued.getFirst();
        // PNC's vanilla tablet data directs both delivery formats back to this station.
        var tablet = new ItemStack(Itemss.AMADRON_TABLET);
        ItemAmadronTablet.setItemProvidingLocation(tablet, pos, world.provider.getDimension());
        ItemAmadronTablet.setLiquidProvidingLocation(tablet, pos, world.provider.getDimension());
        var drone = ContainerAmadron.retrieveOrderItems(order.offer(), order.count(), world, pos, world, pos);
        if (drone != null) { drone.setHandlingOffer(order.offer(), order.count(), tablet, order.player()); queued.removeFirst(); saveChanges(); }
    }
    public void returnOutputs() {
        var storage = network(); if (storage == null) return;
        // Reserved payments stay available to collection drones until they actually extract them.
        var leftReserved = new KeyCounter(); leftReserved.addAll(reserved);
        for (int i=0;i<buffer.getSlots();i++) {
            var stack = buffer.getStackInSlot(i); if (stack.isEmpty()) continue;
            var key = AEItemKey.of(stack);
            long hold = Math.min(stack.getCount(),leftReserved.get(key)); leftReserved.remove(key,hold);
            long returned = storage.insert(key,stack.getCount()-hold,Actionable.MODULATE,source());
            if (returned > 0) { var copy = stack.copy(); copy.shrink((int)returned); buffer.setStackInSlot(i,copy); }
        }
        var fluid = tank.getFluid();
        if (fluid != null) {
            var key = AEFluidKey.of(fluid); long count = Math.max(0,fluid.amount - reserved.get(key));
            int returned = (int) storage.insert(key,count,Actionable.MODULATE,source());
            if (returned > 0) { var copy = fluid.copy(); copy.amount -= returned; tank.setFluid(copy.amount == 0 ? null : copy); saveChanges(); }
        }
    }
    @Override public NBTTagCompound guiState() { var t=new NBTTagCompound(); t.setString("kind","amadron");t.setBoolean("active",getMainNode().isActive());t.setBoolean("sky",skyClear());t.setInteger("queued",queuedCount());return t; }
    @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) { return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap,side); }
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        if (cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(deliveryAccess);
        if (cap==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
        return super.getCapability(cap,side);
    }
    @Override public void saveAdditional(NBTTagCompound t) {
        super.saveAdditional(t);t.setInteger("priority",priority);t.setTag("patterns",patterns.serializeNBT());t.setTag("buffer",buffer.serializeNBT());t.setTag("tank",tank.writeToNBT(new NBTTagCompound()));
        var reservations = new NBTTagList(); reserved.forEach((key,n) -> {if(n>0){var a=key.toTagGeneric();a.setLong("amount",n);reservations.appendTag(a);}});t.setTag("reservations",reservations);
        var orders=new NBTTagList();for(var order:queued){var a=new NBTTagCompound();order.offer().writeToNBT(a);a.setInteger("count",order.count());a.setString("player",order.player());orders.appendTag(a);}t.setTag("orders",orders);
    }
    @Override public void loadTag(NBTTagCompound t) {
        super.loadTag(t);priority=t.getInteger("priority");loading=true;try{patterns.deserializeNBT(t.getCompoundTag("patterns"));buffer.deserializeNBT(t.getCompoundTag("buffer"));tank.readFromNBT(t.getCompoundTag("tank"));}finally{loading=false;}
        reserved.clear();var list=t.getTagList("reservations",10);for(int i=0;i<list.tagCount();i++){var a=list.getCompoundTagAt(i);var key=AEKey.fromTagGeneric(a);if(key!=null&&a.getLong("amount")>0)reserved.add(key,a.getLong("amount"));}
        queued.clear();list=t.getTagList("orders",10);for(int i=0;i<Math.min(512,list.tagCount());i++){var a=list.getCompoundTagAt(i);var offer=AmadronOffer.loadFromNBT(a);if(offer!=null&&a.getInteger("count")>0)queued.add(new Order(offer,Math.min(256,a.getInteger("count")),a.getString("player")));}
    }
    @Override public void addAdditionalDrops(List<ItemStack> drops) { super.addAdditionalDrops(drops);for(int i=0;i<patterns.getSlots();i++)if(!patterns.getStackInSlot(i).isEmpty())drops.add(patterns.getStackInSlot(i).copy());for(int i=0;i<buffer.getSlots();i++)if(!buffer.getStackInSlot(i).isEmpty())drops.add(buffer.getStackInSlot(i).copy()); }
    @Override public void clearContent() {super.clearContent();for(int i=0;i<patterns.getSlots();i++)patterns.setStackInSlot(i,ItemStack.EMPTY);for(int i=0;i<buffer.getSlots();i++)buffer.setStackInSlot(i,ItemStack.EMPTY);queued.clear();reserved.clear();tank.setFluid(null);}
    public static class Extended extends MEAmadronProcessStationBlockEntity { public Extended() { super(36); } }
}
