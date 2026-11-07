package com.wintercogs.appliedpneumatics.common.me.strategies;

import ae2.api.behaviors.*;
import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.util.AirAccess;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public final class AirBusStrategies {
    private AirBusStrategies() {}
    public static void register() {
        ExternalStorageStrategy.register(AirKeyType.INSTANCE, External::new);
        StackImportStrategy.register(AirKeyType.INSTANCE, (w,p,s) -> context -> importAir(w,p,s,context));
        StackExportStrategy.register(AirKeyType.INSTANCE, Export::new);
        ContainerItemStrategy.register(AirKeyType.INSTANCE, AirKey.class, new AirContainerStrategy());
    }
    private record External(WorldServer world, BlockPos pos, EnumFacing side) implements ExternalStorageStrategy {
        @Override public MEStorage createWrapper(boolean extractableOnly, Runnable changed) {
            var handler = AirAccess.machine(world, pos, side);
            return handler == null ? null : new AirMachineStorage(handler, changed);
        }
    }
    private static boolean importAir(WorldServer world, BlockPos pos, EnumFacing side, StackTransferContext context) {
        var key = AirKey.INSTANCE;
        if (!context.hasOperationsLeft() || !context.isKeyTypeEnabled(AirKeyType.INSTANCE)
            || context.isInFilter(key) == context.isInverted()) return false;
        var handler = AirAccess.machine(world, pos, side);
        if (handler == null) return false;
        var source = context.getActionSource();
        long want = Math.min((long) context.getOperationsRemaining() * 1000, Math.max(0, handler.getAir()));
        var storage = context.getInternalStorage().getInventory();
        long fit = StorageHelper.poweredInsert(context.getEnergySource(), storage, key, want, source, Actionable.SIMULATE);
        if (fit <= 0) return false;
        // Old PNC addAir is exact and synchronous; remove only what ME accepted.
        long moved = StorageHelper.poweredInsert(context.getEnergySource(), storage, key, fit, source, Actionable.MODULATE);
        handler.addAir(-AirAccess.bounded(moved));
        if(moved>0)AirAccess.changed(handler);
        if (moved > 0) context.reduceOperationsRemaining(Math.max(1, (moved + 999) / 1000));
        return moved > 0;
    }
    private record Export(WorldServer world, BlockPos pos, EnumFacing side) implements StackExportStrategy {
        @Override public long transfer(StackTransferContext context, AEKey key, long amount) {
            if (key != AirKey.INSTANCE || amount <= 0) return 0;
            var handler = AirAccess.machine(world, pos, side);
            if (handler == null) return 0;
            long wanted = Math.min(Integer.MAX_VALUE, Math.min(amount, AirAccess.machineSpace(handler)));
            long moved = StorageHelper.poweredExtraction(context.getEnergySource(), context.getInternalStorage().getInventory(),
                key, wanted, context.getActionSource(), Actionable.MODULATE);
            if (moved > 0) {handler.addAir(AirAccess.bounded(moved));AirAccess.changed(handler);}
            return moved;
        }
        @Override public long push(AEKey key, long amount, Actionable mode) {
            var handler = AirAccess.machine(world, pos, side);
            return handler == null ? 0 : new AirMachineStorage(handler, () -> {}).insert(key, amount, mode, IActionSource.empty());
        }
    }
}
