package com.leoregulus.smoothquestcanvas.mixin;

import com.leoregulus.smoothquestcanvas.client.CanvasConfig;
import com.leoregulus.smoothquestcanvas.client.InertialPan;
import com.leoregulus.smoothquestcanvas.client.SmoothQuestPan;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only replaces QuestScreen's left-button canvas pan branch, never general scroll setters. */
@Mixin(value = QuestScreen.class, priority = 1100, remap = false)
public abstract class QuestScreenPanMixin extends BaseScreen implements SmoothQuestPan {
    @Shadow @Final public QuestPanel questPanel;
    @Shadow @Final private ClientQuestFile file;
    @Shadow private Chapter selectedChapter;
    @Shadow private MouseButton grabbed;
    @Shadow private boolean movingObjects;
    @Shadow private double scrollWidth;
    @Shadow private double scrollHeight;
    @Shadow private int prevMouseX;
    @Shadow private int prevMouseY;
    @Shadow public abstract boolean isViewingQuest();

    @Unique private final InertialPan sqc$pan = new InertialPan();
    @Unique private long sqc$lastFrame;
    @Unique private Chapter sqc$chapter;
    @Unique private double sqc$width, sqc$height;
    @Unique private int sqc$viewWidth, sqc$viewHeight;
    @Unique private boolean sqc$ownsMotion;

    @Unique
    private boolean sqc$enabled() {
        return CanvasConfig.SMOOTH_QUEST_PANNING.get() && selectedChapter != null
                && !file.canEdit() && !movingObjects && !isViewingQuest();
    }

    @ModifyExpressionValue(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/ui/input/MouseButton;isLeft()Z"), require = 1)
    private boolean sqc$replaceCanvasDrag(boolean left) {
        return left && !sqc$enabled();
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void sqc$advancePan(CallbackInfo ci) {
        long now = System.nanoTime();
        double dt = (now - sqc$lastFrame) * 1e-9;
        boolean externalMove = Math.abs(questPanel.getScrollX() - sqc$pan.x()) > 1e-5
                || Math.abs(questPanel.getScrollY() - sqc$pan.y()) > 1e-5;
        boolean changed = selectedChapter != sqc$chapter || scrollWidth != sqc$width
                || scrollHeight != sqc$height || questPanel.width != sqc$viewWidth
                || questPanel.height != sqc$viewHeight;
        if (!sqc$enabled() || !Minecraft.getInstance().isWindowActive() || sqc$lastFrame == 0
                || dt > 0.2 || changed || externalMove) {
            sqc$pan.reset(questPanel.getScrollX(), questPanel.getScrollY());
            sqc$ownsMotion = false;
        } else {
            boolean held = grabbed != null && grabbed.isLeft();
            if (held || sqc$ownsMotion) {
                sqc$pan.advance(held, held ? prevMouseX - getMouseX() : 0,
                        held ? prevMouseY - getMouseY() : 0, dt,
                        CanvasConfig.QUEST_PAN_FOLLOW.get(), CanvasConfig.QUEST_PAN_FRICTION.get(),
                        InertialPan.Bounds.canvas(scrollWidth, scrollHeight, questPanel.width, questPanel.height));
                questPanel.setScrollX(sqc$pan.x());
                questPanel.setScrollY(sqc$pan.y());
                sqc$ownsMotion = held || !sqc$pan.isSettled();
            }
        }
        if (sqc$enabled() && grabbed != null && grabbed.isLeft()) {
            prevMouseX = getMouseX();
            prevMouseY = getMouseY();
        }
        sqc$lastFrame = now;
        sqc$chapter = selectedChapter;
        sqc$width = scrollWidth;
        sqc$height = scrollHeight;
        sqc$viewWidth = questPanel.width;
        sqc$viewHeight = questPanel.height;
    }

    @Override
    public boolean sqc$subpixelRendering() {
        // Keep the same fractional position at rest and behind quest details; dropping it would cause a final jump.
        return CanvasConfig.SMOOTH_QUEST_PANNING.get() && selectedChapter != null && !file.canEdit() && !movingObjects;
    }

    @Override
    public void sqc$stopPan() {
        sqc$pan.reset(questPanel.getScrollX(), questPanel.getScrollY());
        sqc$lastFrame = 0;
        sqc$ownsMotion = false;
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        sqc$stopPan();
        return super.mousePressed(button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        sqc$stopPan();
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Inject(method = {"addZoom", "selectChapter", "scrollTo", "onClosed", "viewQuest(Ldev/ftb/mods/ftbquests/quest/Quest;)V"},
            at = @At("HEAD"))
    private void sqc$navigation(CallbackInfo ci) { sqc$stopPan(); }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void sqc$keyboard(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        sqc$stopPan();
    }
}
