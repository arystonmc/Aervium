package com.aryston.aervium.client.culling;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

public final class EntityOcclusion {
    private static final String HELION_MOD_ID = "helion";
    private static final double ENTITY_MARGIN = 1.0;
    private static final double BLOCK_ENTITY_MARGIN = 0.5;
    private static final boolean SUN_SHADOWS = ModList.get().isLoaded(HELION_MOD_ID);

    private static int tested;
    private static int hidden;

    private EntityOcclusion() {
    }

    public static boolean hidden(Entity entity, float partialTicks) {
        if (!TerrainOcclusion.enabled() || !cullable(entity) || castsSunShadow(entity.level(), entity.blockPosition())) {
            return false;
        }
        AABB bounds = entity.getInterpolatedBoundingBox(partialTicks);
        AABB box = bounds.inflate(ENTITY_MARGIN);
        boolean isHidden = count(!TerrainOcclusion.isVisibleToCamera(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
        if (isHidden && CullingAudit.ENABLED) {
            TerrainOcclusion.auditHiddenBox(bounds);
        }
        return isHidden;
    }

    public static boolean hidden(BlockEntity blockEntity, AABB renderBox) {
        if (!TerrainOcclusion.enabled() || blockEntity.getLevel() == null || castsSunShadow(blockEntity.getLevel(), blockEntity.getBlockPos())) {
            return false;
        }
        AABB box = renderBox.inflate(BLOCK_ENTITY_MARGIN);
        return count(!TerrainOcclusion.isVisibleToCamera(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
    }

    public static int takeTested() {
        int value = tested;
        tested = 0;
        return value;
    }

    public static int takeHidden() {
        int value = hidden;
        hidden = 0;
        return value;
    }

    private static boolean count(boolean isHidden) {
        tested++;
        if (isHidden) {
            hidden++;
        }
        return isHidden;
    }

    private static boolean cullable(Entity entity) {
        if (!(entity instanceof Mob || entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof ArmorStand)) {
            return false;
        }
        if (entity.isCurrentlyGlowing() || entity.hasCustomName() || entity.isPassenger() || entity.isVehicle()) {
            return false;
        }
        return !(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null;
    }

    private static boolean castsSunShadow(Level level, BlockPos position) {
        return SUN_SHADOWS && (!(level instanceof ClientLevel) || level.getBrightness(LightLayer.SKY, position) > 0);
    }
}
