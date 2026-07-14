package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureTiltSettingsPacket;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureTurretModePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModPackets {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ConfigureTiltSettingsPacket.TYPE,
                ConfigureTiltSettingsPacket.STREAM_CODEC,
                ConfigureTiltSettingsPacket::handle);
        registrar.playToServer(
                ConfigureTurretModePacket.TYPE,
                ConfigureTurretModePacket.STREAM_CODEC,
                ConfigureTurretModePacket::handle);
    }
}
