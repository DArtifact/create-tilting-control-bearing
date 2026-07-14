package net.birdsys.createtiltingcontrol;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static class TiltRanges {
        public final ModConfigSpec.DoubleValue minTiltAngle;
        public final ModConfigSpec.DoubleValue maxTiltAngle;
        public final ModConfigSpec.DoubleValue minTiltSpeed;
        public final ModConfigSpec.DoubleValue maxTiltSpeed;

        private TiltRanges(ModConfigSpec.Builder builder, String screenName, double defaultMaxTilt) {
            minTiltAngle = builder
                    .comment("Lowest per-block maximum tilt angle (degrees) accepted by the " + screenName + " screen.")
                    .defineInRange("minTiltAngle", 0.0D, 0.0D, 90.0D);
            maxTiltAngle = builder
                    .comment("Highest per-block maximum tilt angle (degrees) accepted by the " + screenName + " screen.",
                            "A full strength (15) redstone link signal tilts by the block's configured angle.")
                    .defineInRange("maxTiltAngle", defaultMaxTilt, 0.0D, 90.0D);
            minTiltSpeed = builder
                    .comment("Lowest per-block tilt speed (degrees per tick) accepted by the " + screenName + " screen.")
                    .defineInRange("minTiltSpeed", 0.1D, 0.1D, 90.0D);
            maxTiltSpeed = builder
                    .comment("Highest per-block tilt speed (degrees per tick) accepted by the " + screenName + " screen.")
                    .defineInRange("maxTiltSpeed", 15.0D, 0.1D, 90.0D);
        }

        public double clampMaxTilt(double value) {
            double lo = Math.min(minTiltAngle.get(), maxTiltAngle.get());
            double hi = Math.max(minTiltAngle.get(), maxTiltAngle.get());
            return Math.clamp(value, lo, hi);
        }

        public double clampTiltSpeed(double value) {
            double lo = Math.min(minTiltSpeed.get(), maxTiltSpeed.get());
            double hi = Math.max(minTiltSpeed.get(), maxTiltSpeed.get());
            return Math.clamp(value, lo, hi);
        }
    }

    public static final TiltRanges TILTING_PROPELLER_BEARING;
    public static final TiltRanges TILTING_SWIVEL_BEARING;
    public static final ModConfigSpec.DoubleValue STRESS_IMPACT;
    public static final ModConfigSpec.DoubleValue SWIVEL_STRESS_IMPACT;

    static {
        BUILDER.comment("Settings for the Tilting Propeller Bearing").push("tiltingPropellerBearing");
        TILTING_PROPELLER_BEARING = new TiltRanges(BUILDER, "Tilting Propeller Bearing", 15.0D);
        STRESS_IMPACT = BUILDER
                .comment("Stress impact of the Tilting Bearing, applied per attached sail block")
                .defineInRange("stressImpact", 2.5D, 0.0D, 4096.0D);
        BUILDER.pop();

        BUILDER.comment("Settings for the Tilting Swivel Bearing").push("tiltingSwivelBearing");
        TILTING_SWIVEL_BEARING = new TiltRanges(BUILDER, "Tilting Swivel Bearing", 30.0D);
        SWIVEL_STRESS_IMPACT = BUILDER
                .comment("Stress impact of the Tilting Swivel Bearing.")
                .defineInRange("stressImpact", 4.0D, 0.0D, 4096.0D);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
