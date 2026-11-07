package com.wintercogs.appliedpneumatics.common.blocks;

import ae2.block.AEBaseTileBlock;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MachineTile;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MachineBlock extends AEBaseTileBlock<MachineTile> {
    public static final net.minecraft.block.properties.PropertyInteger TEMPERATURE_STATE=net.minecraft.block.properties.PropertyInteger.create("temperature",0,2);
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MachineBlock(Class<? extends MachineTile> tileClass) {
        super(Material.IRON); setHardness(3); setResistance(10); setHarvestLevel("pickaxe", 1);
        setTileEntity((Class) tileClass);
    }
    @Override protected net.minecraft.block.state.BlockStateContainer createBlockState(){
        return new net.minecraftforge.common.property.ExtendedBlockState(this,new net.minecraft.block.properties.IProperty<?>[]{TEMPERATURE_STATE},
            new net.minecraftforge.common.property.IUnlistedProperty<?>[]{FORWARD,UP});
    }
    @Override public int getMetaFromState(IBlockState state){return state.getValue(TEMPERATURE_STATE);}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(TEMPERATURE_STATE,Math.max(0,Math.min(2,meta)));}
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
        EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (player.getHeldItem(hand).getItem() instanceof AmadronWirelessTerminalItem) return false;
        if (super.onBlockActivated(world, pos, state, player, hand, side, x, y, z)) return true;
        if (!world.isRemote && !player.isSneaking()) player.openGui(AppliedPneumatics.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
    @Override public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
        super.neighborChanged(state, world, pos, block, from);
        if (world.getTileEntity(pos) instanceof MEPressureNotifier tile) tile.pressureNeighborsChanged();
    }
    public interface MEPressureNotifier { void pressureNeighborsChanged(); }
}
