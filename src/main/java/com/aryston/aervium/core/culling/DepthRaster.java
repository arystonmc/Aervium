package com.aryston.aervium.core.culling;

import java.util.Arrays;

public final class DepthRaster {
    public static final int TILE = 8;
    private static final float MIN_W = 1.0E-4F;
    private static final float MIN_AREA = 1.0E-3F;
    private static final float DEPTH_EPSILON = 2.0E-6F;
    private static final float EMPTY = Float.POSITIVE_INFINITY;
    private static final int MAX_POLYGON = 8;
    private static final int[][] FACES = {
        {0, 4, 6, 2},
        {1, 3, 7, 5},
        {0, 1, 5, 4},
        {2, 6, 7, 3},
        {0, 2, 3, 1},
        {4, 5, 7, 6}
    };

    private final int width;
    private final int height;
    private final int tilesX;
    private final int tilesY;
    private final float[] depth;
    private final float[] tileFarthest;
    private final float[] cornerX = new float[8];
    private final float[] cornerY = new float[8];
    private final float[] cornerZ = new float[8];
    private final float[] clipX = new float[8];
    private final float[] clipY = new float[8];
    private final float[] clipZ = new float[8];
    private final float[] clipW = new float[8];
    private final float[] faceX = new float[MAX_POLYGON];
    private final float[] faceY = new float[MAX_POLYGON];
    private final float[] faceZ = new float[MAX_POLYGON];
    private final float[] faceW = new float[MAX_POLYGON];
    private final float[] polygonX = new float[MAX_POLYGON];
    private final float[] polygonY = new float[MAX_POLYGON];
    private final float[] polygonZ = new float[MAX_POLYGON];
    private final ClipMatrix clip = new ClipMatrix();
    private int dirtyTileX0;
    private int dirtyTileY0;
    private int dirtyTileX1;
    private int dirtyTileY1;
    private boolean screenBounded;
    private int occluders;

    public DepthRaster(int width, int height) {
        if (width <= 0 || height <= 0 || width % TILE != 0 || height % TILE != 0) {
            throw new IllegalArgumentException("Raster size must be a positive multiple of " + TILE);
        }
        this.width = width;
        this.height = height;
        this.tilesX = width / TILE;
        this.tilesY = height / TILE;
        this.depth = new float[width * height];
        this.tileFarthest = new float[tilesX * tilesY];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int occluders() {
        return occluders;
    }

    public ClipMatrix matrix() {
        return clip;
    }

    public void begin(ClipMatrix view, boolean screenBounded) {
        clip.copy(view);
        this.screenBounded = screenBounded;
        Arrays.fill(depth, EMPTY);
        Arrays.fill(tileFarthest, EMPTY);
        clearDirtyTiles();
        occluders = 0;
    }

    public boolean addOccluder(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int corner = 0; corner < 8; corner++) {
            float x = (corner & 1) == 0 ? minX : maxX;
            float y = (corner & 2) == 0 ? minY : maxY;
            float z = (corner & 4) == 0 ? minZ : maxZ;
            clipX[corner] = clip.x(x, y, z);
            clipY[corner] = clip.y(x, y, z);
            clipZ[corner] = clip.z(x, y, z);
            clipW[corner] = clip.w(x, y, z);
        }
        boolean drawn = false;
        for (int[] face : FACES) {
            int count = clipFace(face);
            if (count >= 3) {
                drawn |= drawPolygon(count);
            }
        }
        if (drawn) {
            occluders++;
        }
        return drawn;
    }

