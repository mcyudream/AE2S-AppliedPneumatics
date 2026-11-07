package com.wintercogs.appliedpneumatics.common.items;

import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import net.minecraft.item.Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NBTTagCompound;

public final class AmadronProcessUpgradeItem extends Item {
    @Override public EnumActionResult onItemUse(EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z) {
        if(!(world.getTileEntity(pos) instanceof MEAmadronProcessStationBlockEntity station)||!station.canUpgrade()||!station.mayUse(player))return EnumActionResult.PASS;
        if(!world.isRemote){
            var saved=station.writeToNBT(new NBTTagCompound());saved.getCompoundTag("patterns").setInteger("Size",36);
            station.clearContent();world.setBlockState(pos,APContent.EXTENDED_STATION.getDefaultState(),3);
            var upgraded=(MEAmadronProcessStationBlockEntity)world.getTileEntity(pos);upgraded.loadTag(saved);upgraded.saveChanges();
            if(!player.capabilities.isCreativeMode)player.getHeldItem(hand).shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
