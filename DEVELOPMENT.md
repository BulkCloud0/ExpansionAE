# ExpansionAE development baseline

## Toolchain

- Minecraft 1.16.5
- Forge 36.2.42
- ForgeGradle 5.1.x
- Gradle 7.3.3
- Java 8
- Applied Energistics 2 8.4.7
- MCP snapshot mappings `20210309-1.16.5`

## Why MCP mappings are required

AE2 8.4.7 was built in the 1.16.5 MCP/SRG mapping ecosystem and its Mixin configuration contains targets that depend on that naming model.

ExpansionAE initially compiled with Mojang official mappings, but a real `runServer` exposed incompatible AE2 Mixin targets. Disabling the Mixin refmap only changed which target failed; it did not solve the underlying namespace mismatch.

The development workspace therefore uses the same MCP snapshot generation as AE2's 1.16.x branch:

```properties
mappings_channel=snapshot
mappings_version=20210309-1.16.5
```

Do not switch the project back to `official` mappings without also proving AE2 8.4.7 client/server startup. A successful Java compilation is not sufficient for this dependency because its Mixins are applied at runtime.

## Validation gates

Every change must at least pass:

```text
./gradlew --no-daemon clean build
```

The pull-request workflow additionally launches the Forge dedicated-server development runtime. The smoke test is intended to catch:

- AE2 Mixin/refmap mapping incompatibilities;
- accidental client-only class references from common code;
- mod-loading failures;
- registry/bootstrap errors;
- dedicated-server crashes before the server reaches the ready state.

A successful JAR build does not replace this runtime gate.

## DISK persistence model

The DISK vertical slice deliberately stores its full contents in overworld `WorldSavedData`, keyed by UUID. The ItemStack only carries the storage UUID and small cached metadata.

The UUID identifies the backing storage, not the physical ItemStack. Exact copies with the same UUID are aliases of one logical DISK. They must observe the same contents and must never duplicate those contents.

Once assigned, a UUID remains assigned even when the DISK becomes empty. Its empty backing record is preserved so existing aliases continue to resolve to the same logical storage.

If an ItemStack indicates stored content but its backing record is missing, reads and writes fail closed instead of silently recreating an empty record.
