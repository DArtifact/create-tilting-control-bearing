package net.birdsys.createtiltingcontrol.client.tilting_swivel;

import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TiltingSwivelBearingVisual extends OrientedRotatingVisual<SwivelBearingBlockEntity> {

    private final RotatingInstance topShaft;
    private final RotatingInstance cogInstance;

    public TiltingSwivelBearingVisual(VisualizationContext context, SwivelBearingBlockEntity blockEntity,
                                      float partialTick) {
        super(context, blockEntity, partialTick, Direction.SOUTH,
                blockEntity.getBlockState().getValue(BlockStateProperties.FACING).getOpposite(),
                Models.partial(SimPartialModels.SHAFT_SIXTEENTH));

        topShaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(SimPartialModels.SHAFT_SIXTEENTH))
                .createInstance()
                .rotateToFace(Direction.SOUTH, blockEntity.getBlockState().getValue(BlockStateProperties.FACING))
                .setup(blockEntity)
                .setPosition(getVisualPosition());

        cogInstance = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.TILTING_SWIVEL_BEARING_COG))
                .createInstance()
                .rotateToFace(Direction.UP,
                        blockEntity.getBlockState().getValue(BlockStateProperties.FACING).getOpposite())
                .setup(blockEntity.getExtraKinetics())
                .setPosition(getVisualPosition());

        topShaft.setVisible(!blockEntity.isAssembled());
        topShaft.setChanged();
        cogInstance.setChanged();
    }

    @Override
    public void update(float pt) {
        super.update(pt);
        topShaft.setVisible(!blockEntity.isAssembled());
        topShaft.setup(blockEntity).setChanged();
        cogInstance.setup(blockEntity.getExtraKinetics()).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(topShaft);
        relight(cogInstance);
    }

    @Override
    protected void _delete() {
        super._delete();
        topShaft.delete();
        cogInstance.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(topShaft);
        consumer.accept(cogInstance);
    }
}
