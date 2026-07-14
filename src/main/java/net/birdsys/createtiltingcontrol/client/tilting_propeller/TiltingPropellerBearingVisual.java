package net.birdsys.createtiltingcontrol.client.tilting_propeller;

import org.joml.Quaternionf;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TiltingPropellerBearingVisual extends OrientedRotatingVisual<TiltingPropellerBearingBlockEntity>
        implements SimpleDynamicVisual {

    private final TransformedInstance topInstance;
    private final Axis rotationAxis;
    private final Quaternionf blockOrientation;

    public TiltingPropellerBearingVisual(VisualizationContext context, TiltingPropellerBearingBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Direction.SOUTH,
                blockEntity.getBlockState().getValue(BlockStateProperties.FACING).getOpposite(),
                Models.partial(AllPartialModels.SHAFT_HALF));
        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        this.rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE, rotationAxis()).step());
        this.blockOrientation = SimMathUtils.getBlockStateOrientation(facing);
        this.topInstance = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ModPartialModels.TILTING_PROPELLER_BEARING_PLATE))
                .createInstance();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        float interpolatedAngle = blockEntity.getInterpolatedAngle(ctx.partialTick() - 1);
        Quaternionf tilt = new Quaternionf(blockEntity.previousTiltQuat)
                .slerp(new Quaternionf(blockEntity.tiltQuat), ctx.partialTick());

        topInstance.setIdentityTransform();
        topInstance.translate(getVisualPosition());
        topInstance
                .translate((float) (blockEntity.blockNormal.x * 0.25),
                        (float) (blockEntity.blockNormal.y * 0.25),
                        (float) (blockEntity.blockNormal.z * 0.25))
                .rotateCentered(tilt)
                .rotateCentered(rotationAxis.rotationDegrees(interpolatedAngle))
                .translate((float) (blockEntity.blockNormal.x * -0.25),
                        (float) (blockEntity.blockNormal.y * -0.25),
                        (float) (blockEntity.blockNormal.z * -0.25));
        topInstance.rotateCentered(blockOrientation);
        topInstance.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(topInstance);
    }

    @Override
    protected void _delete() {
        super._delete();
        topInstance.delete();
    }
}
