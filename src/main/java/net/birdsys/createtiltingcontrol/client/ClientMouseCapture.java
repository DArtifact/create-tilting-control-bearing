package net.birdsys.createtiltingcontrol.client;

import net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever.ThrottleDragClient;
import net.birdsys.createtiltingcontrol.client.linked_joystick.JoystickControlClient;
import net.minecraft.client.Minecraft;

public final class ClientMouseCapture {

    private ClientMouseCapture() {}

    public static boolean tryFeed(double deltaX, double deltaY) {
        if (Minecraft.getInstance().screen != null)
            return false;

        if (JoystickControlClient.isControlling()) {
            JoystickControlClient.feedMouseDelta(deltaX, deltaY);
            return true;
        }

        if (ThrottleDragClient.isDragging()) {
            ThrottleDragClient.feedMouseDelta(deltaX, deltaY);
            return true;
        }

        return false;
    }
}
