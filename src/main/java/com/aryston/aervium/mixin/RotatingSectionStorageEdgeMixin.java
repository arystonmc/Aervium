package com.aryston.aervium.mixin;

import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotatingSectionStorage.class)
abstract class RotatingSectionStorageEdgeMixin<T extends RotatingSectionStorage.Value> {
    @Unique
    private static final int AERVIUM$UNPLACED = Integer.MIN_VALUE;

    @Shadow
    @Final
    private RotatingSectionStorage.Node<T>[] nodes;

    @Shadow
    @Final
    private int radius;

    @Shadow
    @Final
    private int minY;

    @Shadow
    @Final
    private int sectionGridSizeY;

    @Shadow
    @Final
    private int sectionGridSizeXZ;

    @Shadow
    private SectionPos centerSectionPos;

    @Inject(method = "repositionCenter", at = @At("HEAD"), cancellable = true)
    private void aervium$moveOnlyTheEdges(SectionPos newCenter, CallbackInfoReturnable<Boolean> callback) {
        SectionPos oldCenter = centerSectionPos;
        if (newCenter.equals(oldCenter) || oldCenter.x() == AERVIUM$UNPLACED
            || Math.abs(newCenter.x() - oldCenter.x()) >= sectionGridSizeXZ || Math.abs(newCenter.z() - oldCenter.z()) >= sectionGridSizeXZ) {
            return;
        }
        int lowestX = newCenter.x() - radius;
        int lowestZ = newCenter.z() - radius;
        int oldLowestX = oldCenter.x() - radius;
        int oldLowestZ = oldCenter.z() - radius;
        for (int gridX = 0; gridX < sectionGridSizeXZ; gridX++) {
            int sectionX = lowestX + Math.floorMod(gridX - lowestX, sectionGridSizeXZ);
            boolean movedX = sectionX != oldLowestX + Math.floorMod(gridX - oldLowestX, sectionGridSizeXZ);
            for (int gridZ = 0; gridZ < sectionGridSizeXZ; gridZ++) {
                int sectionZ = lowestZ + Math.floorMod(gridZ - lowestZ, sectionGridSizeXZ);
                if (movedX || sectionZ != oldLowestZ + Math.floorMod(gridZ - oldLowestZ, sectionGridSizeXZ)) {
                    aervium$moveColumn(gridX, gridZ, sectionX, sectionZ);
                }
            }
        }
        centerSectionPos = newCenter;
        callback.setReturnValue(true);
    }

    @Unique
    private void aervium$moveColumn(int gridX, int gridZ, int sectionX, int sectionZ) {
        for (int gridY = 0; gridY < sectionGridSizeY; gridY++) {
            T value = nodes[(gridZ * sectionGridSizeY + gridY) * sectionGridSizeXZ + gridX].value();
            long sectionNode = SectionPos.asLong(sectionX, minY + gridY, sectionZ);
            if (value.getSectionNode() != sectionNode) {
                value.setSectionNode(sectionNode);
            }
        }
    }
}
