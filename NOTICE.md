# Credits and source provenance

ExpansionAE is an unofficial development backport, not a release by the original authors.

- ExtendedAE: GlodBlock and contributors; textures/models credited upstream to Sea_Kerman. Source branch `1.20.1-forge`, commit `b002b849614188db7cd3fe08839364c64b6ea890`, version `1.20-1.4.20-forge`. LGPL-3.0.
- AdvancedAE: Pedroksl and contributors. Reference branch `forge/1.20.1`, commit `3137d63403dd35211c886744f094886b6a81e76a`, version `1.3.6-1.20.1`. Upstream Gradle metadata declares LGPL-3.0. Provider and encoder textures are reused; ingredient face routing is reimplemented against the AE2 8.4 item crafting API. Full feature parity is pending.
- Applied Energistics 2: AlgorithmX2, TeamAppliedEnergistics and contributors. Runtime dependency `8.4.7`; adapted interface implementation from branch `8.4.x-1.16.x`, commit `b45c590639a0ed0e6dd2cd60a3263b4546cd53c5`. LGPL-3.0-or-later; API files carry their own MIT notices.

The ExpandedDuality, ExpandedCraftingTracker, ExpandedInterfaceTile, ExpandedInterfaceBlock, ExpandedContainer and ExpandedScreen classes derive from AE2 and retain original copyright notices. Modified 2026-09-18 for configurable inventory sizes, registry separation and custom layouts. ExtendedAE and AdvancedAE texture files are copied without modification and their model references are adapted to the ExpansionAE namespace.

Full LGPL and GPL license text is included in LICENSE. New source is distributed under LGPL-3.0-or-later. This project does not bundle Minecraft, Forge or the AE2 binary.
