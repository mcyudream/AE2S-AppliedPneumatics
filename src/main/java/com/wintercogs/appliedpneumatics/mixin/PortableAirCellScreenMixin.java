package com.wintercogs.appliedpneumatics.mixin;

import ae2.client.gui.me.common.GuiMEStorage;
import ae2.container.me.common.ContainerMEStorage;
import ae2.items.contents.PortableCellGuiHost;
import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AE2S's legacy style parser ignores maxRows; restore the upstream air-cell screen's three-row cap. */
@Mixin(value=GuiMEStorage.class,remap=false)
public abstract class PortableAirCellScreenMixin {
    @Inject(method="getConfiguredRows",at=@At("RETURN"),cancellable=true,remap=false)
    private void appliedpneumatics$limitAirCellRows(CallbackInfoReturnable<Integer> result){
        var screen=(GuiMEStorage<?>)(Object)this;
        if(screen.inventorySlots instanceof ContainerMEStorage menu&&menu.getTarget() instanceof PortableCellGuiHost<?> host
            &&host.getItemStack().getItem() instanceof PortableAirStorageCell)result.setReturnValue(Math.min(3,result.getReturnValue()));
    }
}
