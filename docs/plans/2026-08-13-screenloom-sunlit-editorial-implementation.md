# Screenloom Sunlit Editorial Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Screenloom's near-black application shell and uniformly dark poster presets with the approved Sunlit Editorial interface and exported-poster system while preserving every editor, picker, layout, privacy, and export contract.

**Architecture:** Keep the existing single-Activity, immutable-state editor and the two rendering paths.
Extend the existing `PosterPalette` value object with explicit visual roles, consume those same values and decoration geometry from both Compose preview and Android Canvas export, and confine the application redesign to the current theme, screen, controls, strings, and Android window theme.
Do not add a design-system module, dependency, editor state, or renderer abstraction.

**Tech Stack:** Kotlin with AGP built-in Kotlin 2.2.10, Jetpack Compose BOM 2026.06.00, Material 3, Activity Compose 1.13.0, Android Canvas, Android Gradle Plugin 9.3.0, Gradle 9.6.1, JDK 17, JUnit4, AndroidX Compose UI tests, and the existing API 34 `flutter_emulator_2` AVD.

## Global Constraints

- Repository root: `/Volumes/dongminyu/Development/01_personal/screenloom`.
- Approved visual specification: `docs/specs/2026-08-13-screenloom-sunlit-editorial-design.md`.
- Existing product specification: `docs/specs/2026-08-12-screenloom-design.md`.
- Application ID and namespace remain `kr.donminzzi.screenloom`.
- SDK configuration remains `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, and Build Tools 36.0.0.
- Screenloom remains a single-module, offline native Android application.
- Do not add accounts, networking, analytics, ads, billing, a database, dependency injection, navigation, saved projects, theme switching, or custom poster dimensions.
- Do not change `EditorDocument`, `EditorUiState`, `EditorAction`, `LayoutMode`, `ShadowLevel`, or the six `PaletteId` enum identifiers and their order.
- Keep `PaletteId.Ink`, `PaletteId.Moss`, and `PaletteId.Violet` as internal identifiers; only their display labels become `Paper`, `Mint`, and `Iris`.
- Do not change `PosterLayout`, its Focus/Stack/Split geometry, rotations, draw order, one-image fallbacks, or source-aspect fitting.
- Import remains `ActivityResultContracts.PickMultipleVisualMedia(2)` and export remains `ActivityResultContracts.CreateDocument("image/png")`.
- The only exported asset remains an exact 1080 by 1920 PNG.
- The only allowed merged `<uses-permission>` remains `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- Keep all user-facing copy and palette display names in `strings.xml`.
- No dependency manifest or lockfile change is expected or authorized by this plan.
- Compose preview and Android Canvas export must receive the same palette roles, decoration geometry, layer order, type intent, frame color, shadow tint, and screenshot placement.
- Keep the preview at 9:16 with one concise content description, fixed poster font scale, and the existing placement animation.
- Keep all controls visible while disabled, preserve selected semantics, retain the disabled Split state description, and keep interactive targets at least 48 dp.
- Normal-size application text and poster copy must meet at least 4.5:1 contrast against their immediate background.
- Within application chrome, Cobalt is a selection and focus color, not a filled normal-text surface; Coral is reserved for `Choose screenshots` and `Export PNG`.
- Run only one emulator boot, Gradle build, or other heavy mobile job at a time.
- Every `connectedDebugAndroidTest` invocation must set `ANDROID_SERIAL=emulator-5556` so Gradle never targets another attached device.
- Do not reuse the 2026-08-13 pre-redesign manual captures as evidence for this change.
- Pixel change and correct dimensions do not establish visual quality; actual captures and reopened exports require human inspection.
- Preserve the already-approved specification edits and this plan as author-known task changes, and preserve every unrelated or author-unknown path without staging, restoring, or deleting it.
- Implementation authority does not imply commit authority.
  The final step of each task contains a suggested commit for use only after the operator separately authorizes commits; otherwise leave verified changes unstaged and report them.
- When commit authority exists, stage the exact listed paths, inspect `git diff --cached`, and never use `git commit -- <pathspec>` or bypass hooks.

---

## Execution Preflight

- [ ] Confirm the absolute repository and preserve the existing documentation changes:

```bash
git -C /Volumes/dongminyu/Development/01_personal/screenloom rev-parse --show-toplevel
git -C /Volumes/dongminyu/Development/01_personal/screenloom status --short
git -C /Volumes/dongminyu/Development/01_personal/screenloom diff -- docs/specs/2026-08-12-screenloom-design.md
```

Expected: the top level is exactly `/Volumes/dongminyu/Development/01_personal/screenloom`; the approved base-spec edit, new Sunlit Editorial spec, and this plan remain present; no file is staged.

- [ ] Inspect attached Android devices and running heavy jobs before starting an emulator:

```bash
adb devices -l
ps -axo pid,command | rg 'emulator|qemu|gradle|GradleDaemon|xcodebuild' || true
```

Expected: either `emulator-5556` is already the dedicated `flutter_emulator_2` instance, or no conflicting heavy mobile job prevents starting it.
Do not stop an unrelated emulator or build.

