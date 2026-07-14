package net.birdsys.createtiltingcontrol.client.tilting_swivel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TiltingSwivelBearingRenderer extends KineticBlockEntityRenderer<SwivelBearingBlockEntity> {

    public TiltingSwivelBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SwivelBearingBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        BlockState state = be.getBlockState();
        Direction.Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
        SuperByteBuffer cogwheel = kineticRotationTransform(
                CachedBuffers.partialFacingVertical(ModPartialModels.TILTING_SWIVEL_BEARING_COG, state,
                        state.getValue(SwivelBearingBlock.FACING).getOpposite()),
                be.getExtraKinetics(), axis,
                getAngleForBe(be.getExtraKinetics(), be.getBlockPos(), axis), light);
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        cogwheel.renderInto(ms, vb);

        if (!be.isAssembled())
            renderRotatingBuffer(be,
                    CachedBuffers.partialFacing(SimPartialModels.SHAFT_SIXTEENTH, state,
                            state.getValue(SwivelBearingBlock.FACING)),
                    ms, vb, light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(SwivelBearingBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(SimPartialModels.SHAFT_SIXTEENTH, state,
                state.getValue(SwivelBearingBlock.FACING).getOpposite());
    }
}
