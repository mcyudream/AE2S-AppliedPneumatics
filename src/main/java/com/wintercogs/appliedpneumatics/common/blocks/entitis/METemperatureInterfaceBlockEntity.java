package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import ae2.api.config.*;
import ae2.core.definitions.AEItems;
import ae2.util.SettingsFrom;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicTicking;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayer;

public class METemperatureInterfaceBlockEntity extends MachineTile implements IHeatExchanger {
    private final IHeatExchangerLogic heat = new HeatExchangerLogicTicking();
    private double target = 300;
    @Override protected Item machineItem() { return Item.getItemFromBlock(APContent.TEMPERATURE); }
    @Override public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) { return heat; }
    @Override public void onReady() { super.onReady(); heat.initializeAsHull(world, pos, EnumFacing.values()); }
    @Override public void setTarget(double value) { if (Double.isFinite(value)) target = Math.max(0, Math.min(2273, value)); saveChanges(); }
    @Override protected void upgradesChanged() {
        super.upgradesChanged();
        if (heat != null) { heat.setThermalCapacity(1000d * (1 << getUpgrades().getInstalledUpgrades(APContent.VOLUME_CARD))); heat.setThermalResistance(1); }
    }
    @Override public void serverTick() {
        if (getMainNode().isActive()) interactWithME(); heat.update(); saveChanges();
        int temperatureState=heat.getTemperature()<173?1:heat.getTemperature()>473?2:0;
        var state=world.getBlockState(pos);
        if(state.getBlock()==APContent.TEMPERATURE&&state.getValue(com.wintercogs.appliedpneumatics.common.blocks.MachineBlock.TEMPERATURE_STATE)!=temperatureState)
            world.setBlockState(pos,state.withProperty(com.wintercogs.appliedpneumatics.common.blocks.MachineBlock.TEMPERATURE_STATE,temperatureState),2);
    }
    public void interactWithME() {
        var storage = network(); var grid = getMainNode().getGrid(); if (storage == null || grid == null) return;
        double difference = target - heat.getTemperature();
        double wanted = Math.min(Math.abs(difference), 1 << getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD.asItem())) * heat.getThermalCapacity();
        if (wanted <= 0) return;
        var energy = grid.getEnergyService();
        long air = storage.extract(AirKey.INSTANCE, (long) Math.ceil(wanted * 19.145), Actionable.SIMULATE, source());
        double power = energy.extractAEPower(wanted * 24.889, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        double heatToApply = Math.min(wanted, Math.min(air / 19.145, power / 24.889));
        if (heatToApply <= 0) return;
        long usedAir = storage.extract(AirKey.INSTANCE, (long) Math.ceil(heatToApply * 19.145), Actionable.MODULATE, source());
        double usedPower = energy.extractAEPower(heatToApply * 24.889, Actionable.MODULATE, PowerMultiplier.CONFIG);
        double actual = Math.min(heatToApply, Math.min(usedAir / 19.145, usedPower / 24.889));
        heat.addHeat(Math.signum(difference) * actual);
    }
    @Override public NBTTagCompound guiState() { var t = new NBTTagCompound(); t.setString("kind", "temperature"); t.setDouble("target", target); t.setDouble("heat_capacity",heat.getThermalCapacity()); t.setDouble("value", heat.getTemperature()); t.setBoolean("active", getMainNode().isActive()); return t; }
    @Override public void loadTag(NBTTagCompound t) { super.loadTag(t); heat.readFromNBT(t.getCompoundTag("heat")); if (t.hasKey("target")) target = t.getDouble("target"); }
    @Override public void saveAdditional(NBTTagCompound t) { super.saveAdditional(t); var h = new NBTTagCompound(); heat.writeToNBT(h); t.setTag("heat", h); t.setDouble("target", target); }
    @Override public void exportSettings(SettingsFrom mode, NBTTagCompound t) { super.exportSettings(mode, t); t.setDouble("ap_temperature", target); }
    @Override public void importSettings(SettingsFrom mode, NBTTagCompound t, EntityPlayer p) { super.importSettings(mode, t, p); if (t.hasKey("ap_temperature")) setTarget(t.getDouble("ap_temperature")); }
}
