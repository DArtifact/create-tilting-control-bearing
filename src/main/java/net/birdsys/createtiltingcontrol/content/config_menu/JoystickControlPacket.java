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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record JoystickControlPacket(BlockPos pos, boolean entering) implements CustomPacketPayload {

    public static final Type<JoystickControlPacket> TYPE =
            new Type<>(CreateTiltingControlMod.loc("joystick_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JoystickControlPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, JoystickControlPacket::pos,
                    ByteBufCodecs.BOOL, JoystickControlPacket::entering,
                    JoystickControlPacket::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JoystickControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof LinkedJoystickBlockEntity joystick))
                return;
            joystick.setControlActive(player.getUUID(), packet.entering());
        });
    }
}
