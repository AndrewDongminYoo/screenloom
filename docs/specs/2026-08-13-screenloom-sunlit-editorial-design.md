# Screenloom Sunlit Editorial Design

## Status

Approved by the operator on 2026-08-13 after reviewing visual comparisons for the overall direction, visual system, application screens, and exported poster system.

This document supersedes only the visual direction and presentation details in [Screenloom Design](./2026-08-12-screenloom-design.md).
The existing product scope, offline architecture, privacy boundary, editor behavior, picker flows, export contract, and non-goals remain authoritative.

## Summary

Screenloom will move from a near-black editorial workspace and uniformly dark poster presets to a bright, warm, high-contrast design called **Sunlit Editorial**.
The application will feel like a small design studio built from warm paper, deep ink typography, and crossing coral and cobalt threads.
The exported posters will use the same visual language so the application interface does not promise a brightness or polish that the final PNG fails to deliver.

The redesign is presentation-focused.
It does not introduce a new dependency, navigation structure, editor state, layout mode, storage behavior, permission, or network capability.

## Problem

The current application shell is dominated by a near-black background, dark gray control surfaces, low-opacity decorative lines, and restrained accent areas.
The six poster presets also begin from dark colors and use fixed warm-white copy, so choosing another preset changes hue without substantially changing perceived brightness.
The result is functionally correct but visually heavy in the empty state, editor, preview, and reopened 1080 by 1920 PNG.

The redesign must solve both halves of that problem together:

1. Make the application workspace feel bright, welcoming, and purpose-built without becoming a generic white Material interface.
2. Make the exported posters visibly brighter and more energetic while keeping imported screenshots legible and dominant.

## Design Direction

### Concept

**Warm paper, electric threads.**

Screenloom uses a warm paper canvas as its dominant field.
Coral communicates primary action, cobalt communicates selection, and deep ink anchors text and device frames.
Broad translucent ribbons cross the composition like woven threads, giving the product a recognizable motif that relates directly to the Screenloom name.

The design is editorial rather than toy-like.
Brightness comes from a high-key canvas, saturated accents, explicit color roles, and generous spacing instead of adding decoration to every component.

### Differentiator

The memorable element is the pair of broad crossing color ribbons.
They appear quietly in the application background and preview stage, then become a stronger compositional device in exported posters.
The motif connects the editing experience to the result without obscuring controls or imported images.

## Visual System

### Application Color Roles

| Role           | Value     | Use                                                       |
| -------------- | --------- | --------------------------------------------------------- |
| Paper          | `#FFF8E9` | Application canvas and warm neutral fields                |
| Elevated paper | `#FFFFFF` | Control sheets, cards, and text fields                    |
| Ink            | `#18213D` | Primary copy, device frames, and strong selected surfaces |
| Muted ink      | `#667087` | Supporting copy and metadata                              |
| Coral          | `#FF6B4A` | `Choose screenshots`, `Export PNG`, and progress emphasis |
| Cobalt         | `#566EFF` | Selected options, switches, and focus indication          |
| Sun            | `#FFD466` | Small highlights and warm ribbon fields                   |
| Mint           | `#6BD7B3` | Supporting palette accents                                |
| Outline        | `#E6DCCB` | Quiet borders on light surfaces                           |

Coral is reserved for actions that start or finish the core workflow.
Cobalt is reserved for selection and state.
Keeping those roles separate makes the hierarchy understandable without relying on text labels alone.

### Typography

Display headlines use the platform generic serif family at a bold weight, tight tracking, and compact line height.
Body copy, controls, metadata, and the wordmark use the existing platform sans family.
Uppercase metadata remains small and widely tracked, but its color and contrast increase against the light canvas.

No downloadable or bundled font dependency will be added for this redesign.
The Compose preview and Android bitmap renderer will both use their platform generic serif mapping for poster headlines and their platform sans mapping for supporting copy.

### Surfaces and Depth

Control surfaces use white or warm-paper fills with quiet outlines.
Shadows use a low-opacity ink tint instead of pure black so cards remain distinct without making the light theme look muddy.
The preview sits inside a dedicated light stage rather than floating directly on the application background.

Corners remain rounded, but the hierarchy is simplified to three main radii: compact controls, cards and sheets, and poster or hero containers.
Interactive targets remain at least 48 dp.

### Motion

The existing spring-based poster placement transition remains the primary motion moment.
Control-tab content keeps its short fade transition.
The color ribbons are static and no autonomous decorative animation is added.
System animator-duration settings continue to govern Compose transitions.

## Application Screens

### Empty State

The empty state opens on the warm paper canvas with quiet cobalt and coral ribbons behind the content.
The Screenloom wordmark and `01 / POSTER` metadata remain at the top.
A bright sample poster is the visual center and shows the approved Paper palette, serif headline, and two sample device frames.

The headline and explanation follow the sample poster.
`Choose screenshots` is the only filled coral action and includes a forward cue.
The privacy note remains visible below the action in muted ink.

