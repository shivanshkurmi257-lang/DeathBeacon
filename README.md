# DeathBeacon

A client-side Fabric mod for Minecraft 1.21.11: automatic death waypoints,
custom waypoints, beacon-style beams, a top-screen locator compass, and a
Xaero-inspired waypoint manager GUI.

## Building

1. Requires JDK 21 and a network connection (Loom downloads Minecraft,
   mappings, and dependencies on first run).
2. `./gradlew build` - output jar lands in `build/libs/`.
3. This project was written and organized in a sandbox **without internet
   access**, so it has not been compiled or run against the real 1.21.11
   Fabric API jars. Treat the first build as the real correctness check -
   see "Known risk areas" below for where a mismatch is most likely.

## Feature coverage

| Feature | Status |
|---|---|
| Automatic death tracking | Implemented (`DeathBeaconMod`, hooks the vanilla death screen) |
| Custom waypoints | Implemented (`WaypointManager`, `EditWaypointScreen`) |
| Beam rendering | Implemented (`BeamRenderer`, `DeathBeaconRenderLayers`) |
| Death beams | Implemented (shares beam renderer, separate color/config) |
| Locator HUD (compass) | Implemented (`LocatorHud`) |
| Waypoint list HUD | Implemented (`WaypointListHud`) |
| Main GUI (tabs/search/list) | Implemented (`WaypointListScreen`) |
| Add waypoint screen | Implemented (`EditWaypointScreen`) - simplified color picker, see below |
| Edit waypoint screen | Implemented (same class as Add) |
| Settings GUI | Implemented via Cloth Config (`DeathBeaconConfig`) |
| Favorites | Implemented (flag on `Waypoint`, dedicated tab) |
| Global waypoints | Implemented (`global.json`, separate tab) |
| Commands (`/db ...`) | Implemented (`DeathBeaconCommand`) |
| Keybinds | Implemented (`KeyBindings`) |
| Storage (JSON, per-world) | Implemented (`WaypointManager`) |
| Mod Menu integration | Implemented (`ModMenuIntegrationImpl`) |

## Known risk areas (read before filing "it doesn't compile")

1.21.11 is an extremely recent Minecraft release (the last Yarn-mapped one
before Mojang's unobfuscation switch), so parts of the client rendering API
in particular are the least certain pieces here:

- **`BeamRenderer` / `DeathBeaconRenderLayers`**: the `VertexConsumer`
  chain (`buffer.vertex(...).color(...)`) and `RenderLayer.of(...)` builder
  signature drift between MC versions almost every release. If this doesn't
  compile as-is, open `RenderLayer` in your IDE (via the Yarn-mapped sources
  Loom downloads) and match the current builder shape.
- **`WorldRenderEvents.AFTER_ENTITIES`**: double check this constant exists
  in the Fabric API version pinned in `gradle.properties` - `AFTER_TRANSLUCENT`
  and `LAST` are the safer bets if it's missing.
- **Cloth Config / Mod Menu versions** in `gradle.properties` are
  placeholders - verify real compatible builds for 1.21.11 on Modrinth before
  building.
- **Death detection**: there's no dedicated "player died" client event in
  Fabric API, so this hooks the moment `DeathScreen` opens. This is the
  standard approach other client-side death-tracking mods use, but it means
  a death is only recorded once the death screen actually renders.

## Simplifications vs. the reference screenshots

- The Add/Edit color picker is a hex/RGB text field + swatch + Random/Reset
  buttons, not the full draggable HSV gradient + hue-strip widget shown in
  the screenshots. That would need a bespoke OpenGL gradient-quad widget;
  functionally equivalent, visually simpler.
- GUI panels use flat vanilla-style fills/borders rather than Xaero's exact
  texture assets (which are that mod's own copyrighted art).

## Config files (`.minecraft/config/deathbeacon/`)

- `waypoints.json`, `deaths.json`, `global.json` - waypoint data, written by
  `WaypointManager` with automatic `.bak` backups on every save.
- `deathbeacon.json` - settings, written by Cloth Config's AutoConfig.
