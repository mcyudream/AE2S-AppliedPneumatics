package com.wintercogs.appliedpneumatics.common.me.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.*;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.common.recipes.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

public final class AmadronPatternDetails implements IPatternDetails {
    private final AEItemKey definition;
    private final AmadronOffer offer;
    private final GenericStack input, output;
    private final IInput[] inputs;
    public AmadronPatternDetails(AEItemKey definition) {
        this.definition = definition;
        var stack = definition.toStack();
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("ap_offer",10)) throw new IllegalArgumentException("Unencoded Amadron pattern");
        var encoded = AmadronOffer.loadFromNBT(stack.getTagCompound().getCompoundTag("ap_offer"));
        offer = AmadronOfferManager.getInstance().get(encoded);
        if (offer == null || offer.getStock() != -1 || offer instanceof AmadronOfferCustom) throw new IllegalArgumentException("Trade unavailable or limited");
        input = resource(offer.getInput()); output = resource(offer.getOutput());
        inputs = new IInput[]{new Input(input)};
    }
    public static GenericStack resource(Object resource) {
        if (resource instanceof ItemStack item) return new GenericStack(Objects.requireNonNull(AEItemKey.of(item)), item.getCount());
        if (resource instanceof FluidStack fluid) return new GenericStack(Objects.requireNonNull(AEFluidKey.of(fluid)), fluid.amount);
        throw new IllegalArgumentException("Invalid Amadron resource");
    }
    public static void encode(ItemStack stack, AmadronOffer offer) {
        if (offer == null || offer.getStock() != -1 || offer instanceof AmadronOfferCustom) throw new IllegalArgumentException("Only unlimited trades can be encoded");
        var tag = new NBTTagCompound(); offer.writeToNBT(tag); AirAccess.tag(stack).setTag("ap_offer", tag);
    }
    public AmadronOffer offer() { return offer; }
    public GenericStack input() { return input; }
    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs.clone(); }
    @Override public List<GenericStack> getOutputs() { return List.of(output); }
    @Override public boolean equals(Object other) { return other instanceof AmadronPatternDetails p && definition.equals(p.definition); }
    @Override public int hashCode() { return definition.hashCode(); }
    private record Input(GenericStack stack) implements IInput {
        @Override public GenericStack[] possibleInputs() { return new GenericStack[]{new GenericStack(stack.what(), 1)}; }
        @Override public long getMultiplier() { return stack.amount(); }
        @Override public boolean isValid(AEKey key, World world) { return stack.what().equals(key); }
        @Override public AEKey getRemainingKey(AEKey key) { return null; }
    }
}
