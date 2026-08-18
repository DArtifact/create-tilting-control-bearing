package net.birdsys.createtiltingcontrol.client.linked_joystick;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlock;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class LinkedJoystickRenderer extends SafeBlockEntityRenderer<LinkedJoystickBlockEntity> {

    private static final float PIVOT_OFFSET = 3f / 16f - 0.5f;

    public LinkedJoystickRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(LinkedJoystickBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        float x = JoystickControlClient.isControlling(be.getBlockPos())
                ? JoystickControlClient.joyX() : be.lerpedX.getValue(partialTicks);
        float y = JoystickControlClient.isControlling(be.getBlockPos())
                ? JoystickControlClient.joyY() : be.lerpedY.getValue(partialTicks);

        Quaternionf tilt = new Quaternionf()
                .rotateX((float) Math.toRadians(y * LinkedJoystickVisual.MAX_VISUAL_TILT_DEGREES))
                .rotateZ((float) Math.toRadians(-x * LinkedJoystickVisual.MAX_VISUAL_TILT_DEGREES));

        Quaternionf orientation = LinkedJoystickBlock.getOrientation(be.getBlockState());

        SuperByteBuffer superBuffer = CachedBuffers.partial(
                be.getBlockState().getValue(LinkedJoystickBlock.LINKED)
                        ? ModPartialModels.LINKED_JOYSTICK_LEVER_LINKED
                        : ModPartialModels.LINKED_JOYSTICK_LEVER,
                be.getBlockState());
        superBuffer.rotateCentered(orientation);
        superBuffer.translate(0, PIVOT_OFFSET, 0);
        superBuffer.rotateCentered(tilt);
        superBuffer.translate(0, -PIVOT_OFFSET, 0);
        superBuffer.light(light);
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}