package com.aryston.kinetic.client.culling;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.culling.ClipMatrix;
import com.aryston.kinetic.core.culling.OcclusionPass;
import com.aryston.kinetic.core.culling.OcclusionPipeline;
import com.aryston.kinetic.core.culling.VoxelRay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class CullingAudit {
    static final boolean ENABLED = Boolean.getBoolean("kinetic.cullingAudit");
    private static final int FRAME_INTERVAL = 30;
    private static final int MAX_SECTIONS = 48;
    private static final int RAYS_PER_SECTION = 24;
    private static final int REPORT_INTERVAL = 20;
    private static final int MAX_LOGGED_VIOLATIONS = 20;
    private static final double MIN_ENTRY_DISTANCE = 0.5;
    private static final double STEP_BACK = 1.0E-4;
    private static final float SECTION = OcclusionPass.SECTION_SIZE;

    private static final BlockPos.MutableBlockPos POSITION = new BlockPos.MutableBlockPos();
    private static final int BOX_INTERVAL = 7;
    private static int frames;
    private static int boxes;
    private static int audits;
    private static long sections;
    private static long rays;
    private static long violations;
    private static long seed = 0x9E3779B97F4A7C15L;

    private CullingAudit() {
    }

    static void afterCameraPass(OcclusionPipeline.Frame result, Vec3 camera) {
        if (++frames % FRAME_INTERVAL != 0) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int checked = 0;
        for (int index = 0; index < result.size() && checked < MAX_SECTIONS; index++) {
            if (result.isVisible(index)) {
                continue;
            }
            checked++;
            audit(level, (SectionRenderDispatcher.RenderSection) result.target(index), camera, result.clip());
        }
        sections += checked;
        if (++audits % REPORT_INTERVAL == 0) {
            Kinetic.LOGGER.info("Kinetic culling audit: {} hidden sections, {} hidden entities and particles, {} rays, {} rays reached a hidden target", sections, boxes, rays, violations);
        }
    }

    private static void audit(ClientLevel level, SectionRenderDispatcher.RenderSection section, Vec3 camera, ClipMatrix clip) {
        BlockPos origin = section.getRenderOrigin();
        trace(level, camera, clip, origin.getX(), origin.getY(), origin.getZ(),
            origin.getX() + SECTION, origin.getY() + SECTION, origin.getZ() + SECTION, "section at " + origin);
    }

    private static void trace(ClientLevel level, Vec3 camera, ClipMatrix clip,
                                 double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String target) {
        for (int ray = 0; ray < RAYS_PER_SECTION; ray++) {
            double x = minX + random() * (maxX - minX);
            double y = minY + random() * (maxY - minY);
            double z = minZ + random() * (maxZ - minZ);
            float relativeX = (float) (x - camera.x);
            float relativeY = (float) (y - camera.y);
            float relativeZ = (float) (z - camera.z);
            if (!onScreen(clip, relativeX, relativeY, relativeZ)) {
                continue;
            }
            double entry = VoxelRay.entry(camera.x, camera.y, camera.z, x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
            if (Double.isNaN(entry)) {
                continue;
            }
            double length = Math.sqrt(relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ);
            if (entry * length < MIN_ENTRY_DISTANCE) {
                continue;
            }
            double before = Math.max(0.0, entry - STEP_BACK / length);
            double endX = camera.x + (x - camera.x) * before;
            double endY = camera.y + (y - camera.y) * before;
            double endZ = camera.z + (z - camera.z) * before;
            rays++;
            if (VoxelRay.reaches(camera.x, camera.y, camera.z, endX, endY, endZ, (bx, by, bz) -> level.getBlockState(POSITION.set(bx, by, bz)).isSolidRender())) {
                if (violations++ < MAX_LOGGED_VIOLATIONS) {
                    Kinetic.LOGGER.warn("Kinetic culling audit: {} was hidden but the camera at {} sees it through ({}, {}, {})", target, camera, x, y, z);
                }
            }
        }
    }

    static void hiddenBox(AABB box, Vec3 camera, ClipMatrix clip) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ++boxes % BOX_INTERVAL != 0) {
            return;
        }
        trace(level, camera, clip, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, "hidden box " + box);
    }

    private static boolean onScreen(ClipMatrix clip, float x, float y, float z) {
        float w = clip.w(x, y, z);
        return w > 0.5F && Math.abs(clip.x(x, y, z)) <= w && Math.abs(clip.y(x, y, z)) <= w;
    }

    private static double random() {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (seed >>> 11) * 0x1.0p-53;
    }
}
