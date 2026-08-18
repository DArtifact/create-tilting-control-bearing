package net.birdsys.createtiltingcontrol.content.config_menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record JoystickUnlinkPacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JoystickUnlinkPacket> TYPE =
            new CustomPacketPayload.Type<>(CreateTiltingControlMod.loc("joystick_unlink"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JoystickUnlinkPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, JoystickUnlinkPacket::pos,
                    JoystickUnlinkPacket::new);

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JoystickUnlinkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof LinkedJoystickBlockEntity joystick))
                return;
            if (!joystick.isUser(player.getUUID()))
                return;
            joystick.unlink();
        });
    }
}
