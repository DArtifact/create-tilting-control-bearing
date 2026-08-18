package net.birdsys.createtiltingcontrol.client.linked_joystick;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.utility.ControlsUtil;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.config_menu.JoystickControlPacket;
import net.birdsys.createtiltingcontrol.content.config_menu.JoystickInputPacket;
import net.birdsys.createtiltingcontrol.content.config_menu.JoystickUnlinkPacket;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModKeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class JoystickControlClient {

    private static final float PIXELS_TO_FULL_DEFLECTION = 240f;
    private static final float SEND_EPSILON = 0.005f;

    @Nullable
    private static BlockPos boundPos;
    private static boolean controlling;
    private static boolean temporary;

    private static float joyX;
    private static float joyY;
    private static boolean shiftDown;
    private static boolean spaceDown;

    private static float lastSentX = Float.NaN;
    private static float lastSentY = Float.NaN;
    private static boolean lastSentShift;
    private static boolean lastSentSpace;
    private static boolean prevPhysicalShift;
    private static boolean prevPhysicalSpace;

    private JoystickControlClient() {}

    public static boolean isLinked() {
        return boundPos != null;
    }

    public static boolean isControlling() {
        return boundPos != null && controlling;
    }

    public static boolean isControlling(BlockPos pos) {
        return controlling && pos.equals(boundPos);
    }

    @Nullable
    public static BlockPos boundPos() {
        return boundPos;
    }

    public static float joyX() {
        return joyX;
    }

    public static float joyY() {
        return joyY;
    }

    public static boolean shiftDown() {
        return shiftDown;
    }

    public static boolean spaceDown() {
        return spaceDown;
    }

    public static void onLinked(BlockPos pos, boolean temporaryLink) {
        boundPos = pos;
        temporary = temporaryLink;
        if (temporaryLink)
            beginControl();
        else
            message("linked", ModKeyMappings.JOYSTICK_CONTROL.getTranslatedKeyMessage());
    }

    public static void exitIfBound(BlockPos pos) {
        if (!pos.equals(boundPos))
            return;
        reset();
        message("unlinked");
    }

    public static void requestUnlink() {
        BlockPos pos = boundPos;
        if (pos == null)
            return;
        LinkedJoystickBlockEntity be = beAt(pos);
        boolean latch = be != null && be.isLatchMode();
        if (controlling && !latch)
            PacketDistributor.sendToServer(new JoystickInputPacket(pos, 0, 0, false, false));
        PacketDistributor.sendToServer(new JoystickUnlinkPacket(pos));
        if (be != null && !latch)
            be.applyClientPreview(0, 0);
        reset();
        message("unlinked");
    }

    private static void beginControl() {
        Minecraft mc = Minecraft.getInstance();
        controlling = true;
        LinkedJoystickBlockEntity be = boundBE();
        boolean latch = be != null && be.isLatchMode();
        joyX = be != null ? be.getJoyX() : 0;
        joyY = be != null ? be.getJoyY() : 0;
        shiftDown = latch && be.isShiftDown();
        spaceDown = latch && be.isSpaceDown();
        prevPhysicalShift = isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_MODIFIER_1);
        prevPhysicalSpace = isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_MODIFIER_2);
        lastSentX = Float.NaN;
        lastSentY = Float.NaN;
        lastSentShift = shiftDown;
        lastSentSpace = spaceDown;
        if (boundPos != null && !temporary)
            PacketDistributor.sendToServer(new JoystickControlPacket(boundPos, true));
    }

    private static void endControl(boolean notifyServer) {
        BlockPos pos = boundPos;
        boolean wasTemporary = temporary;
        controlling = false;
        LinkedJoystickBlockEntity be = pos != null ? beAt(pos) : null;
        boolean latch = be != null && be.isLatchMode();
        joyX = 0;
        joyY = 0;
        shiftDown = false;
        spaceDown = false;
        if (be != null && !latch)
            be.applyClientPreview(0, 0);
        if (notifyServer && pos != null) {
            if (!latch)
                PacketDistributor.sendToServer(new JoystickInputPacket(pos, 0, 0, false, false));
            PacketDistributor.sendToServer(new JoystickControlPacket(pos, false));
        }
        if (wasTemporary) {
            reset();
            message("unlinked");
        }
    }

    private static void reset() {
        boundPos = null;
        controlling = false;
        temporary = false;
        joyX = 0;
        joyY = 0;
        shiftDown = false;
        spaceDown = false;
        lastSentX = Float.NaN;
        lastSentY = Float.NaN;
    }

    public static void feedMouseDelta(double dx, double dy) {
        if (!isControlling())
            return;
        joyX = Mth.clamp(joyX + (float) dx / PIXELS_TO_FULL_DEFLECTION, -1f, 1f);
        joyY = Mth.clamp(joyY + (float) dy / PIXELS_TO_FULL_DEFLECTION, -1f, 1f);
        LinkedJoystickBlockEntity be = boundBE();
        if (be != null)
            be.applyClientPreview(joyX, joyY);
        sendIfNeeded();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (boundPos == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }

        LinkedJoystickBlockEntity be = boundBE();
        if (be == null || be.isRemoved()) {
            reset();
            return;
        }

        if (!controlling) {
            if (mc.screen == null && isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_CONTROL))
                beginControl();
            return;
        }

        if (mc.screen != null) {
            endControl(true);
            return;
        }

        if (temporary) {
            if (!isRightMouseDown(mc)) {
                endControl(true);
                return;
            }
        } else if (!isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_CONTROL)) {
            endControl(true);
            return;
        }

        boolean physicalShift = isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_MODIFIER_1);
        boolean physicalSpace = isPhysicallyDown(mc, ModKeyMappings.JOYSTICK_MODIFIER_2);
        if (be.isLatchMode()) {
            if (physicalShift != prevPhysicalShift)
                shiftDown = physicalShift;
            if (physicalSpace != prevPhysicalSpace)
                spaceDown = physicalSpace;
        } else {
            shiftDown = physicalShift;
            spaceDown = physicalSpace;
        }
        prevPhysicalShift = physicalShift;
        prevPhysicalSpace = physicalSpace;
        sendIfNeeded();

        for (KeyMapping mapping : ControlsUtil.getControls())
            drain(mapping);
        Options options = mc.options;
        drain(options.keyAttack);
        drain(options.keyUse);
        drain(options.keyPickItem);
        drain(options.keyDrop);
        drain(options.keySwapOffhand);
        for (KeyMapping hotbar : options.keyHotbarSlots)
            drain(hotbar);
        drain(options.keyPlayerList);
        drain(ModKeyMappings.JOYSTICK_CONTROL);
        drain(ModKeyMappings.JOYSTICK_MODIFIER_1);
        drain(ModKeyMappings.JOYSTICK_MODIFIER_2);
    }

    private static boolean isPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        if (key.getValue() == InputConstants.UNKNOWN.getValue())
            return false;
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static boolean isRightMouseDown(Minecraft mc) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {}
        mapping.setDown(false);
    }

    private static void sendIfNeeded() {
        if (boundPos == null || !controlling)
            return;
        boolean moved = Float.isNaN(lastSentX)
                || Math.abs(joyX - lastSentX) > SEND_EPSILON
                || Math.abs(joyY - lastSentY) > SEND_EPSILON
                || shiftDown != lastSentShift
                || spaceDown != lastSentSpace;
        if (!moved)
            return;
        lastSentX = joyX;
        lastSentY = joyY;
        lastSentShift = shiftDown;
        lastSentSpace = spaceDown;
        PacketDistributor.sendToServer(new JoystickInputPacket(boundPos, joyX, joyY, shiftDown, spaceDown));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isControlling())
            return;
        var input = event.getInput();
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onCrosshairRender(RenderGuiLayerEvent.Pre event) {
        if (isControlling() && VanillaGuiLayers.CROSSHAIR.equals(event.getName()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @Nullable
    private static LinkedJoystickBlockEntity boundBE() {
        return boundPos == null ? null : beAt(boundPos);
    }

    @Nullable
    private static LinkedJoystickBlockEntity beAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return null;
        BlockEntity be = mc.level.getBlockEntity(pos);
        return be instanceof LinkedJoystickBlockEntity joystick ? joystick : null;
    }

    private static void message(String key, Object... args) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
            player.displayClientMessage(
                    Component.translatable("create_tilting_control.joystick." + key, args), true);
    }
}