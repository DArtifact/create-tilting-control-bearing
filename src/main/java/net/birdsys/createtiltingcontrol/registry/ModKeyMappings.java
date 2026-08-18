package net.birdsys.createtiltingcontrol.registry;

import com.mojang.blaze3d.platform.InputConstants;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {

    public static final KeyMapping JOYSTICK_CONTROL = new KeyMapping(
            "key.create_tilting_control.joystick_control",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_TAB,
            "key.categories.create_tilting_control");

    public static final KeyMapping JOYSTICK_MODIFIER_1 = new KeyMapping(
            "key.create_tilting_control.joystick_modifier_1",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_S,
            "key.categories.create_tilting_control");

    public static final KeyMapping JOYSTICK_MODIFIER_2 = new KeyMapping(
            "key.create_tilting_control.joystick_modifier_2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_W,
            "key.categories.create_tilting_control");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JOYSTICK_CONTROL);
        event.register(JOYSTICK_MODIFIER_1);
        event.register(JOYSTICK_MODIFIER_2);
    }
}
