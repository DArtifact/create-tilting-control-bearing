package net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public class TiltingSwivelBearingBlock extends SwivelBearingBlock implements BlockSubLevelCollisionShape {

    public TiltingSwivelBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull ItemInteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                       @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull InteractionHand hand,
                                                       @NonNull BlockHitResult hitResult) {
        if (!player.mayBuild())
            return ItemInteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> {
                if (be instanceof TiltingSwivelBearingBlockEntity bearing
                        && player instanceof ServerPlayer serverPlayer)
                    serverPlayer.openMenu(bearing, buf -> buf.writeBlockPos(pos));
            });
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockEntityType<? extends SwivelBearingBlockEntity> getBlockEntityType() {
        return ModBlockEntities.TILTING_SWIVEL_BEARING.get();
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
        // Works better like this
        return Shapes.empty();
    }

}
