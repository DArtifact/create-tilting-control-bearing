package net.birdsys.createtiltingcontrol.content.config_menu;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.TiltingSwivelBearingBlockEntity;
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

public record ConfigureTurretModePacket(BlockPos pos, boolean turretMode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigureTurretModePacket> TYPE =
            new CustomPacketPayload.Type<>(CreateTiltingControlMod.loc("configure_turret_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureTurretModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ConfigureTurretModePacket::pos,
                    ByteBufCodecs.BOOL, ConfigureTurretModePacket::turretMode,
                    ConfigureTurretModePacket::new);

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureTurretModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > 8 * 8 * 4)
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof TiltingSwivelBearingBlockEntity bearing))
                return;
            bearing.setTurretMode(packet.turretMode());
        });
    }
}
