# Screenloom Audit Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve every actionable finding from the 2026-08-13 full-plan audit while preserving Screenloom's existing product scope and preparing explicit evidence for the remaining human visual-quality gate.

**Architecture:** Keep the existing single-Activity, state-driven editor and the shared pure poster geometry.
Serialize imports at the ViewModel boundary, carry resource-backed export failures through the existing result type, animate shared image placements only in the Compose preview, and strengthen tests and Gradle verification without adding frameworks or dependencies.

**Tech Stack:** Kotlin with AGP built-in Kotlin 2.2.10, Kotlin Compose plugin 2.2.10, Jetpack Compose BOM 2026.06.00, Material 3, Activity Compose 1.13.0, Lifecycle 2.10.0, Android Gradle Plugin 9.3.0, Gradle 9.6.1, JDK 17, JUnit4, and AndroidX Compose UI tests.

## Global Constraints

- Repository root: `/Volumes/dongminyu/Development/01_personal/screenloom`.
- Application ID and namespace remain `kr.donminzzi.screenloom`.
- SDK configuration remains `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, and Build Tools 36.0.0.
- Screenloom remains a single-module, offline native Android application.
- Do not add accounts, networking, analytics, ads, billing, a database, dependency injection, or a navigation framework.
- Import remains `ActivityResultContracts.PickMultipleVisualMedia(2)` and export remains `ActivityResultContracts.CreateDocument("image/png")`.
- The only exported asset remains a 1080 by 1920 PNG.
- The only allowed merged `<uses-permission>` is `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- Keep user-facing copy and recoverable messages in string resources.
- Keep pure editor and layout behavior separate from Android bitmap and URI code.
- Preserve shared deterministic geometry between preview and export.
- Use no new dependency unless the existing Kotlin, JUnit4, AndroidX, Compose, and Gradle APIs cannot express a required check.
- Run only one heavy Android command at a time.
- Do not claim human visual quality, Play publication, release signing, or pricing is complete from automated verification.
- Every commit stages explicit paths and uses an English Conventional Commit message without Co-Author lines.

---

### Task 1: Serialize Imports and Preserve Export Failure Identity

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/media/PosterExporter.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorViewModelTest.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt`

**Interfaces:**

- Consumes: `EditorUiState.isImporting`, `EditorUiState.isExporting`, `ImageLoader.decode`, and `PosterWriter.export`.
- Produces: one active import at a time and `ExportResult.Failure(@StringRes val messageRes: Int)` propagated unchanged into `EditorUiState.message`.

- [ ] **Step 1: Add a failing overlapping-import test**

Add this behavior to `EditorViewModelTest` with a suspended first decode and a second request issued while the first is active:

```kotlin
@Test
fun overlappingImportRequestIsIgnoredWhileTheFirstDecodeIsActive() = runBlocking {
    val first = Uri.parse("content://screenloom/first")
    val second = Uri.parse("content://screenloom/second")
    val firstRelease = CompletableDeferred<Unit>()
    val decodedUris = mutableListOf<Uri>()
    val viewModel = editorViewModel(
        loader = ImageLoader { uri, _ ->
            decodedUris += uri
            if (uri == first) firstRelease.await()
            Result.success(testBitmap())
        },
    )

    viewModel.import(listOf(first))
    viewModel.awaitState { it.isImporting }
    viewModel.import(listOf(second))
    delay(100)

    assertEquals(listOf(first), decodedUris)
    firstRelease.complete(Unit)
    val state = viewModel.awaitState { !it.isImporting && it.images.size == 1 }
    assertEquals(first, state.images.single().uri)
}
```

- [ ] **Step 2: Run the ViewModel test and verify RED**

Run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorViewModelTest
```

Expected: the new test fails because the second URI is decoded while `isImporting` is true.

- [ ] **Step 3: Make import admission synchronous and exclusive**

Read the current state before launching the coroutine, reject a request when either operation is busy, and publish the busy state before `viewModelScope.launch`:

