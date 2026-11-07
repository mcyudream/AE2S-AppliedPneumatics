package com.wintercogs.appliedpneumatics.common.me.storage;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.cells.*;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import java.util.IdentityHashMap;
import java.util.Map;

/** Storage cells must publish synchronous signed deltas for AE2S's aggregate cache. */
public final class AirCellInventory implements StorageCell, StorageCellStatistics {
    public static final String AIR_TAG = "ae2s-appliedpneumatics_air";
    private final ItemStack stack;
    private final IAirStorageCell type;
    private final ISaveProvider host;
    private final Map<MEStorageChangeListener,Object> listeners = new IdentityHashMap<>();

    public AirCellInventory(ItemStack stack, ISaveProvider host) {
        this.stack = stack;
        this.type = (IAirStorageCell) stack.getItem();
        this.host = host;
    }
    public static long getAir(ItemStack stack) {
        return stack.hasTagCompound() ? Math.max(0, stack.getTagCompound().getLong(AIR_TAG)) : 0;
    }
    public static void setAir(ItemStack stack, long amount) {
        AirAccess.tag(stack).setLong(AIR_TAG, Math.max(0, amount));
    }
    @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key != AirKey.INSTANCE || amount <= 0) return 0;
        long old = getAir(stack);
        long actual = Math.min(amount, Math.max(0, type.getAirCapacity() - old));
        boolean vacuum = type.getUpgrades(stack).isInstalled(APContent.VACUUM_CARD);
        if (!mode.isSimulate()) {
            if (actual > 0) change(old + actual, actual);
            if (amount > actual && !vacuum && !type.getUpgrades(stack).isInstalled(APContent.SECURITY_CARD)) {
                var tile = hostTile();
                if (tile != null) APContent.scheduleOverpressure(tile);
            }
        }
        return vacuum ? amount : actual;
    }
    @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key != AirKey.INSTANCE || amount <= 0) return 0;
        long old = getAir(stack);
        long actual = Math.min(old, amount);
        if (!mode.isSimulate() && actual > 0) change(old - actual, -actual);
        return actual;
    }
    private void change(long value, long delta) {
        setAir(stack, value); // Write before host callbacks and block drops.
        if (host != null) host.saveChanges();
        RuntimeException failure = null;
        for (var entry : new IdentityHashMap<>(listeners).entrySet()) {
            var listener = entry.getKey();
            if (listeners.get(listener) != entry.getValue()) continue;
            try {
                if (listener.isValid(entry.getValue())) listener.onStackChange(AirKey.INSTANCE, delta);
                else listeners.remove(listener);
            } catch (RuntimeException error) {
                listeners.remove(listener);
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
    private TileEntity hostTile() {
        if (host instanceof TileEntity tile) return tile;
        if (host instanceof ae2.parts.AEBasePart part) return part.getTileEntity();
        if (host != null) for (var field : host.getClass().getDeclaredFields()) {
            try {
                if (field.trySetAccessible() && field.get(host) instanceof TileEntity tile) return tile;
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }
    @Override public void addListener(MEStorageChangeListener listener, Object token) {
        if (listeners.containsKey(listener)) throw new IllegalStateException("Listener already registered");
        listeners.put(java.util.Objects.requireNonNull(listener), java.util.Objects.requireNonNull(token));
    }
    @Override public void removeListener(MEStorageChangeListener listener) { listeners.remove(listener); }
    @Override public void getAvailableStacks(KeyCounter out) { long air = getAir(stack); if (air > 0) out.add(AirKey.INSTANCE, air); }
    @Override public ITextComponent getDescription() { return new TextComponentString(stack.getDisplayName()); }
    @Override public boolean isPreferredStorageFor(AEKey key, IActionSource source) {
        return key == AirKey.INSTANCE && !type.getUpgrades(stack).isInstalled(APContent.VACUUM_CARD);
    }
    @Override public CellState getStatus() { return getAir(stack) == 0 ? CellState.EMPTY : getAir(stack) < type.getAirCapacity() ? CellState.NOT_EMPTY : CellState.FULL; }
    @Override public double getIdleDrain() { return type.getIdleDrain(); }
    @Override public boolean canFitInsideCell() { return getAir(stack) == 0; }
    @Override public void persist() {}
    @Override public long getTotalBytes() { return type.getTotalBytes(); }
    @Override public long getUsedBytes() { return (getAir(stack) + 249L) / 250L; }
    @Override public long getStoredTypes() { return getAir(stack) > 0 ? 1 : 0; }
    @Override public long getTotalTypes() { return 1; }
}
