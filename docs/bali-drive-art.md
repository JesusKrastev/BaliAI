# Bali Drive actor art

Seven transparent, lossless WebP textures in `app/src/main/res/drawable-nodpi/` draw the actors of Bali Drive: player car, traffic car, ambulance, adult pedestrian, child, scooter rider and cyclist. The game and the animated catalogue cover share them through `rememberDriveArt()`, which loads them once per composition, never inside the frame loop.

## Art direction

One isolated orthographic overhead sprite per subject, in Bali's friendly style: rounded cartoon shapes, clean slate outlines, glossy highlights, saturated Bali colours, transparent background, no text, no ground or cast shadow. Vehicles and riders face up; pedestrians face right and the renderer turns them when they walk left.

- `drive_player`: orange `#FF6B35` hatchback with an open roof for Bali.
- `drive_traffic`: neutral pearl hatchback, tinted at runtime so each traffic car has its own colour.
- `drive_ambulance`: white and orange, blue Star of Life, light bar.
- `drive_pedestrian`, `drive_child`: blue-shirt adult and orange-shirt running child.
- `drive_scooter`, `drive_bicycle`: orange-helmet, teal-shirt riders.

## Processing

Crop the transparent padding and downscale to at most 384×512 (the child: 384×384), keeping the alpha channel. Car sprites are drawn at `CAR_WIDTH × CAR_LENGTH`, so they match the engine's collision boxes.

Everything that carries gameplay stays in the renderer and the engine, not in the art: shadows, brake lights, the siren flash, walker sway, rider wobble, the scooter's 1.5 m safety zone and the ambulance warning.
