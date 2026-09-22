# Development baseline

## Toolchain

- Minecraft 1.16.5
- Forge 36.2.42
- ForgeGradle 5.1.x
- Gradle 7.3.3
- Java 8 target
- Applied Energistics 2 8.4.7
- Mojang official mappings for 1.16.5

ForgeGradle itself is deliberately kept in the 5.1 generation because Forge 36.2.42 cannot be resolved correctly by the older ForgeGradle 4.1 setup used by early AE2 8.4.x development.

## First validation gate

The scaffold is considered usable only when all of the following are true:

- `./gradlew clean build` passes;
- compiled classes target Java 8;
- Forge resolves AE2 8.4.7 from ModMaven;
- the produced jar is reobfuscated;
- `mods.toml` declares AE2 as mandatory;
- ExpansionAE compiles against the public AE2 API;
- `AE2Bridge` is discoverable through AE2's `@AEAddon` mechanism;
- no client-only class is referenced from common bootstrap code.

After this gate passes, the next implementation target is the first isolated AE2 feature rather than an external-mod integration.


## First gameplay vertical slice: DISK

The first gameplay feature is a custom item storage cell inspired by AE2Things' DISK.

Initial scope:
- 1k, 4k, 16k and 64k item-capacity tiers;
- no item-type limit;
- custom AE2 cell handler;
- contents persisted in overworld `WorldSavedData` under a UUID rather than embedded in the item NBT;
- only the UUID and cached total item count remain on the item;
- storage cells are rejected as DISK contents to prevent recursive/nested storage in the first implementation;
- no fuzzy/inverter workbench configuration yet.
