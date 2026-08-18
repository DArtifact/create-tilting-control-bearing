package net.birdsys.createtiltingcontrol.content.config_menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record ConfigureJoystickSettingsPacket(BlockPos pos, double deadzone, int maxSignal, boolean holdMode, boolean latchMode)
        implements CustomPacketPayload {

    public static final Type<ConfigureJoystickSettingsPacket> TYPE =
            new Type<>(CreateTiltingControlMod.loc("configure_linked_joystick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureJoystickSettingsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ConfigureJoystickSettingsPacket::pos,
                    ByteBufCodecs.DOUBLE, ConfigureJoystickSettingsPacket::deadzone,
                    ByteBufCodecs.VAR_INT, ConfigureJoystickSettingsPacket::maxSignal,
                    ByteBufCodecs.BOOL, ConfigureJoystickSettingsPacket::holdMode,
                    ByteBufCodecs.BOOL, ConfigureJoystickSettingsPacket::latchMode,
                    ConfigureJoystickSettingsPacket::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureJoystickSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > 8 * 8 * 4)
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof LinkedJoystickBlockEntity joystick))
                return;
            if (joystick.hasUser() && !joystick.isUser(player.getUUID()))
                return;
            joystick.setJoystickSettings(packet.deadzone(), packet.maxSignal(), packet.holdMode(), packet.latchMode());
        });
    }
}