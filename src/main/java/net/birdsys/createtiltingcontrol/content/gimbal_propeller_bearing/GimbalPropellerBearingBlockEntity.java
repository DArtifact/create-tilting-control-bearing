package net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing;

import java.util.List;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.content.TiltControlledBearing;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.menu.GimbalPropellerBearingMenu;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class GimbalPropellerBearingBlockEntity extends TiltingPropellerBearingBlockEntity {

    public static final double DEFAULT_ON_TILT_GYRO_MULT = 0.3D;

    private static final double TILT_INPUT_RISE_RATE = 0.35D;
    private static final double TILT_INPUT_FALL_RATE = 0.10D;

    private double gyroStrength = 0.0D;
    private double onTiltGyroMult = DEFAULT_ON_TILT_GYRO_MULT;

    private double smoothedTiltInput = 0.0D;
    private final Vector3d commandedAxisLocal = new Vector3d();
    private boolean commandedAxisValid = false;
    private double physicsGyroStrength = 0.0D;

    private double lastPushedGyroImpact = Double.NaN;

    private final Vector3d thrustForceScratch = new Vector3d();

    private double gyroAuthority = 0.0D;
    private double lastSyncedGyroAuthority = 0.0D;

    public GimbalPropellerBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static double gyroStrengthCap(double speed) {
        double max = Config.GIMBAL_MAX_GYRO_STRENGTH.get();
        double rpm = Math.abs(speed);
        if (rpm < 1.0e-3)
            return max;
        double reference = Math.max(1.0D, Config.GIMBAL_CAP_REFERENCE_RPM.get());
        double fraction = Math.min(1.0D, Math.log1p(rpm) / Math.log1p(reference));
        return max * fraction;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || isVirtual())
            return;
        updateGyroState();
        pushGyroStressIfChanged();
    }

    private void updateGyroState() {
        Direction facing = getBlockState().getValue(BearingBlock.FACING);

        Vector3d lateral = new Vector3d();
        for (int i = 0; i < 4; i++) {
            int signal = getSignal(i);
            if (signal == 0)
                continue;
            Direction direction = TiltControlledBearing.linkDirection(facing, i);
            lateral.add(
                    direction.getStepX() * (signal / 15.0),
                    direction.getStepY() * (signal / 15.0),
                    direction.getStepZ() * (signal / 15.0));
        }
        double input = Math.min(1.0D, lateral.length());

        double rate = input > smoothedTiltInput ? TILT_INPUT_RISE_RATE : TILT_INPUT_FALL_RATE;
        smoothedTiltInput += (input - smoothedTiltInput) * rate;
        if (smoothedTiltInput < 1.0e-4)
            smoothedTiltInput = 0.0D;

        if (lateral.lengthSquared() > 1.0e-8) {
            commandedAxisLocal.set(blockNormal).cross(lateral);
            commandedAxisValid = commandedAxisLocal.lengthSquared() > 1.0e-8;
            if (commandedAxisValid)
                commandedAxisLocal.normalize();
        } else if (smoothedTiltInput <= 0.0D) {
            commandedAxisValid = false;
        }

        boolean active = isRunning() && !disassemblySlowdown && getSpeed() != 0 && gyroStrength > 0;
        physicsGyroStrength = active ? Math.min(getGyroStrength(), gyroStrengthCap(getSpeed())) : 0.0D;
        if (!active)
            gyroAuthority = 0.0D;

        if (level != null && level.getGameTime() % 10 == 0
                && Math.abs(gyroAuthority - lastSyncedGyroAuthority) > 0.01D) {
            lastSyncedGyroAuthority = gyroAuthority;
            sendData();
        }
    }

    public void reportGyroAuthority(double authority) {
        this.gyroAuthority = Math.clamp(authority, 0.0D, 1.0D);
    }

    public double getGyroContribution() {
        return physicsGyroStrength;
    }

    public double getOnTiltFactor() {
        return Mth.lerp((float) smoothedTiltInput, 1.0D, getOnTiltGyroMult());
    }

    public boolean getCommandedAxisLocal(Vector3d dest) {
        if (!commandedAxisValid)
            return false;
        dest.set(commandedAxisLocal);
        return true;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        super.sable$physicsTick(subLevel, handle, timeStep);
        double partialPhysicsTick = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getPartialPhysicsTick();
        GimbalGyroController.of(subLevel).tick(partialPhysicsTick, handle, timeStep);
    }

    public double gyroStressImpact() {
        if (!isRunning() || disassemblySlowdown || gyroStrength <= 0)
            return 0.0D;
        return Config.GIMBAL_GYRO_RPM_MULT.get() * Math.min(getGyroStrength(), gyroStrengthCap(getSpeed()));
    }

    @Override
    public float calculateStressApplied() {
        float sailStress = super.calculateStressApplied();
        if (sailStress == 0)
            return 0;
        double gyroImpact = gyroStressImpact();
        if (gyroImpact <= 0)
            return sailStress;
        float total = sailStress + (float) gyroImpact;
        this.lastStressApplied = total;
        return total;
    }

    private void pushGyroStressIfChanged() {
        double impact = gyroStressImpact();
        if (Double.compare(impact, lastPushedGyroImpact) == 0)
            return;
        lastPushedGyroImpact = impact;
        if (hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }

    public double getGyroStrength() {
        return Math.clamp(gyroStrength, 0.0D, Config.GIMBAL_MAX_GYRO_STRENGTH.get());
    }

    public double getOnTiltGyroMult() {
        return Math.clamp(onTiltGyroMult, 0.0D, 1.0D);
    }

    public void setGyroSettings(double gyroStrength, double onTiltGyroMult) {
        double newStrength = Math.clamp(gyroStrength, 0.0D, Config.GIMBAL_MAX_GYRO_STRENGTH.get());
        double newMult = Math.clamp(onTiltGyroMult, 0.0D, 1.0D);
        if (newStrength == this.gyroStrength && newMult == this.onTiltGyroMult)
            return;
        this.gyroStrength = newStrength;
        this.onTiltGyroMult = newMult;
        setChanged();
        notifyUpdate();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putDouble("GyroStrength", gyroStrength);
        compound.putDouble("OnTiltGyroMult", onTiltGyroMult);
        if (clientPacket)
            compound.putDouble("GyroAuthority", gyroAuthority);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (compound.contains("GyroStrength"))
            gyroStrength = compound.getDouble("GyroStrength");
        if (compound.contains("OnTiltGyroMult"))
            onTiltGyroMult = compound.getDouble("OnTiltGyroMult");
        if (clientPacket && compound.contains("GyroAuthority"))
            gyroAuthority = compound.getDouble("GyroAuthority");
        super.read(compound, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean result = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal("    ")
                .append(Component.translatable("create_tilting_control.gimbal.goggles.header")
                        .withStyle(ChatFormatting.GRAY)));

        double strength = getGyroStrength();
        if (strength <= 0) {
            tooltip.add(Component.literal("     ")
                    .append(Component.translatable("create_tilting_control.gimbal.goggles.inactive")
                            .withStyle(ChatFormatting.DARK_GRAY)));
            return true;
        }

        double cap = gyroStrengthCap(getSpeed());
        double effective = Math.min(strength, cap);
        Component strengthValue = Component.literal(String.format("%.1f", effective))
                .withStyle(ChatFormatting.AQUA);
        if (effective < strength - 1.0e-6) {
            tooltip.add(Component.literal("     ")
                    .append(Component.translatable("create_tilting_control.gimbal.goggles.strength_capped",
                                    strengthValue,
                                    Component.literal(String.format("%.1f", strength)))
                            .withStyle(ChatFormatting.GRAY)));
        } else {
            tooltip.add(Component.literal("     ")
                    .append(Component.translatable("create_tilting_control.gimbal.goggles.strength", strengthValue)
                            .withStyle(ChatFormatting.GRAY)));
        }

        if (gyroAuthority > 0) {
            tooltip.add(Component.literal("     ")
                    .append(Component.translatable("create_tilting_control.gimbal.goggles.authority",
                                    Component.literal(String.format("%.0f", gyroAuthority * 100))
                                            .withStyle(ChatFormatting.AQUA))
                            .withStyle(ChatFormatting.GRAY)));
        }

        double impact = Config.GIMBAL_GYRO_RPM_MULT.get() * effective;
        tooltip.add(Component.literal("     ")
                .append(Component.translatable("create_tilting_control.gimbal.goggles.stress",
                                Component.literal(String.format("%.1f", impact))
                                        .withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY)));
        return true;
    }

    public boolean getThrustTorqueImpulse(Vector3dc centerOfMass, double timeStep, Vector3d dest) {
        if (!isActive())
            return false;
        double thrust = getScaledThrust();
        if (thrust == 0.0 || !Double.isFinite(thrust))
            return false;
        BlockPos pos = getBlockPos();
        dest.set(pos.getX() + 0.5 - centerOfMass.x(),
                pos.getY() + 0.5 - centerOfMass.y(),
                pos.getZ() + 0.5 - centerOfMass.z());
        thrustForceScratch.set(thrustDirection).mul(thrust * timeStep);
        dest.cross(thrustForceScratch);
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
        return GimbalPropellerBearingMenu.create(id, inventory, this);
    }
}