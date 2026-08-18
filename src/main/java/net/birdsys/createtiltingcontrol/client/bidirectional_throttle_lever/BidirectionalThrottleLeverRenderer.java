package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlock;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BidirectionalThrottleLeverRenderer
        extends SafeBlockEntityRenderer<BidirectionalThrottleLeverBlockEntity> {

    public BidirectionalThrottleLeverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(BidirectionalThrottleLeverBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (!be.isVirtual() && hit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(be.getBlockPos()))
            renderHandleOutline(be, partialTicks, ms, buffer);

        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        Quaternionf orientation = BidirectionalThrottleLeverVisual.getOrientation(be.getBlockState());
        Quaternionf tilt = BidirectionalThrottleLeverVisual.getTilt(be, partialTicks);

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                ModPartialModels.BIDIRECTIONAL_THROTTLE_LEVER_HANDLE, be.getBlockState());
        superBuffer.rotateCentered(orientation);
        superBuffer.translate(0, BidirectionalThrottleLeverVisual.PIVOT_OFFSET, 0);
        superBuffer.rotateCentered(tilt);
        superBuffer.translate(0, -BidirectionalThrottleLeverVisual.PIVOT_OFFSET, 0);
        superBuffer.light(light);
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private static void renderHandleOutline(BidirectionalThrottleLeverBlockEntity be, float partialTicks,
                                            PoseStack ms, MultiBufferSource buffer) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        ms.pushPose();
        BidirectionalThrottleLeverVisual.applyHandleTransform(ms, be.getBlockState(), be, partialTicks);
        renderShapeOutline(ms, consumer, BidirectionalThrottleLeverBlock.HANDLE_SHAPE, 0f, 0f, 0f, 0.4f);
        ms.popPose();
    }

    private static void renderShapeOutline(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape,
                                           float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;
            consumer.addVertex(pose.pose(), (float) x1, (float) y1, (float) z1)
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, dx, dy, dz);
            consumer.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, dx, dy, dz);
        });
    }
}
