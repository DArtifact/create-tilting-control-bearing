package net.birdsys.createtiltingcontrol.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;

public class ModPartialModels {
    public static final PartialModel TILTING_BEARING_PLATE = block("tilting_bearing/bearing_plate");

    private static PartialModel block(String path) {
        return PartialModel.of(CreateTiltingControlMod.loc("block/" + path));
    }

    public static void register() {}
}
