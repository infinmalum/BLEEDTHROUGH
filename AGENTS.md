# BLEEDTHROUGH Repository Rules

## Platform

- Target Minecraft 1.20.1, Forge 47.4.22, and Java 17.
- Maintain Forge only until the core is stable; do not introduce a multiloader abstraction preemptively.
- Run Gradle with JDK 17. Store dependencies and build caches in a persistent user-level global directory or this workspace, never in a system temporary directory.

## Architecture

- Publish the core gameplay as one `Meatscape Core` JAR with strong internal module boundaries.
- Keep world state and storage below gameplay, progression, and client presentation layers.
- Never reference client-only classes from common or dedicated-server code.
- Keep Create, Immersive Engineering, TaCZ, Refurbished Furniture, and Backpacked optional. Core world loading and migrations must not reference their types.
- Prefer tags, datapacks, and data generation to hard-coded content rules.
- Avoid Mixins unless a documented Forge extension point cannot satisfy the requirement.

## World safety and performance

- Never implement infection with per-block random ticks.
- Existing chunks evolve only through a budgeted scheduler.
- Treat BlockEntities and protected settlements as non-destructive targets.
- Version persistent data from its first implementation and include migration tests.
- Do not retain strong references to unloaded chunks, entities, or levels.

## Assets and validation

- Use GeckoLib only for entities or BlockEntities that genuinely require skeletal animation.
- Keep editable Blockbench sources under `assets/source/blockbench/`; exports do not replace source assets.
- AI-generated assets require Blockbench viewport and in-game visual review.
- Complete features as small vertical loops: code, data, assets, automated tests, client verification, and dedicated-server verification.
- Do not batch-generate unverified blocks, entities, or content variants.
