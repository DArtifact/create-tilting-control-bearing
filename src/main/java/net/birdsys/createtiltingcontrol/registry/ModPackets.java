package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.config_menu.*;
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
        registrar.playToServer(
                ConfigureGyroSettingsPacket.TYPE,
                ConfigureGyroSettingsPacket.STREAM_CODEC,
                ConfigureGyroSettingsPacket::handle);
        registrar.playToServer(
                ConfigureJoystickSettingsPacket.TYPE,
                ConfigureJoystickSettingsPacket.STREAM_CODEC,
                ConfigureJoystickSettingsPacket::handle);
        registrar.playToServer(
                JoystickControlPacket.TYPE,
                JoystickControlPacket.STREAM_CODEC,
                JoystickControlPacket::handle);
        registrar.playToServer(
                JoystickInputPacket.TYPE,
                JoystickInputPacket.STREAM_CODEC,
                JoystickInputPacket::handle);
        registrar.playToServer(
                JoystickUnlinkPacket.TYPE,
                JoystickUnlinkPacket.STREAM_CODEC,
                JoystickUnlinkPacket::handle);
        registrar.playToServer(
                ThrottleDragPacket.TYPE,
                ThrottleDragPacket.STREAM_CODEC,
                ThrottleDragPacket::handle);
        registrar.playToServer(
                ConfigureThrottleLeverPacket.TYPE,
                ConfigureThrottleLeverPacket.STREAM_CODEC,
                ConfigureThrottleLeverPacket::handle);
    }
}