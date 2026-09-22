# Development baseline

## Toolchain

- Minecraft 1.16.5
- Forge 36.2.42
- ForgeGradle 4.1.x
- Gradle 6.8.3
- Java 8
- Applied Energistics 2 8.4.7
- MCP snapshot mappings 20210309-1.16.5

## First validation gate

The scaffold is considered usable only when all of the following are true:

- `./gradlew clean build` passes on Java 8;
- Forge resolves AE2 8.4.7 from ModMaven;
- the produced jar is reobfuscated;
- `mods.toml` declares AE2 as mandatory;
- ExpansionAE compiles against the public AE2 API;
- `AE2Bridge` is discoverable through AE2's `@AEAddon` mechanism;
- no client-only class is referenced from common bootstrap code.

After this gate passes, the next implementation target is the first isolated AE2 feature rather than an external-mod integration.
