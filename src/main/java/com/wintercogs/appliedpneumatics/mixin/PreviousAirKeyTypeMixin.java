package com.wintercogs.appliedpneumatics.mixin;

import ae2.api.stacks.AEKeyType;
import ae2.api.stacks.AEKeyTypesInternal;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve previous saved air-key IDs without registering a second visible resource type. */
@Mixin(value=AEKeyTypesInternal.class,remap=false)
public abstract class PreviousAirKeyTypeMixin {
    @Inject(method="get(Lnet/minecraft/util/ResourceLocation;)Lae2/api/stacks/AEKeyType;",at=@At("HEAD"),cancellable=true,remap=false)
    private static void ae2s$readPreviousAirType(ResourceLocation id,CallbackInfoReturnable<AEKeyType> result){
        if(id.getNamespace().equals("appliedpneumatics")&&id.getPath().equals("air_type"))result.setReturnValue(AirKeyType.INSTANCE);
    }
}