```kotlin
fun import(uris: List<Uri>) {
    val selected = uris.take(MaxImages)
    if (selected.isEmpty()) return
    val previous = mutableState.value
    if (previous.isImporting || previous.isExporting) return

    mutableState.value = previous.copy(isImporting = true, message = null)
    viewModelScope.launch {
        val results = selected.map { uri ->
            imageLoader.decode(uri, PreviewMaxDimension).map { bitmap ->
                ImportedImage(uri, bitmap)
            }
        }
        val images = results.mapNotNull { result -> result.getOrNull() }
        if (images.isEmpty()) {
            mutableState.value = previous.copy(message = R.string.import_failure)
            return@launch
        }

        previous.images.forEach { image -> recycle(image.bitmap) }
        mutableState.value = EditorUiState(
            document = EditorReducer.reduce(
                previous.document,
                EditorAction.SetImageCount(images.size),
            ),
            images = images,
            message = R.string.import_failure.takeIf { images.size < results.size },
        )
    }
}
```

Do not introduce an import queue, request token, mutex, or new state machine.

- [ ] **Step 4: Add a failing resource-backed export failure test**

Change the failure fake in `EditorViewModelTest` to return a distinct existing resource and require that exact resource to reach UI state:

```kotlin
writer = PosterWriter { _, _, _ -> ExportResult.Failure(R.string.import_failure) }

assertEquals(R.string.import_failure, state.message)
```

Expected: the assertion fails because the current ViewModel replaces every writer failure with `R.string.export_failure`.

- [ ] **Step 5: Make export failures resource-backed and propagate them unchanged**

Replace the string reason with a string resource identifier:

```kotlin
sealed interface ExportResult {
    data object Success : ExportResult

    data class Failure(@StringRes val messageRes: Int) : ExportResult
}
```

Return `ExportResult.Failure(R.string.export_failure)` from every recoverable `PosterExporter` path and map `is ExportResult.Failure -> result.messageRes` in `EditorViewModel`.

- [ ] **Step 6: Run focused and complete Task 1 verification**

Run sequentially:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorViewModelTest,kr.donminzzi.screenloom.render.PosterRendererTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
```

Expected: both commands exit zero.

- [ ] **Step 7: Commit Task 1**

```bash
git add app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt app/src/main/java/kr/donminzzi/screenloom/media/PosterExporter.kt app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorViewModelTest.kt app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt
git diff --cached --check
git commit -m "fix(editor): serialize media operations"
```

### Task 2: Complete Layout and Export-Failure Coverage

**Files:**

- Modify: `app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt`

**Interfaces:**

- Consumes: `PosterLayout.placements`, `PosterExporter.export`, and `ExportResult.Failure` from Task 1.
- Produces: exact one-image and two-image geometry coverage for every layout plus a real output-stream exception regression test.

- [ ] **Step 1: Add the exact geometry matrix**

Add assertions for these missing cases:

```kotlin
@Test
fun focusWithTwoImagesStillProducesTheSingleFocusPlacement() {
    assertEquals(
        listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)),
        PosterLayout.placements(output, LayoutMode.Focus, imageCount = 2),
    )
}

@Test
fun stackWithOneImageUsesTheForegroundStackPlacement() {
    assertEquals(
        listOf(PosterPlacement(130f, 680f, 650f, 1010f, -6f)),
        PosterLayout.placements(output, LayoutMode.Stack, imageCount = 1),
    )
}

@Test
fun splitWithOneImageFallsBackToFocusGeometry() {
    assertEquals(
        listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)),
        PosterLayout.placements(output, LayoutMode.Split, imageCount = 1),
    )
}

@Test
fun splitWithTwoImagesUsesBothExactPlacements() {
    assertEquals(
        listOf(
            PosterPlacement(75f, 600f, 440f, 1030f, -2f),
            PosterPlacement(565f, 650f, 440f, 1030f, 2f),
        ),
        PosterLayout.placements(output, LayoutMode.Split, imageCount = 2),
    )
}
```

These are coverage assertions over existing behavior and are expected to pass without production changes.

- [ ] **Step 2: Add an output-stream write exception test**

Use an `OutputStream` whose `write` method throws `IOException`:

```kotlin
@Test
fun exporterReportsWriteFailure() = runBlocking {
    val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
    val exporter = PosterExporter(
        PosterRenderer(),
        OutputStreamProvider {
            object : OutputStream() {
                override fun write(value: Int) = throw IOException("disk full")
            }
        },
    )

    val result = exporter.export(Uri.EMPTY, EditorDocument(imageCount = 1), listOf(source))

    assertEquals(ExportResult.Failure(R.string.export_failure), result)
}
```

- [ ] **Step 3: Run Task 2 verification**

Run sequentially:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests '*PosterLayoutTest'
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest
```

