package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import ae2.api.config.*;
import com.wintercogs.appliedpneumatics.common.blocks.MachineBlock;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.api.tileentity.*;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.common.pressure.AirHandlerSupplier;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.*;
import java.util.List;
import ae2.util.SettingsFrom;

public class MEPressureInterfaceBlockEntity extends MachineTile implements IPneumaticMachine, MachineBlock.MEPressureNotifier {
    private final IAirHandler air = AirHandlerSupplier.getInstance().createTierTwoAirHandler(1000);
    private double target = 2;
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return IPressurizable.of(stack) != null; }
        @Override protected void onContentsChanged(int slot) { saveChanges(); }
    };
    @Override protected Item machineItem() { return Item.getItemFromBlock(APContent.PRESSURE); }
    @Override public IAirHandler getAirHandler(EnumFacing side) { return air; }
    @Override public void onReady() { super.onReady(); air.validate(this); }
    @Override protected void upgradesChanged() {
        super.upgradesChanged();
        if (air == null) return;
        setTarget(target);
        air.setDefaultVolume(1000 * (1 << (2 * getUpgrades().getInstalledUpgrades(APContent.VOLUME_CARD))));
    }
    @Override public void setTarget(double value) {
        if (Double.isFinite(value)) target = Math.max(getUpgrades().isInstalled(APContent.VACUUM_CARD) ? -1 : 0, Math.min(20, value));
        saveChanges();
    }
    public double getExpectedPressure() { return target; }
    @Override public void pressureNeighborsChanged() { air.onNeighborChange(); }
    @Override public void serverTick() {
        // AE2S schedules node readiness separately from the tile's first server tick.
        if (air.getWorld() != world || !pos.equals(air.getPos())) air.validate(this);
        if (getMainNode().isActive()) interactWithME();
        var stack = inventory.getStackInSlot(0);
        var item = IPressurizable.of(stack);
        if (item != null && !stack.isEmpty()) {
            double difference = air.getPressure() - item.getPressure(stack);
            int move;
            if (difference > .01) {
                long max = stack.getItem() instanceof com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell cell ? cell.getAirCapacity()
                    : (long) (item.maxPressure(stack) * (double) item.getVolume(stack));
                move = AirAccess.bounded(Math.min(1000, Math.min(air.getAir(), Math.min(max - AirAccess.itemAir(stack), difference / 2 * item.getVolume(stack)))));
                if (move > 0) { item.addAir(stack, move); air.addAir(-move); }
            } else if (difference < -.01) {
                move = AirAccess.bounded(Math.min(1000, Math.min(AirAccess.itemAir(stack), -difference / 2 * air.getVolume())));
                if (move > 0) { item.addAir(stack, -move); air.addAir(move); }
            }
        }
        if (getUpgrades().isInstalled(APContent.SECURITY_CARD)) {
            int excess = air.getAir() - 20 * air.getVolume();
            if (excess > 0) air.addAir(-excess);
        }
        air.update();
        saveChanges();
    }
    public void interactWithME() {
        var storage = network(); var grid = getMainNode().getGrid();
        if (storage == null || grid == null) return;
        int wanted = (int) ((target - air.getPressure()) * air.getVolume());
        if (wanted == 0) return;
        var energy = grid.getEnergyService();
        double cost = wanted > 0 ? .125 : 1.25;
        int demand = Math.abs(wanted);
        long available = wanted > 0 ? storage.extract(AirKey.INSTANCE, demand, Actionable.SIMULATE, source())
            : storage.insert(AirKey.INSTANCE, demand, Actionable.SIMULATE, source());
        int planned = AirAccess.bounded(Math.min(available, energy.extractAEPower(demand * cost, Actionable.SIMULATE, PowerMultiplier.CONFIG) / cost));
        if (planned == 0) return;
        int powered = (int) Math.floor(energy.extractAEPower(planned * cost, Actionable.MODULATE, PowerMultiplier.CONFIG) / cost + 1e-7);
        long actual = wanted > 0 ? storage.extract(AirKey.INSTANCE, powered, Actionable.MODULATE, source())
            : storage.insert(AirKey.INSTANCE, powered, Actionable.MODULATE, source());
        if (actual > 0) air.addAir(wanted > 0 ? (int) actual : -(int) actual);
    }
    @Override public NBTTagCompound guiState() {
        var t = new NBTTagCompound(); t.setString("kind", "pressure"); t.setDouble("target", target);
        t.setInteger("air",air.getAir()); t.setDouble("danger",air.getDangerPressure()); t.setDouble("value", air.getPressure()); t.setInteger("volume", air.getVolume()); t.setBoolean("active", getMainNode().isActive()); return t;
    }
    @Override public void loadTag(NBTTagCompound t) { super.loadTag(t); air.readFromNBT(t.getCompoundTag("air")); inventory.deserializeNBT(t.getCompoundTag("inventory")); if (t.hasKey("target")) target = t.getDouble("target"); }
    @Override public void saveAdditional(NBTTagCompound t) { super.saveAdditional(t); var a = new NBTTagCompound(); air.writeToNBT(a); t.setTag("air", a); t.setTag("inventory", inventory.serializeNBT()); t.setDouble("target", target); }
    @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) { return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, side); }
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) { return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(cap, side); }
    @Override public void addAdditionalDrops(List<ItemStack> drops) { super.addAdditionalDrops(drops); if (!inventory.getStackInSlot(0).isEmpty()) drops.add(inventory.getStackInSlot(0).copy()); }
    @Override public void clearContent() { super.clearContent(); inventory.setStackInSlot(0, ItemStack.EMPTY); }
    @Override public void exportSettings(SettingsFrom mode, NBTTagCompound t) { super.exportSettings(mode, t); t.setDouble("ap_pressure", target); }
    @Override public void importSettings(SettingsFrom mode, NBTTagCompound t, EntityPlayer p) { super.importSettings(mode, t, p); if (t.hasKey("ap_pressure")) setTarget(t.getDouble("ap_pressure")); }
}
