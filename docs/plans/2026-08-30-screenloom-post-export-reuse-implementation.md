# Screenloom Post-Export Reuse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight post-export reuse loop with sharing, style-only persistence, and one-step Reset Undo.

**Architecture:** Keep `EditorStyle` Android-free and store it through one `SharedPreferences` adapter created in `MainActivity`.
Keep the export URI and reset snapshot in `EditorViewModel` memory only.
Use the existing system document URI directly for the Sharesheet with a temporary read grant.

**Tech Stack:** Kotlin, Jetpack Compose, Android `SharedPreferences`, Android Sharesheet intents, AndroidX Compose UI tests, JUnit 4.

**Spec:** `docs/specs/2026-08-30-screenloom-post-export-reuse-design.md`

## Global Constraints

- Preserve the application ID and namespace `kr.donminzzi.screenloom`.
- Preserve the offline single-module architecture and system-picker import and export flows.
- Add no account, network client, analytics, billing, database, dependency-injection framework, navigation framework, dependency, or Android platform permission.
- Persist only layout, palette, frame state, and shadow level.
- Do not persist source images, source URIs, title, subtitle, export URI, or undo state.
- Do not delete selected output documents after an export failure.
- Do not stage, commit, push, or create a pull request without explicit operator authority.

---

### Task 1: Add the Style Value and Storage Boundary

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`
- Create: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorStylePreferences.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorViewModelTest.kt`
- Create: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorStylePreferencesTest.kt`

**Interfaces:**

- Produces: `EditorStyle(layout: LayoutMode, palette: PaletteId, frameEnabled: Boolean, shadow: ShadowLevel)`.
- Produces: `EditorStylePreferences.load(): EditorStyle` and `EditorStylePreferences.save(EditorStyle)`.
- Produces: `EditorDocument.style(): EditorStyle` and `EditorStyle.toDocument(): EditorDocument`.

- [x] **Step 1: Write the failing style-only storage tests.**

Add one test that stores `Split`, `Violet`, disabled frame, and `Strong`, then loads the same value from a new adapter.
Add one test that makes `EditorViewModel` publish the changed style through an injected save callback without publishing title or subtitle.

- [x] **Step 2: Run the focused instrumented tests and verify RED.**

Run `ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorStylePreferencesTest`.
Expected: compilation fails because `EditorStylePreferences` does not exist.

- [x] **Step 3: Add the smallest style model and adapter.**

Add `EditorStyle` beside `EditorDocument`.
Use explicit `SharedPreferences` keys for the four style values.
Use the default style when stored enum names do not map to current entries.

- [x] **Step 4: Run the focused tests and verify GREEN.**

Run the Task 1 command again.
Expected: the adapter round trip and style-only callback assertions pass.

### Task 2: Preserve the Style Through the ViewModel Lifecycle

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorViewModelTest.kt`

**Interfaces:**

- Consumes: `EditorStylePreferences.load()` and `EditorStylePreferences.save(EditorStyle)`.
- Produces: `EditorViewModel(imageLoader, posterWriter, initialStyle, onStyleChanged)`.
- Produces: an empty initial document that contains the persisted style and no images or copy.

- [x] **Step 1: Write failing ViewModel lifecycle tests.**

Add a test that constructs a ViewModel with a non-default initial style and observes the same style in the empty document.
Add a test that changes a palette and verifies the callback receives only the new `EditorStyle`.

- [x] **Step 2: Run the focused ViewModel test and verify RED.**

Run `ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.editor.EditorViewModelTest`.
Expected: compilation fails because the constructor cannot receive the style dependencies.

- [x] **Step 3: Wire the adapter through manual construction.**

Load the style from application-context `SharedPreferences` in `MainActivity`.
Pass the loaded value and save callback to `EditorViewModel`.
Save only when layout, palette, frame, or shadow changes.

- [x] **Step 4: Run the focused ViewModel test and verify GREEN.**

Run the Task 2 command again.
Expected: existing import and export coordination tests plus the new lifecycle tests pass.

### Task 3: Add Successful-Export Actions and One-Step Reset Undo

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorModels.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorViewModel.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/editor/EditorScreen.kt`
- Modify: `app/src/main/java/kr/donminzzi/screenloom/ScreenloomApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ko/strings.xml`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorViewModelTest.kt`
- Modify: `app/src/androidTest/java/kr/donminzzi/screenloom/editor/EditorScreenTest.kt`

**Interfaces:**