Expected: both commands exit zero.

- [ ] **Step 4: Commit Task 2**

```bash
git add app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt
git diff --cached --check
git commit -m "test(render): cover layout and write failures"
```

### Task 3: Verify Real Bounded Decode and Composite EXIF Orientation

**Files:**

- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/media/ImageDecoderInstrumentedTest.kt`
- Modify only if a regression test fails: `app/src/main/java/kr/donminzzi/screenloom/media/ImageDecoder.kt`

**Interfaces:**

- Consumes: `ImageDecoder.decode(Uri, maxDimension)` and AndroidX `ExifInterface` orientation metadata.
- Produces: device-level evidence that decoded output is bounded and that combined mirror-plus-rotation orientation is applied correctly.

- [ ] **Step 1: Add a real oversized bitmap decode test**

Create a 4096 by 1024 PNG in the target context cache, decode it with `maxDimension = 2048`, and require the longest decoded edge to be no greater than 2048:

```kotlin
@Test
fun oversizedImageDecodesWithinTheRequestedLongestEdge() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val source = Bitmap.createBitmap(4096, 1024, Bitmap.Config.ARGB_8888)
    val file = File(context.cacheDir, "image-decoder-oversized.png")
    file.outputStream().use { output ->
        assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
    }

    val decoded = ImageDecoder(context.contentResolver)
        .decode(Uri.fromFile(file), 2048)
        .getOrThrow()

    assertTrue(maxOf(decoded.width, decoded.height) <= 2048)
    decoded.recycle()
    source.recycle()
    file.delete()
}
```

- [ ] **Step 2: Add combined mirror-and-rotation EXIF tests**

Create a 400 by 200 JPEG with a red left half and blue right half, then cover both composite orientations.
AndroidX exposes them as a horizontal flip followed by `rotationDegrees`: `ORIENTATION_TRANSPOSE` produces red above blue, while `ORIENTATION_TRANSVERSE` produces blue above red.

```kotlin
assertEquals(200, decoded.width)
assertEquals(400, decoded.height)
val top = decoded.getPixel(100, 20)
val bottom = decoded.getPixel(100, 380)
assertTrue(Color.red(top) > Color.blue(top))
assertTrue(Color.blue(bottom) > Color.red(bottom))
```

Repeat the decode with `ExifInterface.ORIENTATION_TRANSVERSE` and invert the two color expectations.

- [ ] **Step 3: Run the decoder test class**

Run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.media.ImageDecoderInstrumentedTest
```

Expected: the command exits zero.
If either corrected composite orientation assertion fails, adjust only `ImageDecoder.oriented` to reproduce AndroidX's mirror-first then rotation transform and rerun until green.

- [ ] **Step 4: Commit Task 3**

```bash
git add app/src/androidTest/java/kr/donminzzi/screenloom/media/ImageDecoderInstrumentedTest.kt app/src/main/java/kr/donminzzi/screenloom/media/ImageDecoder.kt
git diff --cached --check
git commit -m "test(media): cover bounded EXIF decoding"
```

