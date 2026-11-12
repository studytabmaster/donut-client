package dev.donut.client.mixin;

import dev.donut.client.DonutClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBarRenderer.class)
public abstract class ExperienceBarRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void donut$hideXpBarRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DonutClient.hideXpBarActive()) ci.cancel();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void donut$hideXpBarBackground(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DonutClient.hideXpBarActive()) ci.cancel();
    }
}
