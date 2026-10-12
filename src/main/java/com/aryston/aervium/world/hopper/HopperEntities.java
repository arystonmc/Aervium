package com.aryston.aervium.world.hopper;

import com.aryston.aervium.mixin.EntityCapabilityAccessor;
import com.aryston.aervium.mixin.PersistentEntitySectionManagerAccessor;
import com.aryston.aervium.mixin.ServerLevelEntityManagerAccessor;
import com.aryston.aervium.world.entity.SectionCounts;
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

/**
 * Which entities a hopper's entity searches can find. Every hopper without a block above or below looks for item
 * entities and for entities it can take from or put into (chest minecarts, chest boats, and anything with a NeoForge
 * item automation capability). Sections count both kinds, so a search over sections that hold none can be answered
 * empty without walking their entities.
 */
public final class HopperEntities {
    private HopperEntities() {
    }

    public static boolean isItem(Object entity) {
        return entity instanceof ItemEntity;
    }

    /**
     * True for every entity the NeoForge hopper container search could accept: containers, and entity types with an
     * item automation provider registered (a provider may still answer null for some entities; those count too).
     */
    public static boolean mayHoldItems(Object entity) {
        if (entity instanceof Container) {
            return true;
        }
        if (!(entity instanceof Entity e)) {
            return false;
        }
        Map<EntityType<?>, ? extends List<?>> providers = ((EntityCapabilityAccessor) (Object) Capabilities.Item.ENTITY_AUTOMATION).aervium$providers();
        List<?> forType = providers.get(e.getType());
        return forType != null && !forType.isEmpty();
    }

    /** True when no section the item search for {@code box} visits holds an item entity. */
    public static boolean noItemsNear(Level level, AABB box) {
        EntitySectionStorage<Entity> storage = storage(level);
        return storage != null && SectionCounts.noneNear(storage, box, section -> ((Counter) section).aervium$items());
    }

    /** True when no section the container search for {@code box} visits holds an entity that may hold items. */
    public static boolean noContainersNear(Level level, AABB box) {
        EntitySectionStorage<Entity> storage = storage(level);
        return storage != null && SectionCounts.noneNear(storage, box, section -> ((Counter) section).aervium$containers());
    }

    /** Server levels only, and none with part entities: those are searched outside the sections. */
    private static EntitySectionStorage<Entity> storage(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.dragonParts().isEmpty()) {
            return null;
        }
        return ((PersistentEntitySectionManagerAccessor) ((ServerLevelEntityManagerAccessor) serverLevel).aervium$entityManager()).aervium$sectionStorage();
    }

    /** Implemented by entity sections: how many item entities and entities that may hold items they contain. */
    public interface Counter {
        int aervium$items();

        int aervium$containers();
    }
}
