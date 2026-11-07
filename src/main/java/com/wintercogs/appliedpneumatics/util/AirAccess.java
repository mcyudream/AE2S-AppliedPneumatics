package com.wintercogs.appliedpneumatics.util;

import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IPneumaticMachine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AirAccess {
    private AirAccess() {}
    public static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }
    public static IAirHandler machine(World world, BlockPos pos, EnumFacing side) {
        if (!world.isBlockLoaded(pos)) return null;
        var tile = world.getTileEntity(pos);
        if (tile == null) return null;
        // PNC 1.12 discovers machines by interface, whereas modern PNC uses capabilities.
        if (tile instanceof IPneumaticMachine machine) return machine.getAirHandler(side);
        if (tile instanceof ae2.api.parts.IPartHost host) {
            var part = host.getPart(side);
            if (part instanceof com.wintercogs.appliedpneumatics.common.me.p2p.AirP2PTunnelPart tunnel) return tunnel.getAirHandler();
        }
        return null;
    }
    public static long itemAir(ItemStack stack) {
        if (stack.getItem() instanceof PortableAirStorageCell) return AirCellInventory.getAir(stack);
        var pressure = IPressurizable.of(stack);
        return pressure == null ? 0 : Math.max(0, Math.round((double) pressure.getPressure(stack) * pressure.getVolume(stack)));
    }
    public static int bounded(long amount) { return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, amount)); }
    public static int bounded(double amount) { return Double.isFinite(amount) ? (int) Math.max(0d, Math.min(Integer.MAX_VALUE, amount)) : 0; }
    public static void changed(IAirHandler handler) {
        var world=handler.getWorld();var pos=handler.getPos();
        if(world!=null&&!world.isRemote&&pos!=null&&world.isBlockLoaded(pos)){
            var tile=world.getTileEntity(pos);if(tile!=null)tile.markDirty();
        }
    }
    public static long machineSpace(IAirHandler handler) { return Math.max(0L, (long) (handler.getDangerPressure() * (double) handler.getVolume()) - handler.getAir()); }
}
