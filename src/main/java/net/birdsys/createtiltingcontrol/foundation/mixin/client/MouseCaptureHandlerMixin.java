package net.birdsys.createtiltingcontrol.foundation.mixin.client;

import net.birdsys.createtiltingcontrol.client.ClientMouseCapture;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseCaptureHandlerMixin {

    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void createtiltingcontrol$captureMouseInput(double movementTime, CallbackInfo ci) {
        if (ClientMouseCapture.tryFeed(this.accumulatedDX, this.accumulatedDY))
            ci.cancel();
    }
}
