package net.birdsys.createtiltingcontrol.client.linked_joystick;

import java.util.function.Consumer;

import org.joml.Quaternionf;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlock;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;

public class LinkedJoystickVisual extends AbstractBlockEntityVisual<LinkedJoystickBlockEntity>
        implements SimpleDynamicVisual {

    public static final float MAX_VISUAL_TILT_DEGREES = 22f;
    private static final float PIVOT_OFFSET = 3f / 16f - 0.5f;

    private final TransformedInstance leverInstance;
    private final Quaternionf blockOrientation;

    public LinkedJoystickVisual(VisualizationContext context, LinkedJoystickBlockEntity blockEntity,
                                float partialTick) {
        super(context, blockEntity, partialTick);
        this.blockOrientation = LinkedJoystickBlock.getOrientation(blockState);
        this.leverInstance = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        blockState.getValue(LinkedJoystickBlock.LINKED)
                                ? ModPartialModels.LINKED_JOYSTICK_LEVER_LINKED
                                : ModPartialModels.LINKED_JOYSTICK_LEVER))
                .createInstance();
        animate(partialTick);
    }

    @Override
    public void beginFrame(Context ctx) {
        animate(ctx.partialTick());
    }

    private void animate(float partialTick) {
        float x = JoystickControlClient.isControlling(pos)
                ? JoystickControlClient.joyX() : blockEntity.lerpedX.getValue(partialTick);
        float y = JoystickControlClient.isControlling(pos)
                ? JoystickControlClient.joyY() : blockEntity.lerpedY.getValue(partialTick);

        Quaternionf tilt = new Quaternionf()
                .rotateX((float) Math.toRadians(y * MAX_VISUAL_TILT_DEGREES))
                .rotateZ((float) Math.toRadians(-x * MAX_VISUAL_TILT_DEGREES));

        leverInstance.setIdentityTransform();
        leverInstance.translate(getVisualPosition());
        leverInstance.rotateCentered(blockOrientation);
        leverInstance
                .translate(0, PIVOT_OFFSET, 0)
                .rotateCentered(tilt)
                .translate(0, -PIVOT_OFFSET, 0);
        leverInstance.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(leverInstance);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(leverInstance);
    }

    @Override
    protected void _delete() {
        leverInstance.delete();
    }
}