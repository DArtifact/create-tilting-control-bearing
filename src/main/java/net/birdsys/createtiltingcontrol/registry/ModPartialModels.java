package net.birdsys.createtiltingcontrol.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;

public class ModPartialModels {
    public static final PartialModel TILTING_PROPELLER_BEARING_PLATE = block("tilting_propeller_bearing/bearing_plate");
    public static final PartialModel GIMBAL_PROPELLER_BEARING_PLATE = block("gimbal_propeller_bearing/bearing_plate");
    public static final PartialModel TILTING_SWIVEL_BEARING_COG = block("tilting_swivel_bearing/ironcog");
    public static final PartialModel LINKED_JOYSTICK_LEVER = block("linked_joystick/lever");
    public static final PartialModel LINKED_JOYSTICK_LEVER_LINKED = block("linked_joystick/lever_linked");
    public static final PartialModel BIDIRECTIONAL_THROTTLE_LEVER_HANDLE = block("bidirectional_throttle_lever/handle");

    private static PartialModel block(String path) {
        return PartialModel.of(CreateTiltingControlMod.loc("block/" + path));
    }

    public static void register() {}
}