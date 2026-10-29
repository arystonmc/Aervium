package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.collision.EntityCollisionShapes;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
abstract class ClientLevelEntityCollisionsMixin {
    @Shadow
    @Final
    private TransientEntitySectionManager<Entity> entityStorage;

    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        return EntityCollisionShapes.of(
            (ClientLevel) (Object) this, ((TransientEntitySectionManagerAccessor) entityStorage).kinetic$sectionStorage(), source, testArea
        );
    }
}
