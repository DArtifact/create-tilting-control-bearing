package net.birdsys.createtiltingcontrol.foundation.mixin.client;

import net.birdsys.createtiltingcontrol.client.linked_joystick.JoystickControlClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class JoystickKeyboardHandlerMixin {

    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void createtiltingcontrol$interceptControlKeys(long window, int key, int scanCode,
                                                           int action, int modifiers, CallbackInfo ci) {
        if (!JoystickControlClient.isLinked())
            return;
        if (Minecraft.getInstance().screen != null)
            return;
        if (action != GLFW.GLFW_PRESS)
            return;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            JoystickControlClient.requestUnlink();
            ci.cancel();
        }
    }
}
