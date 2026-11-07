package com.wintercogs.appliedpneumatics.core;

import java.util.List;
import zone.rong.mixinbooter.ILateMixinLoader;

public final class LateMixinLoader implements ILateMixinLoader {
    @Override public List<String> getMixinConfigs() { return List.of("mixins.ae2s-appliedpneumatics.json"); }
}
