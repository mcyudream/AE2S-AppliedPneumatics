package com.wintercogs.appliedpneumatics.common.me.p2p;

import ae2.api.parts.*;
import ae2.items.parts.PartModels;
import ae2.parts.p2p.*;
import ae2.util.Platform;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import java.util.*;

public final class AirP2PTunnelPart extends P2PTunnelPart<AirP2PTunnelPart> {
    private static final P2PModels MODELS = new P2PModels(com.wintercogs.appliedpneumatics.common.init.APContent.renderId("part/p2p/p2p_tunnel_air"));
    private boolean querying;
    private final IAirHandler input = new Input();
    private final IAirHandler output = new VirtualAirHandler() {
        @Override public void addAir(int amount) {}
        @Override public int getVolume() { return 1; }
        @Override public int getAir() { return 50; } // Legacy diffusion compares air/volume directly.
        @Override public float getPressure() { return 50; }
    };
    public AirP2PTunnelPart(IPartItem<?> item) { super(item); }
    @PartModels public static List<IPartModel> getModels() { return MODELS.getModels(); }
    @Override public IPartModel getStaticModels() { return MODELS.getModel(isPowered(), isActive()); }
    public IAirHandler getAirHandler() { return isOutput() ? output : input; }
    private List<IAirHandler> targets() {
        if (querying || !isActive()) return List.of();
        querying = true;
        try {
            var result = new ArrayList<IAirHandler>();
            for (var out : getOutputs()) {
                if (!out.isActive() || out.getTileEntity() == null) continue;
                var h = AirAccess.machine(out.getLevel(), out.getTileEntity().getPos().offset(out.getSide()), out.getSide().getOpposite());
                if (h != null && !(h instanceof VirtualAirHandler)) result.add(h);
            }
            return result;
        } finally { querying = false; }
    }
    @Override public void onTunnelNetworkChange() { if (getTileEntity() != null) { Platform.notifyBlocksOfNeighbors(getLevel(), getTileEntity().getPos()); getHost().markForUpdate(); } }
    @Override public void onNeighborChanged(IBlockAccess world, BlockPos pos, BlockPos neighbor) { onTunnelNetworkChange(); }
    private final class Input extends VirtualAirHandler {
        @Override public int getAir() {
            var targets = targets(); if (targets.isEmpty()) return 50;
            return AirAccess.bounded(targets.stream().mapToLong(h -> Math.max(0, h.getAir())).sum());
        }
        @Override public int getVolume() { return Math.max(1, AirAccess.bounded(targets().stream().mapToLong(IAirHandler::getVolume).sum())); }
        @Override public float getPressure() { return getAir() / (float) getVolume(); }
        @Override public float getDangerPressure() { return 20; }
        @Override public void addAir(int amount) {
            if (amount == 0 || !isActive() || getLevel() == null || getLevel().isRemote) return;
            var targets = targets();
            if(targets.isEmpty())return;
            long remaining = Math.abs((long) amount),moved=0;
            var source=AirAccess.machine(getLevel(),getTileEntity().getPos().offset(getSide()),getSide().getOpposite());
            double sourcePressure=source==null?50:source.getPressure();
            double[] weights=new double[targets.size()];double total=0;
            for(int i=0;i<targets.size();i++)total+=weights[i]=amount>0?Math.max(0,sourcePressure-targets.get(i).getPressure()):Math.max(0,targets.get(i).getAir());
            if(total<=0){Arrays.fill(weights,1);total=weights.length;}
            // PNC's void addAir contract cannot reject accepted air. Preserve every mL,
            // including overpressure, which PNC handles on the receiving machine's tick.
            for(int i=0;i<targets.size();i++){
                long share=i==targets.size()-1?remaining:Math.min(remaining,Math.round(Math.abs((long)amount)*weights[i]/total));
                if(amount<0)share=Math.min(share,Math.max(0,targets.get(i).getAir()));
                int n=AirAccess.bounded(share);if(n>0){targets.get(i).addAir(amount>0?n:-n);remaining-=n;moved+=n;}
            }
            if(amount<0&&remaining>0)for(var h:targets){int n=AirAccess.bounded(Math.min(remaining,Math.max(0,h.getAir())));if(n>0){h.addAir(-n);remaining-=n;moved+=n;}}
            if (moved > 0) deductTransportCost(moved, AirKeyType.INSTANCE);
        }
    }
}
