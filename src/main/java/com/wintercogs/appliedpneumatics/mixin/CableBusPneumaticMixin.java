package com.wintercogs.appliedpneumatics.mixin;

import ae2.api.parts.IPartHost;
import com.wintercogs.appliedpneumatics.common.me.p2p.*;
import me.desht.pneumaticcraft.api.tileentity.*;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;

/** PNC 1.12 checks Java interfaces on tiles, rather than a part capability. */
@Mixin(value = ae2.tile.networking.TileCableBus.class, remap = false)
public abstract class CableBusPneumaticMixin implements IPneumaticMachine, IHeatExchanger {
    @Override public IAirHandler getAirHandler(EnumFacing side) {
        if (side == null) return null;
        var part = ((IPartHost) this).getPart(side);
        return part instanceof AirP2PTunnelPart tunnel ? tunnel.getAirHandler() : null;
    }
    @Override public IHeatExchangerLogic getHeatExchangerLogic(EnumFacing side) {
        if (side == null) return null;
        var part = ((IPartHost) this).getPart(side);
        return part instanceof HeatP2PTunnelPart tunnel ? tunnel.getHeatHandler() : null;
    }
}
