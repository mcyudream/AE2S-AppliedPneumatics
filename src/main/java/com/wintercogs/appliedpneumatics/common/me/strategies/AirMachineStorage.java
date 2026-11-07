package com.wintercogs.appliedpneumatics.common.me.strategies;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.*;
import ae2.api.storage.MEStorage;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.util.text.ITextComponent;

public final class AirMachineStorage implements MEStorage {
    private final IAirHandler handler;
    private final Runnable changed;
    public AirMachineStorage(IAirHandler handler, Runnable changed) { this.handler = handler; this.changed = changed; }
    @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key != AirKey.INSTANCE || amount <= 0) return 0;
        int moved = AirAccess.bounded(Math.min(amount, AirAccess.machineSpace(handler)));
        if (!mode.isSimulate() && moved > 0) { handler.addAir(moved); AirAccess.changed(handler); changed.run(); }
        return moved;
    }
    @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key != AirKey.INSTANCE || amount <= 0) return 0;
        int moved = AirAccess.bounded(Math.min(amount, Math.max(0, handler.getAir())));
        if (!mode.isSimulate() && moved > 0) { handler.addAir(-moved); AirAccess.changed(handler); changed.run(); }
        return moved;
    }
    @Override public void getAvailableStacks(KeyCounter counter) { if (handler.getAir() > 0) counter.add(AirKey.INSTANCE, handler.getAir()); }
    @Override public ITextComponent getDescription() { return AirKeyType.NAME; }
}
