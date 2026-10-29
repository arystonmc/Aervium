package com.aryston.kinetic.client.pipeline;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.cache.CacheStatistics;
import com.aryston.kinetic.core.cache.DiskCache;
import com.aryston.kinetic.core.cache.ShaderCacheKey;
import com.aryston.kinetic.core.cache.SpirvBinary;
import com.aryston.kinetic.core.job.JobSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.SPIRVModule;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.ShadercIncludeResult;

public final class SpirvCache {
    private final DiskCache disk;
    private final JobSystem jobs;
    private final CacheStatistics statistics = new CacheStatistics();

    SpirvCache(DiskCache disk, JobSystem jobs) {
        this.disk = disk;
        this.jobs = jobs;
    }

    public SpvModule compile(Request request, Supplier<SpvModule> compiler) {
        long start = System.nanoTime();
        String key = ShaderCacheKey.of(request.keyInput(), include -> includeSource(request.shaderSource(), include));
        Optional<byte[]> cached = read(key);
        if (cached.isPresent()) {
            SpvModule module = new SPIRVModule(nativeCopy(cached.get()), request.type());
            statistics.hit(System.nanoTime() - start);
            return module;
        }
        SpvModule module = compiler.get();
        byte[] binary = heapCopy(module.spv());
        statistics.miss(System.nanoTime() - start);
        jobs.submit(() -> write(key, binary));
        return module;
    }

    CacheStatistics statistics() {
        return statistics;
    }

    private Optional<byte[]> read(String key) {
        try {
            return disk.read(key).filter(SpirvBinary::isValid);
        } catch (UncheckedIOException failure) {
            Kinetic.LOGGER.warn("Kinetic could not read a cached shader", failure);
            return Optional.empty();
        }
    }

    private void write(String key, byte[] binary) {
        try {
            disk.write(key, binary);
        } catch (UncheckedIOException failure) {
            Kinetic.LOGGER.warn("Kinetic could not cache a compiled shader", failure);
        }
    }

    private static Optional<String> includeSource(ShaderSource shaderSource, String include) {
        Identifier id = Identifier.tryParse(include);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(shaderSource.getInclude(id))
            .map(cached -> MemoryUtil.memUTF8(ShadercIncludeResult.create(cached.includeResultPtr()).content()));
    }

    private static ByteBuffer nativeCopy(byte[] binary) {
        ByteBuffer buffer = MemoryUtil.memCalloc(binary.length);
        buffer.put(binary).flip();
        return buffer;
    }

    private static byte[] heapCopy(ByteBuffer spv) {
        ByteBuffer view = spv.duplicate();
        byte[] binary = new byte[view.remaining()];
        view.get(binary);
        return binary;
    }

    public record Request(String name, String source, ShaderType type, ShaderDefines defines, ShaderSource shaderSource, boolean depthZeroToOne, boolean drawParameters) {
        ShaderCacheKey.Input keyInput() {
            return new ShaderCacheKey.Input(
                SharedConstants.getCurrentVersion().name(),
                name,
                type.name(),
                source,
                defines.values(),
                defines.flags(),
                depthZeroToOne,
                RenderSystem.getDevice().getDeviceInfo().hintsAndWorkarounds().isExplicitDepthRequired(),
                drawParameters
            );
        }
    }
}
