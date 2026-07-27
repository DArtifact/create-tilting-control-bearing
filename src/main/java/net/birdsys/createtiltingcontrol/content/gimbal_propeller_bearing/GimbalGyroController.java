package net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class GimbalGyroController {

    private static final Map<ServerSubLevel, GimbalGyroController> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final ServerSubLevel subLevel;
    private final List<GimbalPropellerBearingBlockEntity> contributors = new ArrayList<>();
    private double[] contributions = new double[4];
    private double lastStamp = Double.NEGATIVE_INFINITY;

    private final Vector3d worldUp = new Vector3d(0, 1, 0);
    private final Vector3d worldUpLocal = new Vector3d();
    private final Vector3d currentUpWorld = new Vector3d();
    private final Vector3d errorAxisWorld = new Vector3d();
    private final Vector3d errorAxisLocal = new Vector3d();
    private final Vector3d angularVelocityWorld = new Vector3d();
    private final Vector3d angularVelocityLocal = new Vector3d();
    private final Vector3d dampingImpulse = new Vector3d();
    private final Vector3d deltaOmega = new Vector3d();
    private final Vector3d inertiaUp = new Vector3d();
    private final Vector3d thrustTorque = new Vector3d();
    private final Vector3d thrustAccel = new Vector3d();
    private final Vector3d impulse = new Vector3d();
    private final Vector3d scratch = new Vector3d();

    private GimbalGyroController(ServerSubLevel subLevel) {
        this.subLevel = subLevel;
    }

    public static GimbalGyroController of(ServerSubLevel subLevel) {
        return INSTANCES.computeIfAbsent(subLevel, GimbalGyroController::new);
    }

    static void detach(ServerSubLevel subLevel) {
        INSTANCES.remove(subLevel);
    }

    public void tick(double partialPhysicsTick, RigidBodyHandle handle, double timeStep) {
        double stamp = subLevel.getLevel().getGameTime() + partialPhysicsTick;
        if (stamp <= lastStamp)
            return;
        lastStamp = stamp;

        contributors.clear();
        double totalStrength = 0.0;
        for (BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            if (!(actor instanceof GimbalPropellerBearingBlockEntity bearing))
                continue;
            double strength = bearing.getGyroContribution();
            if (strength <= 0.0)
                continue;
            if (contributors.size() == contributions.length) {
                double[] grown = new double[contributions.length * 2];
                System.arraycopy(contributions, 0, grown, 0, contributions.length);
                contributions = grown;
            }
            contributions[contributors.size()] = strength;
            contributors.add(bearing);
            totalStrength += strength;
        }
        if (totalStrength <= 0.0)
            return;

        MassData mass = subLevel.getMassTracker();
        if (mass.isInvalid())
            return;
        Matrix3dc inertiaTensor = mass.getInertiaTensor();
        Matrix3dc inverseInertia = mass.getInverseInertiaTensor();
        Vector3dc centerOfMass = mass.getCenterOfMass();
        double inertiaTrace = inertiaTensor.m00() + inertiaTensor.m11() + inertiaTensor.m22();
        if (!Double.isFinite(inertiaTrace) || inertiaTrace <= 0.0)
            return;

        double contraptionMass = Math.max(mass.getMass(), 1.0e-3);
        double referenceMass = Math.max(Config.GIMBAL_GYRO_REFERENCE_MASS.get(), 1.0e-3);
        double halfStrength = Config.GIMBAL_GYRO_HALF_STRENGTH.get()
                * Math.pow(contraptionMass / referenceMass, Config.GIMBAL_GYRO_MASS_EXPONENT.get());
        double authority = totalStrength / (totalStrength + Math.max(halfStrength, 1.0e-6));

        for (GimbalPropellerBearingBlockEntity bearing : contributors)
            bearing.reportGyroAuthority(authority);

        double kp = Config.GIMBAL_GYRO_STIFFNESS.get() * authority;
        double kd = Config.GIMBAL_GYRO_DAMPING.get() * authority;

        Vector3d gravity = DimensionPhysicsData.getGravity(subLevel.getLevel(), subLevel.logicalPose().position());
        double gravityStrength = gravity.length();
        if (gravityStrength > 1.0e-6)
            worldUp.set(gravity).mul(-1.0 / gravityStrength);
        else
            worldUp.set(0, 1, 0);

        Quaterniond orientation = subLevel.logicalPose().orientation();

        handle.getAngularVelocity(angularVelocityWorld);
        orientation.transformInverse(angularVelocityWorld, angularVelocityLocal);
        orientation.transformInverse(worldUp, worldUpLocal);

        double yawRate = angularVelocityLocal.dot(worldUpLocal);
        angularVelocityLocal.fma(-yawRate, worldUpLocal);

        orientation.transform(scratch.set(0, 1, 0), currentUpWorld);
        currentUpWorld.cross(worldUp, errorAxisWorld);
        orientation.transformInverse(errorAxisWorld, errorAxisLocal);

        deltaOmega.set(errorAxisLocal).mul(kp * timeStep);

        dampingImpulse.set(angularVelocityLocal).mul(-kd * timeStep);
        dampingImpulse.mul(dampingClamp(angularVelocityLocal, dampingImpulse));
        deltaOmega.add(dampingImpulse);

        for (int i = 0; i < contributors.size(); i++) {
            GimbalPropellerBearingBlockEntity bearing = contributors.get(i);
            double onTiltFactor = bearing.getOnTiltFactor();
            if (onTiltFactor >= 1.0 - 1.0e-6)
                continue;
            double removal = (contributions[i] / totalStrength) * (1.0 - onTiltFactor);
            if (bearing.getCommandedAxisLocal(scratch)) {
                scratch.fma(-scratch.dot(worldUpLocal), worldUpLocal);
                double axisLength = scratch.length();
                if (axisLength > 1.0e-6) {
                    scratch.mul(1.0 / axisLength);
                    deltaOmega.fma(-removal * deltaOmega.dot(scratch), scratch);
                } else {
                    deltaOmega.mul(1.0 - removal);
                }
            } else {
                deltaOmega.mul(1.0 - removal);
            }
        }

        deltaOmega.fma(-deltaOmega.dot(worldUpLocal), worldUpLocal);

        inertiaTensor.transform(deltaOmega, impulse);

        inertiaTensor.transform(worldUpLocal, inertiaUp);

        double compensation = Config.GIMBAL_THRUST_YAW_COMPENSATION.get() * authority;
        if (compensation > 0.0) {
            for (int i = 0; i < contributors.size(); i++) {
                if (!contributors.get(i).getThrustTorqueImpulse(centerOfMass, timeStep, thrustTorque))
                    continue;
                inverseInertia.transform(thrustTorque, thrustAccel);
                impulse.fma(-compensation * thrustAccel.dot(worldUpLocal), inertiaUp);
            }
        }

        double yawDamping = Config.GIMBAL_GYRO_YAW_DAMPING.get();
        if (yawDamping > 0.0 && Math.abs(yawRate) > 1.0e-6) {
            double fraction = Math.min(1.0, yawDamping * authority * timeStep);
            impulse.fma(-fraction * yawRate, inertiaUp);
        }

        double lengthSquared = impulse.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared <= 0.0)
            return;

        handle.applyTorqueImpulse(impulse);
    }

    private static double dampingClamp(Vector3dc omega, Vector3dc expectedDeltaOmega) {
        double opposing = -omega.dot(expectedDeltaOmega);
        if (opposing <= 0.0)
            return 1.0;
        double speedSquared = omega.lengthSquared();
        if (speedSquared < 1.0e-12)
            return 0.0;
        return Math.min(1.0, speedSquared / opposing);
    }

    @SubscribeEvent
    public static void onSubLevelContainerReady(ForgeSableSubLevelContainerReadyEvent event) {
        SubLevelContainer container = event.getContainer();
        container.addObserver(new SubLevelObserver() {
            @Override
            public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
                if (subLevel instanceof ServerSubLevel serverSubLevel)
                    detach(serverSubLevel);
            }
        });
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        INSTANCES.clear();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        INSTANCES.clear();
    }
}