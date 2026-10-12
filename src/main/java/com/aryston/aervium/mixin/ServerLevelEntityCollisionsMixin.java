package com.aryston.aervium.mixin;

import com.aryston.aervium.world.collision.CollidableEntities;
import com.aryston.aervium.world.collision.CollisionAudit;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Every mob move asks for the entities it could bump into, and vanilla walks every entity in the nearby sections to
 * find that almost none can be bumped into (only boats, shulkers and happy ghasts). Sections count those entities, so
 * when none of the sections vanilla would visit holds one, the answer is the same empty list without the walk.
 * Anything else takes the vanilla path.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelEntityCollisionsMixin {
    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        if (testArea.getSize() < 1.0E-7) {
            return List.of();
        }
        Predicate<Entity> canCollide = source == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(source::canCollideWith);
        if ((source == null || CollidableEntities.defaultSource(source)) && aervium$noCollidableNear(testArea.inflate(1.0E-7))) {
            if (CollisionAudit.ENABLED) {
                CollisionAudit.skipped(((ServerLevel) (Object) this).getEntities(source, testArea.inflate(1.0E-7), canCollide).isEmpty());
            }
            return List.of();
        }
        List<Entity> collidingEntities = ((ServerLevel) (Object) this).getEntities(source, testArea.inflate(1.0E-7), canCollide);
        if (collidingEntities.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder<VoxelShape> shapes = ImmutableList.builderWithExpectedSize(collidingEntities.size());
        for (Entity entity : collidingEntities) {
            shapes.add(Shapes.create(entity.getBoundingBox()));
        }
        return shapes.build();
    }

    /** Same section range as EntitySectionStorage.forEachAccessibleNonEmptySection, plus the level's dragon parts. */
    @Unique
    private boolean aervium$noCollidableNear(AABB box) {
        if (!((ServerLevel) (Object) this).dragonParts().isEmpty()) {
            return false;
        }
        EntitySectionStorage<Entity> storage = ((PersistentEntitySectionManagerAccessor) entityManager).aervium$sectionStorage();
        int xMin = SectionPos.posToSectionCoord(box.minX - 2.0);
        int yMin = SectionPos.posToSectionCoord(box.minY - 4.0);
        int zMin = SectionPos.posToSectionCoord(box.minZ - 2.0);
        int xMax = SectionPos.posToSectionCoord(box.maxX + 2.0);
        int yMax = SectionPos.posToSectionCoord(box.maxY + 0.0);
        int zMax = SectionPos.posToSectionCoord(box.maxZ + 2.0);
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    EntitySection<Entity> section = storage.getSection(SectionPos.asLong(x, y, z));
                    if (section != null && ((CollidableEntities.Counter) section).aervium$collidable() != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
