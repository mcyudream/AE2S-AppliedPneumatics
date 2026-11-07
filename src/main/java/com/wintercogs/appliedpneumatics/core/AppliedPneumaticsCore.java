package com.wintercogs.appliedpneumatics.core;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("AppliedPneumaticsCleanroom")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.wintercogs.appliedpneumatics.core.")
public final class AppliedPneumaticsCore implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() {
        return net.minecraftforge.fml.relauncher.FMLLaunchHandler.side().isServer()
            && "0.4.0".equals(com.cleanroommc.common.CleanroomVersion.getVersion())
            ? new String[] { Cleanroom040ServerTransformer.class.getName() } : new String[0];
    }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}

