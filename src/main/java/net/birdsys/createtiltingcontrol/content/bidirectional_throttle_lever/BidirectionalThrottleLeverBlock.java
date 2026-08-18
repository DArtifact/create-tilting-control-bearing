package net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever.ThrottleDragClient;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class BidirectionalThrottleLeverBlock extends HorizontalDirectionalBlock
        implements IBE<BidirectionalThrottleLeverBlockEntity>, IWrenchable {

    public static final MapCodec<BidirectionalThrottleLeverBlock> CODEC =
            simpleCodec(BidirectionalThrottleLeverBlock::new);

    public BidirectionalThrottleLeverBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NonNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected @NonNull BlockState rotate(@NonNull BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NonNull BlockState mirror(@NonNull BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean canSurvive(@NonNull BlockState state, @NonNull LevelReader level, @NonNull BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull Direction direction,
                                              @NonNull BlockState neighborState, @NonNull LevelAccessor level,
                                              @NonNull BlockPos pos, @NonNull BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected @NonNull ItemInteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                       @NonNull Level level, @NonNull BlockPos pos,
                                                       @NonNull Player player, @NonNull InteractionHand hand,
                                                       @NonNull BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level,
                                                        @NonNull BlockPos pos, @NonNull Player player,
                                                        @NonNull BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        return interact(state, level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private boolean interact(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.mayBuild())
            return false;

        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                return true;
            withBlockEntityDo(level, pos, be -> {
                if (player instanceof ServerPlayer serverPlayer)
                    serverPlayer.openMenu(be, buf -> buf.writeBlockPos(pos));
            });
            return true;
        }

        if (level.isClientSide && player.isLocalPlayer())
            ThrottleDragClient.startDrag(pos);
        return true;
    }

    @Override
    protected boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter level,
                            @NonNull BlockPos pos, @NonNull Direction side) {
        Direction facing = state.getValue(FACING);
        return getBlockEntityOptional(level, pos).map(be -> {
            if (side == facing.getOpposite())
                return be.getForwardPower();
            if (side == facing)
                return be.getBackwardPower();
            if (side == Direction.UP)
                return Math.abs(be.getThrottle());
            return 0;
        }).orElse(0);
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter level,
                                  @NonNull BlockPos pos, @NonNull Direction side) {
        if (side != Direction.UP)
            return 0;
        return getBlockEntityOptional(level, pos)
                .map(be -> Math.abs(be.getThrottle()))
                .orElse(0);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return side != null && side.getAxis() == state.getValue(FACING).getAxis();
    }

    static void updateNeighbors(BlockState state, @Nullable Level level, BlockPos pos) {
        if (level == null || level.isClientSide)
            return;
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.below(), state.getBlock());
    }

    private static final VoxelShaper BASE_SHAPE = VoxelShaper.forHorizontal(
            Shapes.or(
                    Block.box(4.5, 0,     2,   11.5, 3,     14),
                    Block.box(5,   3,     5.5, 11,   6,     10.5)),
            Direction.NORTH);

    public static final VoxelShape HANDLE_SHAPE = Shapes.or(
            Block.box(7,   4,     7,   9,    20,    9),
            Block.box(6.5, 13.25, 6.5, 9.5,  19.25, 9.5));

    @Override
    protected @NonNull VoxelShape getShape(BlockState state, @NonNull BlockGetter level,
                                           @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return BASE_SHAPE.get(state.getValue(FACING));
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(BlockState state, @NonNull BlockGetter level,
                                                    @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(@NonNull BlockState state, @NonNull PathComputationType type) {
        return false;
    }

    @Override
    public Class<BidirectionalThrottleLeverBlockEntity> getBlockEntityClass() {
        return BidirectionalThrottleLeverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BidirectionalThrottleLeverBlockEntity> getBlockEntityType() {
        return ModBlockEntities.BIDIRECTIONAL_THROTTLE_LEVER.get();
    }
}
