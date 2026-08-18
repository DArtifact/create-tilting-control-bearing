package net.birdsys.createtiltingcontrol.content.linked_joystick;

import net.minecraft.util.Mth;

public final class JoystickMath {

    public static final int LINK_UP = 0;
    public static final int LINK_DOWN = 1;
    public static final int LINK_LEFT = 2;
    public static final int LINK_RIGHT = 3;
    public static final int LINK_SHIFT = 4;
    public static final int LINK_SPACE = 5;
    public static final int LINK_COUNT = 6;

    private JoystickMath() {}

    public record Snapshot(float x, float y, float up, float down, float left, float right) {
        public static final Snapshot ZERO = new Snapshot(0, 0, 0, 0, 0, 0);

        public float axis(int link) {
            return switch (link) {
                case LINK_UP -> up;
                case LINK_DOWN -> down;
                case LINK_LEFT -> left;
                case LINK_RIGHT -> right;
                default -> 0;
            };
        }
    }

    public static Snapshot compute(float rawX, float rawY, double deadzone) {
        float x = Mth.clamp(rawX, -1f, 1f);
        float y = Mth.clamp(rawY, -1f, 1f);
        double dz = Mth.clamp(deadzone, 0.0, 0.95);
        double magnitude = Math.sqrt(x * x + y * y);
        if (magnitude <= dz)
            return new Snapshot(x, y, 0, 0, 0, 0);

        double maxComponent = Math.max(Math.abs(x), Math.abs(y));
        double boundary = magnitude / Math.max(maxComponent, 1.0e-6);
        double remapped = Mth.clamp((magnitude - dz) / Math.max(1.0e-6, boundary - dz), 0.0, 1.0);
        double scale = remapped * boundary / Math.max(magnitude, 1.0e-6);
        float sx = (float) Mth.clamp(x * scale, -1.0, 1.0);
        float sy = (float) Mth.clamp(y * scale, -1.0, 1.0);
        return new Snapshot(x, y,
                Math.max(0, -sy),
                Math.max(0, sy),
                Math.max(0, -sx),
                Math.max(0, sx));
    }

    public static int toStrength(float value, int maxSignal) {
        int max = Mth.clamp(maxSignal, 1, 15);
        return Mth.clamp(Math.round(Mth.clamp(value, 0f, 1f) * max), 0, max);
    }
}
