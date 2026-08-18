package net.birdsys.createtiltingcontrol.content.config_menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
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

public record ConfigureThrottleLeverPacket(BlockPos pos, boolean autoReturn, int returnTicksPerLevel)
        implements CustomPacketPayload {

    public static final Type<ConfigureThrottleLeverPacket> TYPE =
            new Type<>(CreateTiltingControlMod.loc("configure_throttle_lever"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureThrottleLeverPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ConfigureThrottleLeverPacket::pos,
                    ByteBufCodecs.BOOL, ConfigureThrottleLeverPacket::autoReturn,
                    ByteBufCodecs.VAR_INT, ConfigureThrottleLeverPacket::returnTicksPerLevel,
                    ConfigureThrottleLeverPacket::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureThrottleLeverPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > 8 * 8 * 4)
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof BidirectionalThrottleLeverBlockEntity lever))
                return;
            if (lever.hasHolder() && !lever.isHolder(player.getUUID()))
                return;
            lever.setThrottleSettings(packet.autoReturn(), packet.returnTicksPerLevel());
        });
    }
}