This screen stays vertically scrollable so it remains usable with enlarged text and shorter windows.

### Editor Preview Stage

The editor keeps the existing top-to-bottom workflow and single scroll container.
The header continues to show the wordmark, loaded-frame count, and 1080 by 1920 output size.
The poster preview sits inside a light stage with quiet ribbons and a restrained tinted shadow, making palette colors readable against a neutral surround.

The preview keeps its current 9:16 aspect ratio and accessibility description.
Image-placement animation and imported-image memoization remain unchanged.

### Control Sheet

The `Layout`, `Copy`, and `Style` tabs remain in their current fixed order.
They move visually into one elevated white sheet with a light neutral segmented-track background.
The selected tab uses an ink fill, while selected options use a warm fill with a cobalt outline.

Layout choices, copy fields, palette choices, shadow choices, and the device-frame switch keep their current actions, state ownership, disabled behavior, semantics, and minimum target sizes.
The palette controls show the revised colors and display names.

### Actions and Feedback

`Replace` and `Reset` remain equal-width outlined secondary actions.
`Export PNG` remains the full-width terminal action and uses the coral fill.
Import and export progress remain inline within their corresponding primary buttons.

Snackbar behavior and copy remain unchanged except for colors supplied by the light theme.
When importing or exporting locks the editor, disabled controls remain visible with readable reduced emphasis rather than disappearing.

### System Surfaces

The Android launch background, status bar, and navigation bar use the warm paper family.
System bars use dark icons against the light background.
Edge-to-edge safe drawing behavior remains unchanged.

## Exported Poster System

### Shared Composition

Every poster combines four layers:

1. A bright or high-chroma base gradient with a text-safe top-left copy zone.
2. A large translucent sun field that adds scale without becoming the focal point.
3. Two broad crossing ribbons placed behind the screenshots.
4. The existing screenshot placements, device frames, title, and subtitle.

The ribbons deliberately occupy otherwise empty parts of aspect-fitted layouts.
They must remain behind imported screenshots and outside the title's critical reading area.

### Palette Presets

The `PaletteId` enum values remain unchanged so reducer state and public behavior do not migrate.
Three user-facing labels change to describe their new appearance: `Ink` becomes `Paper`, `Moss` becomes `Mint`, and `Violet` becomes `Iris`.

| Palette ID | Display name | Base gradient          | Copy                             | Threads and sun                 |
| ---------- | ------------ | ---------------------- | -------------------------------- | ------------------------------- |
| `Ink`      | Paper        | `#FFF8E9` to `#FFD9A2` | Ink                              | Cobalt, coral, and sun          |
| `Cobalt`   | Cobalt       | `#3557F0` to `#78DBEF` | Warm white in the dark copy zone | Sun, coral, and warm white      |
| `Coral`    | Coral        | `#FF765C` to `#FFC46D` | Ink                              | Warm white, cobalt, and cream   |
| `Moss`     | Mint         | `#6BD7B3` to `#D8EF6A` | Ink                              | Ink tint, coral, and warm white |
| `Violet`   | Iris         | `#5D50D8` to `#F3A1C7` | Warm white in the dark copy zone | Sun and warm white              |
| `Sunrise`  | Sunrise      | `#FFE26C` to `#FF7C56` | Ink                              | Cobalt and warm white           |

Each palette explicitly supplies start color, end color, headline color, supporting-copy color, frame color, shadow tint, two ribbon colors, and sun color.
The Compose preview and bitmap renderer consume the same palette values and shared geometry constants.

### Poster Copy

Poster headlines use the generic serif family in both preview and export.
They remain limited to two lines and continue to ellipsize at the current title boundary.
Subtitles remain limited to two lines and use the matching palette's supporting-copy color instead of a single fixed translucent white.

The existing copy positions and length limits remain unchanged unless concrete emulator evidence shows clipping after the typeface change.
Any metric adjustment must be made in both renderers and covered by tests.

### Screenshot Layouts

`Focus`, `Stack`, and `Split` retain their current placement geometry, rotations, draw order, and one-image fallbacks.
Aspect fitting remains authoritative so the imported screenshot is not cropped to fill a template box.
The redesign does not add a new layout or modify editor state.

Device frames use each palette's explicit frame color.
Shadows use the same frame hue at a reduced alpha across the existing layered-shadow algorithm.
This keeps depth visible without adding a black haze to bright posters.

### Preview and Export Parity

Compose and Android Canvas require separate drawing operations, but they must share palette data, geometry constants, layer order, and text intent.
Every new ribbon, sun field, frame color, and shadow tint must be implemented in both paths in the same change.
Preview-only decoration or export-only correction is not acceptable.

## Architecture and Data Flow

The existing editor state and action flow remains unchanged:

```plaintext
EditorDocument
  -> shared PosterPalette and PosterLayout values
  -> Compose PosterPreview
  -> Android Canvas PosterRenderer
  -> 1080 x 1920 PNG
```