    public boolean isVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (!project(minX, minY, minZ, maxX, maxY, maxZ)) {
            return true;
        }
        float left = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.POSITIVE_INFINITY;
        float top = Float.NEGATIVE_INFINITY;
        float nearest = Float.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            left = Math.min(left, cornerX[corner]);
            right = Math.max(right, cornerX[corner]);
            bottom = Math.min(bottom, cornerY[corner]);
            top = Math.max(top, cornerY[corner]);
            nearest = Math.min(nearest, cornerZ[corner]);
        }
        if (right <= 0.0F || left >= width || top <= 0.0F || bottom >= height) {
            return true;
        }
        int x0 = Math.max(0, (int) Math.floor(left));
        int x1 = Math.min(width - 1, (int) Math.ceil(right) - 1);
        int y0 = Math.max(0, (int) Math.floor(bottom));
        int y1 = Math.min(height - 1, (int) Math.ceil(top) - 1);
        if (!screenBounded && (left < 0.0F || right > width || bottom < 0.0F || top > height)) {
            return true;
        }
        updateTiles();
        float hiddenBeyond = nearest - DEPTH_EPSILON;
        for (int tileY = y0 / TILE; tileY <= y1 / TILE; tileY++) {
            for (int tileX = x0 / TILE; tileX <= x1 / TILE; tileX++) {
                if (tileFarthest[tileY * tilesX + tileX] < hiddenBeyond) {
                    continue;
                }
                int startX = Math.max(x0, tileX * TILE);
                int endX = Math.min(x1, tileX * TILE + TILE - 1);
                int startY = Math.max(y0, tileY * TILE);
                int endY = Math.min(y1, tileY * TILE + TILE - 1);
                for (int y = startY; y <= endY; y++) {
                    int row = y * width;
                    for (int x = startX; x <= endX; x++) {
                        if (depth[row + x] >= hiddenBeyond) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    float depthAt(int x, int y) {
        return depth[y * width + x];
    }

    private boolean project(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int corner = 0; corner < 8; corner++) {
            float x = (corner & 1) == 0 ? minX : maxX;
            float y = (corner & 2) == 0 ? minY : maxY;
            float z = (corner & 4) == 0 ? minZ : maxZ;
            float w = clip.w(x, y, z);
            if (!(w > MIN_W)) {
                return false;
            }
            float inverse = 1.0F / w;
            float ndcZ = clip.z(x, y, z) * inverse;
            if (!(ndcZ >= 0.0F)) {
                return false;
            }
            cornerX[corner] = (clip.x(x, y, z) * inverse * 0.5F + 0.5F) * width;
            cornerY[corner] = (clip.y(x, y, z) * inverse * 0.5F + 0.5F) * height;
            cornerZ[corner] = ndcZ;
        }
        return true;
    }

    private int clipFace(int[] face) {
        int count = 0;
        for (int corner = 0; corner < 4; corner++) {
            int current = face[corner];
            int next = face[(corner + 1) & 3];
            float currentZ = clipZ[current];
            float nextZ = clipZ[next];
            boolean currentInside = currentZ >= 0.0F && clipW[current] > MIN_W;
            boolean nextInside = nextZ >= 0.0F && clipW[next] > MIN_W;
            if (currentInside) {
                count = emit(count, clipX[current], clipY[current], currentZ, clipW[current]);
            }
            if (currentInside != nextInside) {
                float t = currentZ / (currentZ - nextZ);
                float w = clipW[current] + (clipW[next] - clipW[current]) * t;
                if (w > MIN_W) {
                    count = emit(count,
                        clipX[current] + (clipX[next] - clipX[current]) * t,
                        clipY[current] + (clipY[next] - clipY[current]) * t,
                        0.0F,
                        w);
                }
            }
        }
        for (int vertex = 0; vertex < count; vertex++) {
            float inverse = 1.0F / faceW[vertex];
            polygonX[vertex] = (faceX[vertex] * inverse * 0.5F + 0.5F) * width;
            polygonY[vertex] = (faceY[vertex] * inverse * 0.5F + 0.5F) * height;
            polygonZ[vertex] = faceZ[vertex] * inverse;
        }
        return count;
    }

    private int emit(int count, float x, float y, float z, float w) {
        if (count == MAX_POLYGON) {
            return count;
        }
        faceX[count] = x;
        faceY[count] = y;
        faceZ[count] = z;
        faceW[count] = w;
        return count + 1;
    }

    private boolean drawPolygon(int count) {
        float area = 0.0F;
        for (int vertex = 0; vertex < count; vertex++) {
            int next = vertex + 1 == count ? 0 : vertex + 1;
            area += polygonX[vertex] * polygonY[next] - polygonX[next] * polygonY[vertex];
        }
        if (area < MIN_AREA) {
            return false;
        }
        int first = 0;
        float bestDeterminant = 0.0F;
        for (int vertex = 1; vertex + 1 < count; vertex++) {
            float determinant = Math.abs(determinant(vertex));
            if (determinant > bestDeterminant) {
                bestDeterminant = determinant;
                first = vertex;
            }
        }
        if (bestDeterminant < MIN_AREA) {
            return false;
        }
        int second = first + 1;
        float d1x = polygonX[first] - polygonX[0];
        float d1y = polygonY[first] - polygonY[0];
        float d2x = polygonX[second] - polygonX[0];
        float d2y = polygonY[second] - polygonY[0];
        float dz1 = polygonZ[first] - polygonZ[0];
        float dz2 = polygonZ[second] - polygonZ[0];
        float det = d1x * d2y - d2x * d1y;
        float depthX = (dz1 * d2y - dz2 * d1y) / det;
        float depthY = (d1x * dz2 - d2x * dz1) / det;
        float depthSlack = 0.5F * (Math.abs(depthX) + Math.abs(depthY));

        float left = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.POSITIVE_INFINITY;
        float top = Float.NEGATIVE_INFINITY;
        float farthestCorner = 0.0F;
        for (int vertex = 0; vertex < count; vertex++) {
            left = Math.min(left, polygonX[vertex]);
            right = Math.max(right, polygonX[vertex]);
            bottom = Math.min(bottom, polygonY[vertex]);
            top = Math.max(top, polygonY[vertex]);
            farthestCorner = Math.max(farthestCorner, polygonZ[vertex]);
        }
        int x0 = Math.max(0, (int) Math.floor(left));
        int x1 = Math.min(width - 1, (int) Math.ceil(right) - 1);
        int y0 = Math.max(0, (int) Math.floor(bottom));
        int y1 = Math.min(height - 1, (int) Math.ceil(top) - 1);
        if (x0 > x1 || y0 > y1) {
            return false;
        }

        boolean wrote = false;
        for (int y = y0; y <= y1; y++) {
            float centerY = y + 0.5F;
            int row = y * width;
            for (int x = x0; x <= x1; x++) {
                float centerX = x + 0.5F;
                if (!pixelInside(count, centerX, centerY)) {
                    continue;
                }
                float value = polygonZ[0] + depthX * (centerX - polygonX[0]) + depthY * (centerY - polygonY[0]) + depthSlack;
                value = Math.min(value, farthestCorner);
                if (value < depth[row + x]) {
                    depth[row + x] = value;
                    wrote = true;
                }
            }
        }
        if (wrote) {
            markDirtyTiles(x0 / TILE, y0 / TILE, x1 / TILE, y1 / TILE);
        }
        return wrote;
    }

    private float determinant(int first) {
        int second = first + 1;
        return (polygonX[first] - polygonX[0]) * (polygonY[second] - polygonY[0]) - (polygonX[second] - polygonX[0]) * (polygonY[first] - polygonY[0]);
    }

    private boolean pixelInside(int count, float centerX, float centerY) {
        for (int vertex = 0; vertex < count; vertex++) {
            int next = vertex + 1 == count ? 0 : vertex + 1;
            float edgeX = polygonX[next] - polygonX[vertex];
            float edgeY = polygonY[next] - polygonY[vertex];
            float value = edgeX * (centerY - polygonY[vertex]) - edgeY * (centerX - polygonX[vertex]);
            if (value - 0.5F * (Math.abs(edgeX) + Math.abs(edgeY)) < 0.0F) {
                return false;
            }
        }
        return true;
    }

    private void markDirtyTiles(int tileX0, int tileY0, int tileX1, int tileY1) {
        dirtyTileX0 = Math.min(dirtyTileX0, tileX0);
        dirtyTileY0 = Math.min(dirtyTileY0, tileY0);
        dirtyTileX1 = Math.max(dirtyTileX1, tileX1);
        dirtyTileY1 = Math.max(dirtyTileY1, tileY1);
    }

    private void clearDirtyTiles() {
        dirtyTileX0 = tilesX;
        dirtyTileY0 = tilesY;
        dirtyTileX1 = -1;
        dirtyTileY1 = -1;
    }

    private void updateTiles() {
        if (dirtyTileX0 > dirtyTileX1) {
            return;
        }
        for (int tileY = dirtyTileY0; tileY <= dirtyTileY1; tileY++) {
            for (int tileX = dirtyTileX0; tileX <= dirtyTileX1; tileX++) {
                float farthest = 0.0F;
                for (int y = tileY * TILE; y < tileY * TILE + TILE; y++) {
                    int row = y * width;
                    for (int x = tileX * TILE; x < tileX * TILE + TILE; x++) {
                        farthest = Math.max(farthest, depth[row + x]);
                    }
                }
                tileFarthest[tileY * tilesX + tileX] = farthest;
            }
        }
        clearDirtyTiles();
    }
}
