package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.birdsys.createtiltingcontrol.content.config_menu.ThrottleDragPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ThrottleDragClient {

    private static final float PIXELS_TO_FULL_DEFLECTION = 240f;

    @Nullable
    private static BlockPos dragPos;
    private static float value;
    private static int lastSentLevel;

    private static float animatedValue;
    private static float lastAnimatedValue;

    private static int dragTicks;

    private static final int GRAB_GRACE_TICKS = 10;

    private ThrottleDragClient() {}

    public static boolean isDragging() {
        return dragPos != null;
    }

    public static boolean isDragging(BlockPos pos) {
        return pos.equals(dragPos);
    }

    public static int displayLevel() {
        return lastSentLevel;
    }

    public static float animatedValue(float partialTicks) {
        return Mth.lerp(partialTicks, lastAnimatedValue, animatedValue);
    }

    public static void startDrag(BlockPos pos) {
        if (pos.equals(dragPos))
            return;

        BidirectionalThrottleLeverBlockEntity be = beAt(pos);
        if (be == null)
            return;

        dragPos = pos.immutable();
        lastSentLevel = be.getThrottle();
        value = lastSentLevel / (float) BidirectionalThrottleLeverBlockEntity.MAX_THROTTLE;
        animatedValue = lastAnimatedValue = value;
        dragTicks = 0;
        PacketDistributor.sendToServer(ThrottleDragPacket.grab(dragPos));
    }

    public static void stopIfDragging(BlockPos pos) {
        if (pos.equals(dragPos))
            stop(true);
    }

    private static void stop(boolean notifyServer) {
        BlockPos pos = dragPos;
        dragPos = null;
        value = 0;
        if (notifyServer && pos != null)
            PacketDistributor.sendToServer(ThrottleDragPacket.release(pos));
    }

    public static void feedMouseDelta(double deltaX, double deltaY) {
        if (dragPos == null)
            return;

        value = Mth.clamp(value - (float) deltaY / PIXELS_TO_FULL_DEFLECTION, -1f, 1f);
        int level = Math.round(value * BidirectionalThrottleLeverBlockEntity.MAX_THROTTLE);

        BidirectionalThrottleLeverBlockEntity be = beAt(dragPos);
        if (be != null)
            be.applyClientPreview(level);

        if (level == lastSentLevel)
            return;
        lastSentLevel = level;
        PacketDistributor.sendToServer(ThrottleDragPacket.move(dragPos, level));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (dragPos == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stop(false);
            return;
        }

        BidirectionalThrottleLeverBlockEntity be = beAt(dragPos);
        if (mc.screen != null || !isRightMouseDown(mc) || be == null) {
            stop(true);
            return;
        }

        if (!be.isWithinGripRange(mc.player)) {
            stop(true);
            return;
        }

        if (++dragTicks > GRAB_GRACE_TICKS && !be.hasHolder()) {
            stop(false);
            return;
        }

        lastAnimatedValue = animatedValue;
        animatedValue = animatedValue * 0.15f + value * 0.85f;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isDragging())
            return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onCrosshairRender(RenderGuiLayerEvent.Pre event) {
        if (isDragging() && VanillaGuiLayers.CROSSHAIR.equals(event.getName()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop(false);
    }

    private static boolean isRightMouseDown(Minecraft mc) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    @Nullable
    private static BidirectionalThrottleLeverBlockEntity beAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return null;
        BlockEntity be = mc.level.getBlockEntity(pos);
        return be instanceof BidirectionalThrottleLeverBlockEntity lever ? lever : null;
    }
}