### Task 4: Animate Shared Poster Placements and Strengthen Compose Regression Tests

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterLayout.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterPreviewTest.kt`
- Modify: `app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt`

**Interfaces:**

- Consumes: original image order, `LayoutMode`, `PosterLayout.placements`, and Compose animation APIs.
- Produces: `PosterLayout.imagePlacements(IntSize, LayoutMode, Int): List<PosterImagePlacement>`, renderer/preview mapping through that shared helper, and spring-interpolated preview positions without changing export output.

- [ ] **Step 1: Add failing image-identity mapping tests**

Introduce the expected shared mapping type in tests:

```kotlin
data class PosterImagePlacement(
    val imageIndex: Int,
    val placement: PosterPlacement,
)
```

Require Stack with two images to map original image 1 to the rear positive-rotation placement and original image 0 to the foreground negative-rotation placement.
Require Split to preserve original image order and Focus to expose only image 0.

- [ ] **Step 2: Add a failing spring-position preview test**

In `PosterPreviewTest`, render two distinct images in Focus, disable automatic clock advance, change the document to Stack, advance a small amount, capture the intermediate frame, then advance until idle and capture the final frame:

```kotlin
compose.mainClock.autoAdvance = false
val focus = compose.onNodeWithTag("preview-capture").captureToImage()
compose.runOnIdle { document = document.copy(layout = LayoutMode.Stack) }
compose.mainClock.advanceTimeBy(32)
val intermediate = compose.onNodeWithTag("preview-capture").captureToImage()
compose.mainClock.advanceTimeBy(5_000)
val stack = compose.onNodeWithTag("preview-capture").captureToImage()

assertFalse(imagesAreEqual(focus, intermediate))
assertFalse(imagesAreEqual(intermediate, stack))
```

Expected: the test fails because the current preview jumps directly to Stack.

- [ ] **Step 3: Implement shared original-image placement mapping**

Add `PosterImagePlacement` and `PosterLayout.imagePlacements`:

```kotlin
fun imagePlacements(
    canvasSize: IntSize,
    layout: LayoutMode,
    imageCount: Int,
): List<PosterImagePlacement> {
    val placements = placements(canvasSize, layout, imageCount)
    return when {
        layout == LayoutMode.Stack && imageCount >= 2 -> listOf(
            PosterImagePlacement(imageIndex = 1, placement = placements[0]),
            PosterImagePlacement(imageIndex = 0, placement = placements[1]),
        )
        else -> placements.mapIndexed { index, placement ->
            PosterImagePlacement(imageIndex = index, placement = placement)
        }
    }
}
```

Use this helper in both `PosterRenderer` and `PosterPreview` instead of maintaining separate `orderedImages` logic.

- [ ] **Step 4: Implement spring interpolation in the Compose preview only**

Use `updateTransition` with a target containing `layout`, `imageCount`, and the constrained canvas size.
For each original image index, animate `left`, `top`, `width`, `height`, and `rotationDegrees` with a non-bouncy spring.
When an image enters or leaves Focus, use the Focus placement as its positional fallback and animate alpha with a short tween.

```kotlin
transition.animateFloat(
    transitionSpec = {
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    },
    label = "poster image $imageIndex left",
) { target -> target.placementFor(imageIndex).left }
```

Keep `PosterRenderer` deterministic and animation-free.
Use Compose animation APIs so the system animator-duration setting remains authoritative.

- [ ] **Step 5: Add selected-state and restoration tests**

In `EditorScreenTest`:

- Assert `Layout` is selected initially and `Style` becomes selected after clicking it.
- Assert the selected layout, palette, and shadow expose `SemanticsProperties.Selected = true`.
- Use `StateRestorationTester` to select `Style`, emulate saved-instance restoration, and assert `Style` remains selected and the palette controls remain visible.

- [ ] **Step 6: Add a representative preview/export parity test**

Render a Split, Moss, frame-disabled, Strong-shadow document with two solid-color images.
Compare the two screenshot centers `(295, 1115)` and `(785, 1165)` plus background samples `(108, 192)` and `(972, 1728)` in export coordinates.
Map each coordinate into the captured preview through `preview.width / 1080f` and `preview.height / 1920f`.
Require the red, green, blue, and alpha channels to differ by at most 8 at every sample and do not require full bitmap identity.

- [ ] **Step 7: Run Task 4 verification**

Run sequentially:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests '*PosterLayoutTest'
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorScreenTest,kr.donminzzi.screenloom.render.PosterPreviewTest,kr.donminzzi.screenloom.render.PosterRendererTest
```

Expected: both commands exit zero and the motion test observes a distinct intermediate frame.

