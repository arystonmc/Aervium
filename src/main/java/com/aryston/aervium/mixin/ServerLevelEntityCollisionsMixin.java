package com.aryston.aervium.mixin;

import com.aryston.aervium.world.collision.EntityCollisionShapes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
abstract class ServerLevelEntityCollisionsMixin {
    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        return EntityCollisionShapes.of(
            (ServerLevel) (Object) this, ((PersistentEntitySectionManagerAccessor) entityManager).aervium$sectionStorage(), source, testArea
        );
    }
}
