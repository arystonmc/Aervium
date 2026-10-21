package com.aryston.aervium.client.lod;

import com.aryston.aervium.core.lod.LodCoverage;
import com.aryston.aervium.core.lod.LodSample;
import com.aryston.aervium.core.lod.LodStore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Arrays;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public final class DistantTerrainCoverage {
    private static final int UPDATES_PER_FRAME = 1024;
    private static final int REQUIREMENTS_PER_FRAME = 64;
    private static final int REFRESHES_PER_FRAME = 16;
    private static final int SECTION_SLOTS = 256;
    private static final int SECTION_OFFSET = SECTION_SLOTS / 2;
    private static final boolean[] SEEN_SECTIONS = new boolean[SECTION_SLOTS];
    private static final long REQUIREMENT_BUDGET_NANOS = 1_000_000L;
    private static final long UPLOAD_INTERVAL_NANOS = 25_000_000L;
    private static final LodCoverage COVERAGE = new LodCoverage();
    private static final ConcurrentHashMap<Long, Boolean> PENDING = new ConcurrentHashMap<>();
    private static final LongLinkedOpenHashSet REQUIREMENTS = new LongLinkedOpenHashSet();
    private static final byte[] PIXELS = new byte[LodCoverage.SIZE * LodCoverage.SIZE];
    private static final ByteBuffer STAGING = ByteBuffer.allocateDirect(PIXELS.length);
    private static @Nullable GpuTexture texture;
    private static @Nullable GpuTextureView view;
    private static long uploaded;
    private static int refreshCursor;

    private DistantTerrainCoverage() {
    }

    public static void changed(long section, boolean ready) {
        if (DistantTerrain.enabled()) {
            PENDING.put(section, ready);
        }
    }

    static void captured(int x, int z, long[] samples) {
        long now = System.nanoTime();
        var area = Minecraft.getInstance().levelRenderer.viewArea();
        if (area != null) {
            Arrays.fill(SEEN_SECTIONS, false);
            for (long sample : samples) {
                int y = Math.floorDiv(LodSample.height(sample), LodStore.CHUNK_SIZE);
                if (LodSample.visible(sample) && firstVisit(y)) {
                    var section = area.getRenderSectionAt(new BlockPos(x * LodStore.CHUNK_SIZE, y * LodStore.CHUNK_SIZE, z * LodStore.CHUNK_SIZE));
                    COVERAGE.compiled(x, y, z, section != null && section.getSectionNode() == SectionPos.asLong(x, y, z)
                        && section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED, now);
                }
            }
        }
        COVERAGE.require(x, z, samples, now);
    }

    private static boolean firstVisit(int sectionY) {
        int slot = sectionY + SECTION_OFFSET;
        if (slot < 0 || slot >= SECTION_SLOTS) {
            return true;
        }
        boolean first = !SEEN_SECTIONS[slot];
        SEEN_SECTIONS[slot] = true;
        return first;
    }

    static void loaded(long position) {
        REQUIREMENTS.add(position);
    }

    private static void require(LevelChunk chunk) {
        long[] surfaces = new long[LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE];
        for (int z = 0; z < LodStore.CHUNK_SIZE; z++) {
            for (int x = 0; x < LodStore.CHUNK_SIZE; x++) {
                int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                surfaces[z * LodStore.CHUNK_SIZE + x] = LodSample.of(height, 0, 0,
                    height < chunk.getMinY() ? LodSample.FLAG_VOID : 0);
            }
        }
        captured(chunk.getPos().x(), chunk.getPos().z(), surfaces);
    }

    static void forget(int x, int z) {
        COVERAGE.forget(x, z);
        REQUIREMENTS.remove(ChunkPos.pack(x, z));
    }

    static void prepare(double cameraX, double cameraZ, int radius) {
        long now = System.nanoTime();
        int count = 0;
        for (var entry : PENDING.entrySet()) {
            if (count++ >= UPDATES_PER_FRAME) {
                break;
            }
            if (PENDING.remove(entry.getKey(), entry.getValue())) {
                long node = entry.getKey();
                int x = SectionPos.x(node);
                int z = SectionPos.z(node);
                if (entry.getValue() && !COVERAGE.known(x, z)) {
                    REQUIREMENTS.add(ChunkPos.pack(x, z));
                }
                COVERAGE.compiled(x, SectionPos.y(node), z, entry.getValue(), now);
            }
        }
        int cameraChunkX = (int) Math.floor(cameraX / LodStore.CHUNK_SIZE);
        int cameraChunkZ = (int) Math.floor(cameraZ / LodStore.CHUNK_SIZE);
        refreshSurrounding(cameraChunkX, cameraChunkZ, radius);
        prepareRequirements();
        if (texture == null) {
            texture = RenderSystem.getDevice().createTexture("Aervium distant coverage", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                GpuFormat.R8_UNORM, LodCoverage.SIZE, LodCoverage.SIZE, 1, 1);
            view = RenderSystem.getDevice().createTextureView(texture);
        }
        if (now - uploaded < UPLOAD_INTERVAL_NANOS) {
            return;
        }
        uploaded = now;
        COVERAGE.trim(cameraChunkX, cameraChunkZ, Math.min(radius + 8, LodCoverage.SIZE / 2 - 1));
        COVERAGE.rasterize(PIXELS, now);
        STAGING.clear();
        STAGING.put(PIXELS).flip();
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, STAGING, 0, 0, 0, 0, LodCoverage.SIZE, LodCoverage.SIZE);
    }

    private static void refreshSurrounding(int cameraChunkX, int cameraChunkZ, int radius) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var area = Minecraft.getInstance().levelRenderer.viewArea();
        long now = System.nanoTime();
        int side = 2 * radius + 1;
        int cells = side * side;
        int start = Math.floorMod(refreshCursor, cells);
        int refreshed = 0;
        for (int step = 0; step < cells; step++) {
            int cell = (start + step) % cells;
            int cx = cameraChunkX + cell % side - radius;
            int cz = cameraChunkZ + cell / side - radius;
            if (!COVERAGE.known(cx, cz)) {
                REQUIREMENTS.add(ChunkPos.pack(cx, cz));
            } else if (area != null && COVERAGE.value(cx, cz, now) == 0) {
                if (refreshed >= REFRESHES_PER_FRAME) {
                    refreshCursor = cell;
                    return;
                }
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk != null) {
                    require(chunk);
                    refreshed++;
                }
            }
        }
        refreshCursor = start;
    }

    static void bind(RenderPass pass) {
        pass.setUniform("Sampler0", view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    static void clear() {
        COVERAGE.clear();
        PENDING.clear();
        REQUIREMENTS.clear();
        uploaded = 0L;
    }

    private static void prepareRequirements() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long deadline = System.nanoTime() + REQUIREMENT_BUDGET_NANOS;
        for (int count = 0; count < REQUIREMENTS_PER_FRAME && !REQUIREMENTS.isEmpty() && System.nanoTime() < deadline; count++) {
            long position = REQUIREMENTS.removeFirstLong();
            LevelChunk chunk = level.getChunkSource().getChunk(ChunkPos.getX(position), ChunkPos.getZ(position), false);
            if (chunk != null) {
                require(chunk);
            }
        }
    }

    static void close() {
        clear();
        if (view != null) {
            view.close();
            view = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }
}
