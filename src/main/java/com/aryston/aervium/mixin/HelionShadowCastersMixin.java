package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.HelionShadowCasters;
import java.util.List;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.aryston.helion.integration.vanilla.VanillaShadowCasterSource")
abstract class HelionShadowCastersMixin {
    @Inject(method = "collectSections(Ljava/util/List;)Ljava/util/List;", at = @At("RETURN"), require = 0)
    private void aervium$cullHiddenCasters(List<?> cascades, CallbackInfoReturnable<List<List<SectionRenderDispatcher.RenderSection>>> callback) {
        HelionShadowCasters.filter(cascades, callback.getReturnValue());
    }
}
