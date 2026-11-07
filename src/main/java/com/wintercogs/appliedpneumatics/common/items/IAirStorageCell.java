package com.wintercogs.appliedpneumatics.common.items;

import ae2.api.storage.cells.ICellWorkbenchItem;

public interface IAirStorageCell extends ICellWorkbenchItem {
    long getTotalBytes();
    double getIdleDrain();
    default long getAirCapacity() { return Math.multiplyExact(getTotalBytes(), 250L); }
}
