# Bali Drive actor art

Seven transparent, lossless WebP textures in `app/src/main/res/drawable-nodpi/` replace the procedural actor drawings: player car, traffic car, ambulance, adult pedestrian, child, scooter rider and cyclist. The game and animated catalogue cover share these textures through `rememberDriveArt()`.

## Art direction and generation

Generated with the built-in image generation tool, using the existing `happy_bali.png` mascot as a style reference only. Shared prompt: one isolated orthographic overhead sprite, friendly rounded cartoon shapes, clean slate outlines, glossy highlights, saturated Bali colors, transparent background, no text, ground or cast shadow. Forward direction is up for vehicles and riders; pedestrians face right and turn in the renderer when travelling left.

Subjects: orange #FF6B35 hatchback with an open roof for Bali; neutral pearl traffic hatchback for runtime tinting; white/orange ambulance with a blue Star of Life and lightbar; blue-shirt adult pedestrian; orange-shirt child with running pose; orange-helmet/teal-shirt electric scooter rider; orange-helmet/teal-shirt cyclist.

Production processing only crops transparent padding and downsizes to at most 384×512 (child: 384×384), preserving alpha. Car dimensions remain `CAR_WIDTH × CAR_LENGTH`. Shadows, braking lights, siren animation, walker sway, rider wobble, safety margins, warnings and gameplay remain renderer/engine driven. Images are decoded by Compose resources, never inside the frame loop. Existing engine invariants have not been changed or weakened.

## Validation

- Local `./gradlew test` could not start: downloading Gradle 8.13 fails with `Network is unreachable`. This cloud workspace has no Android SDK, ADB/device or Windows Brain vault.
- The proposed PR Checks change records `BaliDriveScreenshotTest` and uploads `bali-drive-screenshots` on this feature branch for real Android/Robolectric visual review. Upload was blocked: local git has no push credentials and the GitHub connector rejected blob writes with HTTP 403. The new commits have therefore not run in CI; only the unchanged base 4e19d59 was confirmed green.
- A physical-device playtest and Brain write-back remain pending. No merge, Play upload, engine, database schema or landing repository change is included.

## Pending Brain write-back (apply using vault protocol on the machine that owns it)

Read `00-INDEX.md`, `01-Proyectos/BaliAI/BaliAI.md`, and the BaliAI entries in `03-Sync/SYNC-PENDIENTES.md` first; locate the matching contract with grep before editing.

- `BaliAI-estado.md`: 2026-10-05 — Bali Drive now uses seven coherent transparent actor sprites in the game and animated cover; Android validation is in PR #48, local/mobile checks pending.
- `BaliAI-decisiones.md`: actor art follows Bali's rounded glossy style, uses cached lossless WebP resources, and preserves procedural safety cues and engine collision dimensions.
- Matching `02-Contratos/` game contract: describe the new matching car/person/rider sprites; controls and rules unchanged.
- `03-Sync/SYNC-PENDIENTES.md`: BaliAI → BaliAIPage: refresh Bali Drive visuals/screenshots after PR #48 is approved; do not publish unmerged feature screenshots as released functionality.

The earlier engine decisions/error from 4e19d59 must be checked against existing Brain entries before logging again; this session did not modify those rules.
