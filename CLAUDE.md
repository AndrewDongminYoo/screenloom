# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Screenloom is a single-module offline Android app (Kotlin, Compose, Material 3) that turns one or two screenshots into a 1080x1920 promotional poster PNG.

Owned elsewhere, do not restate here:

- `AGENTS.md` — product scope and hard constraints.
- `docs/testing.md` — the full verification gate, the merged-manifest permission check, the SDK path, and the last verified baseline.
- `docs/specs/2026-08-12-screenloom-design.md` — approved design; `docs/plans/2026-08-12-screenloom-implementation.md` — implementation plan.
- `README.md` — build command and commercial scope.

## Environment

JDK 17 and `minSdk` 26; toolchain versions live in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`.

`local.properties` is gitignored, so a fresh clone has no `sdk.dir` and **every Gradle invocation needs the SDK passed in**:

```bash
ANDROID_SDK_ROOT=<android-sdk-path> ./gradlew <task>
```

The path is in `docs/testing.md`.
Keep it in the invocation or the environment; never commit it into a tracked Gradle file.

Run Android tasks one at a time — this machine crashes under stacked emulator plus build load.

## Running a single test

```bash
# One unit-test class (verified 2026-08-13, no emulator needed)
ANDROID_SDK_ROOT=<android-sdk-path> ./gradlew testDebugUnitTest --tests "kr.donminzzi.screenloom.editor.EditorReducerTest"

# One instrumented class or method (AGP form; needs a booted emulator or device)
ANDROID_SDK_ROOT=<android-sdk-path> ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest
```

## Test placement rule

`src/test` is **pure JVM only** — no Android framework types.
That is why `ImageDecoderTest` covers only the static `calculateInSampleSize` and leaves decoding to `ImageDecoderInstrumentedTest`.
Anything touching `Bitmap`, `Canvas`, `Uri`, or Compose goes in `src/androidTest` and runs on a real device.

There is no Robolectric and no mocking framework; test seams are hand-written fakes against the `fun interface` boundaries below.

`.agents/skills/testing-setup/` is vendored Google reference material that recommends Hilt, Robolectric, Dropshots, Jacoco, and Compose UI tests in `src/test`.
None of that is installed here and the repository convention above wins — do not "fix" the testing setup to match that skill.

## Architecture

### The poster is rendered twice, and parity is hand-maintained

This is the repository's main hazard.
Two independent renderers must produce the same poster:

- `render/PosterPreview.kt` — Compose `DrawScope`, draws the live preview, title and subtitle as real `Text` composables.
- `render/PosterRenderer.kt` — `android.graphics.Canvas`, draws the exported bitmap, title and subtitle as `StaticLayout`.

Both scale every dimension from the same 1080x1920 reference, but they share only:

- `PosterLayout.placements()` — screenshot rectangles and rotations for `Focus` / `Stack` / `Split`.
- `PaletteId.colors()` — the six gradients.
- `ShadowLevel.posterShadowSpec()`, `POSTER_SHADOW_LAYER_COUNT`, `POSTER_TITLE_LINE_HEIGHT`, `POSTER_SUBTITLE_LINE_HEIGHT`.

Everything else is duplicated as literals in both files: the background gradient and accent glow, the 9x6 dot texture, the copy-block geometry (`90f` inset, `150f` top, `78f` title, `32f` subtitle, `22f` gap), the `42f` corner radius, the `16f` frame inset, the `Stack` image-order reversal, and `centerCrop`.

**Any visual change must be made in both files or the export silently stops matching the preview.**
Three separate fix commits already exist for exactly this drift: `7a13b0b` (preview vs export), `ca6d4c7` (multiline title spacing), `a5eaf8f` (subtitle line spacing).
`PosterRendererTest` and `PosterPreviewTest` are where a parity regression gets caught.

### State

`EditorDocument` is the pure, Android-free composition: it holds `imageCount`, not the images.
`EditorUiState` wraps it and owns the decoded `ImportedImage` bitmaps plus transient `isImporting` / `isExporting` / `message` fields.

That split is what makes `EditorReducer.reduce()` a total, pure JVM-testable function, and it is why the renderers take an `EditorDocument` plus a separate `List<Bitmap>` rather than one combined object.
Keep new composition fields on `EditorDocument` and new transient UI fields on `EditorUiState`.

The reducer also enforces the invariants: `Split` is refused below two images and falls back to `Focus` when the count drops, and `Reset` preserves `imageCount`.

### Seams and construction

No DI framework.
`MainActivity` builds `ImageDecoder` and `PosterExporter` by hand and passes them through an anonymous `ViewModelProvider.Factory`.
The testable boundaries are `fun interface`s: `ImageLoader` (decode), `PosterWriter` (export), `OutputStreamProvider` (open the target URI).
Fake those in tests instead of touching `ContentResolver`.

Both system pickers live in `MainActivity` (`PickMultipleVisualMedia(2)` and `CreateDocument("image/png")`); the ViewModel only ever sees the resulting `Uri`s.

### Where UI changes go

`EditorScreen.kt` owns the scaffold, snackbar, empty state, and workspace layout.
Its empty state feeds synthetic gradient bitmaps from `createSampleImage` into the real `PosterPreview`, so a preview change also changes the sample poster.

`EditorControls.kt` owns the three-tab panel: the private `EditorTab` enum plus `LayoutControls`, `CopyControls`, and `StyleControls`.
A new control belongs in the matching tab composable there, dispatching a new `EditorAction`.

### Bitmap ownership

The ViewModel recycles the previous images on a successful import and all images in `onCleared`.
`PosterExporter` recycles its own output bitmap in a `finally`.
Sources are decoded with `inSampleSize` down to a 2048 px longest edge before any preview or export work.

## Conventions

- Text limits are counted in **code points**, not chars — `takeCodePoints` in `EditorReducer` and the 48-point slug cap in `posterFileName` both use `offsetByCodePoints`, so emoji and surrogate pairs stay intact. Preserve that when touching either.
- `EditorUiState.message` is a `@StringRes Int?`, so all user-facing copy goes to `app/src/main/res/values/strings.xml` — never inline a literal string in a composable.
- Failures are recoverable snackbars that never clear the composition; `import` and `export` refuse to run while the other is in flight.
- Dependencies are declared in `gradle/libs.versions.toml` only.
