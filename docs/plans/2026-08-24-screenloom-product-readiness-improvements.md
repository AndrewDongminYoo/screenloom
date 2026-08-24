# Screenloom Product Readiness Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:test-driven-development` while implementing each behavior and `superpowers:verification-before-completion` before claiming completion.

**Goal:** Make exported PNGs compatible with Google Play's opaque truecolor requirement, make the poster preview state understandable to assistive technology, and close locale-dependent Compose test issue #7.

**Architecture:** Keep the existing single-activity editor, Android bitmap renderer, platform PNG encoder, and merged preview semantics node.
Mark the renderer-owned bitmap opaque at creation so the platform encoder emits truecolor without an alpha channel, derive a localized preview summary from existing editor state, and make instrumented selectors resolve the same resources as the UI under test.

**Tech Stack:** Kotlin, Android Bitmap and PNG APIs, Jetpack Compose, Material 3, AndroidX Compose UI tests, JUnit 4.

**Spec:** `docs/specs/2026-08-12-screenloom-design.md` and `docs/specs/2026-08-13-screenloom-sunlit-editorial-design.md`.

## Global Constraints

- Preserve the application ID and namespace `kr.donminzzi.screenloom`.
- Preserve the exact 1080 by 1920 PNG product contract.
- Preserve preview and export palette, geometry, typography, and screenshot placement parity.
- Add no account, network client, analytics, billing, database, dependency-injection framework, navigation framework, broad media permission, or Android platform permission.
- Use English for code identifiers and technical documentation, and retain Korean user-facing strings.
- Do not commit, push, or create a pull request until explicitly requested.

---

### Task 1: Export Opaque Truecolor PNGs for Issue #8

**Files:**

- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt`
- Modify: `docs/testing.md`

**Interfaces:**

- Consumes: `PosterRenderer.render(EditorDocument, List<Bitmap>, Int, Int): Bitmap` and `PosterExporter.export(Uri, EditorDocument, List<Bitmap>): ExportResult`.
- Produces: PNG bytes whose IHDR bit depth byte is `8` and color type byte is `2`, while the decoded bitmap remains 1080 by 1920.

- [x] **Step 1: Extend the existing exporter test with the PNG contract**

Read the PNG signature and IHDR bytes from the real `PosterExporter` output and assert the contract directly:

```kotlin
assertEquals(8, encoded[24].toInt() and 0xFF)
assertEquals(2, encoded[25].toInt() and 0xFF)
```

- [x] **Step 2: Run the focused instrumented test and verify RED**

Run:

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest#exporterWritesOpaqueTruecolorPngAtExactDimensions
```

Expected: FAIL because the current output reports PNG color type `6` instead of `2`.

- [x] **Step 3: Mark the renderer-owned bitmap opaque at its source**

Create the ARGB bitmap with `hasAlpha = false` using the API 26 overload already covered by the project's minimum SDK:

```kotlin
val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, false)
```

Do not copy to `RGB_565`, add a custom PNG encoder, or add a dependency.

- [x] **Step 4: Run the focused test and verify GREEN**

Expected: the PNG color type assertion passes and decoded dimensions remain unchanged.

- [x] **Step 5: Record the actual format property in the manual verification contract**

Add an opaque truecolor check to `docs/testing.md` using a generated export and a format inspector that reports the PNG color model.

### Task 2: Expose a Localized Dynamic Preview Summary for Issue #9

**Files:**

- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ko/strings.xml`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterPreviewTest.kt`

**Interfaces:**

- Consumes: `EditorDocument.layout`, `EditorDocument.palette`, and the preview image list size.
- Produces: one merged semantics node describing layout, screenshot count, and palette in the active locale.

- [x] **Step 1: Write a failing dynamic-semantics test**

Render a two-image `Split` document with the `Cobalt` palette and expect the localized summary rather than the current generic description.
Continue asserting that title and subtitle canvas text are absent from the semantics tree.

- [x] **Step 2: Run the focused test and verify RED**

Run the `PosterPreviewTest#previewExposesOnlyItsConciseDescription` method and confirm it fails because the current description is static.

- [x] **Step 3: Add localized summary resources**

Add a quantity resource for one or two screenshots and a format string whose arguments are layout label, screenshot-count phrase, and palette label.
Keep `Focus`, `Stack`, `Split`, and palette names aligned with their current intentionally unlocalized display labels.

- [x] **Step 4: Build the description from current preview state**

Resolve the layout label, screenshot-count quantity, and palette label inside `PosterPreview`, then keep `clearAndSetSemantics` so decorative children remain hidden.

- [x] **Step 5: Run the focused test and verify GREEN**

Confirm the dynamic summary is visible as one content description and the canvas copy remains excluded from semantics.

### Task 3: Close Locale-Dependent Test Issue #7

**Files:**

- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt`
- Modify: `docs/testing.md`

**Interfaces:**

- Consumes: the same string resources used by `EditorScreen` and a locale-specific Android `Context` for explicit Korean coverage.
- Produces: selectors that follow the active locale and one test that renders the real Korean resources without changing global device locale.

- [x] **Step 1: Add explicit Korean-locale coverage and verify RED**

Create a `ko-KR` configuration context for one empty-state test, provide it to the composition, and assert the Korean import action and dynamic preview description.
The first run must fail while the test harness still assumes English resources.

- [x] **Step 2: Replace translated hard-coded selectors**

Resolve translated labels through `getString` from the context used by the UI under test.
Keep only strings marked `translatable="false"` as stable literals when that improves readability.

- [x] **Step 3: Verify the English and Korean tests pass independently**

Run the focused English test and the explicit Korean test.
Do not mutate the emulator's persistent per-app locale merely to make the test pass.

- [x] **Step 4: Remove the obsolete English-only workaround**

Replace the current `docs/testing.md` warning with a statement that selectors are resource-backed and the suite contains explicit Korean coverage.

### Task 4: Complete the Repository Verification Contract

**Files:**

- Verify only; do not broaden production scope.

- [x] **Step 1: Run JVM tests**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
```

- [x] **Step 2: Run Android lint and the merged-manifest permission check**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
```

- [x] **Step 3: Assemble the debug APK**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

- [x] **Step 4: Run the complete connected suite on the dedicated emulator**

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
```

- [x] **Step 5: Run Trunk and whitespace checks on the exact change set**

```bash
trunk check
git diff --check
```

- [x] **Step 6: Run the seven-scenario manual emulator flow**

Confirm import, one-image and two-image layouts, editor changes, picker cancellation, rotation, export, 1080 by 1920 dimensions, and opaque truecolor PNG output.
Record any assistive-technology scenarios that remain manual follow-up in issue #9 rather than claiming them complete.

## Self-Review

- Spec coverage: output dimensions, offline picker flow, preview/export parity, localized semantics, permission boundary, and verification gates are covered.
- Scope exclusions: post-export sharing, saved styles, store-set batching, billing, analytics, accounts, and cloud work remain in issues #10 and #11.
- Placeholder scan: no implementation step relies on a placeholder or an undefined production interface.
- Type consistency: all tasks retain the existing `PosterRenderer`, `PosterExporter`, `PosterPreview`, and `EditorScreen` public signatures.
