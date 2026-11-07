package com.wintercogs.appliedpneumatics.common.me.keys;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import java.util.List;

/** One unit is one mL of PneumaticCraft air at atmospheric pressure. */
public final class AirKey extends AEKey {
    public static final AirKey INSTANCE = new AirKey();
    private static final ResourceLocation ID = new ResourceLocation("ae2s-appliedpneumatics", "air_key");
    private AirKey() {}
    @Override public AEKeyType getType() { return AirKeyType.INSTANCE; }
    @Override public AEKey dropSecondary() { return this; }
    @Override public NBTTagCompound toTag() { return new NBTTagCompound(); }
    @Override public Object getPrimaryKey() { return this; }
    @Override public ResourceLocation getId() { return ID; }
    @Override public void writeToPacket(PacketBuffer buffer) {}
    @Override protected ITextComponent computeDisplayName() { return AirKeyType.NAME; }
    @Override public void addDrops(long amount, List<ItemStack> drops, World world, BlockPos pos) {}
    @Override public Object getReadOnlyStack() { return null; }
    @Override public net.minecraft.nbt.NBTBase get(String id) { return null; }
    @Override public int hashCode() { return AirKey.class.hashCode(); }
    @Override public boolean equals(Object other) { return other instanceof AirKey; }
}
