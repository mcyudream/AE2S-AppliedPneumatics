package com.wintercogs.appliedpneumatics.common.blocks.entitis;

import ae2.api.config.Settings;
import ae2.api.networking.GridFlags;
import ae2.api.networking.security.IActionSource;
import ae2.api.storage.MEStorage;
import ae2.api.upgrades.*;
import ae2.tile.ServerTickingTile;
import ae2.tile.grid.AENetworkedTile;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayer;
import ae2.util.SettingsFrom;
import java.util.*;

public abstract class MachineTile extends AENetworkedTile implements ServerTickingTile, IUpgradeableObject {
    protected IUpgradeInventory upgrades;
    protected abstract Item machineItem();
    protected MachineTile() {
        getMainNode().setIdlePowerUsage(8).setFlags(GridFlags.REQUIRE_CHANNEL).setExposedOnSides(EnumSet.allOf(EnumFacing.class));
    }
    @Override public ItemStack getItemFromTile() { var item = machineItem(); return item == null ? ItemStack.EMPTY : new ItemStack(item); }
    protected int upgradeSlotCount(){return 5;}
    @Override public IUpgradeInventory getUpgrades() {
        if (upgrades == null) upgrades = UpgradeInventories.forMachine(machineItem(), upgradeSlotCount(), this::upgradesChanged);
        return upgrades;
    }
    protected void upgradesChanged() { saveChanges(); }
    public MEStorage network() { var grid = getMainNode().getGrid(); return grid == null ? null : grid.getStorageService().getInventory(); }
    protected IActionSource source() { return IActionSource.ofMachine(this); }
    public boolean mayUse(EntityPlayer player) {
        return player.capabilities.allowEdit && world != null && world.isBlockModifiable(player, pos);
    }
    public abstract NBTTagCompound guiState();
    public void setTarget(double target) {}
    @Override public void loadTag(NBTTagCompound tag) { super.loadTag(tag); getUpgrades().readFromNBT(tag, "upgrades"); }
    @Override public void saveAdditional(NBTTagCompound tag) { super.saveAdditional(tag); getUpgrades().writeToNBT(tag, "upgrades"); }
    @Override public void onReady() { super.onReady(); getMainNode().setVisualRepresentation(getItemFromTile()); upgradesChanged(); }
    @Override public void addAdditionalDrops(List<ItemStack> drops) { super.addAdditionalDrops(drops); getUpgrades().forEach(s -> { if (!s.isEmpty()) drops.add(s.copy()); }); }
    @Override public void clearContent() { super.clearContent(); getUpgrades().clear(); }
}
