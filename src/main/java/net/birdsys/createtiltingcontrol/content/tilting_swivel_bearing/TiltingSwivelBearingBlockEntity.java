package net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.content.TiltControlledBearing;
import net.birdsys.createtiltingcontrol.content.tilt_link.TiltLinkBehaviour;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.menu.TiltingSwivelBearingMenu;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.plate.TiltingSwivelBearingPlateBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class TiltingSwivelBearingBlockEntity extends SwivelBearingBlockEntity
        implements MenuProvider, TiltControlledBearing {

    public final Vector3d blockNormal = new Vector3d(0, 1, 0);
    public final Vector3d tiltVector = new Vector3d(0, 1, 0);

    private final Vector3d targetTiltVector = new Vector3d(0, 1, 0);
    private boolean initialized = false;
    private boolean tiltVectorLoaded = false;

    private TiltLinkBehaviour[] links;
    private int[] signals;

    private double maxTiltAngle = DEFAULT_MAX_TILT;
    private double tiltSpeed = DEFAULT_TILT_SPEED;

    @Nullable
    private GenericConstraintHandle tiltHandle;
    @Nullable
    private UUID constraintSubLevelId;
    private final Vector3d constraintTilt = new Vector3d(0, 1, 0);
    private boolean constraintTurretMode;

    private boolean turretMode;

    private double lastTargetAngle;

    private boolean tiltingAssembling;

    public TiltingSwivelBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Direction facing = state.getValue(SwivelBearingBlock.FACING);
        blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        tiltVector.set(blockNormal);
        targetTiltVector.set(blockNormal);
        constraintTilt.set(blockNormal);
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
    public TiltLinkBehaviour[] getLinks() {
        return links;
    }

    @Override
    public void tick() {
        Level level = getLevel();
        boolean server = level != null && !level.isClientSide;
        boolean wasLocked = server && isLockedState();
        double previousTarget = getTargetAngleDegrees();

        super.tick();

        if (!server)
            return;

        boolean lockChanged = isLockedState() != wasLocked;
        lastTargetAngle = lockChanged ? getTargetAngleDegrees() : previousTarget;

        Direction facing = getBlockState().getValue(SwivelBearingBlock.FACING);
        blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        if (!initialized) {
            if (!tiltVectorLoaded)
                tiltVector.set(blockNormal);
            targetTiltVector.set(tiltVector);
            constraintTilt.set(tiltVector);
            initialized = true;
        }

        if (!isAssembled()) {
            tiltVector.set(blockNormal);
            targetTiltVector.set(blockNormal);
            maintainConstraint(lockChanged);
            return;
        }

        updateTargetTilt(facing);
        stepTiltTowardsTarget();
        maintainConstraint(lockChanged);
    }

    private void updateTargetTilt(Direction facing) {
        targetTiltVector.set(blockNormal);

        if (!isAssembled())
            return;

        Vector3d lateral = new Vector3d();
        for (int i = 0; i < 4; i++) {
            if (signals[i] == 0)
                continue;
            Direction direction = TiltControlledBearing.linkDirection(facing, i);
            lateral.add(
                    direction.getStepX() * (signals[i] / 15.0),
                    direction.getStepY() * (signals[i] / 15.0),
                    direction.getStepZ() * (signals[i] / 15.0));
        }
        if (lateral.lengthSquared() < 1.0e-8)
            return;

        double maxTilt = Math.toRadians(Config.TILTING_SWIVEL_BEARING.clampMaxTilt(maxTiltAngle));
        targetTiltVector.fma(Math.tan(maxTilt), lateral);
        targetTiltVector.normalize();
        SimMathUtils.clampIntoCone(targetTiltVector, blockNormal, maxTilt);
        targetTiltVector.normalize();
    }

    private void stepTiltTowardsTarget() {
        double maxStep = Math.toRadians(Config.TILTING_SWIVEL_BEARING.clampTiltSpeed(tiltSpeed));
        Vector3d difference = new Vector3d(targetTiltVector).sub(tiltVector);
        if (difference.lengthSquared() > maxStep * maxStep)
            tiltVector.add(difference.normalize().mul(maxStep));
        else
            tiltVector.set(targetTiltVector);
        tiltVector.normalize();
    }

    private boolean isLockedState() {
        return getBlockState().hasProperty(BlockStateProperties.POWERED)
                && getBlockState().getValue(BlockStateProperties.POWERED);
    }

    private void maintainConstraint(boolean lockChanged) {
        if (tiltHandle != null && !tiltHandle.isValid())
            tiltHandle = null;

        if (!isAssembled() || getPlatePos() == null || getSubLevelID() == null) {
            removeTiltHandle();
            return;
        }

        boolean tiltChanged = constraintTilt.distanceSquared(tiltVector) > 1.0e-10;
        boolean modeChanged = constraintTurretMode != turretMode;
        if (tiltHandle != null && !lockChanged && !tiltChanged && !modeChanged)
            return;

        attachTiltedConstraint(getAttachedServerSubLevel());
    }

    @Override
    public void reattachConstraint(@Nullable ServerSubLevel plateSubLevel, boolean updatePlate) {
        if (level == null || level.isClientSide)
            return;
        if (tiltHandle != null && !tiltHandle.isValid())
            tiltHandle = null;

        UUID newId = plateSubLevel != null ? plateSubLevel.getUniqueId() : null;
        if (tiltHandle != null && Objects.equals(newId, constraintSubLevelId))
            return;

        if (updatePlate)
            associatePlateWithParent();
        attachTiltedConstraint(plateSubLevel);
    }

    private void attachTiltedConstraint(@Nullable ServerSubLevel plateSubLevel) {
        removeTiltHandle();
        if (level == null || level.isClientSide)
            return;
        BlockPos platePos = getPlatePos();
        if (platePos == null)
            return;
        BlockState plateState = level.getBlockState(platePos);
        if (!plateState.is(ModBlocks.TILTING_SWIVEL_BEARING_PLATE.get()))
            return;

        Direction facing = getBlockState().getValue(SwivelBearingBlock.FACING);
        Direction plateFacing = plateState.getValue(SwivelBearingPlateBlock.FACING);
        Vec3 facingVec = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 plateFacingVec = Vec3.atLowerCornerOf(plateFacing.getNormal());

        Vector3d anchorPos = JOMLConversion.toJOML(
                getBlockPos().relative(facing).getCenter()
                        .subtract(facingVec.scale(0.501)));
        Vector3d attachPos = JOMLConversion.toJOML(
                platePos.relative(plateFacing).getCenter()
                        .subtract(plateFacingVec.scale(0.501)));

        Quaterniond base1 = new Quaterniond().rotationTo(1, 0, 0,
                facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Quaterniond base2 = new Quaterniond().rotationTo(1, 0, 0,
                plateFacing.getStepX(), plateFacing.getStepY(), plateFacing.getStepZ());
        Quaterniond orientation1;
        Quaterniond orientation2;
        if (turretMode) {
            orientation1 = base1;
            orientation2 = new Quaterniond()
                    .rotationTo(tiltVector.x, tiltVector.y, tiltVector.z,
                            blockNormal.x, blockNormal.y, blockNormal.z)
                    .mul(base2);
        } else {
            orientation1 = new Quaterniond()
                    .rotationTo(blockNormal.x, blockNormal.y, blockNormal.z,
                            tiltVector.x, tiltVector.y, tiltVector.z)
                    .mul(base1);
            orientation2 = base2;
        }

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        ServerSubLevel containingSubLevel = (ServerSubLevel) Sable.HELPER.getContaining((BlockEntity) this);
        if (containingSubLevel == plateSubLevel)
            return;
        if (containingSubLevel == null && plateSubLevel == null)
            return;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        tiltHandle = pipeline.addConstraint(containingSubLevel, plateSubLevel,
                new GenericConstraintConfiguration(anchorPos, attachPos, orientation1, orientation2,
                        EnumSet.of(
                                ConstraintJointAxis.LINEAR_X,
                                ConstraintJointAxis.LINEAR_Y,
                                ConstraintJointAxis.LINEAR_Z,
                                ConstraintJointAxis.ANGULAR_Y,
                                ConstraintJointAxis.ANGULAR_Z)));
        constraintSubLevelId = plateSubLevel != null ? plateSubLevel.getUniqueId() : null;
        constraintTilt.set(tiltVector);
        constraintTurretMode = turretMode;

        if (containingSubLevel != null)
            pipeline.wakeUp(containingSubLevel);
        if (plateSubLevel != null)
            pipeline.wakeUp(plateSubLevel);
    }

    private void removeTiltHandle() {
        if (tiltHandle != null) {
            if (tiltHandle.isValid())
                tiltHandle.remove();
            tiltHandle = null;
        }
        constraintSubLevelId = null;
    }

    @Nullable
    private ServerSubLevel getAttachedServerSubLevel() {
        if (level == null || getSubLevelID() == null)
            return null;
        SubLevel subLevel = SubLevelContainer.getContainer(level).getSubLevel(getSubLevelID());
        return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }

    @Override
    public void updateServoCoefficients() {
        if (tiltHandle != null && !tiltHandle.isValid())
            tiltHandle = null;
        if (!isAssembled() || tiltHandle == null || level == null)
            return;

        SimPhysics config = SimConfigService.INSTANCE.server().physics;

        if (!isLockedState()) {
            tiltHandle.setMotor(ConstraintJointAxis.ANGULAR_X,
                    0.0, 0.0, config.swivelBearingFriction.getF(), false, 0.0);
            return;
        }

        SubLevel subLevelA = Sable.HELPER.getContaining((BlockEntity) this);
        SubLevel subLevelB = level.isClientSide ? null
                : SubLevelContainer.getContainer(level).getSubLevel(getSubLevelID());

        Vector3d axis = turretMode
                ? new Vector3d(blockNormal).normalize()
                : new Vector3d(tiltVector).normalize();
        double inertiaA = Double.MAX_VALUE;
        double inertiaB = Double.MAX_VALUE;
        Vector3d temp = new Vector3d();
        if (subLevelA instanceof ServerSubLevel serverA)
            inertiaA = serverA.getMassTracker().getInertiaTensor().transform(axis, temp).dot(axis);
        if (subLevelB instanceof ServerSubLevel serverB)
            inertiaB = serverB.getMassTracker().getInertiaTensor().transform(axis, temp).dot(axis);

        double totalInertia = Math.max(10.0, subLevelA != null && subLevelB != null
                ? Math.max(inertiaA, inertiaB)
                : Math.min(inertiaA, inertiaB));

        SubLevelPhysicsSystem physicsSystem =
                ((ServerSubLevelContainer) SubLevelContainer.getContainer(level)).physicsSystem();
        double kP = config.swivelBearingStiffness.getF() * totalInertia;
        double kD = config.swivelBearingDamping.getF() * totalInertia;
        double goal = AngleHelper.rad(AngleHelper.angleLerp(
                physicsSystem.getPartialPhysicsTick(), lastTargetAngle, getTargetAngleDegrees()));

        tiltHandle.setMotor(ConstraintJointAxis.ANGULAR_X, goal, kP, kD, false, 0.0);
        tiltHandle.setContactsEnabled(false);
    }

    @Override
    public void assemble() {
        if (level == null || level.isClientSide)
            return;
        BlockPos pos = getBlockPos();
        BlockPos toAssemble = pos.relative(getBlockState().getValue(SwivelBearingBlock.FACING));

        SimAssemblyHelper.AssemblyResult result;
        try {
            result = SimAssemblyHelper.assembleFromSingleBlock(level, pos, toAssemble, false, false);
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }
        sendData();

        BlockState link = ModBlocks.TILTING_SWIVEL_BEARING_PLATE.get().defaultBlockState()
                .setValue(SwivelBearingPlateBlock.FACING, getBlockState().getValue(SwivelBearingBlock.FACING));

        ServerSubLevel assembledSubLevel;
        BlockPos assembleOffset;
        if (result != null) {
            assembledSubLevel = (ServerSubLevel) result.subLevel();
            assembleOffset = result.offset();
        } else {
            ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
            Pose3d pose = new Pose3d();
            pose.position().set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            assembledSubLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
            ServerLevelPlot plot = assembledSubLevel.getPlot();
            ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);
            plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, link, 3);
            BlockPos plotAnchor = plot.getCenterBlock();
            Vector3dc centerOfMass = assembledSubLevel.getMassTracker().getCenterOfMass();
            Vector3d subLevelCenter = JOMLConversion.atLowerCornerOf(pos);
            if (centerOfMass != null) {
                subLevelCenter.add(
                        centerOfMass.x() - plotAnchor.getX(),
                        centerOfMass.y() - plotAnchor.getY(),
                        centerOfMass.z() - plotAnchor.getZ());
            } else {
                assembledSubLevel.logicalPose().rotationPoint().set(
                        plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
            }
            assembledSubLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
            assembleOffset = plotAnchor.subtract(pos);
            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            PhysicsPipeline pipeline = physicsSystem.getPipeline();
            SubLevel containingSubLevel = Sable.HELPER.getContaining((BlockEntity) this);
            if (containingSubLevel != null) {
                SubLevelAssemblyHelper.kickFromContainingSubLevel((ServerLevel) level, physicsSystem, pipeline,
                        assembledSubLevel, containingSubLevel);
                assembledSubLevel.logicalPose().orientation().set(containingSubLevel.logicalPose().orientation());
            }
            pipeline.teleport(assembledSubLevel,
                    assembledSubLevel.logicalPose().position(), assembledSubLevel.logicalPose().orientation());
            assembledSubLevel.updateLastPose();
            level.playSound(null, pos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        level.setBlockAndUpdate(pos, getBlockState().setValue(SwivelBearingBlock.ASSEMBLED, true));
        setSubLevelID(assembledSubLevel.getUniqueId());
        BlockPos plotPos = pos.offset(assembleOffset);
        if (result != null)
            level.setBlockAndUpdate(plotPos, link);
        if (level.getBlockEntity(plotPos) instanceof TiltingSwivelBearingPlateBlockEntity plateBE) {
            plateBE.setParent(this);
            setPlatePos(plotPos);
        }
        SimAdvancements.YOU_SPIN_ME_RIGHT_ROUND.awardToNearby(pos, level);
    }

    @Override
    public void beforeAssembly() {
        super.beforeAssembly();
        tiltingAssembling = true;
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide && !tiltingAssembling)
            destroyTiltingPlate();
        super.remove();
    }

    private void destroyTiltingPlate() {
        BlockPos platePos = getPlatePos();
        if (platePos == null || level == null)
            return;
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null)
            return;
        UUID id = getSubLevelID();
        SubLevel subLevel = id != null ? container.getSubLevel(id) : null;
        if (id != null && subLevel == null)
            return;
        if (level.getBlockState(platePos).is(ModBlocks.TILTING_SWIVEL_BEARING_PLATE.get())) {
            ModBlocks.TILTING_SWIVEL_BEARING_PLATE.get().withBlockEntityDo(level, platePos,
                    SwivelBearingPlateBlockEntity::beforeAssembly);
            level.setBlock(platePos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @Override
    public void disassemble() {
        if (isRemoved())
            return;
        removeTiltHandle();
        destroyTiltingPlate();
        super.disassemble();
        tiltVector.set(blockNormal);
        targetTiltVector.set(blockNormal);
        constraintTilt.set(blockNormal);
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    @Override
    public void invalidate() {
        removeTiltHandle();
        super.invalidate();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putIntArray("LinkSignals", signals.clone());
        compound.putDouble("MaxTiltAngle", maxTiltAngle);
        compound.putDouble("TiltSpeed", tiltSpeed);
        compound.putDouble("TiltX", tiltVector.x);
        compound.putDouble("TiltY", tiltVector.y);
        compound.putDouble("TiltZ", tiltVector.z);
        compound.putBoolean("TurretMode", turretMode);
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
        turretMode = compound.getBoolean("TurretMode");
        super.read(compound, registries, clientPacket);
    }

    @Override
    public double getMaxTiltAngle() {
        return Config.TILTING_SWIVEL_BEARING.clampMaxTilt(maxTiltAngle);
    }

    @Override
    public double getTiltSpeed() {
        return Config.TILTING_SWIVEL_BEARING.clampTiltSpeed(tiltSpeed);
    }

    @Override
    public void setTiltSettings(double maxTiltAngle, double tiltSpeed) {
        double newMaxTilt = Config.TILTING_SWIVEL_BEARING.clampMaxTilt(maxTiltAngle);
        double newSpeed = Config.TILTING_SWIVEL_BEARING.clampTiltSpeed(tiltSpeed);
        if (newMaxTilt == this.maxTiltAngle && newSpeed == this.tiltSpeed)
            return;
        this.maxTiltAngle = newMaxTilt;
        this.tiltSpeed = newSpeed;
        setChanged();
        notifyUpdate();
    }

    public boolean isTurretMode() {
        return turretMode;
    }

    public void setTurretMode(boolean turretMode) {
        if (this.turretMode == turretMode)
            return;
        this.turretMode = turretMode;
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
        return TiltingSwivelBearingMenu.create(id, inventory, this);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}
