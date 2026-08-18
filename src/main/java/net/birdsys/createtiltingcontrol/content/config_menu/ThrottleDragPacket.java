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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record ThrottleDragPacket(BlockPos pos, int phase, int throttle) implements CustomPacketPayload {

    public enum Phase {
        GRAB, MOVE, RELEASE;

        private static final Phase[] VALUES = values();

        static Phase of(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : RELEASE;
        }
    }

    public static ThrottleDragPacket grab(BlockPos pos) {
        return new ThrottleDragPacket(pos, Phase.GRAB.ordinal(), 0);
    }

    public static ThrottleDragPacket move(BlockPos pos, int throttle) {
        return new ThrottleDragPacket(pos, Phase.MOVE.ordinal(), throttle);
    }

    public static ThrottleDragPacket release(BlockPos pos) {
        return new ThrottleDragPacket(pos, Phase.RELEASE.ordinal(), 0);
    }

    public static final Type<ThrottleDragPacket> TYPE =
            new Type<>(CreateTiltingControlMod.loc("throttle_drag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrottleDragPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ThrottleDragPacket::pos,
                    ByteBufCodecs.VAR_INT, ThrottleDragPacket::phase,
                    ByteBufCodecs.VAR_INT, ThrottleDragPacket::throttle,
                    ThrottleDragPacket::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ThrottleDragPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos()))
                return;
            if (!(level.getBlockEntity(packet.pos()) instanceof BidirectionalThrottleLeverBlockEntity lever))
                return;

            switch (Phase.of(packet.phase())) {
                case GRAB -> lever.tryGrab(player);
                case MOVE -> lever.applyDrag(player.getUUID(), packet.throttle());
                case RELEASE -> lever.releaseGrip(player.getUUID());
            }
        });
    }
}
