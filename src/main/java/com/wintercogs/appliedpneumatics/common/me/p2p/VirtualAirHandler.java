package com.wintercogs.appliedpneumatics.common.me.p2p;

import me.desht.pneumaticcraft.api.tileentity.*;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;

abstract class VirtualAirHandler implements IAirHandler {
    @Override public void update() {}
    @Override public void readFromNBT(NBTTagCompound tag) {}
    @Override public void writeToNBT(NBTTagCompound tag) {}
    @Override public void validate(TileEntity tile) {}
    @Override public void onNeighborChange() {}
    @Override public void airLeak(EnumFacing side) {}
    @Override public List<Pair<EnumFacing, IAirHandler>> getConnectedPneumatics() { return List.of(); }
    @Override public void setDefaultVolume(int volume) {}
    @Override public float getMaxPressure() { return 50; }
    @Override public float getDangerPressure() { return 50; }
    @Override public float getCriticalPressure() { return 50; }
    @Override public void setUpgradeSlots(int... slots) {}
    @Override public int[] getUpgradeSlots() { return new int[0]; }
    @Override public World getWorld() { return null; }
    @Override public BlockPos getPos() { return BlockPos.ORIGIN; }
    @Override public void setWorld(World world) {}
    @Override public void setPos(BlockPos pos) {}
    @Override public void setPneumaticMachine(IPneumaticMachine machine) {}
    @Override public void setAirListener(IAirListener listener) {}
    @Override public void createConnection(IAirHandler handler) {}
    @Override public void removeConnection(IAirHandler handler) {}
    @Override public void printManometerMessage(EntityPlayer player, List<String> info) { info.add(String.format(Locale.ROOT,"%.2f bar",getPressure())); }
    @Override public Set<Item> getApplicableUpgrades() { return Set.of(); }
    @Override public String getName() { return "Applied Pneumatics P2P"; }
}
