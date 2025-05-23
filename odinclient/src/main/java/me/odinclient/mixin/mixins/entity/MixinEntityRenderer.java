package me.odinclient.mixin.mixins.entity;

import me.odinclient.features.impl.render.Camera;
import me.odinclient.features.impl.render.NoDebuff;
import me.odinmain.events.impl.RenderOverlayNoCaching;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static me.odinmain.utils.Utils.postAndCatch;

/*
 * Camera stuff from Floppa Client
 * https://github.com/FloppaCoding/FloppaClient/blob/master/src/main/java/floppaclient/mixins/render/EntityRendererMixin.java
 */
@Mixin(value = EntityRenderer.class)
abstract public class MixinEntityRenderer implements IResourceManagerReloadListener {

    // idea from oneconfig https://github.com/Polyfrost/OneConfig/commit/15d616ec6e57f741ca64b07ff76ba30aaec115a4
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    private void drawHud(float partialTicks, long nanoTime, CallbackInfo ci) {
        postAndCatch(new RenderOverlayNoCaching(partialTicks));
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    private void lockPlayerLooking(EntityPlayerSP instance, float x, float y) {
        if (!Camera.getFreelookToggled()) instance.setAngles(x, y);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void updateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci, boolean flag, float f, float f1, float f2, float f3) {
        if (Camera.INSTANCE.getEnabled()) Camera.updateCameraAndRender(f2, f3);
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", ordinal = 2))
    public void orientCamera(float x, float y, float z, float partialTicks){
        if (Camera.getFreelookToggled()) GlStateManager.translate(0.0F, 0.0F, -Camera.calculateCameraDistance());
        else GlStateManager.translate(x, y, z);
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistance:F"))
    public float tweakThirdPersonDistance(EntityRenderer instance) {
        return Camera.getCameraDistance();
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistanceTemp:F"))
    public float tweakThirdPersonDistanceTemp(EntityRenderer instance) {
        return Camera.getCameraDistance();
    }

    @ModifyConstant(method = "orientCamera", constant = @Constant(intValue = 8))
    public int cameraClip(int constant) {
        return Camera.getCameraClipEnabled() ? 0: constant;
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onHurtCam(float partialTicks, CallbackInfo ci) {
        if (NoDebuff.INSTANCE.getNoHurtCam()) ci.cancel();
    }
}