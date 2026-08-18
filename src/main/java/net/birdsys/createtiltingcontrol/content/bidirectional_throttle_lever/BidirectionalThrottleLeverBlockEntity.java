package net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;

import dev.ryanhcode.sable.Sable;
import net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever.ThrottleDragClient;
import net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever.ThrottleLeverPicking;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.menu.BidirectionalThrottleLeverMenu;
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
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class BidirectionalThrottleLeverBlockEntity extends SmartBlockEntity implements MenuProvider {

    public static final int MAX_THROTTLE = 15;

    public static final float MAX_TILT_DEGREES = 42f;

    public static final int MIN_RETURN_TICKS_PER_LEVEL = 1;
    public static final int MAX_RETURN_TICKS_PER_LEVEL = 100;
    public static final int DEFAULT_RETURN_TICKS_PER_LEVEL = 4;
    public static final boolean DEFAULT_AUTO_RETURN = false;

    public static final double GRIP_RANGE = 6.0D;

    private static final float LERP_CHASE_RATE = 0.45f;

    private ThrottleLinkBehaviour[] links;

    private int throttle = 0;
    private boolean autoReturn = DEFAULT_AUTO_RETURN;
    private int returnTicksPerLevel = DEFAULT_RETURN_TICKS_PER_LEVEL;

    @Nullable
    private UUID holder;
    private int returnCounter;

    public final LerpedFloat lerpedThrottle = LerpedFloat.linear();

    public BidirectionalThrottleLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        links = new ThrottleLinkBehaviour[ThrottleLinkBehaviour.LINK_COUNT];
        for (int i = 0; i < ThrottleLinkBehaviour.LINK_COUNT; i++) {
            links[i] = new ThrottleLinkBehaviour(this, i);
            behaviours.add(links[i]);
        }
        behaviours.add(new HoldTipBehaviour(this, Component.translatable(
                "create_tilting_control.gui.bidirectional_throttle_lever.hold_tip")));
    }

    public ThrottleLinkBehaviour[] getLinks() {
        return links;
    }

    public int getThrottle() {
        return throttle;
    }

    public int getForwardPower() {
        return Math.max(0, throttle);
    }

    public int getBackwardPower() {
        return Math.max(0, -throttle);
    }

    public boolean isAutoReturn() {
        return autoReturn;
    }

    public int getReturnTicksPerLevel() {
        return returnTicksPerLevel;
    }

    public boolean hasHolder() {
        return holder != null;
    }

    public boolean isHolder(UUID uuid) {
        return uuid.equals(holder);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;

        if (level.isClientSide) {
            lerpedThrottle.chase(throttle, LERP_CHASE_RATE, LerpedFloat.Chaser.EXP);
            lerpedThrottle.tickChaser();
            ThrottleLeverPicking.track(this);
            return;
        }

        if (holder != null) {
            Player player = level.getPlayerByUUID(holder);
            if (player == null || player.isRemoved() || !player.isAlive() || !isWithinGripRange(player))
                releaseGrip(holder);
            return;
        }

        if (!autoReturn || throttle == 0)
            return;

        if (++returnCounter < returnTicksPerLevel)
            return;
        returnCounter = 0;
        applyThrottle(throttle - Integer.signum(throttle));
    }

    public boolean isWithinGripRange(Player player) {
        if (level == null)
            return false;
        BlockPos pos = getBlockPos();
        return Sable.HELPER.distanceSquaredWithSubLevels(level, player.getEyePosition(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= GRIP_RANGE * GRIP_RANGE;
    }

    public boolean tryGrab(Player player) {
        if (level == null || level.isClientSide)
            return false;
        if (holder != null)
            return holder.equals(player.getUUID());
        holder = player.getUUID();
        returnCounter = 0;
        setChanged();
        sendData();
        return true;
    }

    public void applyDrag(UUID sender, int newThrottle) {
        if (level == null || level.isClientSide)
            return;
        if (!isHolder(sender))
            return;
        applyThrottle(newThrottle);
    }

    public void releaseGrip(UUID sender) {
        if (level == null || level.isClientSide)
            return;
        if (holder == null || !holder.equals(sender))
            return;
        holder = null;
        returnCounter = 0;
        setChanged();
        sendData();
    }

    private void applyThrottle(int newThrottle) {
        int clamped = Mth.clamp(newThrottle, -MAX_THROTTLE, MAX_THROTTLE);
        if (clamped == throttle)
            return;
        throttle = clamped;

        if (level != null)
            level.playSound(null, getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                    0.25f, 0.5f + (Math.abs(throttle) / (float) MAX_THROTTLE) * 0.6f);

        pushStrengths();
        BidirectionalThrottleLeverBlock.updateNeighbors(getBlockState(), level, getBlockPos());
        setChanged();
        sendData();
    }

    private void pushStrengths() {
        if (links == null)
            return;
        links[ThrottleLinkBehaviour.FORWARD].setTransmittedStrength(getForwardPower());
        links[ThrottleLinkBehaviour.BACKWARD].setTransmittedStrength(getBackwardPower());
    }

    public void setThrottleSettings(boolean autoReturn, int returnTicksPerLevel) {
        int newTicks = Mth.clamp(returnTicksPerLevel, MIN_RETURN_TICKS_PER_LEVEL, MAX_RETURN_TICKS_PER_LEVEL);
        if (autoReturn == this.autoReturn && newTicks == this.returnTicksPerLevel)
            return;
        this.autoReturn = autoReturn;
        this.returnTicksPerLevel = newTicks;
        this.returnCounter = 0;
        setChanged();
        notifyUpdate();
    }

    @Override
    public void invalidate() {
        if (level != null && level.isClientSide)
            ThrottleDragClient.stopIfDragging(getBlockPos());
        super.invalidate();
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide && throttle != 0) {
            throttle = 0;
            pushStrengths();
            BidirectionalThrottleLeverBlock.updateNeighbors(getBlockState(), level, getBlockPos());
        }
        super.destroy();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("Throttle", throttle);
        compound.putBoolean("AutoReturn", autoReturn);
        compound.putInt("ReturnTicksPerLevel", returnTicksPerLevel);
        if (clientPacket && holder != null)
            compound.putUUID("Holder", holder);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        autoReturn = compound.getBoolean("AutoReturn");
        if (compound.contains("ReturnTicksPerLevel"))
            returnTicksPerLevel = Mth.clamp(compound.getInt("ReturnTicksPerLevel"),
                    MIN_RETURN_TICKS_PER_LEVEL, MAX_RETURN_TICKS_PER_LEVEL);

        int syncedThrottle = Mth.clamp(compound.getInt("Throttle"), -MAX_THROTTLE, MAX_THROTTLE);

        if (!clientPacket) {
            throttle = syncedThrottle;
            lerpedThrottle.startWithValue(throttle);
            return;
        }

        holder = compound.hasUUID("Holder") ? compound.getUUID("Holder") : null;

        if (!ThrottleDragClient.isDragging(getBlockPos()))
            throttle = syncedThrottle;

        if (level == null || !level.isClientSide)
            return;
        UUID me = clientPlayerUuid();
        if (me != null && holder != null && !me.equals(holder))
            ThrottleDragClient.stopIfDragging(getBlockPos());
    }

    @Nullable
    private static UUID clientPlayerUuid() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : mc.player.getUUID();
    }

    public void applyClientPreview(int newThrottle) {
        throttle = Mth.clamp(newThrottle, -MAX_THROTTLE, MAX_THROTTLE);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
        return BidirectionalThrottleLeverMenu.create(id, inventory, this);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}
