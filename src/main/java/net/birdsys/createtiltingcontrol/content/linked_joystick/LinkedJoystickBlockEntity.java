package net.birdsys.createtiltingcontrol.content.linked_joystick;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.client.linked_joystick.JoystickControlClient;
import net.birdsys.createtiltingcontrol.content.linked_joystick.menu.LinkedJoystickMenu;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class LinkedJoystickBlockEntity extends SmartBlockEntity implements MenuProvider {

    public static final double DEFAULT_DEADZONE = 0.12D;
    public static final int DEFAULT_MAX_SIGNAL = 15;
    private static final float LERP_CHASE_RATE = 0.4f;
    private static final float SYNC_EPSILON = 0.02f;

    private JoystickLinkBehaviour[] links;

    private double deadzone = DEFAULT_DEADZONE;
    private int maxSignal = DEFAULT_MAX_SIGNAL;
    private boolean holdMode = false;
    private boolean latchMode = false;

    @Nullable
    private UUID linkedPlayer;
    private boolean controlActive;
    private boolean temporaryLink;

    private float joyX;
    private float joyY;
    private boolean shiftDown;
    private boolean spaceDown;

    private float lastSyncedX = Float.NaN;
    private float lastSyncedY = Float.NaN;

    public final LerpedFloat lerpedX = LerpedFloat.linear();
    public final LerpedFloat lerpedY = LerpedFloat.linear();

    public LinkedJoystickBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        links = new JoystickLinkBehaviour[JoystickMath.LINK_COUNT];
        for (int i = 0; i < JoystickMath.LINK_COUNT; i++) {
            links[i] = new JoystickLinkBehaviour(this, i);
            behaviours.add(links[i]);
        }
    }

    public JoystickLinkBehaviour[] getLinks() {
        return links;
    }

    public double getDeadzone() {
        return deadzone;
    }

    public int getMaxSignal() {
        return maxSignal;
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public boolean isLatchMode() {
        return latchMode;
    }

    public boolean hasUser() {
        return linkedPlayer != null;
    }

    public boolean isUser(UUID uuid) {
        return uuid.equals(linkedPlayer);
    }

    @Nullable
    public UUID getLinkedPlayer() {
        return linkedPlayer;
    }

    public boolean isControlActive() {
        return controlActive;
    }

    public float getJoyX() {
        return joyX;
    }

    public float getJoyY() {
        return joyY;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public boolean isSpaceDown() {
        return spaceDown;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;

        if (level.isClientSide) {
            lerpedX.chase(joyX, LERP_CHASE_RATE, LerpedFloat.Chaser.EXP);
            lerpedY.chase(joyY, LERP_CHASE_RATE, LerpedFloat.Chaser.EXP);
            lerpedX.tickChaser();
            lerpedY.tickChaser();
            return;
        }

        if (linkedPlayer == null)
            return;

        Player player = level.getPlayerByUUID(linkedPlayer);
        if (player == null || !playerInRange(player)) {
            unlink();
        }
    }

    private boolean playerInRange(Player player) {
        double range = Config.JOYSTICK_LINK_RANGE.get();
        BlockPos pos = getBlockPos();
        return Sable.HELPER.distanceSquaredWithSubLevels(level, player.getEyePosition(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= range * range;
    }

    public boolean tryLink(Player player, boolean temporary) {
        if (level == null || level.isClientSide)
            return false;
        if (linkedPlayer != null)
            return false;
        if (JoystickSessions.get(player.getUUID()) != null)
            return false;

        updateLinkedBlockState(true);
        linkedPlayer = player.getUUID();
        temporaryLink = temporary;
        controlActive = temporary;
        JoystickSessions.put(player.getUUID(), this);
        level.playSound(null, getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, 0.7f);
        setChanged();
        sendData();
        return true;
    }

    public void unlink() {
        if (level == null || level.isClientSide)
            return;
        UUID previous = linkedPlayer;
        linkedPlayer = null;
        controlActive = false;
        temporaryLink = false;
        updateLinkedBlockState(false);
        if (previous != null)
            JoystickSessions.remove(previous, this);
        if (!latchMode)
            resetInput();
        else
            pushStrengths();
        if (!holdMode)
            level.playSound(null, getBlockPos(), SimSoundEvents.LINKED_TYPEWRITER_DING.event(),
                    SoundSource.BLOCKS, 1.0f, 0.95f + 0.1f * level.getRandom().nextFloat());
        setChanged();
        sendData();
    }

    public void setControlActive(UUID sender, boolean active) {
        if (level == null || level.isClientSide)
            return;
        if (!isUser(sender))
            return;
        if (temporaryLink && !active) {
            unlink();
            return;
        }
        if (controlActive == active)
            return;
        controlActive = active;
        if (!active && !latchMode)
            resetInput();
        else {
            setChanged();
            sendData();
        }
    }

    public void applyInput(UUID sender, float x, float y, boolean shift, boolean space) {
        if (level == null || level.isClientSide)
            return;
        if (!isUser(sender) || !controlActive)
            return;

        joyX = Mth.clamp(x, -1f, 1f);
        joyY = Mth.clamp(y, -1f, 1f);
        shiftDown = shift;
        spaceDown = space;
        pushStrengths();
        syncIfMoved(false);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide)
            pushStrengths();
    }

    private void updateLinkedBlockState(boolean linked) {
        if (level == null || level.isClientSide)
            return;
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(LinkedJoystickBlock.LINKED))
            return;
        if (state.getValue(LinkedJoystickBlock.LINKED) == linked)
            return;
        level.setBlock(worldPosition, state.setValue(LinkedJoystickBlock.LINKED, linked), 3);
    }

    private void resetInput() {
        joyX = 0;
        joyY = 0;
        shiftDown = false;
        spaceDown = false;
        pushStrengths();
        syncIfMoved(true);
    }

    private void pushStrengths() {
        if (links == null)
            return;
        JoystickMath.Snapshot snapshot = JoystickMath.compute(joyX, joyY, deadzone);
        boolean changed = false;
        for (int i = 0; i < 4; i++)
            changed |= links[i].setTransmittedStrength(JoystickMath.toStrength(snapshot.axis(i), maxSignal));
        boolean modifiersLive = controlActive || latchMode;
        changed |= links[JoystickMath.LINK_SHIFT]
                .setTransmittedStrength(modifiersLive && shiftDown ? maxSignal : 0);
        changed |= links[JoystickMath.LINK_SPACE]
                .setTransmittedStrength(modifiersLive && spaceDown ? maxSignal : 0);
        if (changed)
            setChanged();
    }

    private void syncIfMoved(boolean force) {
        boolean moved = force
                || Float.isNaN(lastSyncedX)
                || Math.abs(joyX - lastSyncedX) > SYNC_EPSILON
                || Math.abs(joyY - lastSyncedY) > SYNC_EPSILON;
        if (!moved)
            return;
        lastSyncedX = joyX;
        lastSyncedY = joyY;
        sendData();
    }

    public void applyClientPreview(float x, float y) {
        joyX = Mth.clamp(x, -1f, 1f);
        joyY = Mth.clamp(y, -1f, 1f);
    }

    public void setJoystickSettings(double deadzone, int maxSignal, boolean holdMode, boolean latchMode) {
        double newDeadzone = Mth.clamp(deadzone, 0.0D, 0.45D);
        int newMaxSignal = Mth.clamp(maxSignal, 1, 15);
        if (newDeadzone == this.deadzone && newMaxSignal == this.maxSignal
                && holdMode == this.holdMode && latchMode == this.latchMode)
            return;
        boolean droppedLatch = this.latchMode && !latchMode;
        this.deadzone = newDeadzone;
        this.maxSignal = newMaxSignal;
        this.holdMode = holdMode;
        this.latchMode = latchMode;
        if (droppedLatch && !controlActive)
            resetInput();
        else
            pushStrengths();
        setChanged();
        notifyUpdate();
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            if (linkedPlayer != null)
                JoystickSessions.remove(linkedPlayer, this);
        } else if (level != null) {
            JoystickControlClient.exitIfBound(getBlockPos());
        }
        super.invalidate();
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide && linkedPlayer != null)
            unlink();
        super.destroy();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Deadzone", deadzone);
        compound.putInt("MaxSignal", maxSignal);
        compound.putBoolean("HoldMode", holdMode);
        compound.putBoolean("LatchMode", latchMode);
        compound.putFloat("JoyX", joyX);
        compound.putFloat("JoyY", joyY);
        compound.putBoolean("ShiftDown", shiftDown);
        compound.putBoolean("SpaceDown", spaceDown);
        if (clientPacket) {
            if (linkedPlayer != null)
                compound.putUUID("LinkedPlayer", linkedPlayer);
            compound.putBoolean("ControlActive", controlActive);
            compound.putBoolean("TemporaryLink", temporaryLink);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("Deadzone"))
            deadzone = Mth.clamp(compound.getDouble("Deadzone"), 0.0D, 0.45D);
        if (compound.contains("MaxSignal"))
            maxSignal = Mth.clamp(compound.getInt("MaxSignal"), 1, 15);
        holdMode = compound.getBoolean("HoldMode");
        latchMode = compound.getBoolean("LatchMode");

        if (!clientPacket) {
            joyX = compound.getFloat("JoyX");
            joyY = compound.getFloat("JoyY");
            shiftDown = compound.getBoolean("ShiftDown");
            spaceDown = compound.getBoolean("SpaceDown");
            return;
        }

        UUID previous = linkedPlayer;
        linkedPlayer = compound.hasUUID("LinkedPlayer") ? compound.getUUID("LinkedPlayer") : null;
        controlActive = compound.getBoolean("ControlActive");
        temporaryLink = compound.getBoolean("TemporaryLink");
        boolean localControls = level != null && level.isClientSide
                && JoystickControlClient.isControlling(getBlockPos());
        if (!localControls) {
            joyX = compound.getFloat("JoyX");
            joyY = compound.getFloat("JoyY");
        }
        shiftDown = compound.getBoolean("ShiftDown");
        spaceDown = compound.getBoolean("SpaceDown");

        if (level == null || !level.isClientSide)
            return;
        UUID me = clientPlayerUuid();
        if (me == null)
            return;
        if (me.equals(linkedPlayer) && !me.equals(previous))
            JoystickControlClient.onLinked(getBlockPos(), temporaryLink);
        else if (me.equals(previous) && !me.equals(linkedPlayer))
            JoystickControlClient.exitIfBound(getBlockPos());
    }

    @Nullable
    private static UUID clientPlayerUuid() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : mc.player.getUUID();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
        return LinkedJoystickMenu.create(id, inventory, this);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}