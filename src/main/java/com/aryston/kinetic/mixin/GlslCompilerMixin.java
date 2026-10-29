package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.pipeline.ShaderCaches;
import com.aryston.kinetic.client.pipeline.SpirvCache;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import net.minecraft.client.renderer.ShaderDefines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlslCompiler.class)
abstract class GlslCompilerMixin {
    @Shadow
    @Final
    private boolean isZeroToOne;

    @Shadow
    @Final
    private boolean shaderDrawParameters;

    @WrapMethod(method = "compileToSpv")
    private SpvModule kinetic$useSpirvCache(String name, String source, ShaderType type, ShaderDefines defines, ShaderSource shaderSource, Operation<SpvModule> original) {
        SpirvCache.Request request = new SpirvCache.Request(name, source, type, defines, shaderSource, isZeroToOne, shaderDrawParameters);
        return ShaderCaches.spirv()
            .map(cache -> cache.compile(request, () -> original.call(name, source, type, defines, shaderSource)))
            .orElseGet(() -> original.call(name, source, type, defines, shaderSource));
    }
}
