package net.birdsys.createtiltingcontrol.client.gimbal_propeller;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.birdsys.createtiltingcontrol.client.tilting_propeller.TiltingPropellerBearingVisual;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.GimbalPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;

public class GimbalPropellerBearingVisual extends TiltingPropellerBearingVisual {

    public GimbalPropellerBearingVisual(VisualizationContext context, GimbalPropellerBearingBlockEntity blockEntity,
                                        float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    protected PartialModel plateModel() {
        return ModPartialModels.GIMBAL_PROPELLER_BEARING_PLATE;
    }
}
