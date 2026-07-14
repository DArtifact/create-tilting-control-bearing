package net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.plate;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public class TiltingSwivelBearingPlateBlock extends SwivelBearingPlateBlock implements BlockSubLevelCollisionShape {

    public TiltingSwivelBearingPlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends SwivelBearingPlateBlockEntity> getBlockEntityType() {
        return ModBlockEntities.TILTING_SWIVEL_BEARING_PLATE.get();
    }

    @Override
    public @NonNull ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos pos,
                                                @NonNull BlockState state) {
        return new ItemStack(ModBlocks.TILTING_SWIVEL_BEARING_ITEM.get());
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
        return Shapes.empty();
    }
}
