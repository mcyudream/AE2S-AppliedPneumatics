package com.wintercogs.appliedpneumatics.common.items;

import ae2.api.config.FuzzyMode;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.container.GuiIds;
import ae2.items.tools.powered.AbstractPortableCell;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;

/** A full cell is 20 bar; storage remains long even though the old PNC API uses int. */
public final class PortableAirStorageCell extends AbstractPortableCell implements IAirStorageCell, IPressurizable {
    private final long bytes;
    private final double drain;
    public PortableAirStorageCell(int kilobytes, double drain) {
        super(GuiIds.GuiKey.PORTABLE_ITEM_CELL, 20000, 0x80caff);
        this.bytes = kilobytes * 1024L; this.drain = drain;
        setMaxStackSize(1);
    }
    @Override public long getTotalBytes() { return bytes; }
    @Override public double getIdleDrain() { return drain; }
    @Override public IUpgradeInventory getUpgrades(ItemStack stack) { return UpgradeInventories.forItem(stack, 5, super::onUpgradesChanged); }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {}
    @Override public ResourceLocation getRecipeId() { return getRegistryName(); }
    @Override public double getChargeRate(ItemStack stack) { return 160; }
    @Override public float getPressure(ItemStack stack) { return (float) (AirCellInventory.getAir(stack) * 20d / getAirCapacity()); }
    @Override public void addAir(ItemStack stack, int amount) {
        long old = AirCellInventory.getAir(stack);
        AirCellInventory.setAir(stack, Math.max(0, Math.min(getAirCapacity(), old + (long) amount)));
    }
    @Override public float maxPressure(ItemStack stack) { return 20; }
    // PNC 1.12 computes pressure * volume in an int for both current and maximum air.
    // Keep that legacy view representable; the backing long air and true pressure are unchanged.
    @Override public int getVolume(ItemStack stack) { return (int) Math.min(Integer.MAX_VALUE / 20, getAirCapacity() / 20); }
    @Override public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (world.isRemote || world.getTotalWorldTime() % 5 != 0 || !(entity instanceof net.minecraft.entity.player.EntityPlayer player)
            || !getUpgrades(stack).isInstalled(APContent.CHARGING_CARD)) return;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            var target = player.inventory.getStackInSlot(i);
            if (target == stack || target.getItem() instanceof PortableAirStorageCell) continue;
            var pressure = IPressurizable.of(target);
            if (pressure == null || pressure.getPressure(target) >= getPressure(stack)) continue;
            long space = (long) (pressure.maxPressure(target) * pressure.getVolume(target)) - com.wintercogs.appliedpneumatics.util.AirAccess.itemAir(target);
            int moved = (int) Math.min(1000, Math.min(space, AirCellInventory.getAir(stack)));
            if (moved > 0) { pressure.addAir(target, moved); addAir(stack, -moved); player.inventory.markDirty(); }
        }
    }
}
