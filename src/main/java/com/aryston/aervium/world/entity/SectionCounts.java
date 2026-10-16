package com.aryston.aervium.world.entity;

import java.util.function.ToIntFunction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

public final class SectionCounts {
    private SectionCounts() {
    }

    public static boolean noneNear(EntitySectionStorage<Entity> storage, AABB box, ToIntFunction<EntitySection<Entity>> count) {
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
                    if (section != null && count.applyAsInt(section) != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
