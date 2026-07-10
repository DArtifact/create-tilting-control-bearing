package net.birdsys.createtiltingcontrol.content.tilting_bearing.menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.TiltingBearingBlockEntity;
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

public record ConfigureTiltingBearingPacket(BlockPos pos, double maxTiltAngle, double tiltSpeed)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigureTiltingBearingPacket> TYPE =
            new CustomPacketPayload.Type<>(CreateTiltingControlMod.loc("configure_tilting_bearing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureTiltingBearingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ConfigureTiltingBearingPacket::pos,
                    ByteBufCodecs.DOUBLE, ConfigureTiltingBearingPacket::maxTiltAngle,
                    ByteBufCodecs.DOUBLE, ConfigureTiltingBearingPacket::tiltSpeed,
                    ConfigureTiltingBearingPacket::new);

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureTiltingBearingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > 8 * 8 * 4)
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof TiltingBearingBlockEntity bearing))
                return;
            bearing.setTiltSettings(packet.maxTiltAngle(), packet.tiltSpeed());
        });
    }
}
