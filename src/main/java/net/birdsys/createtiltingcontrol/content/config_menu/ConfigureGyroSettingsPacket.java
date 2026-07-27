package net.birdsys.createtiltingcontrol.content.config_menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.GimbalPropellerBearingBlockEntity;
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

public record ConfigureGyroSettingsPacket(BlockPos pos, double gyroStrength, double onTiltGyroMult)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigureGyroSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(CreateTiltingControlMod.loc("configure_gimbal_gyro"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureGyroSettingsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ConfigureGyroSettingsPacket::pos,
                    ByteBufCodecs.DOUBLE, ConfigureGyroSettingsPacket::gyroStrength,
                    ByteBufCodecs.DOUBLE, ConfigureGyroSettingsPacket::onTiltGyroMult,
                    ConfigureGyroSettingsPacket::new);

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureGyroSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > 8 * 8 * 4)
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof GimbalPropellerBearingBlockEntity bearing))
                return;
            bearing.setGyroSettings(packet.gyroStrength(), packet.onTiltGyroMult());
        });
    }
}
