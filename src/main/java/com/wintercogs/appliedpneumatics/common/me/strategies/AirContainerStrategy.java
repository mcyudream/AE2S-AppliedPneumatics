package com.wintercogs.appliedpneumatics.common.me.strategies;

import ae2.api.behaviors.ContainerItemStrategy;
import ae2.api.config.Actionable;
import ae2.api.stacks.GenericStack;
import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;

public final class AirContainerStrategy implements ContainerItemStrategy<AirKey, AirContainerStrategy.Context> {
    public record Context(EntityPlayer player, int slot) {
        public ItemStack stack() { return slot < 0 ? player.inventory.getItemStack() : player.inventory.getStackInSlot(slot); }
        public void replace(ItemStack copy) {
            var original = stack();
            if (original.getCount() == 1) {
                if (slot < 0) player.inventory.setItemStack(copy); else player.inventory.setInventorySlotContents(slot, copy);
            } else { original.shrink(1); player.inventory.placeItemBackInInventory(player.world, copy); }
            player.inventory.markDirty();
        }
    }
    @Override public GenericStack getContainedStack(ItemStack stack) {
        long amount = AirAccess.itemAir(stack);
        return amount > 0 ? new GenericStack(AirKey.INSTANCE, amount) : null;
    }
    @Override public Context findCarriedContext(EntityPlayer player, Container container) {
        return IPressurizable.of(player.inventory.getItemStack()) == null ? null : new Context(player, -1);
    }
    @Override public Context findPlayerSlotContext(EntityPlayer player, int slot) {
        return slot < 0 || slot >= player.inventory.getSizeInventory() || IPressurizable.of(player.inventory.getStackInSlot(slot)) == null ? null : new Context(player, slot);
    }
    @Override public long extract(Context context, AirKey key, long amount, Actionable mode) { return move(context, amount, mode, false); }
    @Override public long insert(Context context, AirKey key, long amount, Actionable mode) { return move(context, amount, mode, true); }
    private long move(Context context, long amount, Actionable mode, boolean insert) {
        if (amount <= 0) return 0;
        var original = context.stack();
        var pressure = IPressurizable.of(original);
        if (pressure == null) return 0;
        long old = AirAccess.itemAir(original);
        long capacity = original.getItem() instanceof PortableAirStorageCell cell ? cell.getAirCapacity()
            : (long) (pressure.maxPressure(original) * (double) pressure.getVolume(original));
        long actual = Math.min(Integer.MAX_VALUE, Math.min(amount, insert ? Math.max(0, capacity - old) : old));
        if (actual > 0 && !mode.isSimulate()) {
            var copy = original.copy(); copy.setCount(1);
            if (copy.getItem() instanceof PortableAirStorageCell) AirCellInventory.setAir(copy, old + (insert ? actual : -actual));
            else pressure.addAir(copy, (int) (insert ? actual : -actual));
            context.replace(copy);
        }
        return actual;
    }
    @Override public GenericStack getExtractableContent(Context context) { return getContainedStack(context.stack()); }
    @Override public void playFillSound(EntityPlayer player, AirKey key) { player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.2f, 1.4f); }
    @Override public void playEmptySound(EntityPlayer player, AirKey key) { playFillSound(player, key); }
}
