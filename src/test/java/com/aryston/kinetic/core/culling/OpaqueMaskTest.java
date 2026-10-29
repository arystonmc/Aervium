package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpaqueMaskTest {
    @Test
    void emptyAndSparseSectionsHaveNoOccluders() {
        OpaqueMask mask = new OpaqueMask();
        assertTrue(mask.boxes().isEmpty());
        mask.set(3, 4, 5);
        mask.set(9, 9, 9);
        assertTrue(mask.boxes().isEmpty());
    }

    @Test
    void solidSectionIsOneBox() {
        OpaqueMask mask = filled();
        OccluderBoxes boxes = mask.boxes();
        assertEquals(1, boxes.count());
        assertEquals(16 * 16 * 16, boxes.volume(0));
    }

    @Test
    void cavePocketSplitsTheSectionIntoFewBoxes() {
        OpaqueMask mask = new OpaqueMask();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (!(x >= 6 && x < 10 && y >= 6 && y < 10 && z >= 6 && z < 10)) {
                        mask.set(x, y, z);
                    }
                }
            }
        }
        OccluderBoxes boxes = mask.boxes();
        assertTrue(boxes.count() <= 7, "boxes " + boxes.count());
        int volume = 0;
        for (int box = 0; box < boxes.count(); box++) {
            volume += boxes.volume(box);
        }
        assertEquals(16 * 16 * 16 - 4 * 4 * 4, volume);
    }

    @Test
    void boxesOnlyCoverOpaqueBlocks() {
        OpaqueMask mask = new OpaqueMask();
        java.util.Random random = new java.util.Random(7);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (random.nextInt(40) != 0) {
                        mask.set(x, y, z);
                    }
                }
            }
        }
        OccluderBoxes boxes = mask.boxes();
        assertTrue(boxes.count() > 0 && boxes.count() <= OpaqueMask.MAX_BOXES);
        for (int box = 0; box < boxes.count(); box++) {
            for (int y = boxes.minY(box); y < boxes.maxY(box); y++) {
                for (int z = boxes.minZ(box); z < boxes.maxZ(box); z++) {
                    for (int x = boxes.minX(box); x < boxes.maxX(box); x++) {
                        assertTrue(mask.get(x, y, z));
                    }
                }
            }
        }
    }

    private static OpaqueMask filled() {
        OpaqueMask mask = new OpaqueMask();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    mask.set(x, y, z);
                }
            }
        }
        return mask;
    }
}
