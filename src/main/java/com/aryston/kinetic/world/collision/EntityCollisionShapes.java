package com.aryston.kinetic.world.collision;

import com.aryston.kinetic.world.entity.SectionCounts;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class EntityCollisionShapes {
    private static final double MIN_SIZE = 1.0E-7;

    private EntityCollisionShapes() {
    }

    public static List<VoxelShape> of(Level level, EntitySectionStorage<Entity> sections, @Nullable Entity source, AABB testArea) {
        if (testArea.getSize() < MIN_SIZE) {
            return List.of();
        }
        AABB area = testArea.inflate(MIN_SIZE);
        Predicate<Entity> canCollide = source == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(source::canCollideWith);
        if ((source == null || CollidableEntities.defaultSource(source)) && noCollidableNear(level, sections, area)) {
            if (CollisionAudit.ENABLED) {
                CollisionAudit.skipped(level.getEntities(source, area, canCollide).isEmpty());
            }
            return List.of();
        }
        List<Entity> collidingEntities = level.getEntities(source, area, canCollide);
        if (collidingEntities.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder<VoxelShape> shapes = ImmutableList.builderWithExpectedSize(collidingEntities.size());
        for (Entity entity : collidingEntities) {
            shapes.add(Shapes.create(entity.getBoundingBox()));
        }
        return shapes.build();
    }

    private static boolean noCollidableNear(Level level, EntitySectionStorage<Entity> sections, AABB box) {
        if (!level.dragonParts().isEmpty()) {
            return false;
        }
        return SectionCounts.noneNear(sections, box, section -> ((CollidableEntities.Counter) section).kinetic$collidable());
    }
}