- Produces: `EditorUiState.lastExportUri: Uri?` and `EditorUiState.canUndoReset: Boolean`.
- Produces: `EditorViewModel.createAnother()` and `EditorViewModel.undoReset()`.
- Produces: `EditorScreen` callbacks for sharing, Create another, and Reset Undo.

- [x] **Step 1: Write failing ViewModel behavior tests.**

Add one test that exposes the successful output URI only after `ExportResult.Success`.
Add one test that Create another recycles old bitmaps, clears copy and export URI, and preserves visual style.
Add one test that Undo restores the Reset pre-image document once and then becomes unavailable.

- [x] **Step 2: Write failing Compose behavior tests.**

Render a state with `lastExportUri` and assert visible `Share PNG` and `Create another` actions.
Render a reset snackbar state and assert its `Undo` action calls the supplied callback.

- [x] **Step 3: Run the focused tests and verify RED.**

Run the Task 2 ViewModel command and the `EditorScreenTest` class command.
Expected: compilation fails because the URI state, callbacks, actions, and undo behavior do not exist.

- [x] **Step 4: Implement the transient post-export and undo state.**

Set the output URI only on successful export.
Clear it on composition changes and Create another.
Recycle sources and create an empty document with the current style for Create another.
Keep one reset snapshot only until Undo or snackbar dismissal.

- [x] **Step 5: Render the actions and localized feedback.**

Add the two explicit post-export actions below the export action.
Add localized strings for Share, Create another, reset confirmation, and Undo.
Use the existing snackbar host for the one-step Undo action.

- [x] **Step 6: Run the focused tests and verify GREEN.**

Run the Task 3 commands again.
Expected: the URI, recycle, style, Undo, and Compose interaction assertions pass.

### Task 4: Launch the Sharesheet From the Successful SAF URI

**Files:**

- Modify: `app/src/main/java/kr/donminzzi/screenloom/MainActivity.kt`
- Create: `app/src/androidTest/java/kr/donminzzi/screenloom/MainActivityTest.kt`

**Interfaces:**

- Produces: `createSharePngIntent(uri: Uri): Intent`.
- Consumes: `EditorScreen` sharing callback with a successful export URI.

- [x] **Step 1: Write a failing share-intent test.**

Assert `ACTION_SEND`, MIME type `image/png`, `EXTRA_STREAM`, matching `ClipData`, and `FLAG_GRANT_READ_URI_PERMISSION` for a `content://` URI.

- [x] **Step 2: Run the focused share-intent test and verify RED.**

Run `ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=kr.donminzzi.screenloom.MainActivityTest`.
Expected: compilation fails because `createSharePngIntent` does not exist.

- [x] **Step 3: Implement the minimal Sharesheet intent.**

Build one `ACTION_SEND` intent from the exported URI.
Set the image MIME type, stream extra, clip data, and temporary read grant.
Launch `Intent.createChooser` only from the explicit UI callback.

- [x] **Step 4: Run the focused share-intent test and verify GREEN.**

Run the Task 4 command again.
Expected: the intent contract assertions pass.

### Task 5: Run the Complete Verification Gate

**Files:**

- Modify: `docs/testing.md`

- [x] **Step 1: Run the JVM suite.**

Run `ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest`.
Expected: the JVM suite exits zero.

- [x] **Step 2: Run lint and debug assembly sequentially.**

Run `ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug`.
Run `ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug` after lint ends.
Expected: both commands exit zero and lint runs `verifyDebugManifestPermissions`.

- [x] **Step 3: Run the complete connected suite.**

Run `ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest`.
Expected: all instrumented tests exit zero.

- [x] **Step 4: Run the manual reuse flow.**

Install the debug APK on the documented emulator only after announcing the device write.
Verify successful export, Sharesheet cancellation, Create another, one-step Reset Undo, one-image Split fallback, two-image Split retention, and a process restart that restores only visual style.

- [x] **Step 5: Record fresh observed evidence.**

Update `docs/testing.md` with the exact APK identity and the observed automated and manual results.

- [x] **Step 6: Run repository checks.**

Run `trunk check` and `git diff --check` from the isolated worktree.
Expected: both commands exit zero.

## Plan Review

The plan covers style persistence, ViewModel lifecycle, post-export behavior, reset recovery, Sharesheet security, localization, automated checks, and manual verification.
The plan does not add a dependency, provider, permission, database, or persistent project model.
The type names and callback ownership are consistent with the existing manual-construction architecture.
