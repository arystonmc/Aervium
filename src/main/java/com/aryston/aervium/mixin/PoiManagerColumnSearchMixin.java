package com.aryston.aervium.mixin;

import com.aryston.aervium.core.collection.ColumnIndexedLongMap;
import com.aryston.aervium.world.poi.PoiAudit;
import com.aryston.aervium.world.poi.PoiColumns;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoiManager.class)
abstract class PoiManagerColumnSearchMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "getInChunk", at = @At("HEAD"), cancellable = true)
    private void aervium$searchOccupiedSections(Predicate<Holder<PoiType>> predicate, ChunkPos chunkPos, PoiManager.Occupancy occupancy,
                                                CallbackInfoReturnable<Stream<PoiRecord>> callback) {
        SectionStorageAccessor<PoiSection> self = (SectionStorageAccessor<PoiSection>) this;
        if (self.aervium$storage() instanceof ColumnIndexedLongMap<Optional<PoiSection>> map) {
            LevelHeightAccessor height = self.aervium$levelHeightAccessor();
            Stream<PoiRecord> records = PoiColumns.records(map, height, chunkPos, section -> section.getRecords(predicate, occupancy));
            if (records == null) {
                return;
            }
            if (PoiAudit.ENABLED) {
                List<PoiRecord> indexed = records.toList();
                PoiAudit.compare(indexed, aervium$vanillaSearch(map, height, predicate, chunkPos, occupancy));
                records = indexed.stream();
            }
            callback.setReturnValue(records);
        }
    }

    @Unique
    private static List<PoiRecord> aervium$vanillaSearch(ColumnIndexedLongMap<Optional<PoiSection>> map, LevelHeightAccessor height,
                                                         Predicate<Holder<PoiType>> predicate, ChunkPos chunkPos, PoiManager.Occupancy occupancy) {
        return IntStream.rangeClosed(height.getMinSectionY(), height.getMaxSectionY())
            .mapToObj(sectionY -> map.get(SectionPos.of(chunkPos, sectionY).asLong()))
            .filter(Optional::isPresent)
            .flatMap(section -> section.get().getRecords(predicate, occupancy))
            .toList();
    }
}
