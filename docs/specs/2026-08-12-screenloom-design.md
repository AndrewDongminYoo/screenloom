# Screenloom Design

## Summary

Screenloom is an offline native Android utility that turns ordinary app screenshots into polished promotional posters for store listings and social media.
It targets independent app developers and creators who need credible visual assets without opening a full design tool.
The first release is a complete paid-download product with no accounts, ads, analytics, network access, subscriptions, or in-app purchases.

## Goals

- Import one or two screenshots through the Android system photo picker without requesting broad media-library access.
- Produce a professional 9:16 poster through a short, obvious editing flow.
- Make every visual adjustment visible immediately in the preview.
- Export a 1080 by 1920 PNG through the Android system document picker.
- Deliver a privacy-respecting tool that works entirely offline.
- Keep the implementation small enough to polish and verify as a shippable first release.

## Non-Goals

- Accounts, cloud sync, collaboration, analytics, or remote configuration.
- General-purpose image editing, freeform layers, drawing, filters, or arbitrary canvas sizes.
- Store-listing automation or direct upload to Google Play Console.
- Subscription billing, feature paywalls, ads, or a simulated purchase flow.
- Tablet-specific editing layouts in the first release.

## Product Experience

### Empty State

The app opens on a dark, editorial workspace with the Screenloom wordmark, a short explanation, and one primary action labeled `Choose screenshots`.
A sample poster behind the action communicates the result before the user imports anything.
The system photo picker accepts one or two images.

### Editor

After import, the poster preview occupies the upper portion of the screen and a compact control panel occupies the lower portion.
The editor offers three tabs in a fixed order:

1. `Layout` selects `Focus`, `Stack`, or `Split` and toggles the neutral device frame.
2. `Copy` edits a title of at most 60 characters and a subtitle of at most 100 characters.
3. `Style` selects one of six curated gradients and adjusts shadow intensity between three named levels.

`Focus` presents one centered screenshot.
`Stack` overlaps two screenshots and uses the first screenshot alone when only one is available.
`Split` places two screenshots side by side and remains visible but disabled until two images are selected.
The editor includes `Replace`, `Reset`, and a prominent `Export PNG` action.

### Export

Tapping `Export PNG` opens the system document picker with a suggested filename based on the current title or `screenloom-poster.png` when the title is empty.
The app renders an exact 1080 by 1920 PNG and writes it to the selected URI.
A successful export shows a confirmation snackbar.
A failed or cancelled export leaves the editor unchanged and never discards the current composition.

## Visual Direction

The interface uses a near-black ink background, warm off-white typography, cobalt and coral accent colors, rounded glass-like control surfaces, and restrained motion.
The poster presets use high-contrast gradients that remain legible behind both light and dark screenshots.
Transitions use short fades and spring-based position changes, while export shows a subtle progress treatment instead of a blocking dialog.
The app avoids decorative effects that obscure controls or make the exported result look like a template demo.

## Accessibility

- All actions and selectable presets have semantic labels and visible selected states.
- Interactive targets are at least 48 dp.
- Text and controls meet Material contrast guidance against their surfaces.
- The poster preview exposes a concise description instead of reading decorative layers individually.
- Motion respects the system animator-duration setting.

## Architecture

The app uses Kotlin, Jetpack Compose, Material 3, a single Activity, and one app module.
It does not use dependency injection, a database, a navigation framework, or a network client.
The implementation plan must record the locally available Android SDK and toolchain versions before scaffolding and select versions that satisfy the current local and Play build requirements.

The main units are:

- `MainActivity` owns the Compose host and system picker launchers.
- `EditorViewModel` owns the immutable `EditorState` and validates user actions.
- `PosterSpec` is the renderer input containing image references, layout, copy, palette, frame, and shadow settings.
- `PosterLayout` contains shared, deterministic geometry used by preview and export rendering.
- `PosterPreview` draws the interactive Compose preview from `PosterSpec` and `PosterLayout`.
- `PosterExporter` renders the same specification to a 1080 by 1920 bitmap off the main thread and writes PNG data to the chosen URI.
- `ImageDecoder` loads bounded preview and export bitmaps from selected content URIs without retaining broader media access.

UI components depend on the state and action interfaces rather than on Android storage APIs.
The exporter and decoder are passed into the ViewModel through a small manual factory so their behavior can be replaced in tests without adding a dependency-injection framework.

## Data Flow

```plaintext
System photo picker
  -> selected content URIs
  -> bounded image decode
  -> EditorState and PosterSpec
  -> shared PosterLayout geometry
  -> live Compose preview
  -> system document URI
  -> background PNG render and write
```

The selected images and edits survive configuration changes through the ViewModel for the current process.
The first release does not reopen unfinished compositions after the process is killed, so it stores no user image references or project database.

## Error Handling

- An unreadable or unsupported image shows a snackbar and is excluded without clearing other valid selections.
- Images are decoded to bounded dimensions before preview or export work to prevent oversized source files from exhausting memory.
- A missing second image disables `Split` and explains the requirement in the control label.
- Export rendering and file I/O run outside the main thread and report failures as recoverable snackbars.
- Cancelling either system picker is a no-op.
- The current composition remains intact after every recoverable failure.

## Privacy and Permissions

Screenloom declares no camera, microphone, location, contacts, storage, advertising, or network permissions.
It receives only the content URIs explicitly selected through the system photo picker and writes only to the URI selected through the system document picker.
It contains no telemetry SDK and sends no user data off the device.

## Monetization

The initial commercial model is a one-time paid Google Play download with every feature enabled.
This avoids a billing dependency, an untestable purchase flow before Play Console configuration exists, and a degraded gift build.
Price, store listing, signing, Play Console setup, and publishing are separate release tasks and are not part of this implementation.
If validated demand later justifies a free trial, that change requires a separate product decision and a real Play Billing integration rather than a local feature flag disguised as payment.

## Testing and Verification

Automated verification covers:

- Layout geometry for all templates with one and two source images.
- Title and subtitle length normalization.
- `Split` availability and state transitions when image count changes.
- Export dimensions, PNG encoding, cancellation, and write failures.
- Compose semantics for core actions and disabled states.

Manual emulator verification covers:

- Launching from a clean install.
- Importing one image and then replacing it with two images.
- Editing every control and confirming immediate preview updates.
- Exporting a PNG, reopening it, and confirming 1080 by 1920 dimensions.
- Cancelling import and export without losing the composition.
- Rotating the emulator and confirming current-process state survives.

The implementation is complete only when the project unit tests, Android lint, debug assembly, and the emulator smoke flow pass without bypassing hooks or checks.

## Acceptance Criteria

1. A clean install reaches the empty state without a permission prompt or network access.
2. The user can import one or two screenshots through the system photo picker.
3. `Focus`, `Stack`, and eligible `Split` layouts render correctly in the live preview.
4. Title, subtitle, frame, palette, and shadow changes update the preview immediately.
5. The user can save a readable 1080 by 1920 PNG through the system document picker.
6. Import, decode, and export cancellation or failure never destroys the active composition.
7. The app declares no sensitive or network permissions.
8. Unit tests, Android lint, debug assembly, and the manual emulator smoke flow pass.
