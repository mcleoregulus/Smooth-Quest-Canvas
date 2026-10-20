package com.leoregulus.smoothquestcanvas.mixin;

import com.leoregulus.smoothquestcanvas.client.SmoothQuestPan;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores the fraction discarded by Panel.setOffset, only inside the quest canvas's clipped content. */
@Mixin(value = Panel.class, remap = false)
public abstract class PanelPanRenderMixin {
    @Unique private boolean sqc$translated;

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/ui/Panel;setOffset(Z)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void sqc$beginContent(GuiGraphics graphics, Theme theme, int x, int y, int width, int height, CallbackInfo ci) {
        sqc$translated = false;
        if ((Object) this instanceof QuestPanel panel && panel.getGui() instanceof SmoothQuestPan pan && pan.sqc$subpixelRendering()) {
            double offsetX = -panel.getScrollX();
            double offsetY = -panel.getScrollY();
            graphics.pose().pushPose();
            graphics.pose().translate(offsetX - (int) offsetX, offsetY - (int) offsetY, 0);
            sqc$translated = true;
        }
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftblibrary/ui/Panel;setOffset(Z)V", ordinal = 1))
    private void sqc$endContent(GuiGraphics graphics, Theme theme, int x, int y, int width, int height, CallbackInfo ci) {
        if (sqc$translated) {
            graphics.pose().popPose();
            sqc$translated = false;
        }
    }
}
