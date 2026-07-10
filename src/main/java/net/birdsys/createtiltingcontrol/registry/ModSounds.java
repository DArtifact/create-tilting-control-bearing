package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/*
Unused.
Couldn't make it sound decent.
 */
public class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateTiltingControlMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TILTING_BEARING_TILT =
            SOUND_EVENTS.register("tilting_bearing_tilt",
                    () -> SoundEvent.createVariableRangeEvent(CreateTiltingControlMod.loc("tilting_bearing_tilt")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
