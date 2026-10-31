# Nightfall Client — Upgrade build

Fabric client mod for Minecraft Java **1.21.11** / **Java 21**.

## Implemented in this update

- **ESP tracers originate at the screen centre / crosshair**, not at the player's feet. Targets outside the current view can produce directional edge arrows and a centre-to-edge guide.
- **Block Highlight** draws a white projected 3D wireframe around the block under the crosshair. Its corners are interpolated between frames for a smoother transition while looking around. It does not draw a tracer to the block.
- **Sus-Chunk world overlay** projects the analysed chunk's ground surface into the 3D view and draws a translucent coloured chunk polygon with a brighter border and label. Green/yellow/orange/red still represent the existing analysis score bands. Normal/low-score chunks are limited by the configurable `Overlay Range` to avoid filling the whole view.
- **Chunk Explorer** now has a persistent details pane; click a chunk to pin its coordinates, score, evidence, coverage and analysis notes.
- **RTP / teleport cleanup:** with `OG Base Finder → Auto Clear RTP` enabled, a horizontal displacement greater than the configured threshold (default 128 blocks) clears stored results, scan age and notification-deduplication state. Dimension changes clear them too.
- **Item ESP** supports boxes, names/counts, crosshair-origin tracers and range. Optional module notifications are available for Item ESP, Player ESP, Entity Intelligence, Storage ESP and Spawner ESP.
- **Central Notifier** offers enable/disable, sound, duration and cooldown. The Base Finder and the ESP modules can each opt into notifications.
- **Slow Swing** slows the local player's hand-swing animation ticker. It does **not** change attack cooldown, hit registration or server-side combat timing.
- **Staff List:** names are deliberately user-maintained in `config/nightfall-staff.txt` (one exact username per line). Only players currently present in the server tab list are checked against that list.
- **AntiVanish diagnostic:** can flag a nearby player entity that is visible in the world but absent from the tab list. This is only a tab/world discrepancy warning, **not proof of vanish** and not a way to reveal players that the server never sends to the client.
- **Resource pack bundled in the mod JAR:** the CS-knife pack is registered as a built-in pack named `Nightfall CS Knife`. Enable it in Minecraft's Resource Packs screen. Fabric's built-in pack registration makes it selectable; it is not silently forced on.
- HUD watermark, coordinates and active-module list are right-aligned. The ClickGUI canvas and Chunk Explorer details layout have been enlarged and cleaned up with the purple palette.

## Important limitations / not completed in this pass

- Base analysis remains a **heuristic over client-loaded chunks only**. The score is not a probability, and no client can inspect chunks/blocks that the server has not sent.
- Staff identification is manual-list based because a player name alone is not evidence of staff status.
- AntiVanish cannot infer players hidden from both the entity stream and tab list. Servers differ in how they expose tab, entity and vanish information.
- A full custom dark-steel sky renderer is **not included yet**. A normal vanilla resource pack does not provide a universal custom sky-rendering hook; this requires a separate render-pipeline implementation and visual testing. No non-functional Sky toggle is shown in the GUI.
- The white block outline is rendered as a projected wireframe in the HUD pass. It is visually smoothed but is not an opaque world-depth geometry replacement.
- This source update has **not been compile-tested** in this environment: Java 21 is present, but Gradle is not installed and the environment cannot resolve external hosts to download it. Run the GitHub Actions workflow or `gradle --no-daemon clean build` with network access before installing the JAR.

## Build

From the project root:

```sh
gradle --no-daemon clean build
```

The JAR is written to `build/libs/`. The workflow in `.github/workflows/build.yml` builds with Java 21 and uploads `build/libs/*.jar` as an artifact.
