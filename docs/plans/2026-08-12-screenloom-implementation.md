# Screenloom Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a polished, offline native Android app that imports one or two screenshots, turns them into a configurable 9:16 promotional poster, and exports an exact 1080 by 1920 PNG.

**Architecture:** A single Compose Activity hosts one state-driven editor.
Pure Kotlin models and layout math are separated from Android bitmap decoding, Android Canvas export, and Compose preview code so geometry and editor behavior remain easy to test.
`MainActivity` owns system picker contracts, `EditorViewModel` owns the active composition, and small manually constructed services handle decoding and export without dependency injection.

**Tech Stack:** Kotlin with AGP built-in Kotlin 2.2.10, Kotlin Compose plugin 2.2.10, Jetpack Compose BOM 2026.06.00, Material 3, Activity Compose 1.13.0, Lifecycle 2.10.0, Android Gradle Plugin 9.3.0, Gradle 9.6.1, JDK 17, JUnit4, AndroidX Compose UI tests.

## Global Constraints

- Repository root: `/Volumes/dongminyu/Development/01_personal/screenloom`.
- Application ID and namespace: `kr.donminzzi.screenloom`.
- SDK configuration: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, Build Tools 36.0.0.
- Local Android SDK: `/Volumes/dongminyu/Android/sdk`.
- Local JDK: `/usr/bin/java`, OpenJDK 17.0.19.
- Validation AVD: `flutter_emulator`, API 34, 1080 by 1920.
- Run at most one emulator boot, Gradle build, or other heavy mobile job at a time.
- The app is English-first for the initial global paid release.
- The app declares no network, storage, camera, microphone, location, contacts, advertising, or analytics permissions.
- The app contains no account, server, database, dependency-injection framework, navigation framework, ads, subscriptions, paywall, or simulated purchase flow.
- Import uses `ActivityResultContracts.PickMultipleVisualMedia(2)` and export uses `ActivityResultContracts.CreateDocument("image/png")`.
- The only output format is a 1080 by 1920 PNG.
- Existing design contract: `docs/specs/2026-08-12-screenloom-design.md`.
- Installed Android testing guidance: `.agents/skills/testing-setup/SKILL.md`.
- Do not add Hilt, Robolectric, Jacoco, a screenshot-testing plugin, or a mocking library unless a concrete test cannot be written with JUnit4, Compose UI tests, and small fakes.
- Use TDD for business logic, layout geometry, renderer output, editor behavior, and Compose semantics.
- Every commit stages explicit paths and uses an English Conventional Commit message without Co-Author lines.

---

## Toolchain Evidence

