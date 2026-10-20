package com.aryston.aervium.debug.benchmark;

public enum BenchmarkScene {
    WALK("walk", "Walking over the surface", SceneSetup.NONE, SceneSite.SPAWN),
    FLIGHT("flight", "Flight at " + BenchmarkRoute.FLIGHT_SPEED + " blocks per tick over new terrain", SceneSetup.NONE, SceneSite.SPAWN),
    UNDERGROUND("underground", "Flight through caves and deepslate", SceneSetup.NONE, SceneSite.SPAWN),
    CROWD("crowd", CrowdSetup.MOBS + " mobs, " + CrowdSetup.ITEMS + " floating items and " + CrowdSetup.ARMOR_STANDS + " armor stands in a glass pen", new CrowdSetup(), new SceneSite(-96, -96, 40)),
    STORAGE("storage", StorageSetup.blockEntities() + " chests, signs, banners, lit furnaces, hoppers and shulker boxes", new StorageSetup(), new SceneSite(0, -160, 40)),
    PARTICLES("particles", ParticleStormSetup.SIZE * ParticleStormSetup.SIZE + " campfires under a storm of " + ParticleStormSetup.particlesPerTick() + " particles per tick", new ParticleStormSetup(), new SceneSite(-96, 96, 40)),
    REDSTONE("redstone", RedstoneSetup.clockCount() + " observer clocks with sticky pistons and lamps", new RedstoneSetup(), new SceneSite(96, -96, 24)),
    EXPLOSION("explosion", "Chain reaction of " + ExplosionSetup.tntCount() + " TNT on the ground", new ExplosionSetup(), new SceneSite(160, 64, 0)),
    SERVER("server", SimulatedPlayerRoutes.PLAYERS + " simulated players exploring away from the spawn", SceneSetup.NONE, SceneSite.SPAWN),
    OVERLOOK("overlook", "Circling high above the spawn and looking down at the terrain", SceneSetup.NONE, SceneSite.SPAWN);

    private final String id;
    private final String description;
    private final SceneSetup setup;
    private final SceneSite site;

    BenchmarkScene(String id, String description, SceneSetup setup, SceneSite site) {
        this.id = id;
        this.description = description;
        this.setup = setup;
        this.site = site;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public SceneSetup setup() {
        return setup;
    }

    public SceneSite site() {
        return site;
    }
}
