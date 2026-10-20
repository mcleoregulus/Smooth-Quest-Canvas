package com.leoregulus.smoothquestcanvas;

import com.leoregulus.smoothquestcanvas.client.CanvasConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = SmoothQuestCanvas.MOD_ID, dist = Dist.CLIENT)
public final class SmoothQuestCanvas {
    public static final String MOD_ID = "smooth_quest_canvas";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SmoothQuestCanvas(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CanvasConfig.SPEC);
    }
}
