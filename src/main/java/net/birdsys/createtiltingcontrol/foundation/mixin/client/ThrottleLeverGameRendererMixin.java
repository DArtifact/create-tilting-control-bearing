package net.birdsys.createtiltingcontrol.foundation.mixin.client;

import dev.ryanhcode.sable.Sable;

import net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever.ThrottleLeverPicking;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class ThrottleLeverGameRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void createtiltingcontrol$pickThrottleLeverHandle(float partialTicks, CallbackInfo ci) {
        if (this.minecraft == null)
            return;
        LocalPlayer player = this.minecraft.player;
        if (player == null)
            return;

        Vec3 eyePos = Sable.HELPER.getEyePositionInterpolated(player, partialTicks);
        HitResult mcHitResult = this.minecraft.hitResult;
        double minDistance = mcHitResult != null && mcHitResult.getType() != HitResult.Type.MISS
                ? Sable.HELPER.distanceSquaredWithSubLevels(player.level(), eyePos, mcHitResult.getLocation())
                : Double.MAX_VALUE;

        for (BidirectionalThrottleLeverBlockEntity lever : ThrottleLeverPicking.getNearby()) {
            if (lever.isRemoved())
                continue;
            Double hitDistance = ThrottleLeverPicking.raycastHandle(
                    eyePos, player.getViewVector(partialTicks), lever, partialTicks);
            if (hitDistance == null || hitDistance >= minDistance)
                continue;
            minDistance = hitDistance;
            this.minecraft.hitResult = new BlockHitResult(
                    lever.getBlockPos().getCenter(), Direction.UP, lever.getBlockPos(), false);
        }
    }
}
