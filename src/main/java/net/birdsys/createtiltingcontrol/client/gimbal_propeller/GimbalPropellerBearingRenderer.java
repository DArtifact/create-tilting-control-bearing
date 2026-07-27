package net.birdsys.createtiltingcontrol.client.gimbal_propeller;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.birdsys.createtiltingcontrol.client.tilting_propeller.TiltingPropellerBearingRenderer;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class GimbalPropellerBearingRenderer extends TiltingPropellerBearingRenderer {

    public GimbalPropellerBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected PartialModel plateModel() {
        return ModPartialModels.GIMBAL_PROPELLER_BEARING_PLATE;
    }
}
