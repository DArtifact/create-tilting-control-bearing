package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ThrottleHudOverlay {

    private static final int FORWARD_TEXT = 0x14666B;
    private static final int BACKWARD_TEXT = 0x6B1414;
    private static final int NEUTRAL_TEXT = 0x442000;

    private static final float CURSOR_TRAVEL = 42f;

    private ThrottleHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!ThrottleDragClient.isDragging() || mc.options.hideGui)
            return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int x = width / 2 - 50 + 16;
        int y = height / 2 - 7;
        PoseStack ps = graphics.pose();

        ps.pushPose();
        ps.translate(x + 50, y + 7, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(90));
        ps.translate(-x - 50, -y - 7, 0);
        AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
        AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + 100 - 4, y);
        AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + 14 - 4);
        AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + 100 - 4, y + 14 - 4);
        UIRenderHelper.drawStretched(graphics, x, y + 4, 3, 6, 2, AllGuiTextures.BRASS_FRAME_LEFT);
        UIRenderHelper.drawStretched(graphics, x + 100 - 3, y + 4, 3, 6, 2, AllGuiTextures.BRASS_FRAME_RIGHT);
        UIRenderHelper.drawCropped(graphics, x + 4, y, 92, 3, 2, AllGuiTextures.BRASS_FRAME_TOP);
        UIRenderHelper.drawCropped(graphics, x + 4, y + 14 - 3, 92, 3, 2, AllGuiTextures.BRASS_FRAME_BOTTOM);
        int barX = x + 3;
        int barWidth = 94;
        for (int w = 0; w < barWidth; w += AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1)
            UIRenderHelper.drawCropped(graphics, barX + w, y + 3,
                    Math.min(AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1, barWidth - w), 8, 2,
                    AllGuiTextures.VALUE_SETTINGS_BAR);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, 4);
        float currentValue = ThrottleDragClient.animatedValue(AnimationTickHolder.getPartialTicks());
        float cursorY = -currentValue * CURSOR_TRAVEL + 2f;
        int cx = x + 50 - 7;
        float cy = y + 7 - 9 + cursorY;
        ps.pushPose();
        ps.translate(0, cy, 0);
        AllGuiTextures.VALUE_SETTINGS_CURSOR_LEFT.render(graphics, cx - 3, 0);
        UIRenderHelper.drawCropped(graphics, cx, 0, 14, 14, 2, AllGuiTextures.VALUE_SETTINGS_CURSOR);
        AllGuiTextures.VALUE_SETTINGS_CURSOR_RIGHT.render(graphics, cx + 14, 0);
        ps.translate(0, 0, 4);
        int level = ThrottleDragClient.displayLevel();
        int color = level > 0 ? FORWARD_TEXT : level < 0 ? BACKWARD_TEXT : NEUTRAL_TEXT;
        graphics.drawString(mc.font, String.valueOf(Math.abs(level)), cx + 1, 3, color, false);
        ps.popPose();
        ps.popPose();
    }
}