- [ ] **Step 8: Commit Task 4**

```bash
git add app/src/main/java/kr/donminzzi/screenloom/render app/src/test/java/kr/donminzzi/screenloom/render app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt app/src/androidTest/java/kr/donminzzi/screenloom/render
git diff --cached --check
git commit -m "feat(render): animate shared poster placements"
```

### Task 5: Bind the Manifest Permission Allowlist to the Standard Gate

**Files:**

- Modify: `app/build.gradle.kts`
- Modify: `docs/testing.md`

**Interfaces:**

- Consumes: the generated debug merged manifest and the fixed application namespace.
- Produces: `verifyDebugManifestPermissions`, automatically required by `lintDebug`.

- [ ] **Step 1: Add the permission verification task**

Register a Gradle task that depends on `processDebugMainManifest`, reads the generated debug manifest, extracts every `<uses-permission android:name="…">`, and requires the exact set below:

```kotlin
val verifyDebugManifestPermissions by tasks.registering {
    group = "verification"
    description = "Verifies Screenloom's exact merged-manifest permission allowlist."
    dependsOn("processDebugMainManifest")

    doLast {
        val manifestCandidates = layout.buildDirectory
            .dir("intermediates/merged_manifests/debug")
            .get()
            .asFile
            .walkTopDown()
            .filter { candidate -> candidate.isFile && candidate.name == "AndroidManifest.xml" }
            .toList()
        check(manifestCandidates.size == 1) {
            "Expected one debug merged manifest but found ${manifestCandidates.size}: $manifestCandidates"
        }
        val manifestFile = manifestCandidates.single()
        val permissions = Regex("""<uses-permission\s+android:name="([^"]+)"""")
            .findAll(manifestFile.readText())
            .map { match -> match.groupValues[1] }
            .toSet()
        val allowedPermissions = setOf(
            "kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        check(permissions == allowedPermissions) {
            "Unexpected permissions $permissions in ${manifestFile.absolutePath}; expected $allowedPermissions"
        }
    }
}
```

The task must fail with a message containing the unexpected set and the manifest path.
Use Gradle and Kotlin standard APIs only.

- [ ] **Step 2: Wire the verifier into `lintDebug`**

Configure the existing Android lint task without changing the documented command:

```kotlin
tasks.matching { it.name == "lintDebug" }.configureEach {
    dependsOn(verifyDebugManifestPermissions)
}
```

- [ ] **Step 3: Verify the task and wiring**

