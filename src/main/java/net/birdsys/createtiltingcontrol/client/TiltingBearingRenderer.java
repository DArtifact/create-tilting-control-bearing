package net.birdsys.createtiltingcontrol.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.TiltingBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class TiltingBearingRenderer extends KineticBlockEntityRenderer<TiltingBearingBlockEntity> {

    public TiltingBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(TiltingBearingBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
        Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Quaternionf tiltQuat = new Quaternionf(be.previousTiltQuat).slerp(be.tiltQuat, partialTicks);

        SuperByteBuffer superBuffer = CachedBuffers.partial(ModPartialModels.TILTING_BEARING_PLATE, be.getBlockState());
        superBuffer.translate(normal.scale(0.25));
        superBuffer.rotateCentered(tiltQuat);
        superBuffer.translate(normal.scale(-0.25));
        float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1);
        kineticRotationTransform(superBuffer, be, facing.getAxis(), (float) (interpolatedAngle / 180f * Math.PI), light);
        if (facing.getAxis().isHorizontal())
            superBuffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
        superBuffer.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TiltingBearingBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state,
                state.getValue(BearingBlock.FACING).getOpposite());
    }
}
