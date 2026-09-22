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

ExpansionAE initially compiled with Mojang official mappings, but a real `runServer` exposed incompatible AE2 Mixin targets. Disabling the Mixin refmap alone only changed which target failed; changing to MCP mappings alone still left the published AE2 refmap targeting SRG names.

The working userdev configuration therefore needs both parts: MCP snapshot mappings for the runtime class/method namespace, and the AE2 refmap disabled in Gradle `run*` configurations so Mixin resolves the source-level MCP targets directly.

The project uses:

```properties
mappings_channel=snapshot
mappings_version=20210309-1.16.5
```

The published AE2 8.4.7 artifact also carries a refmap whose targets are expressed in the production/SRG namespace. ForgeGradle deobfuscates that dependency into the MCP userdev namespace, so the `client`, `server` and `data` development runs set:

```groovy
property 'mixin.env.disableRefMap', 'true'
```

This property is a userdev runtime setting only; it is not packaged into the ExpansionAE JAR. With MCP mappings, disabling the published refmap lets AE2's Mixin source targets resolve against the MCP-named development classes.

Do not switch the project back to `official` mappings or remove the userdev refmap setting without proving AE2 8.4.7 client/server startup. A successful Java compilation is not sufficient for this dependency because its Mixins are applied at runtime.

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
- dedicated-server crashes before the server reaches the ready state;
- broken DISK registration or Cell Workbench contracts;
- regressions in DISK capacity, insertion/extraction and UUID-alias synchronization;
- regressions in external storage persistence across an actual server restart.

When `expansionae.validateDevRuntime=true`, the dedicated-server run asserts the AE2 cell registration, a 63-slot Cell Workbench config inventory, 2 upgrade slots with FUZZY/INVERTER support, 1000-item capacity, cross-alias visibility and empty-record preservation.

The pull-request workflow then runs two server phases against the same world. The `write` phase stages 321 items in a reserved test UUID and shuts the server down via RCON. The `read` phase starts a new server process, requires those 321 items to be recovered from `WorldSavedData`, extracts them and cleans up the test record. Both phases must reach the normal ready state and terminate cleanly.

A successful JAR build does not replace these runtime gates. They are currently green with Forge 36.2.42 + AE2 8.4.7 + MCP `20210309-1.16.5`.

The workflow also uses per-event/ref concurrency with `cancel-in-progress` so obsolete push/PR runs do not consume runner capacity.

## DISK persistence model

The DISK vertical slice deliberately stores its full contents in overworld `WorldSavedData`, keyed by UUID. The ItemStack only carries the storage UUID and small cached metadata.

The UUID identifies the backing storage, not the physical ItemStack. Exact copies with the same UUID are aliases of one logical DISK. They must observe the same contents and must never duplicate those contents.

Once assigned, a UUID remains assigned even when the DISK becomes empty. Its empty backing record is preserved so existing aliases continue to resolve to the same logical storage.

Once an ItemStack has a DISK UUID, its backing record is mandatory, including for an empty DISK. If that record is missing, reads and writes fail closed instead of silently recreating or overwriting storage.
