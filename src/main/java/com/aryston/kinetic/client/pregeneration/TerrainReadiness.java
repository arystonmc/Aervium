package com.aryston.kinetic.client.pregeneration;

import com.aryston.kinetic.core.pregeneration.TerrainProgress;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

final class TerrainReadiness {
    private static final int OUTSIDE = 0;
    private static final int CHUNK_ONLY = 1;
    private static final int WITH_SECTIONS = 2;

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private @Nullable ChunkPos center;
    private int radius;
    private int side;
    private byte[] kind = new byte[0];
    private boolean[] present = new boolean[0];
    private int[] required = new int[0];
    private int[] ready = new int[0];
    private int next;
    private int chunksRequired;
    private int chunksReady;
    private int sectionsRequired;
    private int sectionsReady;

    boolean covers(ChunkPos chunk, int chunks) {
        return chunk.equals(this.center) && chunks == this.radius;
    }

    void reset(ChunkPos chunk, int chunks) {
        this.center = chunk;
        this.radius = chunks;
        this.side = chunks * 2 + 1;
        int cells = this.side * this.side;
        this.kind = new byte[cells];
        this.present = new boolean[cells];
        this.required = new int[cells];
        this.ready = new int[cells];
        this.next = 0;
        this.chunksRequired = 0;
        this.chunksReady = 0;
        this.sectionsRequired = 0;
        this.sectionsReady = 0;
        for (int cell = 0; cell < cells; cell++) {
            int x = chunk.x() + cell % this.side - chunks;
            int z = chunk.z() + cell / this.side - chunks;
            if (ChunkTrackingView.isInViewDistance(chunk.x(), chunk.z(), chunks - 1, x, z)) {
                this.kind[cell] = WITH_SECTIONS;
            } else if (ChunkTrackingView.isInViewDistance(chunk.x(), chunk.z(), chunks, x, z)) {
                this.kind[cell] = CHUNK_ONLY;
            } else {
                this.kind[cell] = OUTSIDE;
            }
            if (this.kind[cell] != OUTSIDE) {
                this.chunksRequired++;
            }
        }
    }

    void advance(ClientLevel level, ViewArea viewArea, SectionOcclusionGraph graph, long deadline) {
        int cells = this.kind.length;
        for (int visited = 0; visited < cells && System.nanoTime() < deadline; visited++) {
            int cell = this.next;
            this.next = (this.next + 1) % cells;
            if (this.kind[cell] != OUTSIDE) {
                this.update(cell, level, viewArea, graph);
            }
        }
    }

    TerrainProgress progress(int lightUpdates) {
        return new TerrainProgress(this.chunksReady, Math.max(1, this.chunksRequired), this.sectionsReady, this.sectionsRequired, lightUpdates);
    }

    private void update(int cell, ClientLevel level, ViewArea viewArea, SectionOcclusionGraph graph) {
        int chunkX = this.center.x() + cell % this.side - this.radius;
        int chunkZ = this.center.z() + cell / this.side - this.radius;
        LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        boolean loaded = chunk != null;
        int cellRequired = 0;
        int cellReady = 0;
        if (loaded && this.kind[cell] == WITH_SECTIONS) {
            for (int index = 0; index < chunk.getSectionsCount(); index++) {
                if (chunk.getSection(index).hasOnlyAir()) {
                    continue;
                }
                this.cursor.set(SectionPos.sectionToBlockCoord(chunkX), SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(index)),
                    SectionPos.sectionToBlockCoord(chunkZ));
                SectionRenderDispatcher.RenderSection section = viewArea.getRenderSectionAt(this.cursor);
                if (section == null || graph.getNode(section) == null) {
                    continue;
                }
                cellRequired++;
                if (section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED) {
                    cellReady++;
                }
            }
        }
        this.chunksReady += (loaded ? 1 : 0) - (this.present[cell] ? 1 : 0);
        this.sectionsRequired += cellRequired - this.required[cell];
        this.sectionsReady += cellReady - this.ready[cell];
        this.present[cell] = loaded;
        this.required[cell] = cellRequired;
        this.ready[cell] = cellReady;
    }
}
