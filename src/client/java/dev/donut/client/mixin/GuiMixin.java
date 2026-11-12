// language: Java, file: src/client/java/dev/donut/client/mixin/GuiMixin.java
package dev.donut.client.mixin;

import dev.donut.client.DonutClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void donut$hideXpBar(CallbackInfo ci) {
        if (DonutClient.hideXpBarActive()) ci.cancel();
    }
}
