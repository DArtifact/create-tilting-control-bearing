package net.birdsys.createtiltingcontrol;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MIN_TILT_ANGLE = BUILDER
            .comment("Lowest per-block maximum tilt angle (degrees) accepted by the Tilting Bearing screen.")
            .defineInRange("minTiltAngle", 0.0D, 0.0D, 90.0D);

    public static final ModConfigSpec.DoubleValue MAX_TILT_ANGLE = BUILDER
            .comment("Highest per-block maximum tilt angle (degrees) accepted by the Tilting Bearing screen.",
                    "A full strength (15) redstone link signal tilts the propeller by the block's configured angle.")
            .defineInRange("maxTiltAngle", 15.0D, 0.0D, 90.0D);

    public static final ModConfigSpec.DoubleValue MIN_TILT_SPEED = BUILDER
            .comment("Lowest per-block tilt speed (degrees per tick) accepted by the Tilting Bearing screen.")
            .defineInRange("minTiltSpeed", 0.1D, 0.1D, 90.0D);

    public static final ModConfigSpec.DoubleValue MAX_TILT_SPEED = BUILDER
            .comment("Highest per-block tilt speed (degrees per tick) accepted by the Tilting Bearing screen.")
            .defineInRange("maxTiltSpeed", 15.0D, 0.1D, 90.0D);

    public static final ModConfigSpec.DoubleValue STRESS_IMPACT = BUILDER
            .comment("Stress impact of the Tilting Bearing, applied per attached sail block")
            .defineInRange("stressImpact", 2.5D, 0.0D, 4096.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double clampMaxTilt(double value) {
        double lo = Math.min(MIN_TILT_ANGLE.get(), MAX_TILT_ANGLE.get());
        double hi = Math.max(MIN_TILT_ANGLE.get(), MAX_TILT_ANGLE.get());
        return Math.clamp(value, lo, hi);
    }

    public static double clampTiltSpeed(double value) {
        double lo = Math.min(MIN_TILT_SPEED.get(), MAX_TILT_SPEED.get());
        double hi = Math.max(MIN_TILT_SPEED.get(), MAX_TILT_SPEED.get());
        return Math.clamp(value, lo, hi);
    }
}
