package com.aryston.kinetic.world.hopper;

import com.aryston.kinetic.mixin.EntityCapabilityAccessor;
import com.aryston.kinetic.mixin.PersistentEntitySectionManagerAccessor;
import com.aryston.kinetic.mixin.ServerLevelEntityManagerAccessor;
import com.aryston.kinetic.world.entity.SectionCounts;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class HopperEntities {
    private HopperEntities() {
    }

    public static boolean isItem(Object entity) {
        return entity instanceof ItemEntity;
    }

    public static boolean mayHoldItems(Object entity) {
        if (entity instanceof Container) {
            return true;
        }
        if (!(entity instanceof Entity e)) {
            return false;
        }
        Map<EntityType<?>, ? extends List<?>> providers = ((EntityCapabilityAccessor) (Object) Capabilities.Item.ENTITY_AUTOMATION).kinetic$providers();
        List<?> forType = providers.get(e.getType());
        return forType != null && !forType.isEmpty();
    }

    public static boolean noItemsNear(Level level, AABB box) {
        EntitySectionStorage<Entity> storage = storage(level);
        return storage != null && SectionCounts.noneNear(storage, box, section -> ((Counter) section).kinetic$items());
    }

    public static boolean noContainersNear(Level level, AABB box) {
        EntitySectionStorage<Entity> storage = storage(level);
        return storage != null && SectionCounts.noneNear(storage, box, section -> ((Counter) section).kinetic$containers());
    }

    private static EntitySectionStorage<Entity> storage(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.dragonParts().isEmpty()) {
            return null;
        }
        return ((PersistentEntitySectionManagerAccessor) ((ServerLevelEntityManagerAccessor) serverLevel).kinetic$entityManager()).kinetic$sectionStorage();
    }

    public interface Counter {
        int kinetic$items();

        int kinetic$containers();
    }
}
