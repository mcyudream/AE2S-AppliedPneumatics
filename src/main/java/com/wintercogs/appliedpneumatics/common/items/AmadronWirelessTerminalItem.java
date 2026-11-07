package com.wintercogs.appliedpneumatics.common.items;

import ae2.api.config.Actionable;
import ae2.api.upgrades.*;
import ae2.items.tools.powered.powersink.AEBasePoweredItem;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public final class AmadronWirelessTerminalItem extends AEBasePoweredItem implements IUpgradeableItem {
    public AmadronWirelessTerminalItem() { super(1600000);setMaxStackSize(1); }
    @Override public IUpgradeInventory getUpgrades(ItemStack stack) { return UpgradeInventories.forItem(stack,2,(changed,upgrades)->setAEMaxPower(changed,1600000d*(1+8*Upgrades.getEnergyCardMultiplier(upgrades)))); }
    @Override public double getChargeRate(ItemStack stack){return 800d*(1+Upgrades.getEnergyCardMultiplier(getUpgrades(stack)));}
    @Override public EnumActionResult onItemUse(EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z) {
        if (!player.isSneaking() || !(world.getTileEntity(pos) instanceof MEAmadronProcessStationBlockEntity station)) return EnumActionResult.PASS;
        if (!world.isRemote && station.mayUse(player)) {
            var stack=player.getHeldItem(hand);var t=AirAccess.tag(stack);
            if(t.hasKey("station")&&t.getLong("station")==pos.toLong()&&t.getInteger("dimension")==world.provider.getDimension())t.removeTag("station");
            else {t.setLong("station",pos.toLong());t.setInteger("dimension",world.provider.getDimension());}
            player.sendStatusMessage(new TextComponentTranslation(t.hasKey("station")?"ae2s-appliedpneumatics.ui.linked":"ae2s-appliedpneumatics.ui.unlinked"),true);
        }
        return EnumActionResult.SUCCESS;
    }
    public static MEAmadronProcessStationBlockEntity station(ItemStack stack) {
        if(!stack.hasTagCompound()||!stack.getTagCompound().hasKey("station"))return null;
        var t=stack.getTagCompound();var world=DimensionManager.getWorld(t.getInteger("dimension"));var pos=BlockPos.fromLong(t.getLong("station"));
        return world!=null&&world.isBlockLoaded(pos)&&world.getTileEntity(pos) instanceof MEAmadronProcessStationBlockEntity station ? station:null;
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world,EntityPlayer player,EnumHand hand) {
        var stack=player.getHeldItem(hand);
        if(!world.isRemote) {
            var station=station(stack);
            if(station!=null&&station.mayUse(player)&&getAECurrentPower(stack)>=1)player.openGui(AppliedPneumatics.INSTANCE,1,world,hand.ordinal(),0,0);
            else player.sendStatusMessage(new TextComponentTranslation("ae2s-appliedpneumatics.ui.terminal_unavailable"),true);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS,stack);
    }
    public boolean consumeTick(ItemStack stack) { return extractAEPower(stack,1,Actionable.MODULATE)>=1; }
}
