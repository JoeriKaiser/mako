# Pigeon Easter Egg Design

**Date:** 2026-07-03  
**Status:** Design approved, ready for implementation.

## Goal
Add a subtle, thematically appropriate easter egg to the Mako launcher: a small pixel-art pigeon that occasionally flies across the home screen.

## Constraints
- Must not interfere with normal launcher use (no touch interception, no battery drain, no extra services).
- Must match the existing pixel-art / SVG icon language used in bohio (`px_*.xml`).
- Must be self-contained and easy to disable or remove later.

## Trigger
- On every `MainActivity.onResume()`, perform a random roll.
- If the roll passes **and** at least **2 hours** have elapsed since the last sighting, show the pigeon.
- Cooldown state is persisted via `PrefsManager` (`lastPigeonTime`).
- The roll keeps appearances rare and surprising; the cooldown prevents streaks and guarantees the easter egg stays subtle.

## Visual
- Asset size: **24dp** square vector, hard-edged pixel paths like other `px_*.xml` icons.
- Two frames: `px_pigeon_1.xml` (wings level) and `px_pigeon_2.xml` (wings up).
- Tinted with `@color/foreground` so it adapts to the active theme automatically.
- The two-frame flap animation is what makes it read as a flying bird despite the small size.

## Animation
- Pigeon enters from one screen edge and exits the opposite edge.
- Path: shallow sine-wave arc, with a small bob to suggest flight.
- Duration: **3–5 seconds**.
- Frame swap: `AnimatorSet` alternates between the two vector frames at a regular rate during the flight.
- Once the animation ends, the pigeon view is hidden until the next trigger.

## Implementation
1. Add a transparent `PigeonEasterEggView` to the `MainActivity` layout (or as an overlay child), with `clickable="false"` and `focusable="false"`.
2. Add `px_pigeon_1.xml` and `px_pigeon_2.xml` under `app/src/main/res/drawable`.
3. Extend `PrefsManager` to store `lastPigeonTime` and expose helper methods for the cooldown check.
4. In `MainActivity.onResume()`, check cooldown and random roll; if both pass, call `PigeonEasterEggView.fly()`.
5. `fly()` builds an `AnimatorSet` that translates the pigeon across the screen while swapping frames.

## Open Questions (none at design time)
- None. All key decisions (placement, trigger, visual style, animation) were validated during the design session.
