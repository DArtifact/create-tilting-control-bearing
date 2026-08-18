package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlock;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class ThrottleLeverPicking {

    private static final PoseStack STACK = new PoseStack();
    private static final Set<BidirectionalThrottleLeverBlockEntity> NEARBY = new HashSet<>();

    private ThrottleLeverPicking() {}

    public static void track(BidirectionalThrottleLeverBlockEntity lever) {
        if (!isInvalid(lever))
            NEARBY.add(lever);
    }

    public static Collection<BidirectionalThrottleLeverBlockEntity> getNearby() {
        return NEARBY;
    }

    private static boolean isInvalid(BidirectionalThrottleLeverBlockEntity lever) {
        if (lever.isRemoved())
            return true;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return true;
        double reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + 2.0;
        return player.distanceToSqr(lever.getBlockPos().getCenter()) > reach * reach;
    }

    @Nullable
    public static Double raycastHandle(Vec3 eyePosMoj, Vec3 viewVectorMoj,
                                       BidirectionalThrottleLeverBlockEntity lever, float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return null;

        BlockPos leverPos = lever.getBlockPos();
        Vector3d eyePos = new Vector3d(eyePosMoj.x, eyePosMoj.y, eyePosMoj.z);
        Vector3d viewVector = new Vector3d(viewVectorMoj.x, viewVectorMoj.y, viewVectorMoj.z);

        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(lever);
        if (subLevel != null) {
            Pose3dc pose = subLevel.renderPose(partialTicks);
            pose.transformPositionInverse(eyePos);
            pose.transformNormalInverse(viewVector);
        }

        STACK.pushPose();
        STACK.translate(leverPos.getX() - eyePos.x, leverPos.getY() - eyePos.y, leverPos.getZ() - eyePos.z);
        BidirectionalThrottleLeverVisual.applyHandleTransform(STACK, lever.getBlockState(), lever, partialTicks);
        Matrix4f inverse = new Matrix4f(STACK.last().pose()).invert();
        STACK.popPose();

        Vector3f localOrigin = inverse.transformPosition(new Vector3f());
        Vector3f localDirection = inverse.transformDirection(
                new Vector3f((float) viewVector.x, (float) viewVector.y, (float) viewVector.z));

        Vec3 start = new Vec3(localOrigin.x, localOrigin.y, localOrigin.z);
        Vec3 end = start.add(new Vec3(localDirection.x, localDirection.y, localDirection.z)
                .scale(player.blockInteractionRange()));

        BlockHitResult hit = BidirectionalThrottleLeverBlock.HANDLE_SHAPE.clip(start, end, BlockPos.ZERO);
        if (hit == null || hit.getType() == HitResult.Type.MISS)
            return null;

        Vec3 location = hit.getLocation();
        return start.distanceToSqr(location);
    }

    @EventBusSubscriber(modid = CreateTiltingControlMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    private static final class Events {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            NEARBY.removeIf(ThrottleLeverPicking::isInvalid);
        }
    }
}
