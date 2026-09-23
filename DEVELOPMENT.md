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
- broken DISK registration or Cell Workbench contracts across 1k/4k/16k/64k;
- regressions in tier capacities, insertion/extraction and UUID-alias synchronization;
- accidental reintroduction of the classic 63-type ceiling (the runtime stores 70 distinct vanilla item types in one 1k DISK);
- stale ItemStack cached item/type counts across aliases, including aliases opened outside an AE2 IActionHost;
- missing or malformed 1k/4k/16k/64k recipes and incorrect recipe outputs;
- regressions in external storage persistence across an actual server restart;
- regressions in DISK acceptance by an ME Drive and visibility through an ME Chest item monitor;
- regressions in Drive NBT/drop/replacement/chunk-unload lifecycle;
- same-grid alias double-counting;
- stale terminal caches when one UUID is hosted by multiple independent AE2 grids;
- recursive storage of a DISK alias inside its own backing UUID;
- client bootstrap/model-loading regressions;
- missing inventory or ME Drive models for any DISK tier;
- broken tooltip keys/arguments for cached counts and tier capacities.

When `expansionae.validateDevRuntime=true`, the dedicated-server run asserts AE2 cell registration, 63-slot Cell Workbench config inventories, two upgrade slots with FUZZY/INVERTER support and exact capacities for 1k/4k/16k/64k. It verifies FUZZY/INVERTER semantics, rejects CAPACITY, stores 70 distinct item types in one 1k DISK, validates cached item/type metadata, confirms all four recipes exist in the server RecipeManager with the correct outputs, and exercises the UUID/quarantine/write-side invariants. Nested-storage compatibility is also tested in both directions: an ExpansionAE DISK may store a healthy empty native AE2 item Storage Cell, but no ExpansionAE DISK may be nested in another ExpansionAE DISK or inside AE2's native `BasicCellInventory`. It also places temporary AE2 hosts in the overworld: an ME Drive must accept a pre-populated DISK and report it as `NOT_EMPTY`, while an ME Chest must expose the DISK contents through its item monitor. The Drive test round-trips tile NBT, invokes `onChunkUnloaded()`, recreates the host from persisted NBT, drops the DISK and reinserts it into a new Drive while preserving UUID and contents.

The pull-request workflow then runs two server phases against the same world. The `write` phase stages 321 items in a reserved test UUID and shuts the server down via RCON. The `read` phase starts a new server process, requires those 321 items to be recovered from `WorldSavedData`, extracts them and cleans up the test record. Both phases must reach the normal ready state and terminate cleanly.

Each phase also builds active AE2 grids with Creative Energy Cells and ME Drives. Two same-UUID aliases on one grid must contribute only one logical copy to the storage monitor; an alias on a second grid must see the same backing contents. Both the dynamic inventory view and the cached terminal list are checked before and after cross-grid mutations. Alias changes are propagated to other active grids once per grid so remote terminal caches cannot remain stale.

After the dedicated-server phases, pull requests launch a real Forge client under Xvfb. The client must pass resource/model loading, register and bake all four DISK inventory models and all four AE2 ME Drive cell models without resolving to the missing model, and validate the three-line tooltip contract for every tier (cached item count/capacity, cached type count and no-type-limit message). This catches client-only lifecycle failures that a dedicated server cannot see.

AE2 8.4.x fires `ModelRegistryEvent` before it announces `IAppEngApi` to addons through `@AEAddon#onAPIAvailable`. For that early model-loading lifecycle only, `ExpansionAEClient` uses `appeng.core.Api.instance()`, which AE2 itself documents as the exceptional access path for API use before addon announcement. Common/server integration continues to use `ExpansionAEApi` from `@AEAddon`.

A successful JAR build does not replace these runtime gates. The required baseline is green build/JUnit + dedicated-server write/read restart smoke + client Xvfb model/tooltip smoke on Forge 36.2.42 + AE2 8.4.7 + MCP `20210309-1.16.5`.

The workflow also uses per-event/ref concurrency with `cancel-in-progress` so obsolete push/PR runs do not consume runner capacity.

## DISK persistence model

The DISK vertical slice deliberately stores its full contents in overworld `WorldSavedData`, keyed by UUID. The ItemStack only carries the storage UUID and small cached metadata.

The UUID identifies the backing storage, not the physical ItemStack. Exact copies with the same UUID are aliases of one logical DISK. They must observe the same contents and must never duplicate those contents. Open alias inventories also refresh the small cached item/type counters on their ItemStacks, even when not attached to an AE2 IActionHost; active-grid notification remains restricted to hosted aliases. Within one active AE2 grid, only one host/slot for a UUID exposes the logical contents to the network; separate grids may each expose that same logical storage and receive cross-grid cache invalidation when it changes.

Once assigned, a UUID remains assigned even when the DISK becomes empty. Its empty backing record is preserved so existing aliases continue to resolve to the same logical storage.

Once an ItemStack has a DISK UUID, its backing record is mandatory, including for an empty DISK. If that record is missing, reads and writes fail closed instead of silently recreating or overwriting storage.


ExpansionAE DISKs are never nestable inside another ExpansionAE DISK, even when currently empty. Because contents live in a UUID-backed external store, an empty DISK can gain contents later through another alias after the nesting decision was made; forbidding all ExpansionAE→ExpansionAE nesting closes that time-of-check/time-of-use capacity bypass. Healthy empty native AE2 item Storage Cells remain nestable inside an ExpansionAE DISK, matching AE2's normal behavior for empty cells.

The reverse direction is blocked as well. AE2 8.4.x native `BasicCellInventory` only recognizes nested cells whose item implements `IStorageCell`; ExpansionAE intentionally cannot implement that interface because AE2's first `BasicCellHandler` would then intercept the item and replace the custom unlimited-type/UUID-backed inventory with the standard cell implementation. A minimal required Mixin therefore vetoes `DiskStorageCellItem` at the head of `BasicCellInventory.injectItems()`. Production discovers `expansionae.mixins.json` from the JAR manifest, while ForgeGradle userdev runs pass `--mixin.config expansionae.mixins.json` explicitly. The Mixin is `remap=false` because it targets an AE2-owned method rather than an obfuscated Minecraft member.

Persisted backing data distinguishes deterministic repair from ambiguous corruption. Derived `item_count` and zero-amount entries can be normalized, but missing/mismatched authoritative `keys`/`amounts`, negative amounts, overflow, invalid NBT types, duplicate UUID records, malformed UUID identity, and invalid root structure are preserved in quarantine and fail closed. The mutation API applies the structural invariants before data enters the in-memory map, so normal runtime code cannot manufacture a record that would only become invalid on the next reload.
