package net.birdsys.createtiltingcontrol.client.linked_joystick;

import net.birdsys.createtiltingcontrol.content.linked_joystick.JoystickMath;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModKeyMappings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class JoystickHudOverlay {

    private static final int PAD_HALF = 24;
    private static final int PAD_BG = 0x66000000;
    private static final int PAD_BORDER = 0xAAFFFFFF;
    private static final int DEADZONE_BORDER = 0x55FFFFFF;
    private static final int DOT_COLOR = 0xFFFFCC44;
    private static final int MODIFIER_ON = 0xFF77DD77;
    private static final int MODIFIER_OFF = 0x77FFFFFF;

    private JoystickHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;

        Font font = mc.font;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        if (!JoystickControlClient.isControlling()) {
            if (!JoystickControlClient.isLinked())
                return;
            Component hint = Component.translatable("create_tilting_control.joystick.hud.enter_hint",
                    ModKeyMappings.JOYSTICK_CONTROL.getTranslatedKeyMessage());
            graphics.drawString(font, hint,
                    (screenWidth - font.width(hint)) / 2, screenHeight - 60, 0xFFFFFF, true);
            return;
        }

        LinkedJoystickBlockEntity be = boundBE(mc);
        double deadzone = be != null ? be.getDeadzone() : LinkedJoystickBlockEntity.DEFAULT_DEADZONE;
        int maxSignal = be != null ? be.getMaxSignal() : LinkedJoystickBlockEntity.DEFAULT_MAX_SIGNAL;

        float joyX = JoystickControlClient.joyX();
        float joyY = JoystickControlClient.joyY();
        JoystickMath.Snapshot snapshot = JoystickMath.compute(joyX, joyY, deadzone);

        int centerX = screenWidth / 2;
        int centerY = screenHeight * 3 / 4;

        graphics.fill(centerX - PAD_HALF, centerY - PAD_HALF,
                centerX + PAD_HALF, centerY + PAD_HALF, PAD_BG);
        outline(graphics, centerX - PAD_HALF, centerY - PAD_HALF,
                centerX + PAD_HALF, centerY + PAD_HALF, PAD_BORDER);

        int dzHalf = (int) Math.round(PAD_HALF * deadzone);
        if (dzHalf > 1)
            outline(graphics, centerX - dzHalf, centerY - dzHalf,
                    centerX + dzHalf, centerY + dzHalf, DEADZONE_BORDER);

        int dotX = centerX + Math.round(joyX * (PAD_HALF - 2));
        int dotY = centerY + Math.round(joyY * (PAD_HALF - 2));
        graphics.fill(dotX - 2, dotY - 2, dotX + 3, dotY + 3, DOT_COLOR);

        drawCentered(graphics, font, strength(snapshot.up(), maxSignal),
                centerX, centerY - PAD_HALF - 11);
        drawCentered(graphics, font, strength(snapshot.down(), maxSignal),
                centerX, centerY + PAD_HALF + 3);
        drawCentered(graphics, font, strength(snapshot.left(), maxSignal),
                centerX - PAD_HALF - 12, centerY - 4);
        drawCentered(graphics, font, strength(snapshot.right(), maxSignal),
                centerX + PAD_HALF + 12, centerY - 4);

        Component shiftLabel = ModKeyMappings.JOYSTICK_MODIFIER_1.getTranslatedKeyMessage();
        Component spaceLabel = ModKeyMappings.JOYSTICK_MODIFIER_2.getTranslatedKeyMessage();
        graphics.drawString(font, shiftLabel, centerX - PAD_HALF, centerY + PAD_HALF + 14,
                JoystickControlClient.shiftDown() ? MODIFIER_ON : MODIFIER_OFF, true);
        graphics.drawString(font, spaceLabel,
                centerX + PAD_HALF - font.width(spaceLabel), centerY + PAD_HALF + 14,
                JoystickControlClient.spaceDown() ? MODIFIER_ON : MODIFIER_OFF, true);

        Component exitHint = Component.translatable("create_tilting_control.joystick.hud.exit_hint",
                ModKeyMappings.JOYSTICK_CONTROL.getTranslatedKeyMessage());
        drawCentered(graphics, font, exitHint, centerX, centerY + PAD_HALF + 26);
    }

    private static Component strength(float axis, int maxSignal) {
        return Component.literal(String.valueOf(JoystickMath.toStrength(axis, maxSignal)));
    }

    private static void drawCentered(GuiGraphics graphics, Font font, Component text, int cx, int y) {
        graphics.drawString(font, text, cx - font.width(text) / 2, y, 0xFFFFFF, true);
    }

    private static void outline(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.hLine(x1, x2 - 1, y1, color);
        graphics.hLine(x1, x2 - 1, y2 - 1, color);
        graphics.vLine(x1, y1, y2 - 1, color);
        graphics.vLine(x2 - 1, y1, y2 - 1, color);
    }

    private static LinkedJoystickBlockEntity boundBE(Minecraft mc) {
        if (JoystickControlClient.boundPos() == null || mc.level == null)
            return null;
        BlockEntity be = mc.level.getBlockEntity(JoystickControlClient.boundPos());
        return be instanceof LinkedJoystickBlockEntity joystick ? joystick : null;
    }
}