Run sequentially:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew verifyDebugManifestPermissions
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug --dry-run
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
```

Expected: the verifier and lint exit zero, and the dry-run output includes `verifyDebugManifestPermissions` before `lintDebug`.

- [ ] **Step 4: Update the testing contract**

State in `docs/testing.md` that `lintDebug` now enforces the exact merged-manifest allowlist.
Retain the standalone command for focused diagnosis.

- [ ] **Step 5: Commit Task 5**

```bash
git add app/build.gradle.kts docs/testing.md
git diff --cached --check
git commit -m "build(android): enforce manifest permission boundary"
```

### Task 6: Run the Complete Automated Gate and Prepare Visual QA Evidence

**Files:**

- Generate ignored evidence under: `app/build/outputs/manual-qa/`
- Do not modify tracked files until the exact APK hash and results are known.

**Interfaces:**

- Consumes: all code and tests from Tasks 1 through 5.
- Produces: fresh unit, lint, instrumented, assembly, manifest, APK-hash, and visual-state evidence for documentation.

- [ ] **Step 1: Run the complete automated gate sequentially**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

Expected: every command exits zero.

- [ ] **Step 2: Verify final artifacts and repository state**

```bash
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
git status --short
```

Record the exact APK SHA-256 and test counts for Task 7.

- [ ] **Step 3: Capture representative visual states**

Install the exact debug APK on the existing `flutter_emulator` only.
Capture the empty state plus Focus, Stack, and Split poster states and the reopened exported PNG under `app/build/outputs/manual-qa/`.
Do not claim these captures establish human visual quality.

- [ ] **Step 4: Inspect evidence for mechanical regressions**

Confirm the captures contain the expected controls, selected states, two-image Split output, readable non-empty copy, and a reopened 1080 by 1920 PNG.
Record `[PARTIAL] human visual review still required` for subjective typography, color, spacing, and polish.

### Task 7: Reconcile Plan and Verification Documentation

**Files:**

- Modify: `README.md`
- Modify: `docs/testing.md`
- Modify: `docs/plans/2026-08-12-screenloom-implementation.md`
- Modify: `docs/plans/2026-08-13-screenloom-audit-remediation.md`

**Interfaces:**

- Consumes: exact commits and fresh evidence from Tasks 1 through 6.
- Produces: accurate implementation status, current APK hash, current test counts, and an explicit remaining human visual-review task.

- [ ] **Step 1: Reconcile the original plan from concrete evidence**

Mark implementation, test-presence, successful verification, and commit steps complete only where code, Git history, or fresh test output proves them.
Leave historical RED runs, the original frontend-design invocation, and subjective human visual QA unchecked when no evidence proves completion.
Add a short execution-status note linking this remediation plan so the remaining unchecked steps cannot be mistaken for missing product code.

- [ ] **Step 2: Refresh README and testing evidence**

Replace the stale README APK hash with the exact Task 6 hash.
Update `docs/testing.md` with the fresh unit and instrumented counts, lint and assembly result, APK hash, manifest allowlist result, and capture paths.
Keep the human visual-quality review explicitly pending.

- [ ] **Step 3: Mark remediation tasks from evidence**

Update this plan's checkboxes only after the matching command, commit, or artifact exists.
Leave the human visual-review acceptance item unchecked until the operator personally approves it.

- [ ] **Step 4: Validate documentation and repository state**

```bash
git diff --check
git status --short
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

Expected: documentation references the exact displayed hash, the only pending acceptance item is clearly stated, and no unrelated paths are modified.

- [ ] **Step 5: Commit Task 7**

```bash
git add README.md docs/testing.md docs/plans/2026-08-12-screenloom-implementation.md docs/plans/2026-08-13-screenloom-audit-remediation.md
git diff --cached --check
git commit -m "docs(screenloom): reconcile implementation evidence"
```

### Task 8: Obtain Human Visual Approval

**Files:**

- Review only: `app/build/outputs/manual-qa/**`
- Modify after approval: `docs/testing.md`
- Modify after approval: `docs/plans/2026-08-13-screenloom-audit-remediation.md`

**Interfaces:**

- Consumes: the exact Task 6 APK and captures.
- Produces: operator-confirmed visual quality or concrete follow-up findings.

- [ ] **Step 1: Operator reviews the exact captured states**

The operator checks typography, text legibility, palette rendering, screenshot crop, frame treatment, shadows, spacing, and overall visual polish.

- [ ] **Step 2: Record the result without overstating scope**

If approved, record the date, APK SHA-256, device, and reviewed states in `docs/testing.md` and mark this task complete.
If rejected, record each concrete visual issue as a new bounded task and leave this task unchecked.

- [ ] **Step 3: Commit the human QA result only after operator approval**

```bash
git add docs/testing.md docs/plans/2026-08-13-screenloom-audit-remediation.md
git diff --cached --check
git commit -m "docs(testing): record human visual approval"
```

## Final Exit Criteria

- The overlapping-import regression test fails before the fix and passes after it.
- `ExportResult.Failure.messageRes` reaches `EditorUiState.message` unchanged.
- All one-image and two-image layout geometries have exact assertions.
- Output-stream exceptions produce a recoverable export failure.
- Real oversized decode and combined mirror-plus-rotation EXIF behavior pass on the API 34 emulator.
- Poster layout changes show a spring-interpolated intermediate preview state while export remains deterministic.
- Selected semantics and Compose saved-state restoration are covered.
- `lintDebug` automatically enforces the exact merged-manifest permission allowlist.
- Unit tests, Android lint, instrumented tests, and debug assembly exit zero sequentially.
- README, testing documentation, and both plans agree with the exact APK hash and evidence.
- Human visual approval remains explicitly pending until the operator performs it.
