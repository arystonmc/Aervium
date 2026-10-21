# Aervium Code Map: Build Files

Part of the [Aervium code map](../CODEMAP.md).

## Build Files

The `build.gradle` test task enables JDK 25 native access and the supported legacy native memory access needed by the shader compiler test; these flags apply only to tests.

| File | Purpose |
|---|---|
| `build.gradle` | ModDevGradle setup, the Arkea Maven repository and compile-only Arkea dependency for the optional client settings screen, JUnit 5 for `src/test/java` with the Minecraft classpath of `main`, `client` run forced to Vulkan, `server` run without GUI, `parity` dedicated server run in `runs/parity` (`setUpParityWorld` writes the flat world settings, or normal terrain with `-PparityWorld=normal`, EULA and module switches from `-PparityModules` and `-PparityOff` and deletes the old world first), metadata expansion, packs `branding/logo.png` into the jar as `aervium.png`, jar name `aervium-neoforge-<minecraft_version>-<version>.jar`. |
| `gradle.properties` | Single place for versions and mod metadata, including the Arkea version and compatible version range. |
| `settings.gradle` | Plugin repositories, Java toolchain resolver, project name. |
| `.github/workflows/build.yml` | CI on every push and pull request: JDK 25, `./gradlew build`, the vanilla parity check with every module on and off (the hash lines must match), uploads the jar. |
| `.github/workflows/release.yml` | On a `v*` tag: checks the tag against `mod_version`, builds, creates the GitHub release with the changelog section, and uploads to Modrinth and CurseForge when their secrets exist. |
| `.github/workflows/listing.yml` | Syncs the Modrinth project page (README, links, license, optional on both sides, category optimization) on pushes to `main`. |
