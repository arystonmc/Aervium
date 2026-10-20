package com.aryston.aervium.client.lod;

import com.aryston.aervium.core.lod.LodAllocator;
import com.aryston.aervium.core.lod.LodCoverage;
import com.aryston.aervium.core.lod.LodMesh;
import com.aryston.aervium.core.lod.LodSelection;
import com.aryston.aervium.core.lod.LodStore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class DistantTerrainRenderer {
    private static final int UPLOADS_PER_FRAME = 12;
    private static final int UPLOAD_BYTES_PER_FRAME = 4 * 1024 * 1024;
    private static final long UPLOAD_BUDGET_NANOS = 1_500_000L;
    private static final int REQUEST_INTERVAL_FRAMES = 4;
    private static final int EVICTION_INTERVAL_FRAMES = 600;
    private static final long UNUSED_FRAMES_BEFORE_EVICTION = 1_200L;
    private static final long RETIRING_FRAMES = 4L;
    private static final int INDICES_PER_QUAD = 6;
    private static final int COVERAGE_BLOCKS = LodCoverage.SIZE * LodStore.CHUNK_SIZE;
    private static final int DETAIL_BLOCKS = 4096;
    private static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f ZERO = new Vector3f();
    private static final Long2ObjectOpenHashMap<TileGpuMesh> MESHES = new Long2ObjectOpenHashMap<>();
    private static final LodSelection SELECTION = new LodSelection();
    private static final TerrainDraws DRAWS = new TerrainDraws();
    private static final List<TerrainDraws.Draw> OPAQUE = new ArrayList<>();
    private static final List<TerrainDraws.Draw> WATER = new ArrayList<>();
    private static final Matrix4f CLIP_AND_FOG = new Matrix4f();
    private static final LodSelection.TileIndex INDEX = new LodSelection.TileIndex() {
        @Override
        public boolean exists(int level, int tileX, int tileZ) {
            return TileMeshing.exists(LodStore.key(level, tileX, tileZ));
        }

        @Override
        public boolean meshed(long key) {
            TileGpuMesh mesh = MESHES.get(key);
            if (mesh != null) {
                mesh.lastUsedFrame = frame;
                return true;
            }
            return false;
        }

        @Override
        public boolean gapsAllowed() {
            return !TerrainGenerator.active();
        }

        @Override
        public boolean covered(long key) {
            TileGpuMesh mesh = MESHES.get(key);
            return gapsAllowed() || mesh != null && mesh.complete;
        }
    };

    private static TerrainGeometry geometry = new TerrainGeometry();
    private static RenderSystem.@Nullable AutoStorageIndexBuffer indices;
    private static int largestQuads = 1;
    private static long frame;
    private static boolean prepared;
    private static @Nullable Object renderedLevel;
    private static int renderedEpoch;
    private static @Nullable LodMesh pendingUpload;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    private DistantTerrainRenderer() {
    }

    public static void prepareFrame(Minecraft minecraft) {
        prepared = false;
        frame++;
        if (!DistantTerrain.drawing() || minecraft.level == null) {
            if (renderedLevel != null) {
                releaseAll();
                DistantTerrainCoverage.close();
                renderedLevel = null;
            }
            return;
        }
        if (minecraft.level != renderedLevel || renderedEpoch != TileMeshing.epoch()) {
            releaseAll();
            renderedLevel = minecraft.level;
            renderedEpoch = TileMeshing.epoch();
        }
        var camera = minecraft.gameRenderer.mainCamera().position();
        cameraX = camera.x;
        cameraY = camera.y;
        cameraZ = camera.z;
        int radius = Math.min(minecraft.options.getEffectiveRenderDistance(), LodCoverage.SIZE / 2 - 1);
        int centerX = SectionPos.blockToSectionCoord(camera.x);
        int centerZ = SectionPos.blockToSectionCoord(camera.z);
        double minX = SectionPos.sectionToBlockCoord(centerX - radius);
        double minZ = SectionPos.sectionToBlockCoord(centerZ - radius);
        double maxX = SectionPos.sectionToBlockCoord(centerX + radius + 1);
        double maxZ = SectionPos.sectionToBlockCoord(centerZ + radius + 1);
        float distance = DistantTerrain.distanceBlocks();
        CLIP_AND_FOG.set(
            (float) (minX - cameraX), (float) (minZ - cameraZ), (float) (maxX - cameraX), (float) (maxZ - cameraZ),
            distance * DistantTerrainFog.START_FRACTION, distance, (float) positiveModulo(cameraX, COVERAGE_BLOCKS), (float) positiveModulo(cameraZ, COVERAGE_BLOCKS),
            (float) positiveModulo(cameraX, DETAIL_BLOCKS), (float) cameraY, (float) positiveModulo(cameraZ, DETAIL_BLOCKS), DistantTerrain.quality().textured() ? 1.0F : 0.0F,
            0.0F, 0.0F, 0.0F, 0.0F);
        long now = System.nanoTime();
        for (TileGpuMesh mesh : MESHES.values()) {
            mesh.retirePrevious();
        }
        upload(now);
        SELECTION.select(INDEX, cameraX, cameraZ, distance, LodSelection.ClipBox.NONE, DistantTerrain.quality());
        DistantTerrainCoverage.prepare(cameraX, cameraZ, radius);
        if (frame % REQUEST_INTERVAL_FRAMES == 0 || !SELECTION.wanted().isEmpty()) {
            request();
        }
        if (frame % REQUEST_INTERVAL_FRAMES == 0) {
            TerrainGenerator.request(cameraX, cameraZ, distance, LodSelection.ClipBox.NONE, DistantTerrain.quality());
        }
        if (frame % EVICTION_INTERVAL_FRAMES == 0) {
            evict(false);
        }
        int largest = 1;
        for (TileGpuMesh mesh : MESHES.values()) {
            largest = Math.max(largest, mesh.quads);
        }
        largestQuads = largest;
        prepared = true;
    }

    public static void draw(RenderPass pass) {
        draw(pass, DistantTerrainPipeline.PIPELINE, DistantTerrainPipeline.WATER_PIPELINE);
    }

    public static void drawGeometry(RenderPass pass) {
        draw(pass, DistantTerrainPipeline.GEOMETRY_PIPELINE, null);
    }

    public static void drawOpaque(RenderPass pass) {
        draw(pass, DistantTerrainPipeline.PIPELINE, null);
    }

    public static void drawWater(RenderPass pass) {
        draw(pass, null, DistantTerrainPipeline.WATER_PIPELINE);
    }

    public static void endFrame() {
        DRAWS.endFrame();
    }

    public static void shutdown() {
        prepared = false;
        releaseAll();
        DistantTerrainCoverage.close();
    }

    public static int meshCount() {
        return MESHES.size();
    }

    public static int drawnCount() {
        return prepared ? SELECTION.drawn().size() : 0;
    }

    private static void draw(RenderPass pass, @Nullable RenderPipeline opaque, @Nullable RenderPipeline water) {
        if (!prepared || SELECTION.drawn().isEmpty()) {
            return;
        }
        OPAQUE.clear();
        WATER.clear();
        for (int index = 0; index < SELECTION.drawn().size(); index++) {
            add(SELECTION.drawn().getLong(index));
        }
        if (OPAQUE.isEmpty() && WATER.isEmpty()) {
            return;
        }
        OPAQUE.sort(Comparator.comparingInt(draw -> draw.allocation().page()));
        WATER.sort(Comparator.comparingDouble(TerrainDraws.Draw::distance).reversed());
        GpuBuffer buffer = sequentialIndices().getBuffer(largestQuads * INDICES_PER_QUAD);
        pass.pushDebugGroup(() -> "Aervium distant terrain");
        pass.setIndexBuffer(buffer, sequentialIndices().type());
        pass.setPipeline(RenderSystem.getCompiledPipeline(opaque == null ? water : opaque));
        RenderSystem.bindDefaultUniforms(pass);
        DistantTerrainCoverage.bind(pass);
        pass.setUniform("Sampler2", Minecraft.getInstance().gameRenderer.levelLightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), WHITE, ZERO, CLIP_AND_FOG));
        if (opaque != null && !OPAQUE.isEmpty()) {
            DRAWS.draw(pass, geometry, OPAQUE);
        }
        if (water != null && !WATER.isEmpty()) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(water));
            DRAWS.draw(pass, geometry, WATER);
        }
        pass.popDebugGroup();
    }

    private static void add(long key) {
        TileGpuMesh mesh = MESHES.get(key);
        if (mesh == null || mesh.allocation == null) {
            return;
        }
        mesh.lastUsedFrame = frame;
        int size = LodSelection.tileBlocks(LodStore.levelOf(key));
        double minX = (double) LodStore.tileXOf(key) * size;
        double minZ = (double) LodStore.tileZOf(key) * size;
        double maxX = minX + size;
        double maxZ = minZ + size;
        if (!Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(
            new AABB(minX, mesh.minY, minZ, maxX, mesh.maxY, maxZ))) {
            return;
        }
        float x = (float) (minX - cameraX);
        float z = (float) (minZ - cameraZ);
        double distance = x * (double) x + z * (double) z;
        if (mesh.opaque > 0) {
            OPAQUE.add(command(mesh, 0, mesh.opaque, x, z, minX, minZ, maxX, maxZ, distance));
        }
        if (mesh.quads > mesh.opaque) {
            WATER.add(command(mesh, mesh.opaque, mesh.quads - mesh.opaque, x, z, minX, minZ, maxX, maxZ, distance));
        }
    }

    private static TerrainDraws.Draw command(TileGpuMesh mesh, int first, int count,
        float x, float z, double minX, double minZ, double maxX, double maxZ, double distance) {
        return new TerrainDraws.Draw(mesh.allocation, first, count, x, (float) -cameraY, z, 0.0F, 1.0F,
            (float) (minX - cameraX), (float) (minZ - cameraZ),
            (float) (maxX - cameraX), (float) (maxZ - cameraZ), distance);
    }

    private static RenderSystem.AutoStorageIndexBuffer sequentialIndices() {
        if (indices == null) {
            indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        }
        return indices;
    }

    private static void upload(long now) {
        long deadline = now + UPLOAD_BUDGET_NANOS;
        int uploadedBytes = 0;
        for (int count = 0; count < UPLOADS_PER_FRAME; count++) {
            LodMesh mesh = pendingUpload == null ? TileMeshing.nextReady() : pendingUpload;
            if (mesh == null) {
                return;
            }
            int bytes = mesh.vertices().remaining();
            if (count > 0 && (uploadedBytes + bytes > UPLOAD_BYTES_PER_FRAME || System.nanoTime() >= deadline)) {
                pendingUpload = mesh;
                return;
            }
            LodAllocator.Allocation allocation = bytes == 0 ? null : geometry.upload(mesh.vertices());
            if (allocation == null && bytes > 0) {
                pendingUpload = mesh;
                evict(true);
                return;
            }
            pendingUpload = null;
            uploadedBytes += bytes;
            TileGpuMesh previous = MESHES.get(mesh.key());
            if (previous != null && previous.complete && !mesh.complete()) {
                geometry.free(allocation);
                continue;
            }
            if (previous != null && previous.previous != null) {
                previous.previous.close();
                previous.previous = null;
            }
            MESHES.put(mesh.key(), new TileGpuMesh(allocation, mesh, previous));
        }
    }

    private static void request() {
        LongArrayList wanted = SELECTION.wanted();
        LongArrayList drawn = SELECTION.drawn();
        long[] keys = new long[wanted.size() + drawn.size()];
        wanted.getElements(0, keys, 0, wanted.size());
        drawn.getElements(0, keys, wanted.size(), drawn.size());
        if (keys.length > 0) {
            TileMeshing.request(keys);
        }
    }

    private static void evict(boolean pressure) {
        var iterator = MESHES.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long unused = frame - entry.getValue().lastUsedFrame;
            if (pressure ? unused > 1 : unused > UNUSED_FRAMES_BEFORE_EVICTION) {
                entry.getValue().close();
                TileMeshing.forget(entry.getLongKey());
                iterator.remove();
            }
        }
    }

    private static void releaseAll() {
        boolean populated = !MESHES.isEmpty() || pendingUpload != null;
        pendingUpload = null;
        MESHES.clear();
        geometry.close();
        geometry = new TerrainGeometry();
        DRAWS.close();
        OPAQUE.clear();
        WATER.clear();
        if (populated) {
            TerrainWorker.submit(store -> TileMeshing.forgetAll());
        }
    }

    private static double positiveModulo(double value, int modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }

    private static final class TileGpuMesh {
        private final LodAllocator.@Nullable Allocation allocation;
        private final int quads;
        private final int opaque;
        private final boolean complete;
        private final float minY;
        private final float maxY;
        private final long uploadedFrame;
        private @Nullable TileGpuMesh previous;
        private long lastUsedFrame;

        private TileGpuMesh(LodAllocator.@Nullable Allocation allocation, LodMesh mesh, @Nullable TileGpuMesh previous) {
            this.allocation = allocation;
            quads = mesh.quads();
            opaque = mesh.opaqueQuads();
            complete = mesh.complete();
            minY = mesh.minY();
            maxY = mesh.maxY();
            uploadedFrame = frame;
            this.previous = previous;
            lastUsedFrame = frame;
        }

        private void retirePrevious() {
            if (previous != null && frame - uploadedFrame >= RETIRING_FRAMES) {
                previous.close();
                previous = null;
            }
        }

        private void close() {
            geometry.free(allocation);
            if (previous != null) {
                previous.close();
                previous = null;
            }
        }
    }
}
