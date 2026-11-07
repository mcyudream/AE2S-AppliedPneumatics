package com.wintercogs.appliedpneumatics.common.me.keys.types;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public final class AirKeyType extends AEKeyType {
    public static final ITextComponent NAME = new TextComponentTranslation("ae2s-appliedpneumatics.me.key.air");
    public static final AirKeyType INSTANCE = new AirKeyType();
    private AirKeyType() { super(new ResourceLocation("ae2s-appliedpneumatics", "air_type"), AirKey.class, NAME); }
    @Override public AEKey readFromPacket(PacketBuffer buffer) { return AirKey.INSTANCE; }
    @Override public AEKey loadKeyFromTag(NBTTagCompound tag) { return AirKey.INSTANCE; }
    @Override public int getAmountPerOperation() { return 1000; }
    @Override public int getAmountPerUnit() { return 1000; }
    @Override public int getAmountPerByte() { return 250; }
    @Override public String getUnitSymbol() { return "L"; }
}
