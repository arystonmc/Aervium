# Aervium Architecture: Configuration and Modules

Part of the [Aervium architecture](../ARCHITECTURE.md).

## Configuration and Modules

Aervium otherwise depends on no other mod, but its optional client-side Arkea dependency provides the enhanced settings screen when installed; `AerviumClient` confirms Arkea before loading `ArkeaSettings`, and otherwise keeps the NeoForge list. The screen groups live client configuration and presets separately from restart-required optimization modules, whose switches are stored in `config/aervium-modules.properties` before mixins load.
