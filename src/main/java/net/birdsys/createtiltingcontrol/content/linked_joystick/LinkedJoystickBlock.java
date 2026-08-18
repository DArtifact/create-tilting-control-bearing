package net.birdsys.createtiltingcontrol.content.linked_joystick;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

public class LinkedJoystickBlock extends DirectionalBlock
        implements IBE<LinkedJoystickBlockEntity>, IWrenchable {

    public static final DirectionProperty ROTATION =
            DirectionProperty.create("rotation", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public static final MapCodec<LinkedJoystickBlock> CODEC = simpleCodec(LinkedJoystickBlock::new);

    public LinkedJoystickBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(ROTATION, Direction.SOUTH)
                .setValue(LINKED, false));
    }

    @Override
    protected @NonNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION, LINKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction rotation = face.getAxis().isVertical()
                ? context.getHorizontalDirection().getOpposite()
                : Direction.SOUTH;
        return defaultBlockState()
                .setValue(FACING, face)
                .setValue(ROTATION, rotation);
    }

    @Override
    protected @NonNull BlockState rotate(@NonNull BlockState state, Rotation rotation) {
        return state
                .setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(ROTATION, rotation.rotate(state.getValue(ROTATION)));
    }

    @Override
    protected @NonNull BlockState mirror(@NonNull BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(ROTATION)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis() == state.getValue(FACING).getAxis()) {
            Level level = context.getLevel();
            BlockState rotated = state.setValue(ROTATION, state.getValue(ROTATION).getClockWise());
            if (!level.isClientSide)
                level.setBlock(context.getClickedPos(), rotated, 3);
            IWrenchable.playRotateSound(level, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        return IWrenchable.super.onWrenched(state, context);
    }

    public static Quaternionf getOrientation(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction rotation = state.getValue(ROTATION);
        int yaw = switch (rotation) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
        switch (facing) {
            case DOWN -> {
                return new Quaternionf()
                        .rotateY((float) Math.toRadians(-((180 + yaw) % 360)))
                        .rotateX((float) Math.toRadians(-180));
            }
            case NORTH, SOUTH, EAST, WEST -> {
                int wallY = switch (facing) {
                    case SOUTH -> 180;
                    case EAST -> 90;
                    case WEST -> 270;
                    default -> 0;
                };
                return new Quaternionf()
                        .rotateY((float) Math.toRadians(-wallY))
                        .rotateX((float) Math.toRadians(-90))
                        .rotateY((float) Math.toRadians(-yaw));
            }
            default -> {
                return new Quaternionf()
                        .rotateY((float) Math.toRadians(-yaw));
            }
        }
    }

    @Override
    protected @NonNull ItemInteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                       @NonNull Level level, @NonNull BlockPos pos, Player player,
                                                       @NonNull InteractionHand hand, @NonNull BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.mayBuild())
            return ItemInteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> {
                if (be.hasUser()) {
                    if (!be.isUser(player.getUUID())) {
                        player.displayClientMessage(
                                Component.translatable("create_tilting_control.joystick.busy"), true);
                        return;
                    }
                    be.unlink();
                }
                if (player instanceof ServerPlayer serverPlayer)
                    serverPlayer.openMenu(be, buf -> buf.writeBlockPos(pos));
            });
            return ItemInteractionResult.SUCCESS;
        }

        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        withBlockEntityDo(level, pos, be -> {
            if (be.isUser(player.getUUID())) {
                if (!be.isHoldMode())
                    be.unlink();
                return;
            }
            if (!be.tryLink(player, be.isHoldMode())) {
                String key = be.hasUser()
                        ? "create_tilting_control.joystick.busy"
                        : "create_tilting_control.joystick.already_linked";
                player.displayClientMessage(Component.translatable(key), true);
            }
        });
        return ItemInteractionResult.SUCCESS;
    }

    private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(
            Shapes.or(
                    Block.box(3, 0, 3, 13, 3, 13),
                    Block.box(6.5, 3, 6.5, 9.5, 12, 9.5)),
            Direction.UP);

    private static final VoxelShaper COLLISION_SHAPE = VoxelShaper.forDirectional(
            Block.box(3, 0, 3, 13, 3, 13),
            Direction.UP);

    @Override
    public @NonNull VoxelShape getShape(BlockState state, @NonNull BlockGetter level,
                                        @NonNull BlockPos pos, @NonNull CollisionContext ctx) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(BlockState state, @NonNull BlockGetter level,
                                                    @NonNull BlockPos pos, @NonNull CollisionContext ctx) {
        return COLLISION_SHAPE.get(state.getValue(FACING));
    }

    @Override
    public Class<LinkedJoystickBlockEntity> getBlockEntityClass() {
        return LinkedJoystickBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LinkedJoystickBlockEntity> getBlockEntityType() {
        return ModBlockEntities.LINKED_JOYSTICK.get();
    }
}
