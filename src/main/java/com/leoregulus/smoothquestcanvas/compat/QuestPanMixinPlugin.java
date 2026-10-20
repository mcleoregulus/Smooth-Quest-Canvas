package com.leoregulus.smoothquestcanvas.compat;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public final class QuestPanMixinPlugin implements IMixinConfigPlugin {
    private Boolean compatible;

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (compatible != null) return compatible;
        String reason;
        try {
            reason = QuestPanCompatibility.incompatibility(name ->
                    MixinService.getService().getBytecodeProvider().getClassNode(name.replace('/', '.')));
        } catch (Exception | LinkageError error) {
            reason = error.toString();
        }
        if (reason != null) {
            LoggerFactory.getLogger("smooth_quest_canvas").warn(
                    "Smooth quest panning disabled; keeping native FTB canvas behavior: {}", reason);
            return compatible = false;
        }
        LoggerFactory.getLogger("smooth_quest_canvas").info("FTB canvas API compatible; smooth panning enabled");
        return compatible = true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