- [ ] When `emulator-5556` is absent, start exactly the documented headless AVD in a long-lived terminal session and wait for it:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk /Volumes/dongminyu/Android/sdk/emulator/emulator -avd flutter_emulator_2 -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot-load -no-snapshot-save -memory 2048 -cores 2 -port 5556
adb -s emulator-5556 wait-for-device
adb -s emulator-5556 shell getprop sys.boot_completed
```

Expected: the final property is `1`.
Keep `-gpu swiftshader_indirect` and `-no-snapshot-load`; omitting either has produced false offline failures in this project.

## File Map and Ownership

### Shared Poster Style

- Create `app/src/test/java/kr/donminzzi/screenloom/render/PosterPaletteTest.kt` for pure role-map, identifier, and contrast coverage.
- Modify `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt:22-255` for the expanded palette, shared decoration geometry, Android Canvas layer order, serif headline, palette frame, and tinted shadow.
- Modify `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt:80-319` for the same decoration data, layer order, copy colors, serif headline, frame, and shadow tint.
- Modify `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt:31-163` for serif ellipsis and export regressions.
- Modify `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterPreviewTest.kt:47-626` for all-palette preview/export parity and frame/shadow evidence.

### Application Theme and Screens

- Modify `app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt:16-84` for the light Material color scheme and approved application tokens.
- Modify `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt:38-43` only for light system-bar styles.
- Modify `app/src/main/res/values/themes.xml:3-10` for the launch background and light system-bar icon flags.
- Modify `app/src/androidTest/java/kr/donminzzi/screenloom/ui/theme/ScreenloomThemeTest.kt:20-42` for exact token and contrast coverage.
- Modify `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt:60-360` for the paper shell, woven ribbons, empty state, preview stage, and action hierarchy.
- Modify `app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt:55-352` for the white control sheet, segmented tabs, option states, and palette presentation.
- Modify `app/src/main/res/values/strings.xml:3-49` for palette display names and the decorative forward cue.
- Modify `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt:37-258` for renamed labels, dispatch mapping, and preserved semantics.

### Verification Evidence

- Modify `docs/testing.md` only after the fresh automated and manual runs, recording exact observed commands, APK digest, captures, output dimensions, and the operator's visual verdict.
- Write captures only under the already-ignored `app/build/outputs/manual-qa/sunlit-editorial/` directory.

### Explicit Non-Changes

- Do not modify `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`.
- Do not modify `app/src/main/java/kr/donminzzi/screenloom/editor/EditorReducer.kt` or `EditorViewModel.kt`.
- Do not modify `app/src/main/java/kr/donminzzi/screenloom/render/PosterLayout.kt`.
- Do not modify `app/src/main/java/kr/donminzzi/screenloom/media/**`.
- Do not modify `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, or any lockfile.

---

### Task 1: Define the Shared Sunlit Poster Palette Contract

**Files:**

- Create: `app/src/test/java/kr/donminzzi/screenloom/render/PosterPaletteTest.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt:22-69`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt:176-181`

**Interfaces:**

- Consumes: the unchanged `PaletteId` values from `EditorModels.kt`.
- Produces: `PosterPalette(startColor, endColor, headlineColor, supportingCopyColor, frameColor, shadowColor, ribbonOneColor, ribbonTwoColor, sunColor)` and `fun PaletteId.colors(): PosterPalette` for both renderers.
- Preserves: enum identifiers and order, with no persisted-state migration.

- [ ] **Step 1: Write the failing exact-role and contrast tests**

Create `PosterPaletteTest.kt` with this complete contract:

```kotlin
package kr.donminzzi.screenloom.render

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kr.donminzzi.screenloom.editor.PaletteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterPaletteTest {
    @Test
    fun paletteIdentifiersRemainStable() {
        assertEquals(
            listOf(
                PaletteId.Ink,
                PaletteId.Cobalt,
                PaletteId.Coral,
                PaletteId.Moss,
                PaletteId.Violet,
                PaletteId.Sunrise,
            ),
            PaletteId.entries,
        )
    }

    @Test
    fun palettesMatchTheApprovedSunlitRoleMap() {
        val ink = 0xFF18213D.toInt()
        val paper = 0xFFFFF8E9.toInt()
        val coral = 0xFFFF6B4A.toInt()
        val cobalt = 0xFF566EFF.toInt()
        val sun = 0xFFFFD466.toInt()
        val expected = mapOf(
            PaletteId.Ink to PosterPalette(
                startColor = paper,
                endColor = 0xFFFFD9A2.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = cobalt,
                ribbonTwoColor = coral,
                sunColor = sun,
            ),
            PaletteId.Cobalt to PosterPalette(
                startColor = 0xFF3557F0.toInt(),
                endColor = 0xFF78DBEF.toInt(),
                headlineColor = paper,
                supportingCopyColor = paper,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = sun,
                ribbonTwoColor = coral,
                sunColor = paper,
            ),
            PaletteId.Coral to PosterPalette(
                startColor = 0xFFFF765C.toInt(),
                endColor = 0xFFFFC46D.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = paper,
                ribbonTwoColor = cobalt,
                sunColor = 0xFFFFF0BD.toInt(),
            ),
            PaletteId.Moss to PosterPalette(
                startColor = 0xFF6BD7B3.toInt(),
                endColor = 0xFFD8EF6A.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = ink,
                ribbonTwoColor = coral,
                sunColor = paper,
            ),
            PaletteId.Violet to PosterPalette(
                startColor = 0xFF5D50D8.toInt(),
                endColor = 0xFFF3A1C7.toInt(),
                headlineColor = paper,
                supportingCopyColor = paper,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = sun,
                ribbonTwoColor = paper,
                sunColor = sun,
            ),
            PaletteId.Sunrise to PosterPalette(
                startColor = 0xFFFFE26C.toInt(),
                endColor = 0xFFFF7C56.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = cobalt,
                ribbonTwoColor = paper,
                sunColor = paper,
            ),
        )

        assertEquals(expected, PaletteId.entries.associateWith { paletteId -> paletteId.colors() })
    }

    @Test
    fun posterCopyMeetsNormalTextContrastAtTheCopyZone() {
        PaletteId.entries.forEach { paletteId ->
            val palette = paletteId.colors()
            listOf(palette.headlineColor, palette.supportingCopyColor).forEach { foreground ->
                val ratio = contrastRatio(foreground, palette.startColor)
                assertTrue("$paletteId copy contrast is $ratio", ratio >= 4.5)
            }
        }
    }

    private fun contrastRatio(first: Int, second: Int): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun component(shift: Int): Double {
            val encoded = ((color ushr shift) and 0xFF) / 255.0
            return if (encoded <= 0.04045) {
                encoded / 12.92
            } else {
                ((encoded + 0.055) / 1.055).pow(2.4)
            }
        }
        return 0.2126 * component(16) + 0.7152 * component(8) + 0.0722 * component(0)
    }
}
```

- [ ] **Step 2: Run the palette test and verify RED**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests kr.donminzzi.screenloom.render.PosterPaletteTest
```

Expected: compilation fails because the current `PosterPalette` exposes only `startColor`, `endColor`, and `accentColor`.

- [ ] **Step 3: Expand the existing palette value object and mapping**

Replace the current `PosterPalette` declaration and `PaletteId.colors()` mapping in `PosterRenderer.kt` with the exact fields and values asserted above:

```kotlin
data class PosterPalette(
    val startColor: Int,
    val endColor: Int,
    val headlineColor: Int,
    val supportingCopyColor: Int,
    val frameColor: Int,
    val shadowColor: Int,
    val ribbonOneColor: Int,
    val ribbonTwoColor: Int,
    val sunColor: Int,
)
```

Copy the six complete constructor entries from `palettesMatchTheApprovedSunlitRoleMap()` into `PaletteId.colors()` so the test and production mapping have an explicit one-to-one review surface.
Do not add a second palette model or transform colors at runtime.

- [ ] **Step 4: Update the two current accent consumers so the project compiles**

In `PosterRenderer.drawBackground`, replace `palette.accentColor` with `palette.sunColor`.
In `PosterPreview`, replace `palette.accentColor` with `palette.sunColor`.
Do not consume the other new roles until Task 2, where both rendering paths change together.

- [ ] **Step 5: Run the palette test and full JVM suite and verify GREEN**

Run sequentially:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests kr.donminzzi.screenloom.render.PosterPaletteTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
```

Expected: both commands exit zero, including the unchanged `PosterLayoutTest` geometry suite.

- [ ] **Step 6: Record the Task 1 checkpoint**

Suggested commit, only when commit authority exists:

```bash
git add app/src/test/java/kr/donminzzi/screenloom/render/PosterPaletteTest.kt app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt
git diff --cached --check
git diff --cached
git commit -m "feat(render): define sunlit poster palettes"
```

Without commit authority, leave the verified files unstaged and continue.

---

### Task 2: Render Matching Sunlit Posters in Compose and Android Canvas

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt:28-255`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt:80-319`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt:122-163`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterPreviewTest.kt:66-626`
- Verify without modification: `app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt`

**Interfaces:**

- Consumes: `PosterPalette` from Task 1, `PosterLayout.imagePlacements`, and the unchanged `EditorDocument`.
- Produces: `PosterRibbonSpec`, `PosterRibbonSpecs`, `POSTER_SUN_ALPHA`, and `POSTER_RIBBON_ALPHA` as package-internal shared decoration data.
- Produces layer order `gradient -> sun and ribbons -> screenshots, frames, and shadows -> copy` in both renderers.
- Preserves: title/subtitle limits, copy positions, 9:16 semantics, font-scale isolation, placement animation, screenshot memoization, and aspect fitting.

- [ ] **Step 1: Add failing decoration, frame, shadow, and serif-parity tests**

Add this decoration parity test to `PosterPreviewTest`:

```kotlin
@Test
fun everySunlitPaletteMatchesTheExportAtDecorationSamples() {
    var document by mutableStateOf(EditorDocument())
    compose.setContent {
        Box(Modifier.width(270.dp).testTag("preview-capture")) {
            PosterPreview(document = document, images = emptyList())
        }
    }

    val baseSample = 108 to 192
    val sunSample = 886 to 346
    val ribbonSamples = listOf(540 to 925, 540 to 1130)
    PaletteId.entries.forEach { paletteId ->
        compose.runOnIdle { document = EditorDocument(palette = paletteId) }
        val preview = compose.onNodeWithTag("preview-capture").captureToImage()
        val export = PosterRenderer().render(document, emptyList(), 1080, 1920)
        try {
            listOf(baseSample, sunSample).plus(ribbonSamples).forEach { (x, y) ->
                assertPixelChannelsWithinTolerance(preview, export, x, y, tolerance = 8)
            }
            ribbonSamples.forEach { (x, y) ->
                assertColorDiffersFromGradient(export.getPixel(x, y), paletteId.colors(), x, y)
            }
        } finally {
            export.recycle()
        }
    }
}
```

Add these helpers beside the existing pixel helpers:

```kotlin
private fun assertColorDiffersFromGradient(color: Int, palette: PosterPalette, x: Int, y: Int) {
    val gradient = expectedGradientColor(palette, x, y)
    val distance = abs(android.graphics.Color.red(color) - android.graphics.Color.red(gradient)) +
        abs(android.graphics.Color.green(color) - android.graphics.Color.green(gradient)) +
        abs(android.graphics.Color.blue(color) - android.graphics.Color.blue(gradient))
    assertTrue("Decoration missing at ($x, $y): distance=$distance", distance >= 20)
}

private fun expectedGradientColor(palette: PosterPalette, x: Int, y: Int): Int {
    val progress = ((x * 1080f + y * 1920f) / (1080f * 1080f + 1920f * 1920f))
        .coerceIn(0f, 1f)
    fun channel(component: (Int) -> Int): Int {
        val start = component(palette.startColor)
        val end = component(palette.endColor)
        return (start + progress * (end - start)).roundToInt()
    }
    return android.graphics.Color.rgb(
        channel(android.graphics.Color::red),
        channel(android.graphics.Color::green),
        channel(android.graphics.Color::blue),
    )
}
```

Extend the existing strong-shadow test with a non-rotated Focus frame sample at `frame.left + 8f` and `frame.top + frame.height / 2f`.
Require the export pixel to equal `document.palette.colors().frameColor`, then use `assertPixelChannelsWithinTolerance` at that coordinate and the existing shadow coordinate to prove preview/export parity.

Add the assertions with the existing `frame`, `strongShadow`, and `exportStrongShadow` values:

```kotlin
val frameSampleX = (frame.left + 8f).roundToInt()
val frameSampleY = (frame.top + frame.height / 2f).roundToInt()
val previewFrameX = (frameSampleX * scale).roundToInt()
val previewFrameY = (frameSampleY * scale).roundToInt()
val palette = EditorDocument().palette.colors()
assertEquals(palette.frameColor, exportStrongShadow.getPixel(frameSampleX, frameSampleY))
assertPixelChannelsWithinTolerance(
    preview = strongShadow,
    export = exportStrongShadow,
    exportX = frameSampleX,
    exportY = frameSampleY,
    tolerance = 8,
)
assertPixelChannelsWithinTolerance(
    preview = strongShadow,
    export = exportStrongShadow,
    exportX = exportSampleX,
    exportY = exportSampleY,
    tolerance = 8,
)
assertTrue(strongShadow.toPixelMap()[previewFrameX, previewFrameY].alpha >= 0.99f)
```

In `PosterRendererTest.ellipsizedText`, change only the title branch to the approved generic serif mapping:

```kotlin
typeface = Typeface.create(
    if (textSize == 78f) Typeface.SERIF else Typeface.DEFAULT,
    if (textSize == 78f) Typeface.BOLD else Typeface.NORMAL,
)
```

- [ ] **Step 2: Run the focused renderer tests and verify RED**

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest,kr.donminzzi.screenloom.render.PosterPreviewTest
```

Expected: the decoration test fails at the ribbon samples because the current renderer draws no ribbons, and the serif ellipsis expectation differs from the current sans headline renderer.

- [ ] **Step 3: Define one shared decoration geometry beside `PosterPalette`**

Add these package-internal declarations to `PosterRenderer.kt`:

```kotlin
internal const val POSTER_SUN_ALPHA = 176
internal const val POSTER_RIBBON_ALPHA = 72

internal data class PosterRibbonSpec(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float,
)

internal val PosterRibbonSpecs = listOf(
    PosterRibbonSpec(left = -170f, top = 820f, width = 1420f, height = 210f, rotationDegrees = -12f),
    PosterRibbonSpec(left = -160f, top = 1040f, width = 1400f, height = 180f, rotationDegrees = 14f),
)
```

Remove `POSTER_GLOW_ALPHA`, `POSTER_TEXTURE_ALPHA`, and the dotted texture loops from both renderers.
The sun remains a hard-edged flat disc, which preserves the previously approved rendering characteristic while changing its color and prominence.

- [ ] **Step 4: Apply the shared layers and palette roles to Android Canvas export**

Resolve the palette once and change `render` to this explicit order:

```kotlin
val palette = document.palette.colors()
drawBackground(canvas, palette, width, height, scale)
drawScreenshots(canvas, document, images, palette, width, height, scale)
drawCopy(canvas, document, palette, width, scale)
```

In `drawBackground`, keep the existing diagonal gradient, draw the sun at `(width * 0.82f, height * 0.18f)` with radius `240f * scale`, then draw `PosterRibbonSpecs` in order.
For each ribbon, create its scaled `RectF`, rotate around the rectangle center, use `height / 2f` as the round radius, set the corresponding `ribbonOneColor` or `ribbonTwoColor`, and set paint alpha to `POSTER_RIBBON_ALPHA`.

Use one helper so both ribbon entries follow the same Canvas path:

```kotlin
private fun drawRibbon(
    canvas: Canvas,
    spec: PosterRibbonSpec,
    color: Int,
    scale: Float,
) {
    val bounds = RectF(
        spec.left * scale,
        spec.top * scale,
        (spec.left + spec.width) * scale,
        (spec.top + spec.height) * scale,
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = POSTER_RIBBON_ALPHA
    }
    canvas.save()
    canvas.rotate(spec.rotationDegrees, bounds.centerX(), bounds.centerY())
    canvas.drawRoundRect(bounds, bounds.height() / 2f, bounds.height() / 2f, paint)
    canvas.restore()
}
```

Call it with `listOf(palette.ribbonOneColor, palette.ribbonTwoColor)` zipped to `PosterRibbonSpecs`.

Change `drawCopy` to accept `PosterPalette` and use:

```kotlin
title.color = palette.headlineColor
title.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
subtitle.color = palette.supportingCopyColor
subtitle.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
```

Do not set an additional subtitle alpha.
Keep the current 90-unit horizontal inset, 150-unit top position, text sizes, line heights, maximum lines, and ellipsis behavior.

Pass `PosterPalette` through `drawScreenshots` into `drawScreenshot`.
Build each shadow layer with the RGB channels from `palette.shadowColor` and the existing `shadowSpec.layerAlpha(layerIndex)`.
Draw the enabled device frame with `palette.frameColor`.
Do not change the current layered-shadow spread, frame inset, clipping path, crop safety net, or `PosterLayout` call.

- [ ] **Step 5: Apply the same roles, geometry, and order to Compose preview**

Inside the preview Canvas, keep the diagonal gradient, draw the same sun and the same two ribbons scaled by `size.width / 1080f`, and then draw animated screenshots.
Use `rotate` around each ribbon's scaled center and `drawRoundRect` with half-height corners.
Resolve `val palette = document.palette.colors()` once inside `BoxWithConstraints` but before the Canvas so the Canvas, screenshot drawing, and copy Column consume the same object.

Use this DrawScope helper and call it for both zipped ribbon colors before screenshots:

```kotlin
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewRibbon(
    spec: PosterRibbonSpec,
    color: Int,
) {
    val scale = size.width / 1080f
    val topLeft = Offset(spec.left * scale, spec.top * scale)
    val ribbonSize = Size(spec.width * scale, spec.height * scale)
    rotate(
        degrees = spec.rotationDegrees,
        pivot = topLeft + Offset(ribbonSize.width / 2f, ribbonSize.height / 2f),
    ) {
        drawRoundRect(
            color = Color(color).copy(alpha = POSTER_RIBBON_ALPHA / 255f),
            topLeft = topLeft,
            size = ribbonSize,
            cornerRadius = CornerRadius(ribbonSize.height / 2f),
        )
    }
}
```

Pass `palette.frameColor` and `palette.shadowColor` into `drawPreviewImage`.
Use the tint RGB with the existing per-layer alpha, and use the palette frame instead of fixed `0xFF14171E`.

Keep the copy Column after the Canvas so copy remains the top layer.
Use these explicit text roles:

```kotlin
fontFamily = FontFamily.Serif
color = Color(palette.headlineColor)
```

for the title, and:

```kotlin
fontFamily = FontFamily.SansSerif
color = Color(palette.supportingCopyColor)
```

for the subtitle.
Keep `LocalDensity` with `fontScale = 1f`, sizes, positions, line heights, maximum lines, and ellipsis unchanged.

- [ ] **Step 6: Rebase the existing Mint fixture helper without hard-coded colors**

Keep `PaletteId.Moss` in the test document because the enum identifier is unchanged.
Rename `assertMossFixtureSample` to `assertMintFixtureSample`, update its diagnostic copy to `Mint`, and continue deriving expected gradient channels from `PaletteId.Moss.colors()`.
Keep the current sample coordinates only when they remain outside both ribbon rectangles and screenshot shadows; otherwise move the background samples to `(108, 192)` and `(972, 1728)` in both preview and export assertions.

- [ ] **Step 7: Run focused rendering verification and verify GREEN**

Run sequentially:

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterPreviewTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests kr.donminzzi.screenloom.render.PosterLayoutTest
```

Expected: all three commands exit zero; preview/export pixel channels stay within the existing tolerance; all geometry tests remain unchanged and green.

- [ ] **Step 8: Record the Task 2 checkpoint**

Suggested commit, only when commit authority exists:

```bash
git add app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterPreviewTest.kt
git diff --cached --check
git diff --cached
git commit -m "feat(render): weave sunlit poster layers"
```

Without commit authority, leave the verified files unstaged and continue.

---

### Task 3: Adopt the Light Application Theme and System Surfaces

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt:16-84`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt:38-43`
- Modify: `app/src/main/res/values/themes.xml:3-10`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/ui/theme/ScreenloomThemeTest.kt:20-42`

**Interfaces:**

- Produces application tokens `Paper`, `ElevatedPaper`, `Ink`, `MutedInk`, `Cobalt`, `Coral`, `Sun`, `Mint`, `Outline`, and `SelectedWash`.
- Produces a Material `lightColorScheme` consumed by all existing Material components.
- Preserves: `ScreenloomTheme(content)` signature, edge-to-edge, Activity construction, picker launchers, and Android service construction.

- [ ] **Step 1: Replace the single contrast assertion with failing exact-role coverage**

Update `ScreenloomThemeTest` to capture the Material scheme and assert the approved values:

```kotlin
@Test
fun themeUsesTheApprovedSunlitRoles() {
    lateinit var colors: ColorScheme
    compose.setContent {
        ScreenloomTheme { colors = MaterialTheme.colorScheme }
    }
    compose.runOnIdle {
        assertEquals(Color(0xFFFFF8E9), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFF18213D), colors.onBackground)
        assertEquals(Color(0xFF667087), colors.onSurfaceVariant)
        assertEquals(Color(0xFF566EFF), colors.primary)
        assertEquals(Color(0xFFFF6B4A), colors.secondary)
        assertEquals(Color(0xFF18213D), colors.onSecondary)
        assertEquals(Color(0xFFF7F4ED), colors.surfaceVariant)
        assertEquals(Color(0xFFE6DCCB), colors.outline)
    }
}

@Test
fun applicationTextRolesMeetNormalTextContrastGuidance() {
    lateinit var colors: ColorScheme
    compose.setContent {
        ScreenloomTheme { colors = MaterialTheme.colorScheme }
    }
    compose.runOnIdle {
        listOf(
            colors.onBackground to colors.background,
            colors.onSurface to colors.surface,
            colors.onSurfaceVariant to colors.background,
            colors.onSurfaceVariant to colors.surfaceVariant,
            colors.onSecondary to colors.secondary,
        ).forEach { (foreground, background) ->
            val ratio = contrastRatio(foreground, background)
            assertTrue("Content contrast is $ratio", ratio >= 4.5f)
        }
    }
}
```

Import `androidx.compose.material3.ColorScheme` and `org.junit.Assert.assertEquals`.
Do not retain the old `primary/onPrimary` normal-text assertion because Cobalt is an outline, focus, switch, and selection color rather than an approved filled text surface.

- [ ] **Step 2: Run the theme test and verify RED**

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.ui.theme.ScreenloomThemeTest
```

Expected: the exact-role test fails because the current theme is a dark scheme with `#090B10` background.

- [ ] **Step 3: Implement the exact light theme tokens**

Replace the current four colors and `darkColorScheme` with:

```kotlin
val Paper = Color(0xFFFFF8E9)
val ElevatedPaper = Color(0xFFFFFFFF)
val Ink = Color(0xFF18213D)
val MutedInk = Color(0xFF667087)
val Cobalt = Color(0xFF566EFF)
val Coral = Color(0xFFFF6B4A)
val Sun = Color(0xFFFFD466)
val Mint = Color(0xFF6BD7B3)
val Outline = Color(0xFFE6DCCB)
val SelectedWash = Color(0xFFFFF0B9)

private val ScreenloomColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Paper,
    secondary = Coral,
    onSecondary = Ink,
    tertiary = Sun,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = ElevatedPaper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF7F4ED),
    onSurfaceVariant = MutedInk,
    outline = Outline,
)
```

Keep the current serif display typography and 14/20/28 dp shape hierarchy.
Do not add dynamic color or a dark theme branch.

- [ ] **Step 4: Make launch and edge-to-edge system surfaces light**

In `MainActivity`, import `androidx.compose.ui.graphics.toArgb`, `Ink`, and `Paper`, then replace both dark styles with:

```kotlin
statusBarStyle = SystemBarStyle.light(Paper.toArgb(), Ink.toArgb())
navigationBarStyle = SystemBarStyle.light(Paper.toArgb(), Ink.toArgb())
```

Do not edit the picker or Create Document registration below this block.

Replace the window attributes in `themes.xml` with:

```xml
<item name="android:windowLightStatusBar">true</item>
<item name="android:windowLightNavigationBar">true</item>
<item name="android:statusBarColor">#FFF8E9</item>
<item name="android:navigationBarColor">#FFF8E9</item>
<item name="android:windowBackground">#FFF8E9</item>
```

Keep the current parent, font family, and action-mode overlay attributes.

- [ ] **Step 5: Run focused theme, lint, and assembly verification**

Run sequentially:

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.ui.theme.ScreenloomThemeTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

Expected: every command exits zero; `lintDebug` also runs the unchanged merged-manifest permission verifier.
System-bar icon legibility remains a manual Task 5 gate.

- [ ] **Step 6: Record the Task 3 checkpoint**

Suggested commit, only when commit authority exists:

```bash
git add app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt app/src/main/res/values/themes.xml app/src/androidTest/java/kr/donminzzi/screenloom/ui/theme/ScreenloomThemeTest.kt
git diff --cached --check
git diff --cached
git commit -m "feat(ui): adopt the sunlit application theme"
```

Without commit authority, leave the verified files unstaged and continue.

---

### Task 4: Rebuild the Empty State, Preview Stage, and Control Sheet

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt:60-360`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt:55-352`
- Modify: `app/src/main/res/values/strings.xml:3-49`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt:37-258`

**Interfaces:**

- Consumes: the application tokens from Task 3 and `PaletteId.colors()` from Task 1.
- Preserves: `EditorScreen`, `EditorControls`, and callback signatures; fixed tab order; `rememberSaveable` tab state; action dispatch; snackbar consumption; import/export locks; Split explanation; and 48 dp targets.
- Produces: Paper/Mint/Iris labels that still dispatch `PaletteId.Ink`, `PaletteId.Moss`, and `PaletteId.Violet`.

- [ ] **Step 1: Write failing palette-label and mapping tests**

Change both existing `"Ink"` expectations in `EditorScreenTest` to `"Paper"`.
Add this test:

```kotlin
@Test
fun renamedPaletteLabelsDispatchTheStablePaletteIdentifiers() {
    val actions = mutableListOf<EditorAction>()
    compose.setContent {
        ScreenloomTheme {
            EditorScreen(
                state = oneImageState(),
                onChooseImages = {},
                onRequestExport = {},
                onAction = actions::add,
                onMessageConsumed = {},
            )
        }
    }

    compose.onNodeWithText("Style").performClick()
    compose.onNodeWithText("Paper").assertIsDisplayed().assertSelected()
    compose.onNodeWithText("Mint").performScrollTo().assertIsDisplayed().performClick()
    compose.onNodeWithText("Iris").performScrollTo().assertIsDisplayed().performClick()

    compose.runOnIdle {
        assertEquals(EditorAction.SetPalette(PaletteId.Moss), actions[actions.lastIndex - 1])
        assertEquals(EditorAction.SetPalette(PaletteId.Violet), actions.last())
    }
}
```

Keep all existing empty-state, Split-disabled, copy-field, frame-label, export-lock, selected-semantics, and restoration tests unchanged.

- [ ] **Step 2: Run the editor screen test and verify RED**

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorScreenTest
```

Expected: `Paper`, `Mint`, and `Iris` are not found because the current resources still expose `Ink`, `Moss`, and `Violet`.

- [ ] **Step 3: Update only the approved display names and forward cue resources**

Apply these string values without changing resource identifiers:

```xml
<string name="palette_ink">Paper</string>
<string name="palette_moss">Mint</string>
<string name="palette_violet">Iris</string>
<string name="action_forward">→</string>
```

Keep the other user-facing copy unchanged.

- [ ] **Step 4: Replace the dark backdrop with static woven ribbons**

Remove the diagonal line loop and dark corner circles from `StudioBackdrop`.
Draw three quiet rounded ribbons against the Scaffold's Paper background using the Task 3 tokens:

```kotlin
@Composable
private fun StudioBackdrop() {
    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
        rotate(-12f, pivot = Offset(size.width * 0.78f, size.height * 0.11f)) {
            drawRoundRect(
                color = Cobalt.copy(alpha = 0.13f),
                topLeft = Offset(size.width * 0.34f, size.height * 0.045f),
                size = Size(size.width * 0.96f, size.height * 0.075f),
                cornerRadius = CornerRadius(size.height * 0.04f),
            )
        }
        rotate(13f, pivot = Offset(size.width * 0.18f, size.height * 0.76f)) {
            drawRoundRect(
                color = Coral.copy(alpha = 0.12f),
                topLeft = Offset(-size.width * 0.48f, size.height * 0.71f),
                size = Size(size.width * 0.98f, size.height * 0.07f),
                cornerRadius = CornerRadius(size.height * 0.04f),
            )
        }
        rotate(-10f, pivot = Offset(size.width * 0.5f, size.height * 0.92f)) {
            drawRoundRect(
                color = Sun.copy(alpha = 0.18f),
                topLeft = Offset(-size.width * 0.1f, size.height * 0.89f),
                size = Size(size.width * 1.2f, size.height * 0.055f),
                cornerRadius = CornerRadius(size.height * 0.03f),
            )
        }
    }
}
```

Add only the required `CornerRadius`, `Size`, and `rotate` imports, and remove imports orphaned by the deleted dark decorations.
The ribbons remain static and non-semantic.

- [ ] **Step 5: Brighten the empty-state sample and reserve Coral for the import action**

Change the sample document to `palette = PaletteId.Ink`, which now displays the Paper preset.
Change the two sample-image gradients to Cobalt/Ink and Coral/Mint combinations using the exact Task 3 token RGB values.

Give the `Choose screenshots` Button explicit action colors:

```kotlin
colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondary,
    contentColor = MaterialTheme.colorScheme.onSecondary,
)
```

After the existing label, add a weighted Spacer and a decorative `action_forward` Text whose semantics are cleared.
Show the cue only in the idle branch, not beside `Importing…`.
Keep the loading spinner, disabled behavior, callback, headline, body, privacy note, sample preview description, and vertical scrolling unchanged.

- [ ] **Step 6: Put the editor preview on a light stage**

Replace the transparent preview Surface in `EditorWorkspace` with a full-width stage that uses `ElevatedPaper`, `Outline`, a 28 dp shape, and a low ink-tinted elevation.
Inside it, use a Box with at least 18 dp vertical padding, two static low-alpha stage ribbons, and a centered poster constrained to `fillMaxWidth(0.62f).widthIn(max = 220.dp)`.

Apply the stage shadow explicitly instead of accepting the default black elevation:

```kotlin
val stageShape = RoundedCornerShape(28.dp)
Modifier.shadow(
    elevation = 18.dp,
    shape = stageShape,
    ambientColor = Ink.copy(alpha = 0.10f),
    spotColor = Ink.copy(alpha = 0.12f),
)
```

The stage structure is:

```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .shadow(
            elevation = 18.dp,
            shape = stageShape,
            ambientColor = Ink.copy(alpha = 0.10f),
            spotColor = Ink.copy(alpha = 0.12f),
        ),
    color = ElevatedPaper,
    shape = stageShape,
    border = BorderStroke(1.dp, Outline),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        PreviewStageBackdrop()
        PosterPreview(
            document = state.document,
            images = previewImages,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
    }
}
```

Keep the current memoized `previewImages`, `PosterPreview(document, images)` call, 9:16 modifier, content description, and clipping.
Do not change the `EditorWorkspace` scroll container or header metadata.

Use this private stage backdrop so the decoration has no semantics:

```kotlin
@Composable
private fun PreviewStageBackdrop() {
    ComposeCanvas(Modifier.fillMaxSize()) {
        rotate(-11f, pivot = center) {
            drawRoundRect(
                color = Cobalt.copy(alpha = 0.15f),
                topLeft = Offset(-size.width * 0.08f, size.height * 0.18f),
                size = Size(size.width * 1.16f, size.height * 0.18f),
                cornerRadius = CornerRadius(size.height * 0.1f),
            )
        }
        rotate(12f, pivot = center) {
            drawRoundRect(
                color = Sun.copy(alpha = 0.24f),
                topLeft = Offset(-size.width * 0.08f, size.height * 0.66f),
                size = Size(size.width * 1.16f, size.height * 0.15f),
                cornerRadius = CornerRadius(size.height * 0.08f),
            )
        }
    }
}
```

- [ ] **Step 7: Turn the controls into one elevated white sheet**

In `EditorControls`, wrap the tabs and `AnimatedContent` in one Surface using `MaterialTheme.colorScheme.surface`, a 24 dp shape, the theme outline, and 6 dp tonal or shadow elevation.
Apply 10 dp inner padding to the sheet.

Keep the neutral segmented track inside the sheet.
Use Ink/Paper for the selected tab, transparent/MutedInk for unselected tabs, and retain the existing `selected` semantics and 48 dp height.

Use these exact tab colors:

```kotlin
colors = ButtonDefaults.buttonColors(
    containerColor = if (selected) Ink else Color.Transparent,
    contentColor = if (selected) Paper else MutedInk,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = MutedInk.copy(alpha = 0.42f),
)
```

Change `OptionButton` to use `SelectedWash` plus a 2 dp Cobalt border when selected, white plus the quiet outline otherwise, and the current visible disabled treatment.
Apply the same selected wash and Cobalt outline to `PaletteButton` without changing its gradient preview or `PaletteId` dispatch.

Use this exact state split in both components:

```kotlin
val outlineWidth = if (selected) 2.dp else 1.dp
val outlineColor = if (selected) Cobalt else Outline
val containerColor = if (selected) SelectedWash else ElevatedPaper
```

Keep the `Layout`, `Copy`, and `Style` order, `rememberSaveable`, 150/90 ms content fade, text fields, frame Switch, horizontal palette scrolling, section labels, and all current labels and hints.

- [ ] **Step 8: Finish the action hierarchy**

Keep `Replace` and `Reset` as equal-width outlined actions.
Keep `Export PNG` full width and use the same explicit secondary/onSecondary colors as the import action.
Add the decorative resource-backed forward cue after the export label while leaving progress state copy and spinner behavior unchanged.
Show the cue only when `state.isExporting` is false.

Do not move reset into the control sheet and do not make export sticky; preserve the single predictable scroll flow.

- [ ] **Step 9: Run focused and combined UI verification and verify GREEN**

Run sequentially:

```bash
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorScreenTest
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.ui.theme.ScreenloomThemeTest,kr.donminzzi.screenloom.editor.EditorScreenTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

Expected: all commands exit zero; existing semantics and state-restoration tests remain green; the APK assembles without a new dependency or resource error.

- [ ] **Step 10: Record the Task 4 checkpoint**

Suggested commit, only when commit authority exists:

```bash
git add app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt app/src/main/res/values/strings.xml app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt
git diff --cached --check
git diff --cached
git commit -m "feat(ui): redesign the editor as a sunlit studio"
```

Without commit authority, leave the verified files unstaged and continue.

---

### Task 5: Run the Full Gate and Obtain Real Visual Approval

**Files:**

- Modify after verification: `docs/testing.md`
- Create ignored evidence: `app/build/outputs/manual-qa/sunlit-editorial/*`
- Verify without modification: all production and test files changed in Tasks 1-4

**Interfaces:**

- Consumes: the complete Sunlit Editorial implementation and the documented `flutter_emulator_2` environment.
- Produces: fresh automated results, exact APK digest, actual app and export captures, 1080 by 1920 dimension evidence, and an explicit human visual verdict.
- Preserves: the seven-scenario manual flow, no-data-loss cancellation behavior, rotation state, and merged-manifest permission allowlist.

- [ ] **Step 1: Run the complete automated gate sequentially**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
trunk check
git -C /Volumes/dongminyu/Development/01_personal/screenloom diff --check
```

Expected: every command exits zero.
`lintDebug` must include `verifyDebugManifestPermissions`, and the connected-test output must name only `emulator-5556` / `flutter_emulator_2`.
Do not infer a full pass from truncated output; preserve the terminal summaries and exit codes.

- [ ] **Step 2: Record the exact APK identity and install it cleanly**

```bash
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 uninstall kr.donminzzi.screenloom || true
adb -s emulator-5556 install app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -W -n kr.donminzzi.screenloom/.MainActivity
adb -s emulator-5556 shell dumpsys package kr.donminzzi.screenloom
```

Expected: install and launch succeed, no runtime permission dialog appears, and the package dump contains no Android platform, network, storage, camera, microphone, location, contacts, or advertising permission.
Record the literal SHA-256 output; do not reuse the digest currently documented in `docs/testing.md`.

- [ ] **Step 3: Create the ignored evidence directory and capture the empty state**

```bash
mkdir -p app/build/outputs/manual-qa/sunlit-editorial
adb -s emulator-5556 shell screencap -p /sdcard/screenloom-empty.png
adb -s emulator-5556 pull /sdcard/screenloom-empty.png app/build/outputs/manual-qa/sunlit-editorial/01-empty-state.png
adb -s emulator-5556 shell uiautomator dump /sdcard/screenloom-empty.xml
adb -s emulator-5556 pull /sdcard/screenloom-empty.xml app/build/outputs/manual-qa/sunlit-editorial/01-empty-state.xml
```

Inspect `01-empty-state.png` at original detail.
Require: warm Paper shell, dark status/navigation icons, readable wordmark and metadata, bright Paper sample poster, serif headline, visible woven ribbons, one Coral import action, and readable privacy note.

- [ ] **Step 4: Drive the one-image and two-image editor scenarios**

Use `uiautomator dump` before each interaction and resolve the nearest `clickable="true"` ancestor of a text node before asserting or tapping its bounds.
The Compose Text node's `enabled` value is not the Button state, and `selected` is not reliably exported by UIAutomator.
Use Compose instrumented tests, not the XML dump, as selected-semantics evidence.

If freshly pushed images are absent from Photo Picker, refresh the media provider exactly as documented:

```bash
adb -s emulator-5556 shell am force-stop com.google.android.providers.media.module
adb -s emulator-5556 shell content call --uri content://media/external/images/media --method scan_volume
```

Do not assume an `adb push` image appears first: Photo Picker orders these fixtures by `date_taken`, and pushed PNGs may have no EXIF date.
Select a visually recognizable grid item and confirm its content in the app instead of treating grid order as evidence.

Complete the existing manual flow with one image, then replace it with two images.
Require Focus and Stack with one image, disabled Split with its explanation, enabled Split with two images, immediate title/subtitle/palette/frame/shadow/layout updates, import and export cancellation without state loss, and rotation with the composition intact.

Before every screenshot or pixel comparison, return the editor to the same top-scroll position and fetch fresh preview bounds from a new UIAutomator dump.
Do not reuse bounds measured before keyboard dismissal or scrolling.

- [ ] **Step 5: Capture the required application states and six palette previews**

Capture at least these files with `screencap` plus `adb pull`, using the same commands as Step 3 with the corresponding filename:

```plaintext
02-layout-focus.png
03-layout-stack.png
04-layout-split.png
05-copy-tab.png
06-style-paper.png
07-style-cobalt.png
08-style-coral.png
09-style-mint.png
10-style-iris.png
11-style-sunrise.png
12-importing.png
13-split-disabled.png
14-exporting.png
15-rotated-state.png
```

Inspect each image at original detail.
Require: light preview stage, one coherent white control sheet, Ink selected tab, Cobalt selected outline, warm selected wash, readable disabled content, Coral terminal actions, accurate Paper/Mint/Iris display labels, unobscured screenshot content, and no clipped copy or controls.

- [ ] **Step 6: Export and reopen Paper, Cobalt, and Iris PNGs**

For each required palette, export through Create Document with a distinct title-derived filename, reopen the saved PNG on the emulator, then pull it from Downloads into the evidence directory.
Verify the local files with:

```bash
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-paper.png
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-cobalt.png
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-iris.png
file app/build/outputs/manual-qa/sunlit-editorial/export-paper.png app/build/outputs/manual-qa/sunlit-editorial/export-cobalt.png app/build/outputs/manual-qa/sunlit-editorial/export-iris.png
```

Expected: every file is a PNG with pixel width 1080 and pixel height 1920.
Inspect each export at original detail and compare it beside the corresponding preview.
Require matching gradient intent, sun and both ribbons, serif headline and sans subtitle intent, frame color, tinted shadow, screenshot draw order, and aspect-fitted placement.

- [ ] **Step 7: Obtain the human visual verdict**

Present the empty-state, editor, all-six-palette, and three reopened-export evidence to the operator.
Ask specifically about overall brightness, copy legibility, ribbon prominence, control hierarchy, imported screenshot dominance, and preview/export consistency.

Do not mark the redesign complete while visual approval is absent or while any requested visual correction remains open.
If the operator requests a correction, return to the owning task, add or update the narrow regression where practical, rerun that task's focused checks, then repeat the affected captures and the full automated gate.

- [ ] **Step 8: Record fresh evidence in `docs/testing.md`**

After the automated gate and human visual verdict are complete, append a dated Sunlit Editorial subsection that records:

- The literal APK SHA-256 printed in Step 2.
- The exact unit and instrumented pass/fail/skip counts reported by Gradle.
- The exact AVD, serial, API level, resolution, and commands used.
- The capture filenames from Steps 3, 5, and 6.
- The observed result of each of the seven manual scenarios.
- The exact dimensions of each reopened Paper, Cobalt, and Iris export.
- The operator's actual visual verdict, quoted or faithfully paraphrased without upgrading it.
- Any remaining limitation using `[PARTIAL]` or `[UNKNOWN]` instead of inference.

Do not copy prior APK hashes, automated counts, or approval statements into the new subsection.

- [ ] **Step 9: Run documentation and final worktree verification**

```bash
trunk check docs/specs/2026-08-12-screenloom-design.md docs/specs/2026-08-13-screenloom-sunlit-editorial-design.md docs/plans/2026-08-13-screenloom-sunlit-editorial-implementation.md docs/testing.md
git -C /Volumes/dongminyu/Development/01_personal/screenloom diff --check
git -C /Volumes/dongminyu/Development/01_personal/screenloom status --short
```

Expected: Trunk and `git diff --check` exit zero; only approved implementation, test, design, plan, and testing-document paths are changed; ignored manual captures do not appear in Git status.

- [ ] **Step 10: Record the Task 5 documentation checkpoint**

Suggested documentation commit, only when commit authority exists and after re-showing the current concern split:

```bash
git add docs/specs/2026-08-12-screenloom-design.md docs/specs/2026-08-13-screenloom-sunlit-editorial-design.md docs/plans/2026-08-13-screenloom-sunlit-editorial-implementation.md docs/testing.md
git diff --cached --check
git diff --cached
git commit -m "docs: record the sunlit editorial redesign"
```

Without commit authority, leave the documentation unstaged and report the exact verification evidence and changed paths.

## Completion Checklist

- [ ] The application shell is Paper-based with legible dark system icons and no near-black workspace remnants.
- [ ] Coral is limited to the two primary workflow actions and Cobalt communicates selection and focus.
- [ ] Empty state, preview stage, white control sheet, secondary actions, and export hierarchy match the approved design.
- [ ] Paper, Cobalt, Coral, Mint, Iris, and Sunrise use the exact approved gradient and foreground roles.
- [ ] Compose preview and Android Canvas export share the sun, ribbons, palette copy, serif/sans type intent, frame color, shadow tint, and layer order.
- [ ] `PaletteId`, `LayoutMode`, `PosterLayout`, aspect fitting, reducer state, picker flows, and export behavior remain unchanged.
- [ ] Unit tests, lint with the permission verifier, debug assembly, and all instrumented tests pass on fresh commands.
- [ ] The complete seven-scenario emulator flow passes on the current APK.
- [ ] Paper, Cobalt, and Iris exports reopen as exact 1080 by 1920 PNGs.
- [ ] Actual captures receive explicit human visual approval.
- [ ] `docs/testing.md` contains only fresh observed evidence for this redesign.
- [ ] No dependency, permission, unrelated refactor, staging, commit, push, merge, signing, or publication change occurred without explicit authority.
