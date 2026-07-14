package net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;

import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public class TiltingPropellerBearingBlock extends BearingBlock implements IBE<TiltingPropellerBearingBlockEntity> {

    public TiltingPropellerBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull ItemInteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hitResult) {
        if (!player.mayBuild())
            return ItemInteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> {
                if (player instanceof ServerPlayer serverPlayer)
                    serverPlayer.openMenu(be, buf -> buf.writeBlockPos(pos));
            });
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.isEmpty()) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> {
                if (be.isRunning()) {
                    be.startDisassemblySlowdown();
                    return;
                }
                be.setAssembleNextTick(true);
            });
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = super.onWrenched(state, context);
        if (!result.consumesAction())
            return result;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState newState = getRotatedBlockState(state, context.getClickedFace());
        if (level.isClientSide) {
            level.setBlock(pos, newState, 2);
            withBlockEntityDo(level, pos, be -> be.forceTilt(newState));
        } else {
            withBlockEntityDo(level, pos, be -> be.wrenchRotate(newState));
        }
        return result;
    }

    private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(
            Shapes.or(
                    Block.box(5, 0, 5, 11, 6, 11),
                    Block.box(0, 7, 0, 16, 16, 16)),
            Direction.UP);

    @Override
    public @NonNull VoxelShape getShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext ctx) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public Class<TiltingPropellerBearingBlockEntity> getBlockEntityClass() {
        return TiltingPropellerBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TiltingPropellerBearingBlockEntity> getBlockEntityType() {
        return ModBlockEntities.TILTING_PROPELLER_BEARING.get();
    }

}
