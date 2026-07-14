package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.JukeboxBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateTiltingControlMod.MODID);

    //Unused
    public static final DeferredHolder<SoundEvent, SoundEvent> TILTING_PROPELLER_BEARING_TILT =
            SOUND_EVENTS.register("tilting_propeller_bearing_tilt",
                    () -> SoundEvent.createVariableRangeEvent(CreateTiltingControlMod.loc("tilting_propeller_bearing_tilt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SOAR_V2 =
            SOUND_EVENTS.register("soar_v2",
                    () -> SoundEvent.createVariableRangeEvent(CreateTiltingControlMod.loc("soar_v2")));
    public static final ResourceKey<JukeboxSong> SOAR_V2_KEY = createSong("soar_v2");

    private static ResourceKey<JukeboxSong> createSong(String name){
        return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(CreateTiltingControlMod.MODID, name));
    }


    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
