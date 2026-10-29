package com.aryston.kinetic.core.lod;

import java.util.ArrayList;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

public final class LodAllocator {
    private final int pageBytes;
    private final int maxPages;
    private final int alignment;
    private final ArrayList<TreeMap<Integer, Integer>> pages = new ArrayList<>();

    public LodAllocator(int pageBytes, int maxPages, int alignment) {
        if (pageBytes <= 0 || maxPages <= 0 || alignment <= 0 || pageBytes % alignment != 0) {
            throw new IllegalArgumentException("Invalid distant terrain allocation limits");
        }
        this.pageBytes = pageBytes;
        this.maxPages = maxPages;
        this.alignment = alignment;
    }

    public @Nullable Allocation allocate(int bytes) {
        if (bytes <= 0 || bytes > pageBytes) {
            return null;
        }
        int aligned = (bytes + alignment - 1) / alignment * alignment;
        for (int page = 0; page < pages.size(); page++) {
            Allocation allocation = take(page, aligned);
            if (allocation != null) {
                return allocation;
            }
        }
        if (pages.size() == maxPages) {
            return null;
        }
        TreeMap<Integer, Integer> ranges = new TreeMap<>();
        ranges.put(0, pageBytes);
        pages.add(ranges);
        return take(pages.size() - 1, aligned);
    }

    public void free(Allocation allocation) {
        TreeMap<Integer, Integer> ranges = pages.get(allocation.page());
        int offset = allocation.offset();
        int length = allocation.bytes();
        var before = ranges.floorEntry(offset);
        if (before != null && before.getKey() + before.getValue() > offset) {
            throw new IllegalArgumentException("Distant terrain allocation already released");
        }
        if (before != null && before.getKey() + before.getValue() == offset) {
            offset = before.getKey();
            length += before.getValue();
            ranges.remove(before.getKey());
        }
        var after = ranges.ceilingEntry(offset);
        if (after != null && offset + length > after.getKey()) {
            throw new IllegalArgumentException("Overlapping distant terrain release");
        }
        if (after != null && offset + length == after.getKey()) {
            length += after.getValue();
            ranges.remove(after.getKey());
        }
        ranges.put(offset, length);
    }

    private @Nullable Allocation take(int page, int bytes) {
        TreeMap<Integer, Integer> ranges = pages.get(page);
        for (var range : ranges.entrySet()) {
            if (range.getValue() >= bytes) {
                int offset = range.getKey();
                int remaining = range.getValue() - bytes;
                ranges.remove(offset);
                if (remaining > 0) {
                    ranges.put(offset + bytes, remaining);
                }
                return new Allocation(page, offset, bytes);
            }
        }
        return null;
    }

    public record Allocation(int page, int offset, int bytes) {
    }
}
