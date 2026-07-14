package net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.plate;

import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TiltingSwivelBearingPlateBlockEntity extends SwivelBearingPlateBlockEntity {

    @Nullable
    private BlockPos tiltingParent;
    private boolean tiltingAssembling;

    public TiltingSwivelBearingPlateBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void setParent(SwivelBearingBlockEntity be) {
        super.setParent(be);
        tiltingParent = be.getBlockPos();
    }

    @Override
    public void beforeAssembly() {
        super.beforeAssembly();
        tiltingAssembling = true;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("ParentPos"))
            tiltingParent = NbtUtils.readBlockPos(compound, "ParentPos").orElse(tiltingParent);
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide && !tiltingAssembling)
            destroyParentBearing();
        super.remove();
    }

    private void destroyParentBearing() {
        if (tiltingParent == null || level == null)
            return;
        if (level.getBlockState(tiltingParent).is(ModBlocks.TILTING_SWIVEL_BEARING.get()))
            level.destroyBlock(tiltingParent, false);
    }
}
