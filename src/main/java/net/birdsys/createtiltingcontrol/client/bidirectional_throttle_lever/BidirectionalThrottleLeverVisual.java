package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

import java.util.function.Consumer;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BidirectionalThrottleLeverVisual
        extends AbstractBlockEntityVisual<BidirectionalThrottleLeverBlockEntity>
        implements SimpleDynamicVisual {

    public static final float MAX_VISUAL_TILT_DEGREES =
            BidirectionalThrottleLeverBlockEntity.MAX_TILT_DEGREES;
    public static final float PIVOT_OFFSET = 5f / 16f - 0.5f;

    private final TransformedInstance handleInstance;
    private final Quaternionf blockOrientation;

    public BidirectionalThrottleLeverVisual(VisualizationContext context,
                                            BidirectionalThrottleLeverBlockEntity blockEntity,
                                            float partialTick) {
        super(context, blockEntity, partialTick);
        this.blockOrientation = getOrientation(blockState);
        this.handleInstance = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED,
                        Models.partial(ModPartialModels.BIDIRECTIONAL_THROTTLE_LEVER_HANDLE))
                .createInstance();
        animate(partialTick);
    }

    public static Quaternionf getOrientation(BlockState state) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        int yaw = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        return new Quaternionf().rotateY((float) Math.toRadians(-yaw));
    }

    public static Quaternionf getTilt(BidirectionalThrottleLeverBlockEntity be, float partialTick) {
        float throttle = be.lerpedThrottle.getValue(partialTick)
                / BidirectionalThrottleLeverBlockEntity.MAX_THROTTLE;
        return new Quaternionf().rotateX((float) Math.toRadians(-throttle * MAX_VISUAL_TILT_DEGREES));
    }

    public static void applyHandleTransform(PoseStack stack, BlockState state,
                                            BidirectionalThrottleLeverBlockEntity be, float partialTick) {
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(getOrientation(state));
        stack.translate(-0.5f, -0.5f, -0.5f);
        stack.translate(0, PIVOT_OFFSET, 0);
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(getTilt(be, partialTick));
        stack.translate(-0.5f, -0.5f, -0.5f);
        stack.translate(0, -PIVOT_OFFSET, 0);
    }

    @Override
    public void beginFrame(Context ctx) {
        animate(ctx.partialTick());
    }

    private void animate(float partialTick) {
        handleInstance.setIdentityTransform();
        handleInstance.translate(getVisualPosition());
        handleInstance.rotateCentered(blockOrientation);
        handleInstance
                .translate(0, PIVOT_OFFSET, 0)
                .rotateCentered(getTilt(blockEntity, partialTick))
                .translate(0, -PIVOT_OFFSET, 0);
        handleInstance.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(handleInstance);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(handleInstance);
    }

    @Override
    protected void _delete() {
        handleInstance.delete();
    }
}
