package net.birdsys.createtiltingcontrol.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;

public class ModPartialModels {
    public static final PartialModel TILTING_PROPELLER_BEARING_PLATE = block("tilting_propeller_bearing/bearing_plate");
    public static final PartialModel TILTING_SWIVEL_BEARING_COG = block("tilting_swivel_bearing/ironcog");

    private static PartialModel block(String path) {
        return PartialModel.of(CreateTiltingControlMod.loc("block/" + path));
    }

    public static void register() {}
}
