package com.aryston.kinetic.client.lod;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.lod.LodData;
import com.aryston.kinetic.core.lod.LodDiskCache;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.jspecify.annotations.Nullable;

final class TerrainCache {
    private static final String DIRECTORY = "kinetic/cache/terrain";
    private static final String VERSION = "terrain-5";
    private static final String DIGEST = "SHA-256";

    private final ServerLevel level;
    private final ResourceManager clientResources;
    private final String quality;
    private final Consumer<MessageDigest> materialFingerprint;
    private @Nullable LodDiskCache cache;
    private boolean disabled;

    TerrainCache(ServerLevel level) {
        this.level = level;
        clientResources = Minecraft.getInstance().getResourceManager();
        quality = DistantTerrain.quality().name();
        materialFingerprint = TerrainMaterials.fingerprintSnapshot();
    }

    synchronized @Nullable LodData read(long key, boolean exact) {
        try {
            LodDiskCache disk = disk();
            return disk == null ? null : disk.read(key, exact);
        } catch (IOException | RuntimeException failure) {
            disable(failure);
            return null;
        }
    }

    synchronized void write(long key, LodData data) {
        try {
            LodDiskCache disk = disk();
            if (disk != null) {
                disk.write(key, data);
            }
        } catch (IOException | RuntimeException failure) {
            disable(failure);
        }
    }

    private @Nullable LodDiskCache disk() throws IOException {
        if (disabled) {
            return null;
        }
        if (cache == null) {
            cache = new LodDiskCache(FMLPaths.GAMEDIR.get().resolve(DIRECTORY), identity());
        }
        return cache;
    }

    private String identity() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST);
            update(digest, VERSION);
            update(digest, Integer.toString(SharedConstants.getCurrentVersion().dataVersion().version()));
            update(digest, Long.toString(level.getSeed()));
            update(digest, quality);
            update(digest, Boolean.toString(level.getServer().getWorldGenSettings().options().generateStructures()));
            update(digest, ChunkGenerator.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()),
                level.getChunkSource().getGenerator()).getOrThrow().toString());
            update(digest, level.dimension().identifier().toString());
            update(digest, level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString());
            ModList.get().getMods().stream().sorted(java.util.Comparator.comparing(mod -> mod.getModId())).forEach(mod -> {
                update(digest, mod.getModId());
                update(digest, mod.getVersion().toString());
            });
            materialFingerprint.accept(digest);
            level.getServer().getResourceManager().listPacks().forEach(pack -> update(digest, pack.packId()));
            clientResources.listPacks().forEach(pack -> update(digest, pack.packId()));
            fingerprint(digest, clientResources, "textures/colormap");
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void fingerprint(MessageDigest digest, ResourceManager resources, String prefix) throws IOException {
        var entries = resources.listResources(prefix, id -> id.getPath().endsWith(".json") || id.getPath().endsWith(".png") || id.getPath().endsWith(".nbt"));
        for (var entry : entries.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList()) {
            update(digest, entry.getKey().toString());
            try (var input = entry.getValue().open()) {
                digest.update(input.readAllBytes());
            }
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(java.nio.ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private void disable(Exception failure) {
        if (!disabled) {
            disabled = true;
            Kinetic.LOGGER.warn("Kinetic distant terrain disk cache disabled for this level", failure);
        }
    }
}
