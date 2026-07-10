package net.birdsys.createtiltingcontrol.content.tilting_bearing;

import java.util.List;

import net.birdsys.createtiltingcontrol.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.menu.TiltingBearingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class TiltingBearingBlockEntity extends PropellerBearingBlockEntity implements MenuProvider {

    public Quaternionf tiltQuat = new Quaternionf();
    public Quaternionf previousTiltQuat = new Quaternionf();
    public final Vector3d blockNormal = new Vector3d(0, 1, 0);
    public final Vector3d tiltVector = new Vector3d(0, 1, 0);

    private final Vector3d targetTiltVector = new Vector3d(0, 1, 0);
    private boolean initialized = false;
    private boolean tiltVectorLoaded = false;

    public TiltLinkBehaviour[] links;
    private int[] signals;

    private double maxTiltAngle = DEFAULT_MAX_TILT;
    private double tiltSpeed = DEFAULT_TILT_SPEED;

    public static final double DEFAULT_MAX_TILT = 15.0D;
    public static final double DEFAULT_TILT_SPEED = 2.5D;

    public TiltingBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Direction facing = state.getValue(BearingBlock.FACING);
        blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        tiltVector.set(blockNormal);
        targetTiltVector.set(blockNormal);
    }

    public static Direction linkDirection(Direction facing, int index) {
        int i = 0;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == facing.getAxis())
                continue;
            if (i == index)
                return direction;
            i++;
        }
        throw new IllegalArgumentException("[TILTING CONTROL - Tilting Bearing] Link index out of range: " + index);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        links = new TiltLinkBehaviour[4];
        signals = new int[4];
        for (int i = 0; i < 4; i++) {
            final int index = i;
            links[i] = new TiltLinkBehaviour(this, index, power -> setSignal(index, power));
            behaviours.add(links[i]);
        }
    }

    private void setSignal(int index, int power) {
        int clamped = Mth.clamp(power, 0, 15);
        if (signals[index] == clamped)
            return;
        signals[index] = clamped;
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    public int getSignal(int index) {
        return signals[index];
    }

    @Override
    public void tick() {
        super.tick();

        Direction facing = getBlockState().getValue(BearingBlock.FACING);
        blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        if (!initialized) {
            if (!tiltVectorLoaded)
                tiltVector.set(blockNormal);
            targetTiltVector.set(tiltVector);
            initialized = true;
            applyTilt();
            previousTiltQuat.set(tiltQuat);
        }

        previousTiltQuat.set(tiltQuat);

        if (isVirtual())
            return;

        updateTargetTilt(facing);
        stepTiltTowardsTarget();
        applyTilt();
    }

    private void updateTargetTilt(Direction facing) {
        targetTiltVector.set(blockNormal);

        if (movedContraption == null)
            return;

        Vector3d lateral = new Vector3d();
        for (int i = 0; i < 4; i++) {
            if (signals[i] == 0)
                continue;
            Direction direction = linkDirection(facing, i);
            lateral.add(
                    direction.getStepX() * (signals[i] / 15.0),
                    direction.getStepY() * (signals[i] / 15.0),
                    direction.getStepZ() * (signals[i] / 15.0));
        }
        if (lateral.lengthSquared() < 1.0e-8)
            return;

        double maxTilt = Math.toRadians(Config.clampMaxTilt(maxTiltAngle));
        targetTiltVector.fma(Math.tan(maxTilt), lateral);
        targetTiltVector.normalize();
        SimMathUtils.clampIntoCone(targetTiltVector, blockNormal, maxTilt);
        targetTiltVector.normalize();
    }

    private void stepTiltTowardsTarget() {
        double maxStep = Math.toRadians(Config.clampTiltSpeed(tiltSpeed));
        Vector3d difference = new Vector3d(targetTiltVector).sub(tiltVector);
        if (difference.lengthSquared() > maxStep * maxStep)
            tiltVector.add(difference.normalize().mul(maxStep));
        else
            tiltVector.set(targetTiltVector);
    }

    public void forceTilt(BlockState state) {
        Direction facing = state.getValue(BearingBlock.FACING);
        blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        tiltVector.set(blockNormal);
        targetTiltVector.set(blockNormal);
        applyTilt();
        previousTiltQuat.set(tiltQuat);
    }

    public void wrenchRotate(BlockState newState) {
        forceTilt(newState);
        setChanged();
        notifyUpdate();
    }

    public void applyTilt() {
        tiltVector.normalize();
        tiltQuat = SimMathUtils.getQuaternionfFromVectorRotation(blockNormal, tiltVector);
        thrustDirection.set(tiltVector);

        PropellerBearingContraptionEntity contraption = getMovedContraption();
        if (contraption == null)
            return;
        contraption.tiltQuat = new Quaternionf(tiltQuat);
        contraption.previousTiltQuat = new Quaternionf(previousTiltQuat);
        contraption.direction = getBlockState().getValue(BearingBlock.FACING);
    }

    @Override
    public void contraptionInitialize() {
        super.contraptionInitialize();
        if (initialized)
            applyTilt();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (isActive())
            applyForces(subLevel, new Vec3(thrustDirection.x, thrustDirection.y, thrustDirection.z), timeStep);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putIntArray("LinkSignals", signals.clone());
        compound.putDouble("MaxTiltAngle", maxTiltAngle);
        compound.putDouble("TiltSpeed", tiltSpeed);
        compound.putDouble("TiltX", tiltVector.x);
        compound.putDouble("TiltY", tiltVector.y);
        compound.putDouble("TiltZ", tiltVector.z);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (compound.contains("MaxTiltAngle"))
            maxTiltAngle = compound.getDouble("MaxTiltAngle");
        if (compound.contains("TiltSpeed"))
            tiltSpeed = compound.getDouble("TiltSpeed");
        int[] readSignals = compound.getIntArray("LinkSignals");
        for (int i = 0; i < Math.min(readSignals.length, 4); i++)
            signals[i] = Mth.clamp(readSignals[i], 0, 15);
        if (compound.contains("TiltX")) {
            tiltVector.set(
                    compound.getDouble("TiltX"),
                    compound.getDouble("TiltY"),
                    compound.getDouble("TiltZ"));
            if (tiltVector.lengthSquared() < 1.0e-8)
                tiltVector.set(0, 1, 0);
            tiltVectorLoaded = true;
        }
        super.read(compound, registries, clientPacket);
    }

    public double getMaxTiltAngle() {
        return Config.clampMaxTilt(maxTiltAngle);
    }

    public double getTiltSpeed() {
        return Config.clampTiltSpeed(tiltSpeed);
    }

    public void setTiltSettings(double maxTiltAngle, double tiltSpeed) {
        double newMaxTilt = Config.clampMaxTilt(maxTiltAngle);
        double newSpeed = Config.clampTiltSpeed(tiltSpeed);
        if (newMaxTilt == this.maxTiltAngle && newSpeed == this.tiltSpeed)
            return;
        this.maxTiltAngle = newMaxTilt;
        this.tiltSpeed = newSpeed;
        setChanged();
        notifyUpdate();
    }

    // Menu Provider

    @Override
    public AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
        return TiltingBearingMenu.create(id, inventory, this);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}