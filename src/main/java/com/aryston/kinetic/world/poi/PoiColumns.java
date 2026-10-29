package com.aryston.kinetic.world.poi;

import com.aryston.kinetic.core.collection.ColumnIndexedLongMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;

public final class PoiColumns {
    private PoiColumns() {
    }

    public static <R> ColumnIndexedLongMap<Optional<R>> createMap(LevelHeightAccessor height) {
        int minSectionY = height.getMinSectionY();
        return new ColumnIndexedLongMap<>(new ColumnIndexedLongMap.Layout() {
            @Override
            public long column(long key) {
                return ChunkPos.pack(SectionPos.x(key), SectionPos.z(key));
            }

            @Override
            public int index(long key) {
                return SectionPos.y(key) - minSectionY;
            }
        }, Optional::isPresent, height.getSectionsCount());
    }

    public static boolean supports(LevelHeightAccessor height) {
        return ColumnIndexedLongMap.supports(height.getSectionsCount());
    }

    public static <R, T> Stream<T> records(ColumnIndexedLongMap<Optional<R>> map, LevelHeightAccessor height, ChunkPos chunk,
                                           Function<R, Stream<T>> records) {
        long column = chunk.pack();
        if (!map.complete(column)) {
            return null;
        }
        long mask = map.occupiedMask(column);
        if (mask == 0L) {
            return Stream.empty();
        }
        int minSectionY = height.getMinSectionY();
        return LongStream.iterate(mask, bits -> bits != 0L, bits -> bits & (bits - 1))
            .mapToObj(bits -> map.get(SectionPos.asLong(chunk.x(), minSectionY + Long.numberOfTrailingZeros(bits), chunk.z())))
            .flatMap(section -> section.map(records).orElseGet(Stream::empty));
    }
}
