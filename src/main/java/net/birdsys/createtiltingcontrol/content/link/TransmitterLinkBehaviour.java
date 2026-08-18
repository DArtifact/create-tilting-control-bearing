package net.birdsys.createtiltingcontrol.content.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class TransmitterLinkBehaviour extends BlockEntityBehaviour implements IRedstoneLinkable {

    private Frequency frequencyFirst;
    private Frequency frequencyLast;

    private final BehaviourType<?> type;
    private final String sideKey;
    private int transmittedStrength;
    private boolean registered;

    protected TransmitterLinkBehaviour(SmartBlockEntity be, BehaviourType<?> type, String keyPrefix, int index) {
        super(be);
        this.frequencyFirst = Frequency.EMPTY;
        this.frequencyLast = Frequency.EMPTY;
        this.type = type;
        this.sideKey = keyPrefix + index;
    }

    @Override
    public boolean isListening() {
        return false;
    }

    @Override
    public int getTransmittedStrength() {
        return transmittedStrength;
    }

    @Override
    public void setReceivedStrength(int networkPower) {}

    public boolean setTransmittedStrength(int strength) {
        int clamped = Mth.clamp(strength, 0, 15);
        if (clamped == transmittedStrength)
            return false;
        transmittedStrength = clamped;
        if (registered && getWorld() != null && !getWorld().isClientSide)
            Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(getWorld(), this);
        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide)
            return;
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
        registered = true;
    }

    @Override
    public void unload() {
        super.unload();
        if (getWorld().isClientSide)
            return;
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);
        registered = false;
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return Couple.create(frequencyFirst, frequencyLast);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.put(sideKey + "FrequencyFirst", frequencyFirst.getStack().saveOptional(registries));
        nbt.put(sideKey + "FrequencyLast", frequencyLast.getStack().saveOptional(registries));
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        frequencyFirst = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyFirst")));
        frequencyLast = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyLast")));
    }

    public void setFrequency(boolean first, ItemStack stack) {
        stack = stack.copy();
        stack.setCount(1);
        ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
        boolean changed = !ItemStack.isSameItemSameComponents(stack, toCompare);

        if (changed && registered && !getWorld().isClientSide)
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);

        if (first)
            frequencyFirst = Frequency.of(stack);
        else
            frequencyLast = Frequency.of(stack);

        if (!changed)
            return;

        blockEntity.sendData();
        if (registered && !getWorld().isClientSide) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
            Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(getWorld(), this);
        }
    }

    public Frequency getFrequency(boolean first) {
        return first ? frequencyFirst : frequencyLast;
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public boolean isAlive() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (blockEntity.isChunkUnloaded())
            return false;
        if (blockEntity.isRemoved())
            return false;
        if (!level.isLoaded(pos))
            return false;
        return level.getBlockEntity(pos) == blockEntity;
    }

    @Override
    public BlockPos getLocation() {
        return getPos();
    }
}