- Local inspection on 2026-08-12 found Android platforms 31, 33, 34, 35, 36, and 36.1 plus Build Tools 35.0.0, 36.0.0, 36.1.0, and 37.0.0 under `/Volumes/dongminyu/Android/sdk`.
- Local `/usr/bin/java -version` reported OpenJDK 17.0.19.
- Local `flutter_emulator` configuration reported an API 34 arm64 image and a 1080 by 1920 display.
- [Android Gradle Plugin 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes) require Gradle 9.5.0 and JDK 17 and list Build Tools 36.0.0 as the default.
- [Android Gradle Plugin Maven metadata](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.3.0/gradle-9.3.0.pom) records built-in Kotlin Gradle Plugin 2.2.10.
- [Gradle 9.6.1 release notes](https://docs.gradle.org/9.6.1/release-notes.html) recommend upgrading to the 9.6.1 patch release.
- [Compose setup documentation](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) records Compose BOM 2026.06.00 and the Kotlin-matched Compose compiler plugin requirement.
- [Compose setup documentation](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) pairs Activity Compose 1.13.0 with Lifecycle ViewModel Compose 2.10.0 for the documented setup used here.

---

## File Map

### Repository and Build

- `.gitignore` excludes Gradle, Android Studio, local SDK, and build output.
- `README.md` describes the product, privacy promise, supported workflow, and build commands.
- `AGENTS.md` points contributors to the specification, plan, and testing document and records the project-specific verification gate.
- `docs/testing.md` records the local unit, lint, build, instrumented, and manual smoke commands.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, and `gradle/libs.versions.toml` define the single-module Android build.
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/**` provide the reproducible Gradle 9.6.1 wrapper.
- `app/build.gradle.kts` configures the app module, SDK levels, Compose, dependencies, and tests.

### Application

- `app/src/main/AndroidManifest.xml` declares only `MainActivity` and app metadata.
- `app/src/main/res/values/strings.xml` contains all user-facing copy.
- `app/src/main/res/values/themes.xml` supplies the launch theme.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` and `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` provide the Screenloom launcher icon.
- `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt` owns picker launchers and service construction.
- `app/src/main/java/kr/donminzzi/screenloom/ScreenloomApp.kt` binds the ViewModel state to the editor screen.
- `app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt` owns colors, type, shapes, and system-bar appearance.

### Editor

- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt` defines the immutable composition and editor state.
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorReducer.kt` applies validated synchronous editor actions.
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt` coordinates import, editing, export, and one-shot messages.
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt` renders the empty and populated editor states.
- `app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt` contains layout, copy, style, reset, and export controls.

### Rendering and Media

- `app/src/main/java/kr/donminzzi/screenloom/render/PosterLayout.kt` calculates deterministic poster placements.
- `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt` renders the scaled Compose preview from shared placements.
- `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt` renders the export bitmap with Android Canvas.
- `app/src/main/java/kr/donminzzi/screenloom/media/ImageDecoder.kt` decodes bounded software bitmaps from selected URIs.
- `app/src/main/java/kr/donminzzi/screenloom/media/PosterExporter.kt` writes the rendered bitmap to the user-selected output URI.

### Tests

- `app/src/test/java/kr/donminzzi/screenloom/editor/EditorReducerTest.kt` covers normalization and state transitions.
- `app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt` covers all layout geometry.
- `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt` verifies bitmap dimensions and PNG encoding on Android.
- `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt` verifies empty, populated, disabled, editable, and accessible Compose states.

---

### Task 1: Bootstrap the Reproducible Android Project

**Files:**

- Create: `.gitignore`
- Create: `README.md`
- Create: `AGENTS.md`
- Create: `docs/testing.md`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt`
- Include: `.agents/skills/testing-setup/**`
- Include: `docs/plans/2026-08-12-screenloom-implementation.md`

**Interfaces:**

- Consumes: JDK 17 at `/usr/bin/java`, Android SDK at `/Volumes/dongminyu/Android/sdk`, approved specification.
- Produces: a buildable single-module Compose application and Gradle wrapper commands used by every later task.

- [ ] **Step 1: Write the build configuration**

Create `settings.gradle.kts` with explicit repositories and one app module:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Screenloom"
include(":app")
```

Create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.3.0"
kotlin = "2.2.10"
compose-bom = "2026.06.00"
activity-compose = "1.13.0"
lifecycle = "2.10.0"
junit4 = "4.13.2"
androidx-test-junit = "1.3.0"
androidx-test-runner = "1.7.0"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
junit4 = { module = "junit:junit", version.ref = "junit4" }
androidx-test-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-test-junit" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test-runner" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

Create `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "kr.donminzzi.screenloom"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "kr.donminzzi.screenloom"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    testImplementation(libs.junit4)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

Do not apply `org.jetbrains.kotlin.android`; AGP 9.3 built-in Kotlin is enabled by default.

- [ ] **Step 2: Generate the Gradle 9.6.1 wrapper**

Run a temporary verified Gradle distribution because no system `gradle` executable is installed:

```bash
wrapper_tmp_dir=$(mktemp -d)
curl -fL https://services.gradle.org/distributions/gradle-9.6.1-bin.zip -o "$wrapper_tmp_dir/gradle-9.6.1-bin.zip"
unzip -q "$wrapper_tmp_dir/gradle-9.6.1-bin.zip" -d "$wrapper_tmp_dir"
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk "$wrapper_tmp_dir/gradle-9.6.1/bin/gradle" wrapper --gradle-version 9.6.1 --distribution-type bin
```

Verify `gradle/wrapper/gradle-wrapper.properties` points to `gradle-9.6.1-bin.zip` and keep the generated wrapper scripts unchanged.

- [ ] **Step 3: Add the smallest launchable Compose shell**

Create a manifest with only the exported launcher Activity and no application-declared `<uses-permission>` entries.
The merged manifest may retain AndroidX Core's signature-protected `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which is required for its pre-Android 13 receiver compatibility behavior and does not create a runtime permission prompt.
Create `MainActivity` with edge-to-edge Compose hosting:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(text = stringResource(R.string.app_name))
                }
            }
        }
    }
}
```

Add `Screenloom` as `app_name`, a non-action-bar launch theme, and a `.gitignore` that excludes `.gradle/`, `.idea/`, `local.properties`, `build/`, `app/build/`, captures, and signing material.

- [ ] **Step 4: Add repository guidance**

Create `README.md`, `AGENTS.md`, and `docs/testing.md` with these exact verification commands:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
```

State that Play pricing, signing, and publishing are outside this repository baseline.
Recommend `$setup-trunk` as a separate follow-up after the Android MVP passes its native gates; do not add Trunk during this plan.

- [ ] **Step 5: Verify the clean baseline**

Run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest lintDebug assembleDebug
```

Expected: all three tasks succeed and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 6: Commit the bootstrap**

```bash
git add .agents .gitignore AGENTS.md README.md app build.gradle.kts docs/plans docs/testing.md gradle gradle.properties gradlew gradlew.bat settings.gradle.kts
git diff --cached --check
git commit -m "chore(android): bootstrap Screenloom app"
```

### Task 2: Build the Editor Domain and Poster Geometry with TDD

**Files:**

- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorReducer.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/render/PosterLayout.kt`
- Create: `app/src/test/java/kr/donminzzi/screenloom/editor/EditorReducerTest.kt`
- Create: `app/src/test/java/kr/donminzzi/screenloom/render/PosterLayoutTest.kt`

**Interfaces:**

- Consumes: no application code beyond the Task 1 build.
- Produces: `EditorDocument`, `LayoutMode`, `PaletteId`, `ShadowLevel`, `EditorAction`, `EditorReducer.reduce(EditorDocument, EditorAction): EditorDocument`, and `PosterLayout.placements(IntSize, LayoutMode, Int): List<PosterPlacement>`.

- [ ] **Step 1: Write failing editor reducer tests**

Create tests that assert the exact limits and fallback behavior:

```kotlin
class EditorReducerTest {
    @Test
    fun titleAndSubtitleAreClampedToProductLimits() {
        val state = EditorDocument()

        val titled = EditorReducer.reduce(state, EditorAction.SetTitle("T".repeat(61)))
        val subtitled = EditorReducer.reduce(titled, EditorAction.SetSubtitle("S".repeat(101)))

        assertEquals(60, subtitled.title.length)
        assertEquals(100, subtitled.subtitle.length)
    }

    @Test
    fun removingSecondImageFallsBackFromSplitToFocus() {
        val split = EditorDocument(imageCount = 2, layout = LayoutMode.Split)

        val result = EditorReducer.reduce(split, EditorAction.SetImageCount(1))

        assertEquals(LayoutMode.Focus, result.layout)
        assertFalse(result.canUseSplit)
    }

    @Test
    fun resetKeepsImportedImagesAndRestoresVisualDefaults() {
        val edited = EditorDocument(
            imageCount = 2,
            layout = LayoutMode.Stack,
            title = "Launch better",
            palette = PaletteId.Coral,
            frameEnabled = false,
        )

        val result = EditorReducer.reduce(edited, EditorAction.Reset)

        assertEquals(2, result.imageCount)
        assertEquals(EditorDocument(imageCount = 2), result)
    }
}
```

- [ ] **Step 2: Write failing geometry tests**

```kotlin
class PosterLayoutTest {
    private val output = IntSize(width = 1080, height = 1920)

    @Test
    fun focusProducesOneCenteredPlacement() {
        val result = PosterLayout.placements(output, LayoutMode.Focus, imageCount = 1)

        assertEquals(listOf(PosterPlacement(190f, 580f, 700f, 1080f, 0f)), result)
    }

    @Test
    fun stackProducesTwoOpposingPlacements() {
        val result = PosterLayout.placements(output, LayoutMode.Stack, imageCount = 2)

        assertEquals(2, result.size)
        assertEquals(-6f, result[0].rotationDegrees)
        assertEquals(6f, result[1].rotationDegrees)
    }

    @Test
    fun splitNeverProducesMorePlacementsThanImages() {
        assertEquals(1, PosterLayout.placements(output, LayoutMode.Split, imageCount = 1).size)
        assertEquals(2, PosterLayout.placements(output, LayoutMode.Split, imageCount = 2).size)
    }
}
```

- [ ] **Step 3: Run the tests and verify the expected failure**

Run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests '*EditorReducerTest' --tests '*PosterLayoutTest'
```

Expected: compilation fails because the editor and layout types do not exist.

- [ ] **Step 4: Implement the minimal immutable domain**

Define exact product options:

```kotlin
enum class LayoutMode { Focus, Stack, Split }
enum class PaletteId { Ink, Cobalt, Coral, Moss, Violet, Sunrise }
enum class ShadowLevel { Soft, Medium, Strong }

data class EditorDocument(
    val imageCount: Int = 0,
    val layout: LayoutMode = LayoutMode.Focus,
    val title: String = "",
    val subtitle: String = "",
    val palette: PaletteId = PaletteId.Ink,
    val frameEnabled: Boolean = true,
    val shadow: ShadowLevel = ShadowLevel.Medium,
) {
    val canUseSplit: Boolean get() = imageCount >= 2
}

sealed interface EditorAction {
    data class SetImageCount(val count: Int) : EditorAction
    data class SetLayout(val layout: LayoutMode) : EditorAction
    data class SetTitle(val value: String) : EditorAction
    data class SetSubtitle(val value: String) : EditorAction
    data class SetPalette(val palette: PaletteId) : EditorAction
    data class SetFrameEnabled(val enabled: Boolean) : EditorAction
    data class SetShadow(val shadow: ShadowLevel) : EditorAction
    data object Reset : EditorAction
}
```

Implement `EditorReducer` as a total `when` expression, clamp image count to `0..2`, clamp copy lengths to 60 and 100 characters, reject `Split` with fewer than two images, and preserve `imageCount` on reset.
Implement `PosterPlacement(left, top, width, height, rotationDegrees)` and deterministic coordinates for all three layouts at 1080 by 1920, scaled proportionally for preview sizes.

- [ ] **Step 5: Run the targeted and complete unit suites**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests '*EditorReducerTest' --tests '*PosterLayoutTest'
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
```

Expected: both commands pass.

- [ ] **Step 6: Commit the domain**

```bash
git add app/src/main/java/kr/donminzzi/screenloom/editor app/src/main/java/kr/donminzzi/screenloom/render/PosterLayout.kt app/src/test
git diff --cached --check
git commit -m "feat(editor): add poster composition model"
```

### Task 3: Render Preview and Export PNG with TDD

**Files:**

- Create: `app/src/main/java/kr/donminzzi/screenloom/render/PosterRenderer.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/render/PosterPreview.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/media/PosterExporter.kt`
- Create: `app/src/androidTest/java/kr/donminzzi/screenloom/render/PosterRendererTest.kt`

**Interfaces:**

- Consumes: `EditorDocument`, `PaletteId`, `PosterLayout.placements`, Android `Bitmap`, `Uri`.
- Produces: `PosterRenderer.render(EditorDocument, List<Bitmap>, Int, Int): Bitmap`, `PosterPreview(EditorDocument, List<ImageBitmap>, Modifier)`, `OutputStreamProvider.open(Uri): OutputStream?`, and `PosterExporter.export(Uri, EditorDocument, List<Bitmap>): ExportResult`.

- [ ] **Step 1: Write failing renderer and exporter instrumented tests**

```kotlin
@RunWith(AndroidJUnit4::class)
class PosterRendererTest {
    @Test
    fun rendererCreatesExactPosterDimensions() {
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 90, 220))
        }

        val rendered = PosterRenderer().render(EditorDocument(imageCount = 1), listOf(source), 1080, 1920)

        assertEquals(1080, rendered.width)
        assertEquals(1920, rendered.height)
    }

    @Test
    fun exporterWritesDecodablePng() = runBlocking {
        val bytes = ByteArrayOutputStream()
        val provider = OutputStreamProvider { bytes }
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        val exporter = PosterExporter(PosterRenderer(), provider)

        val result = exporter.export(Uri.parse("content://screenloom/test"), EditorDocument(imageCount = 1), listOf(source))

        assertEquals(ExportResult.Success, result)
        val decoded = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
        assertEquals(1080, decoded.width)
        assertEquals(1920, decoded.height)
    }

    @Test
    fun exporterReportsUnavailableOutput() = runBlocking {
        val exporter = PosterExporter(PosterRenderer(), OutputStreamProvider { null })
        val source = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)

        val result = exporter.export(Uri.EMPTY, EditorDocument(imageCount = 1), listOf(source))

        assertEquals(ExportResult.Failure("Unable to save PNG"), result)
    }
}
```

- [ ] **Step 2: Run the renderer tests and verify the expected failure**

Boot only `flutter_emulator`, wait for `sys.boot_completed` to equal `1`, then run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest
```

Expected: compilation fails because the renderer and exporter do not exist.

- [ ] **Step 3: Implement shared visual constants and Android Canvas export**

Define all six palettes as ARGB `Int` pairs and keep the export renderer independent from Compose `Color`:

```kotlin
data class PosterPalette(val startColor: Int, val endColor: Int, val accentColor: Int)

fun PaletteId.colors(): PosterPalette = when (this) {
    PaletteId.Ink -> PosterPalette(0xFF0B1020.toInt(), 0xFF243B6B.toInt(), 0xFFFFD166.toInt())
    PaletteId.Cobalt -> PosterPalette(0xFF101B4D.toInt(), 0xFF3457D5.toInt(), 0xFFFFF4E6.toInt())
    PaletteId.Coral -> PosterPalette(0xFF351C35.toInt(), 0xFFF06A6A.toInt(), 0xFFFFE2B8.toInt())
    PaletteId.Moss -> PosterPalette(0xFF10251F.toInt(), 0xFF4D8061.toInt(), 0xFFE9D8A6.toInt())
    PaletteId.Violet -> PosterPalette(0xFF1C1338.toInt(), 0xFF7B5BC7.toInt(), 0xFFFFB4A2.toInt())
    PaletteId.Sunrise -> PosterPalette(0xFF3A1C2E.toInt(), 0xFFF28C54.toInt(), 0xFFFFE8C2.toInt())
}
```

`PosterRenderer` must draw the gradient, title, subtitle, subtle texture dots, screenshot shadow, optional neutral frame, rounded screenshot clipping, and imported bitmaps according to `PosterLayout`.
Use `Canvas.save`, rotation around each placement center, `Path.addRoundRect`, `Canvas.clipPath`, and `Paint.isAntiAlias = true`.
Use system sans-serif typefaces only; do not bundle a font.

- [ ] **Step 4: Implement Compose preview from the same model and placements**

`PosterPreview` must use `Canvas`, scale the 1080 by 1920 coordinate space to the available bounds, and apply the same palette, copy, frame, corner, shadow, and placement values as `PosterRenderer`.
Add the semantic description `Promotional poster preview` to the outer preview node and suppress decorative child semantics.

- [ ] **Step 5: Implement exporter outcomes**

```kotlin
fun interface OutputStreamProvider {
    fun open(uri: Uri): OutputStream?
}

sealed interface ExportResult {
    data object Success : ExportResult
    data class Failure(val reason: String) : ExportResult
}
```

`PosterExporter.export` must render 1080 by 1920 on `Dispatchers.Default`, open the selected URI through the provider, compress PNG at quality 100, flush and close the stream with `use`, recycle the export bitmap, and return `Failure("Unable to save PNG")` for a missing stream or thrown exception.

- [ ] **Step 6: Run renderer tests and native checks**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.render.PosterRendererTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest lintDebug
```

Expected: all commands pass and the renderer test decodes an exact 1080 by 1920 PNG.

- [ ] **Step 7: Commit rendering**

```bash
git add app/src/main/java/kr/donminzzi/screenloom/media/PosterExporter.kt app/src/main/java/kr/donminzzi/screenloom/render app/src/androidTest/java/kr/donminzzi/screenloom/render
git diff --cached --check
git commit -m "feat(render): export promotional posters"
```

### Task 4: Add Bounded Import and Editor Coordination

**Files:**

- Create: `app/src/main/java/kr/donminzzi/screenloom/media/ImageDecoder.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`
- Modify: `app/src/test/java/kr/donminzzi/screenloom/editor/EditorReducerTest.kt`

**Interfaces:**

- Consumes: `EditorReducer`, `PosterExporter`, `ContentResolver`, selected `Uri` values.
- Produces: `ImportedImage(uri: Uri, bitmap: Bitmap)`, `EditorUiState`, `ImageDecoder.decode(Uri, Int): Result<Bitmap>`, and ViewModel methods `import(List<Uri>)`, `dispatch(EditorAction)`, `export(Uri)`, and `consumeMessage()`.

- [ ] **Step 1: Add failing bounded-decoding and editor-state tests**

Extract pure sampling logic into `ImageDecoder.calculateInSampleSize(width, height, maxDimension)` and test it without Android I/O:

```kotlin
@Test
fun sampleSizeKeepsDecodedLongestEdgeNearLimit() {
    assertEquals(4, ImageDecoder.calculateInSampleSize(width = 8000, height = 4000, maxDimension = 2048))
    assertEquals(1, ImageDecoder.calculateInSampleSize(width = 1080, height = 1920, maxDimension = 2048))
}

@Test
fun importCountNeverExceedsTwo() {
    val state = EditorReducer.reduce(EditorDocument(), EditorAction.SetImageCount(8))

    assertEquals(2, state.imageCount)
}
```

- [ ] **Step 2: Run the targeted test and verify the expected failure**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest --tests '*EditorReducerTest'
```

Expected: compilation fails because `ImageDecoder.calculateInSampleSize` does not exist.

- [ ] **Step 3: Implement safe two-pass bitmap decoding**

`ImageDecoder.decode` must open the selected URI once with `BitmapFactory.Options.inJustDecodeBounds = true`, calculate a power-of-two sample size, reopen it for the actual decode, set `inPreferredConfig = Bitmap.Config.ARGB_8888`, and fail with `Result.failure` when either stream or bitmap is unavailable.
The preview decode limit is 2048 pixels on the longest edge.
Do not persist URI permissions or store imported bytes on disk.

Use this exact sampling helper:

```kotlin
companion object {
    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var longestEdge = maxOf(width, height)
        while (longestEdge > maxDimension) {
            sampleSize *= 2
            longestEdge /= 2
        }
        return sampleSize
    }
}
```

- [ ] **Step 4: Implement ViewModel coordination**

```kotlin
data class ImportedImage(val uri: Uri, val bitmap: Bitmap)

data class EditorUiState(
    val document: EditorDocument = EditorDocument(),
    val images: List<ImportedImage> = emptyList(),
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val message: String? = null,
)
```

`import` must decode at most two URIs on `viewModelScope`, keep successfully decoded images in picker order, retain the previous composition when all selected images fail, update `imageCount`, and expose `Unable to read that image` when any selected image fails.
`dispatch` must route only synchronous actions through `EditorReducer`.
`export` must reject empty compositions, prevent overlapping exports, call `PosterExporter`, and expose either `PNG saved` or the exporter failure reason.
`onCleared` must recycle imported bitmaps that are not already recycled.

Expose immutable state and the exact public operations:

```kotlin
class EditorViewModel(
    private val imageDecoder: ImageDecoder,
    private val exporter: PosterExporter,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = mutableState.asStateFlow()

    fun import(uris: List<Uri>) {
        val selected = uris.take(2)
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val previous = mutableState.value
            mutableState.update { it.copy(isImporting = true, message = null) }
            val results = selected.map { uri ->
                imageDecoder.decode(uri, maxDimension = 2048).map { bitmap -> ImportedImage(uri, bitmap) }
            }
            val images = results.mapNotNull { result -> result.getOrNull() }
            if (images.isEmpty()) {
                mutableState.value = previous.copy(message = "Unable to read that image")
                return@launch
            }
            previous.images.forEach { image -> if (!image.bitmap.isRecycled) image.bitmap.recycle() }
            mutableState.value = EditorUiState(
                document = EditorReducer.reduce(previous.document, EditorAction.SetImageCount(images.size)),
                images = images,
                message = if (images.size == results.size) null else "Unable to read that image",
            )
        }
    }

    fun dispatch(action: EditorAction) {
        mutableState.update { state -> state.copy(document = EditorReducer.reduce(state.document, action)) }
    }

    fun export(uri: Uri) {
        val current = mutableState.value
        if (current.images.isEmpty()) {
            mutableState.update { it.copy(message = "Choose a screenshot first") }
            return
        }
        if (current.isExporting) return
        viewModelScope.launch {
            mutableState.update { it.copy(isExporting = true, message = null) }
            val result = exporter.export(uri, current.document, current.images.map(ImportedImage::bitmap))
            mutableState.update {
                it.copy(
                    isExporting = false,
                    message = if (result == ExportResult.Success) "PNG saved" else (result as ExportResult.Failure).reason,
                )
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        mutableState.value.images.forEach { image -> if (!image.bitmap.isRecycled) image.bitmap.recycle() }
        super.onCleared()
    }
}
```

- [ ] **Step 5: Run domain tests and Android lint**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest lintDebug
```

Expected: unit tests and lint pass.

- [ ] **Step 6: Commit import and coordination**

```bash
git add app/src/main/java/kr/donminzzi/screenloom/editor app/src/main/java/kr/donminzzi/screenloom/media/ImageDecoder.kt app/src/test
git diff --cached --check
git commit -m "feat(editor): coordinate screenshot imports"
```

### Task 5: Build the Polished Compose Editor and System Picker Flow

**Files:**

- Create: `app/src/main/java/kr/donminzzi/screenloom/ScreenloomApp.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/ui/theme/ScreenloomTheme.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorControls.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt`

**Interfaces:**

- Consumes: `EditorUiState`, `EditorAction`, `PosterPreview`, `EditorViewModel.import`, `EditorViewModel.export`.
- Produces: `ScreenloomApp(viewModel)`, `EditorScreen(state, onChooseImages, onRequestExport, onAction, onMessageConsumed)`, and the complete user journey.

- [ ] **Step 1: Invoke the frontend-design skill and keep its output within the approved visual contract**

Use the skill to refine spacing, hierarchy, typography, motion, empty-state composition, and control styling.
Reject extra pages, onboarding carousels, generic gradients, floating decorative blobs, and new product features.

- [ ] **Step 2: Write failing Compose behavior tests**

```kotlin
@RunWith(AndroidJUnit4::class)
class EditorScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyStateOffersScreenshotImport() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = EditorUiState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText("Choose screenshots").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("Promotional poster preview").assertIsDisplayed()
    }

    @Test
    fun splitIsDisabledWithOneImage() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText("Split").assertIsNotEnabled()
        compose.onNodeWithText("Export PNG").assertIsEnabled()
    }
}
```

`oneImageState()` creates a 320 by 640 solid-color test bitmap and returns `EditorUiState(EditorDocument(imageCount = 1), listOf(ImportedImage(Uri.EMPTY, bitmap)))`.

- [ ] **Step 3: Run UI tests and verify the expected failure**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorScreenTest
```

Expected: compilation fails because the screen and theme do not exist.

- [ ] **Step 4: Implement the Screenloom design system**

Use a near-black `#090B10` background, warm text `#F5F1E8`, cobalt `#5B7CFA`, coral `#FF7A6E`, rounded 20 dp control surfaces, 14 dp chips, 48 dp minimum targets, and Material typography with explicit title, body, and label weights.
Apply edge-to-edge insets with `WindowInsets.safeDrawing`, use short `AnimatedContent` fades for control tabs, and respect the platform animator scale by relying on Compose animation APIs.
Do not draw blur behind controls because RenderEffect support and performance vary across the supported API range.

Use one dark color scheme instead of adding an unrequested theme preference:

```kotlin
private val ScreenloomColors = darkColorScheme(
    primary = Color(0xFF5B7CFA),
    secondary = Color(0xFFFF7A6E),
    background = Color(0xFF090B10),
    surface = Color(0xFF151922),
    surfaceVariant = Color(0xFF202633),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F1E8),
    onSurface = Color(0xFFF5F1E8),
)

@Composable
fun ScreenloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScreenloomColors,
        typography = Typography(
            displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
```

- [ ] **Step 5: Implement empty and editor states**

The empty state displays a generated sample poster made from gradient and placeholder cards, the Screenloom wordmark, `Store-ready visuals, woven in seconds.`, and `Choose screenshots`.
The editor displays the 9:16 preview, `Layout`, `Copy`, and `Style` tabs, the exact product controls, `Replace`, `Reset`, and `Export PNG`.
Use string resources for all copy and semantic labels.
Disable `Split` with one image and add the state description `Add a second screenshot to use Split`.
Disable editing and export only while the corresponding ViewModel operation is active.

Keep the route stateless and callback-driven:

```kotlin
@Composable
fun EditorScreen(
    state: EditorUiState,
    onChooseImages: () -> Unit,
    onRequestExport: (String) -> Unit,
    onAction: (EditorAction) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            onMessageConsumed()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        if (state.images.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding), onChooseImages = onChooseImages)
        } else {
            EditorWorkspace(
                state = state,
                modifier = Modifier.padding(padding),
                onChooseImages = onChooseImages,
                onRequestExport = onRequestExport,
                onAction = onAction,
            )
        }
    }
}
```

Define `EmptyState` and `EditorWorkspace` as private Composables in `EditorScreen.kt` and move the tab rows, text fields, palette swatches, shadow controls, and action row into public-free Composables in `EditorControls.kt`.

- [ ] **Step 6: Wire real system picker contracts in MainActivity**

Register these launchers before `setContent`:

```kotlin
val imagePicker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(2)) { uris ->
    viewModel.import(uris)
}
val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
    uri?.let(viewModel::export)
}
```

Construct `ImageDecoder`, `PosterRenderer`, `OutputStreamProvider` backed by `contentResolver.openOutputStream(uri)`, `PosterExporter`, and the ViewModel through a manual `ViewModelProvider.Factory`.
Use a sanitized title for the suggested filename and fall back to `screenloom-poster.png`.
Picker cancellation must perform no ViewModel action.

- [ ] **Step 7: Run UI tests, unit tests, lint, and assembly**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorScreenTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest lintDebug assembleDebug
```

Expected: both commands pass.

- [ ] **Step 8: Commit the complete editor flow**

```bash
git add app/src/main/java/kr/donminzzi/screenloom app/src/main/res/values app/src/androidTest/java/kr/donminzzi/screenloom/editor
git diff --cached --check
git commit -m "feat(ui): build Screenloom editor experience"
```

### Task 6: Add Identity, Documentation, and Final Device Verification

**Files:**

- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `README.md`
- Modify: `docs/testing.md`
- Modify: `AGENTS.md`

**Interfaces:**

- Consumes: the complete app from Tasks 1 through 5.
- Produces: a branded debug APK, verified manifest/privacy boundary, documented commands, and reproducible manual evidence.

- [ ] **Step 1: Add a minimal woven-frame launcher icon**

Create a vector foreground containing two interlocking rounded screenshot frames and a single diagonal cobalt-to-coral visual crossing on the ink background.
Use only vector paths and solid colors; do not add a raster asset or generated marketing image.
Reference adaptive and round icons from the manifest.

Use a 96 by 96 vector viewport with these exact foreground paths:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="96"
    android:viewportHeight="96">
    <path
        android:pathData="M25,18 H69 A9,9 0,0 1,78 27 V67 A9,9 0,0 1,69 76 H25 A9,9 0,0 1,16 67 V27 A9,9 0,0 1,25 18 Z"
        android:strokeColor="#F5F1E8"
        android:strokeWidth="5"
        android:fillColor="@android:color/transparent" />
    <path
        android:pathData="M30,28 H72 A8,8 0,0 1,80 36 V70 A8,8 0,0 1,72 78 H30 A8,8 0,0 1,22 70 V36 A8,8 0,0 1,30 28 Z"
        android:strokeColor="#5B7CFA"
        android:strokeWidth="5"
        android:fillColor="@android:color/transparent" />
    <path
        android:pathData="M24,68 L72,30"
        android:strokeColor="#FF7A6E"
        android:strokeWidth="6"
        android:strokeLineCap="round" />
</vector>
```

- [ ] **Step 2: Run the complete automated gate sequentially**

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

Expected: every command exits zero.

- [ ] **Step 3: Verify the manifest privacy boundary**

Run:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew processDebugMainManifest
merged_manifest=$(find app/build/intermediates/merged_manifests/debug -name AndroidManifest.xml -print -quit)
test "$(rg -c '<uses-permission' "$merged_manifest")" = "1"
rg -n 'uses-permission android:name="kr\.donminzzi\.screenloom\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"' "$merged_manifest"
if rg -n 'uses-permission android:name="android\.' "$merged_manifest"; then exit 1; fi
```

Expected: the merged manifest contains exactly the AndroidX Core signature-protected application permission and no `android.*` platform permission.
Treat any additional match as a release blocker and trace the contributing manifest before continuing.

- [ ] **Step 4: Perform the manual emulator smoke flow**

Install the exact built APK on `flutter_emulator`:

```bash
/Volumes/dongminyu/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/Volumes/dongminyu/Android/sdk/platform-tools/adb shell am force-stop kr.donminzzi.screenloom
/Volumes/dongminyu/Android/sdk/platform-tools/adb shell monkey -p kr.donminzzi.screenloom -c android.intent.category.LAUNCHER 1
```

Verify these scenarios in order:

1. A clean launch shows no permission prompt and displays the sample poster.
2. One screenshot imports and enables `Focus`, `Stack`, editing, and export while `Split` remains disabled.
3. Replacing with two screenshots enables `Split`.
4. Title, subtitle, palette, frame, shadow, and layout changes update the preview immediately.
5. Cancelling import and export preserves the active composition.
6. Rotation preserves the active composition while the process remains alive.
7. Export saves a PNG that reopens and reports 1080 by 1920 pixels.

Record the exact tested APK SHA-256 in `docs/testing.md` using:

```bash
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 5: Update user and contributor documentation**

Document the three layouts, six palettes, privacy boundary, output size, build commands, test commands, APK path, and manual smoke result.
Do not describe Play pricing, signing, or publication as completed.

- [ ] **Step 6: Commit polish and verification documentation**

```bash
git add AGENTS.md README.md app/src/main/AndroidManifest.xml app/src/main/res docs/testing.md
git diff --cached --check
git commit -m "chore(release): polish Screenloom MVP"
```

- [ ] **Step 7: Verify final repository state**

```bash
git status --short
git log --oneline --decorate -6
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

Expected: worktree is clean, the planned concern-split commits are present, and the APK hash matches `docs/testing.md`.

## Execution Notes

- Keep the renderer and UI visually aligned through shared palette and placement values, not through screenshot capture of the Compose hierarchy.
- If AGP 9.3.0 and the Compose plugin reveal a concrete compatibility error, stop after capturing the exact error and verify the current official compatibility contract before changing versions.
- If the API 34 AVD cannot run instrumentation, diagnose that AVD before creating or downloading another system image.
- If the Photo Picker cannot access a fixture on the emulator, push exactly one generated PNG to the emulator and retry the manual import; do not request storage permission as a workaround.
- Publishing, pricing, signed release bundles, Play Billing, analytics, remote crash reporting, cloud sync, localization, tablet optimization, and Trunk integration remain separate follow-up scopes.
