package com.wintercogs.appliedpneumatics.common.me.p2p;

import ae2.api.parts.*;
import ae2.items.parts.PartModels;
import ae2.parts.p2p.*;
import ae2.util.Platform;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import me.desht.pneumaticcraft.api.tileentity.IHeatExchanger;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicTicking;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import java.util.*;

public final class HeatP2PTunnelPart extends P2PTunnelPart<HeatP2PTunnelPart> {
    private static final P2PModels MODELS = new P2PModels(com.wintercogs.appliedpneumatics.common.init.APContent.renderId("part/p2p/p2p_tunnel_heat"));
    private final IHeatExchangerLogic adapter = new Adapter();
    private boolean querying;
    public HeatP2PTunnelPart(IPartItem<?> item) { super(item); }
    @PartModels public static List<IPartModel> getModels() { return MODELS.getModels(); }
    @Override public IPartModel getStaticModels() { return MODELS.getModel(isPowered(), isActive()); }
    public IHeatExchangerLogic getHeatHandler() { return adapter; }
    private List<IHeatExchangerLogic> targets() {
        if (querying || !isActive()) return List.of();
        querying = true;
        try {
            var result = new ArrayList<IHeatExchangerLogic>();
            for (var out : getOutputs()) {
                if (!out.isActive() || out.getTileEntity() == null) continue;
                var pos = out.getTileEntity().getPos().offset(out.getSide());
                if (!out.getLevel().isBlockLoaded(pos)) continue;
                var tile = out.getLevel().getTileEntity(pos);
                var logic = tile instanceof IHeatExchanger exchanger ? exchanger.getHeatExchangerLogic(out.getSide().getOpposite()) : null;
                if (logic != null && !(logic instanceof Adapter)) result.add(logic);
            }
            return result;
        } finally { querying = false; }
    }
    @Override public void onTunnelNetworkChange() { if (getTileEntity() != null) { Platform.notifyBlocksOfNeighbors(getLevel(), getTileEntity().getPos()); getHost().markForUpdate(); } }
    @Override public void onNeighborChanged(IBlockAccess world, BlockPos pos, BlockPos neighbor) { onTunnelNetworkChange(); }
    private final class Adapter extends HeatExchangerLogicTicking {
        @Override public void update() {}
        @Override public double getTemperature() {
            var targets = targets();
            double capacity = targets.stream().mapToDouble(IHeatExchangerLogic::getThermalCapacity).sum();
            return capacity <= 0 ? 300 : targets.stream().mapToDouble(h -> h.getTemperature() * h.getThermalCapacity()).sum() / capacity;
        }
        @Override public int getTemperatureAsInt() { return (int) getTemperature(); }
        @Override public double getThermalCapacity() { return Math.max(1, targets().stream().mapToDouble(IHeatExchangerLogic::getThermalCapacity).sum()); }
        @Override public double getThermalResistance() { return isOutput() || !isActive() ? Double.MAX_VALUE : 1; }
        @Override public void addHeat(double joules) {
            if (isOutput() || !isActive() || !Double.isFinite(joules)) return;
            var targets = targets();
            double capacity = targets.stream().mapToDouble(IHeatExchangerLogic::getThermalCapacity).sum();
            if (capacity <= 0) return;
            for (var h : targets) h.addHeat(joules * h.getThermalCapacity() / capacity);
            deductTransportCost((long) Math.abs(joules), AirKeyType.INSTANCE);
        }
        @Override public void setTemperature(double temperature) {}
    }
}