`ScreenloomTheme` owns application-level colors, typography, and shapes.
`EditorScreen` owns the background, empty state, preview stage, and action hierarchy.
`EditorControls` owns the tab sheet and control presentation.
The existing `PosterPalette` model is extended with explicit visual-role colors instead of introducing a second design-system abstraction.

No dependency manifest or lockfile change is expected.
No persistence, URI, ViewModel, reducer, exporter I/O, permission, or manifest behavior changes are required.

## Error Handling and Accessibility

Picker cancellation, decode failure, export cancellation, and export failure retain their current no-data-loss behavior.
The redesign changes only presentation of feedback, not when messages are emitted or consumed.

All normal-size text and interactive content must meet a contrast ratio of at least 4.5 to 1 against its immediate background.
Selected state must remain exposed through semantics and visible without color alone through fill, border, or shape changes.
The disabled `Split` option must retain its explanatory state description.
The poster preview retains one concise content description rather than exposing decorative layers.

## Implementation Surface

The expected implementation is intentionally limited to the presentation and renderer boundaries:

- `app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt`
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt`
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt`
- `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt`
- `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- Directly related unit and instrumented test files
- The cross-reference in `docs/specs/2026-08-12-screenloom-design.md`

The implementation must not refactor editor state, storage, exporter I/O, picker registration, or unrelated application code.

## Verification

### Automated Gates

Run heavy Android commands sequentially with the documented SDK root:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
```

Tests must cover the expanded palette roles, light-theme contrast, existing selected and disabled semantics, renderer dimensions, layout behavior, and copy truncation.
Existing import, cancellation, export, and state-restoration tests must remain green.

### Manual Visual Gate

Visual approval requires looking at actual rendered states on the documented emulator.
Passing screenshot dimensions or pixel-change checks alone is insufficient.

Capture and inspect at minimum:

1. Empty state with sample poster and privacy note.
2. Editor `Layout`, `Copy`, and `Style` tabs.
3. `Focus`, `Stack`, and `Split` previews with representative screenshots.
4. All six revised palette presets.
5. Selected, disabled, importing, and exporting states.
6. Reopened exported PNGs for at least Paper, Cobalt, and Iris.
7. Preview and export side by side for matching copy, colors, ribbons, frames, shadows, and screenshot placement.
8. Portrait and rotated application state with legible system bars and preserved composition.

The exported file must still reopen as a PNG of exactly 1080 by 1920 pixels.
The merged-manifest permission allowlist must remain unchanged through `lintDebug`.

## Risks and Mitigations

### Preview and Bitmap Drift

Risk: Compose and Android Canvas render gradients, type metrics, and translucent shapes differently.
Mitigation: share all values and geometry, add deterministic assertions where practical, and compare real preview and exported captures before approval.

### Light Palette Contrast

Risk: white copy can become unreadable on the light side of Cobalt or Iris gradients.
Mitigation: keep the title in the explicitly dark top-left copy zone, use palette-specific foreground values, and verify real rendered copy rather than sampling only one nominal color.

### Bright Shell Without Product Character

Risk: replacing dark surfaces with plain white Material defaults would solve brightness but erase Screenloom's identity.
Mitigation: retain the warm paper canvas, fixed coral and cobalt roles, serif display voice, and woven-ribbon motif across both application and output.

### Scope Expansion

Risk: a broad visual request can invite state refactors, new layouts, font packages, or renderer abstractions.
Mitigation: keep the change within the listed presentation files and existing palette model, and flag any required expansion before acting.

## Non-Goals

- A dark and light theme toggle.
- User-authored custom colors, gradients, fonts, or canvas sizes.
- New poster layouts or freeform positioning.
- Navigation, projects, saved drafts, or history.
- Changes to import, export, privacy, permissions, monetization, signing, or publication.
- A new dependency or standalone design-system module.

## Acceptance Criteria

1. The empty state and editor use the approved Sunlit Editorial visual system and no longer read as a near-black workspace.
2. Coral, cobalt, ink, and paper retain their approved semantic roles throughout the interface.
3. The preview stage, control sheet, secondary actions, and primary export action match the approved hierarchy.
4. All six poster presets use the approved bright or high-chroma palette system and their accurate display names.
5. Preview and exported PNG share the same palette colors, ribbons, sun field, typography intent, frame color, shadow tint, and screenshot placement.
6. `Focus`, `Stack`, `Split`, aspect fitting, copy limits, frame toggle, shadow levels, picker flows, reset, state restoration, and export behavior remain functionally unchanged.
7. Light application surfaces and poster copy meet the defined contrast and semantics requirements.
8. Android system bars remain legible against the light shell.
9. Unit tests, lint, debug assembly, instrumented tests, and the manual emulator flow pass without bypassing hooks or checks.
10. A reopened export is visually approved and remains an exact 1080 by 1920 PNG.
