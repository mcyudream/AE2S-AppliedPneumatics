package com.wintercogs.appliedpneumatics.common.items;

import ae2.api.config.FuzzyMode;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

public class AirStorageCell extends Item implements IAirStorageCell {
    private final long bytes;
    private final double drain;
    public AirStorageCell(int kilobytes, double drain) { setMaxStackSize(1); this.bytes = kilobytes * 1024L; this.drain = drain; }
    @Override public long getTotalBytes() { return bytes; }
    @Override public double getIdleDrain() { return drain; }
    @Override public IUpgradeInventory getUpgrades(ItemStack stack) { return UpgradeInventories.forItem(stack, 2); }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {}
    @Override public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World world,net.minecraft.entity.player.EntityPlayer player,net.minecraft.util.EnumHand hand){
        var stack=player.getHeldItem(hand);
        if(!player.isSneaking()||AirCellInventory.getAir(stack)>0)return new net.minecraft.util.ActionResult<>(net.minecraft.util.EnumActionResult.PASS,stack);
        var results=ae2.recipes.game.StorageCellDisassemblyRecipe.getDisassemblyResult(world,this);
        if(results.isEmpty())return new net.minecraft.util.ActionResult<>(net.minecraft.util.EnumActionResult.PASS,stack);
        if(!world.isRemote){
            var upgrades=getUpgrades(stack);player.setHeldItem(hand,ItemStack.EMPTY);
            for(var result:results)player.inventory.placeItemBackInInventory(world,result.copy());
            upgrades.forEach(upgrade->{if(!upgrade.isEmpty())player.inventory.placeItemBackInInventory(world,upgrade.copy());});
        }
        return new net.minecraft.util.ActionResult<>(net.minecraft.util.EnumActionResult.SUCCESS,player.getHeldItem(hand));
    }
    @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        lines.add(net.minecraft.client.resources.I18n.format("ae2s-appliedpneumatics.ui.air_amount", AirCellInventory.getAir(stack), getAirCapacity()));
        lines.add(net.minecraft.client.resources.I18n.format("ae2s-appliedpneumatics.ui.cell_warning"));
    }
}
