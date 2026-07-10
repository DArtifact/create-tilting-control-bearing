package net.birdsys.createtiltingcontrol.content.tilting_bearing;

import java.util.function.IntConsumer;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TiltLinkBehaviour extends BlockEntityBehaviour implements IRedstoneLinkable {

    @SuppressWarnings("unchecked")
    public static final BehaviourType<TiltLinkBehaviour>[] TYPES = new BehaviourType[] {
            new BehaviourType<TiltLinkBehaviour>(),
            new BehaviourType<TiltLinkBehaviour>(),
            new BehaviourType<TiltLinkBehaviour>(),
            new BehaviourType<TiltLinkBehaviour>()
    };

    private Frequency frequencyFirst;
    private Frequency frequencyLast;

    public boolean newPosition;
    private final IntConsumer signalCallback;
    private final int index;
    private final String sideKey;

    public TiltLinkBehaviour(SmartBlockEntity be, int index, IntConsumer signalCallback) {
        super(be);
        this.frequencyFirst = Frequency.EMPTY;
        this.frequencyLast = Frequency.EMPTY;
        this.newPosition = true;
        this.index = index;
        this.sideKey = "Link" + index;
        this.signalCallback = signalCallback;
    }

    @Override
    public boolean isListening() {
        return true;
    }

    @Override
    public int getTransmittedStrength() {
        return 0;
    }

    @Override
    public void setReceivedStrength(int networkPower) {
        if (!newPosition)
            return;
        signalCallback.accept(networkPower);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide)
            return;
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
        newPosition = true;
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return Couple.create(frequencyFirst, frequencyLast);
    }

    @Override
    public void unload() {
        super.unload();
        if (getWorld().isClientSide)
            return;
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);
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
        nbt.putLong(sideKey + "LastKnownPosition", blockEntity.getBlockPos().asLong());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        long positionInTag = blockEntity.getBlockPos().asLong();
        long positionKey = nbt.getLong(sideKey + "LastKnownPosition");
        newPosition = positionInTag != positionKey;

        super.read(nbt, registries, clientPacket);
        frequencyFirst = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyFirst")));
        frequencyLast = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyLast")));
    }

    public void setFrequency(boolean first, ItemStack stack) {
        stack = stack.copy();
        stack.setCount(1);
        ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
        boolean changed = !ItemStack.isSameItemSameComponents(stack, toCompare);

        if (changed)
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);

        if (first)
            frequencyFirst = Frequency.of(stack);
        else
            frequencyLast = Frequency.of(stack);

        if (!changed)
            return;

        signalCallback.accept(0);

        blockEntity.sendData();
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPES[index];
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

    public Frequency getFrequency(boolean first) {
        return first ? frequencyFirst : frequencyLast;
    }
}
