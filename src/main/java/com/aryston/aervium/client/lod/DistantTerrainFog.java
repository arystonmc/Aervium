package com.aryston.aervium.client.lod;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class DistantTerrainFog {
    static final float START_FRACTION = 0.85F;

    private DistantTerrainFog() {
    }

    public static void apply(ViewportEvent.RenderFog event) {
        if (!DistantTerrain.drawing() || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        FogData fog = event.getFogData();
        float distance = DistantTerrain.distanceBlocks();
        if (fog.environmentalEnd > 0.0F && fog.environmentalEnd < distance) {
            float stretch = distance / fog.environmentalEnd;
            fog.environmentalStart *= stretch;
            fog.environmentalEnd = distance;
        }
        float end = Math.max(fog.renderDistanceEnd, distance);
        fog.renderDistanceEnd = end;
        fog.renderDistanceStart = end * START_FRACTION;
    }
}
