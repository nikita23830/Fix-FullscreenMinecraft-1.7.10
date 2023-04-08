package com.nikita23830.fixf11;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@SideOnly(Side.CLIENT)
@IFMLLoadingPlugin.Name("MagiHandlers")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class FMLCoreMod implements IFMLLoadingPlugin {

    public FMLCoreMod() {
        MixinBootstrap.init();
        Mixins.addConfiguration("fixf11.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> map) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
