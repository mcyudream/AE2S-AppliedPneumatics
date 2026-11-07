package com.wintercogs.appliedpneumatics.common.menu;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.ToolboxInventory;
import ae2.core.definitions.AEItems;
import ae2.core.gui.locator.GuiHostLocators;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.*;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.common.recipes.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.*;
import java.util.*;

/** Legacy transport for the upstream semantic menus. Offer indices always address a fixed snapshot. */
public final class ContainerAP extends AEBaseContainer {
    public final MachineTile machine;
    public final EntityPlayer player;
    public final EnumHand hand;
    public final ToolboxInventory toolbox;
    private final ItemStack terminal;
    private final IUpgradeInventory upgrades;
    private final ItemStackHandler patternInventory;
    private final List<AmadronOffer> offerSnapshot;
    private final long offersRevision = java.util.concurrent.ThreadLocalRandom.current().nextLong(1,Long.MAX_VALUE);
    private int ticks;
    private String status="";
    public NBTTagCompound state=new NBTTagCompound();
    private NBTTagCompound lastSent;

    public ContainerAP(EntityPlayer player,MachineTile machine,EnumHand hand){
        super(player.inventory,machine);
        this.player=player;this.machine=machine;this.hand=hand;
        terminal=hand==null?ItemStack.EMPTY:player.getHeldItem(hand);
        upgrades=hand!=null&&terminal.getItem() instanceof AmadronWirelessTerminalItem item?item.getUpgrades(terminal):machine.getUpgrades();
        if(hand!=null)lockPlayerInventorySlot(hand==EnumHand.MAIN_HAND?player.inventory.currentItem:getOffhandPlayerInventorySlot(player.inventory.mainInventory.size(),player.inventory.armorInventory.size()));
        toolbox=new ToolboxInventory(this);
        patternInventory=new ItemStackHandler(2){
            @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&stack.getItem()==AEItems.BLANK_PATTERN.asItem();}
            @Override protected void onContentsChanged(int slot){if(!player.world.isRemote)AirAccess.tag(terminal).setTag("patterns",serializeNBT());}
        };
        if(hand!=null){
            if(terminal.hasTagCompound()&&terminal.getTagCompound().hasKey("patterns",10))patternInventory.deserializeNBT(terminal.getTagCompound().getCompoundTag("patterns"));
            addSlot(new SlotItemHandler(patternInventory,0,0,0),SlotSemantics.BLANK_PATTERN);
            addSlot(new SlotItemHandler(patternInventory,1,0,0){@Override public boolean isItemValid(ItemStack s){return false;}},SlotSemantics.ENCODED_PATTERN);
        }else{
            setLocator(GuiHostLocators.forTile(machine));
            if(machine instanceof MEPressureInterfaceBlockEntity pressure)addSlot(new SlotItemHandler(pressure.inventory,0,0,0),SlotSemantics.MACHINE_INPUT);
            if(machine instanceof MEAmadronProcessStationBlockEntity station){
                for(int i=0;i<station.patterns.getSlots();i++)addSlot(new SlotItemHandler(station.patterns,i,0,0),SlotSemantics.ENCODED_PATTERN);
                for(int i=0;i<9;i++)addSlot(new SlotItemHandler(station.buffer,i,0,0),SlotSemantics.MACHINE_INPUT);
                for(int i=9;i<18;i++)addSlot(new SlotItemHandler(station.buffer,i,0,0){@Override public boolean isItemValid(ItemStack s){return false;}},SlotSemantics.MACHINE_OUTPUT);
            }
        }
        setupUpgrades(upgrades);
        addPlayerInventorySlots(8,122);
        offerSnapshot=hand!=null&&!player.world.isRemote?List.copyOf(AmadronOfferManager.getInstance().getAllOffers()):List.of();
    }
    private NBTTagList cachedOffersTag;
    private List<AmadronOfferSnapshot> cachedOffers=List.of();
    public List<AmadronOfferSnapshot> getOfferSnapshot(){
        var tags=state.getTagList("offers",10);
        if(!tags.equals(cachedOffersTag)){
            cachedOffersTag=tags;var offers=new ArrayList<AmadronOfferSnapshot>();
            for(int i=0;i<tags.tagCount();i++){
                var tag=tags.getCompoundTagAt(i);
                offers.add(new AmadronOfferSnapshot(i,AmadronOffer.loadFromNBT(tag),tag.getInteger("_stock"),tag.getBoolean("_static"),tag.getBoolean("_player"),tag.getBoolean("_valid"),tag.getString("_supplier")));
            }
            cachedOffers=List.copyOf(offers);
        }
        return cachedOffers;
    }
    public void sendSavePatternAction(int index){APNetwork.CHANNEL.sendToServer(new APNetwork.Action(windowId,2,index,0,Long.toString(state.getLong("offers_revision"))));}
    public void sendSubmitOrderAction(Map<Integer,Integer> entries){APNetwork.CHANNEL.sendToServer(new APNetwork.Basket(windowId,state.getLong("offers_revision"),entries));}
    public IUpgradeInventory getUpgrades(){return upgrades;}
    public ItemStackHandler getPatternInventory(){return patternInventory;}
    public boolean isWireless(){return hand!=null;}
    @Override public boolean canInteractWith(EntityPlayer p){
        if(p.world.isRemote)return true;
        if(machine==null||machine.isInvalid()||!machine.mayUse(p))return false;
        if(hand==null)return p.world==machine.getWorld()&&p.getDistanceSq(machine.getPos())<=64;
        return p.getHeldItem(hand)==terminal&&terminal.getItem() instanceof AmadronWirelessTerminalItem item&&item.getAECurrentPower(terminal)>0
            &&AmadronWirelessTerminalItem.station(terminal)==machine;
    }
    @Override public void broadcastChanges(){
        if(!player.world.isRemote&&hand!=null&&terminal.getItem() instanceof AmadronWirelessTerminalItem item&&!item.consumeTick(terminal)){player.closeScreen();return;}
        toolbox.tick();super.broadcastChanges();
        if(!player.world.isRemote&&++ticks%5==0)sendState();
    }
    private void sendState(){
        if(!(player instanceof EntityPlayerMP mp)||machine==null)return;
        var next=machine.guiState();next.setBoolean("remote",hand!=null);next.setString("status",status);
        if(hand!=null){
            var offers=new NBTTagList();var manager=AmadronOfferManager.getInstance();
            for(var selected:offerSnapshot){
                var offer=manager.get(selected);var tag=new NBTTagCompound();selected.writeToNBT(tag);
                tag.setBoolean("_valid",offer!=null);tag.setInteger("_stock",offer==null?0:offer.getStock());
                tag.setBoolean("_static",offer!=null&&manager.getStaticOffers().contains(offer)&&!(offer instanceof AmadronOfferCustom));
                tag.setBoolean("_player",offer instanceof AmadronOfferCustom);tag.setString("_supplier",selected.getVendor());offers.appendTag(tag);
            }
            next.setTag("offers",offers);next.setLong("offers_revision",offersRevision);
        }
        state=next;
        if(!next.equals(lastSent)){lastSent=next.copy();APNetwork.CHANNEL.sendTo(new APNetwork.State(windowId,next),mp);}
    }
    public void action(APNetwork.Action action){
        switch(action.operation){
            case 0 -> {if(hand==null)try{machine.setTarget(Double.parseDouble(action.text));}catch(NumberFormatException ignored){}}
            case 2 -> {if(hand!=null&&Long.toString(offersRevision).equals(action.text))encode(action.index);}
            case 5 -> {if(hand==null&&machine instanceof MEAmadronProcessStationBlockEntity station)station.cancelQueued();}
            case 6 -> {
                if(hand!=null)return;
                double delta;
                try{delta=Double.parseDouble(action.text);}catch(NumberFormatException ignored){return;}
                if(machine instanceof MEPressureInterfaceBlockEntity pressure&&Math.abs(delta)<=5)pressure.setTarget(pressure.getExpectedPressure()+delta);
                if(machine instanceof METemperatureInterfaceBlockEntity heat&&Math.abs(delta)<=100)heat.setTarget(heat.guiState().getDouble("target")+delta);
            }
            default -> {}
        }
        sendState();
    }
    public void submit(APNetwork.Basket action){
        if(hand==null||action.revision!=offersRevision||!(machine instanceof MEAmadronProcessStationBlockEntity station))return;
        var basket=new LinkedHashMap<AmadronOffer,Integer>();
        for(int i=0;i<action.indices.length;i++){
            int index=action.indices[i],count=action.counts[i];
            if(index<0||index>=offerSnapshot.size()||count<1||count>256||basket.put(offerSnapshot.get(index),count)!=null)return;
        }
        status=station.orderBasket(basket,player)?"ae2s-appliedpneumatics.ui.order_ok":"ae2s-appliedpneumatics.ui.order_failed";
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation(status),false);
        sendState();
    }
    private void encode(int index){
        if(index<0||index>=offerSnapshot.size())return;
        var offer=AmadronOfferManager.getInstance().get(offerSnapshot.get(index));
        if(offer==null||offer.getStock()!=-1||offer instanceof AmadronOfferCustom||!AmadronOfferManager.getInstance().getStaticOffers().contains(offer)){status="ae2s-appliedpneumatics.ui.encode_limited";return;}
        if(!patternInventory.getStackInSlot(1).isEmpty())return;
        if(patternInventory.getStackInSlot(0).getItem()!=AEItems.BLANK_PATTERN.asItem()){status="ae2s-appliedpneumatics.ui.need_blank";return;}
        var encoded=new ItemStack(APContent.AMADRON_PATTERN);AmadronPatternDetails.encode(encoded,offer);
        patternInventory.extractItem(0,1,false);patternInventory.setStackInSlot(1,encoded);status="ae2s-appliedpneumatics.ui.encoded";
    }
}
