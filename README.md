# The Impossible Desk Drawer

An illustrated cabinet of impossible objects, driven primarily by Kotlin.

The original shape-based prototype has been replaced by six individually illustrated artifacts and a horizontal, immersive cabinet experience. Explore each object, record its reaction, and unlock the paper door at the drawer's impossible bottom.

## Live experience

[Open the Impossible Desk Drawer](https://jean-tmk.github.io/impossible-desk-drawer/)

## Interaction

- Open the aquatic drawer.
- Drag sideways or use the arrow controls to travel through the cabinet.
- Open each illustrated artifact and activate its unique observation.
- Record all six reactions to unlock the paper door.
- Use Escape to close a record and arrow keys to move through the cabinet.

## Architecture

- **Kotlin/JS** is the primary authored language and owns the catalogue model, state machine, progression, reactions, audio cues, drag controls, keyboard navigation, and rendering.
- `runtime.js` is the generated browser bundle.
- Six individually generated, optimized WebP illustrations replace CSS-drawn object shapes.
- The static export is deployed through GitHub Pages.

No frameworks or API keys are required to visit the experience.
