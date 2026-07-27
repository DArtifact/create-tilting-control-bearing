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
    public static final TiltRanges GIMBAL_PROPELLER_BEARING;
    public static final ModConfigSpec.DoubleValue STRESS_IMPACT;
    public static final ModConfigSpec.DoubleValue SWIVEL_STRESS_IMPACT;
    public static final ModConfigSpec.DoubleValue GIMBAL_STRESS_IMPACT;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_RPM_MULT;
    public static final ModConfigSpec.DoubleValue GIMBAL_MAX_GYRO_STRENGTH;
    public static final ModConfigSpec.DoubleValue GIMBAL_CAP_REFERENCE_RPM;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_STIFFNESS;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_DAMPING;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_HALF_STRENGTH;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_REFERENCE_MASS;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_MASS_EXPONENT;
    public static final ModConfigSpec.DoubleValue GIMBAL_GYRO_YAW_DAMPING;
    public static final ModConfigSpec.DoubleValue GIMBAL_THRUST_YAW_COMPENSATION;

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

        BUILDER.comment("Settings for the Gimbal Propeller Bearing").push("gimbalPropellerBearing");
        GIMBAL_PROPELLER_BEARING = new TiltRanges(BUILDER, "Gimbal Propeller Bearing", 15.0D);
        GIMBAL_STRESS_IMPACT = BUILDER
                .comment("Stress impact of the Gimbal Bearing, applied per attached sail block")
                .defineInRange("stressImpact", 2.5D, 0.0D, 4096.0D);
        GIMBAL_GYRO_RPM_MULT = BUILDER
                .comment("Additional stress impact per unit of configured gyroscope strength.",
                        "Total impact = stressImpact * sails + gyroRpmMult * gyroStrength.")
                .defineInRange("gyroRpmMult", 2.0D, 0.0D, 4096.0D);
        GIMBAL_MAX_GYRO_STRENGTH = BUILDER
                .comment("Highest gyroscope strength selectable in the Gimbal Bearing screen.")
                .defineInRange("maxGyroStrength", 32.0D, 0.0D, 256.0D);
        GIMBAL_CAP_REFERENCE_RPM = BUILDER
                .comment("RPM at which the full gyroscope strength unlocks.",
                        "Below it, the selectable strength is capped by",
                        "maxGyroStrength * log(1 + rpm) / log(1 + gyroCapReferenceRpm).",
                        "While the bearing is stopped the full range stays configurable.")
                .defineInRange("gyroCapReferenceRpm", 64.0D, 1.0D, 256.0D);
        GIMBAL_GYRO_STIFFNESS = BUILDER
                .comment("Restoring torque coefficient of the gyroscope at full authority,",
                        "normalized by the contraption's rotational inertia.")
                .defineInRange("gyroStiffness", 14.0D, 0.0D, 1024.0D);
        GIMBAL_GYRO_DAMPING = BUILDER
                .comment("Rotation damping coefficient of the gyroscope at full authority,",
                        "normalized by the contraption's rotational inertia.")
                .defineInRange("gyroDamping", 14.0D, 0.0D, 1024.0D);
        GIMBAL_GYRO_HALF_STRENGTH = BUILDER
                .comment("Gyro strength that yields 50% stabilization authority on a contraption",
                        "of gyroReferenceMass. Authority follows the saturating curve",
                        "strength / (strength + S), where S = gyroHalfStrength *",
                        "(mass / gyroReferenceMass) ^ gyroMassExponent.")
                .defineInRange("gyroHalfStrength", 0.5D, 0.001D, 256.0D);
        GIMBAL_GYRO_REFERENCE_MASS = BUILDER
                .comment("Contraption mass at which gyroHalfStrength applies as-is.")
                .defineInRange("gyroReferenceMass", 20.0D, 0.001D, 1.0e9D);
        GIMBAL_GYRO_MASS_EXPONENT = BUILDER
                .comment("How fast the required strength grows with contraption mass.",
                        "Below 1.0 the growth is sublinear: heavier craft need more strength",
                        "with diminishing returns, but a max-strength gyro always keeps",
                        "meaningful authority - there is no mass at which it stops working.")
                .defineInRange("gyroMassExponent", 0.45D, 0.0D, 2.0D);
        GIMBAL_THRUST_YAW_COMPENSATION = BUILDER
                .comment("How much of the yaw caused by this bearing's own tilted thrust the",
                        "gyroscope cancels, 0 to 1. Thrust is applied as a point force at the",
                        "bearing, so if the contraption's center of mass is not directly under it",
                        "along the fuselage, tilting sideways also yaws the craft. This cancels only that",
                        "control intact and leaving a tail rotor free to steer. Set 1 to enable")
                .defineInRange("thrustYawCompensation", 0.0D, 0.0D, 1.0D);
        GIMBAL_GYRO_YAW_DAMPING = BUILDER
                .comment("Damping applied to the contraption's yaw (heading) rate, per second,",
                        "scaled by the gyro's authority. This does not hold a heading, it only",
                        "bleeds off unwanted spin. Set to 0 to leave yaw untouched.")
                .defineInRange("gyroYawDamping", 0.6D, 0.0D, 64.0D);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
