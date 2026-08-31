# Testing Screenloom

## Environment

- JDK 17.
- Android SDK at `~/Library/Android/sdk`, the Android Studio default location on macOS.
- Compile and target SDK 36.
- Instrumented test AVD `flutter_emulator`, API 34, 1080 by 1920, run headless (see below).

`local.properties` is gitignored, so a fresh clone must export the SDK path before running any live command in this document:

```bash
export ANDROID_SDK_ROOT=~/Library/Android/sdk
```

The dated QA sections quote the commands as they were run on those days, including the SDK path of the time, and are not affected by this.

## Emulator

Instrumented runs use the low-memory headless `flutter_emulator` AVD.
Booting a second instance of the same AVD is refused outright (`Running multiple emulators with the same AVD is an experimental feature`), and without `ANDROID_SERIAL` Gradle installs and runs on **every** attached device.

```bash
$ANDROID_SDK_ROOT/emulator/emulator -avd flutter_emulator \
  -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim \
  -no-snapshot-load -no-snapshot-save -memory 2048 -cores 2 -port 5556 &
adb -s emulator-5556 wait-for-device
ANDROID_SERIAL=emulator-5556 ./gradlew connectedDebugAndroidTest
```

`-gpu swiftshader_indirect` is load-bearing, not decoration.
Dropping it once left the instance stuck at `offline` for 16 minutes with a live qemu process; the same AVD booted in 30 seconds with the flag restored.
`-no-snapshot-load` keeps a stale snapshot from being restored into that same state.

`ANDROID_SERIAL` is what scopes the run; it is verified working — the task output names only `flutter_emulator`.
On a freshly booted AVD the system photo picker keeps its own index, so pushed images show as "No photos or videos" until the media provider is restarted:

```bash
adb -s emulator-5556 shell am force-stop com.google.android.providers.media.module
adb -s emulator-5556 shell content call --uri content://media/external/images/media --method scan_volume
```

## Automated Checks

Run each heavy Android command sequentially.

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

Unit tests protect editor normalization and deterministic layout geometry.

> [!IMPORTANT]
> `EditorScreenTest` resolves translated selectors from the same active resources as the UI and includes explicit Korean-locale coverage for the empty-state action and dynamic preview description.
> The connected suite does not require the emulator to be forced to English.

Inspect the per-app override when diagnosing a locale-specific failure, and clear it only when the test scenario requires the device default:

```bash
adb -s emulator-5556 shell cmd locale get-app-locales kr.donminzzi.screenloom
adb -s emulator-5556 shell cmd locale set-app-locales kr.donminzzi.screenloom --locales ''
```

Instrumented tests protect Android image decoding, bitmap export, ViewModel coordination, and Compose semantics.

## Post-Export Reuse Verification, 2026-08-30

The debug APK SHA-256 was `1a9241467a0cd3c5cb53ac679c043f447b382624edef7a87a8f03a65b3d45f61`.
The fresh JVM report contained 31 tests with zero failures, errors, or skips.
The fresh connected report contained 63 tests with zero failures, errors, or skips on the API 34 `flutter_emulator` AVD.
`lintDebug` ran `verifyDebugManifestPermissions` and exited zero.
`assembleDebug` exited zero.

The ADB-driven manual flow used two local test screenshots through the system Photo Picker.
With one imported image, a Split tap left the preview on Focus.
With two imported images, Split changed the preview description to two screenshots.
The manual flow changed the palette to Coral and the shadow to Strong.
The real Downloads export created `screenloom-rebased.png` with 205,954 bytes.
`file` reported `PNG image data, 1080 x 1920, 8-bit/color RGB, non-interlaced`.
`sips` reported a pixel width of 1080 and a pixel height of 1920.
The exported PNG SHA-256 was `661e51b138e478e1656f89a2f8ca129d0b04c8afe69c0bae2a84d1f939d27dc7`.

The system Sharesheet opened from `Share PNG`.
Back cancellation returned to the same Split and Coral composition with both post-export actions present.
`Create another` returned to the empty state.
The inspected preference file contained only `layout`, `palette`, `frame_enabled`, and `shadow` with the selected style values.
After a process restart, Screenloom displayed the empty state.
After two images were selected again, the preview restored the Split and Coral style.
Reset displayed the default Focus and Paper preview with an Undo action.
Undo restored the Split and Coral preview.

[PARTIAL] This pass verifies state and system surfaces with ADB and UIAutomator.
It does not replace a human visual or assistive-technology review of the new post-export actions.

## Verified Baseline

The automated gate was last run on 2026-08-24 against the headless API 34 `flutter_emulator` AVD at 1080 by 1920, on the `fix/product-readiness` branch.
The result contained 31 passing unit tests and 49 passing instrumented tests with zero failures, errors, or skips.
`lintDebug` and `assembleDebug` both exited zero, and `lintDebug` carried `verifyDebugManifestPermissions` with it.
The complete 49-test instrumented suite passed in separate English-default and `ko-KR` per-app locale runs.

The debug APK SHA-256 for that run is `e2f11d3c659e28fda1896716726dfde28e2ad01267e19b8ca6755493d7771f94`.

The 2026-08-13 automated baseline, for reference, was 28 unit and 42 instrumented tests against APK SHA-256 `f7c7dc7a1e73f25eb04c50463fc317023f92d814f7876bfa4d8d858826ea99b4`.
The 2026-08-12 automated baseline, for reference, was 17 unit and 29 instrumented tests against APK SHA-256 `a9f99a93cb5c4fb25bf7ab98dca335bc4a745d20df0b9d359e89ae986db246da`.

## Manifest Privacy Boundary

The merged manifest requests no Android platform, network, storage, camera, microphone, location, contacts, or advertising permissions.
AndroidX Core 1.18.0 contributes only `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-protected permission scoped to this application for compatibility on older Android versions.
It produces no runtime permission prompt.
`lintDebug` runs `verifyDebugManifestPermissions` and enforces this exact merged-manifest allowlist.

Verify that exact allowlist after manifest merging:

```bash
./gradlew verifyDebugManifestPermissions
```

Use the following standalone merged-manifest inspection when diagnosing the verifier.
Read `intermediates/merged_manifest/debug`, the directory `processDebugMainManifest` writes — the similarly named plural `merged_manifests` tree belongs to `processDebugManifest` and goes stale when only the main manifest task runs:

```bash
./gradlew processDebugMainManifest
merged_manifest=$(find app/build/intermediates/merged_manifest/debug -name AndroidManifest.xml -print -quit)
test "$(rg -c '<uses-permission' "$merged_manifest")" = "1"
rg -n 'uses-permission android:name="kr\.donminzzi\.screenloom\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"' "$merged_manifest"
if rg -n 'uses-permission android:name="android\.' "$merged_manifest"; then exit 1; fi
```

## Manual Emulator Flow

1. Launch from a clean install and confirm no permission prompt appears.
2. Import one screenshot and confirm `Focus`, `Stack`, editing, and export work while `Split` is disabled.
3. Replace the selection with two screenshots and confirm `Split` becomes available.
4. Change title, subtitle, palette, frame, shadow, and layout and confirm the preview updates immediately.
5. Cancel import and export and confirm the active composition remains intact.
6. Rotate the emulator and confirm the active composition survives while the process remains alive.
7. Export a PNG, reopen it, and confirm it is an opaque 8-bit truecolor RGB image with dimensions of 1080 by 1920 pixels.

Inspect the exported artifact itself rather than relying on successful bitmap decoding.
Point `exported_png` at the file the export produced:

```bash
exported_png=~/Downloads/screenloom-poster.png
file "$exported_png"
sips -g pixelWidth -g pixelHeight "$exported_png"
```

The `file` result must report `8-bit/color RGB`, not `RGBA`, and `sips` must report a pixel width of 1080 and a pixel height of 1920.

## Manual Smoke Result

### 2026-08-24, product-readiness improvements

All seven scenarios were driven through `adb`, system Photo Picker, system Create Document, and Google Photos against debug APK SHA-256 `e2f11d3c659e28fda1896716726dfde28e2ad01267e19b8ca6755493d7771f94` on the API 34 `flutter_emulator` AVD.

1. A clean install launched Screenloom with no permission dialog, no runtime permission, and only the AndroidX signature-protected application permission in the package declaration.
2. A one-image import exposed `Focus` and `Stack`, kept the nearest clickable `Split` ancestor disabled, accepted title and subtitle edits, and exported `one-image.png` successfully.
3. Replacing the selection with two images changed the preview summary to two screenshots and made the nearest clickable `Split` ancestor enabled.
4. Title, subtitle, `Split`, device frame, `Coral`, and `Strong` each updated the active composition; layout, frame, palette, and shadow actions each produced a different fixed-position screen pixel hash.
5. Backing out of Photo Picker and Create Document returned to the same title, subtitle, layout, image count, and palette.
6. Rotating to landscape and back preserved the two-image `Split` composition, `Coral` palette, title, and subtitle while the process remained alive.
7. Google Photos reopened `ship-faster.png`; the pulled artifact was a 316,977-byte, non-interlaced `8-bit/color RGB` PNG at exactly 1080 by 1920 pixels with SHA-256 `9b88d99e5b3ecfe155051311de22b5af3e865b959981909e2a18fe7cb3d79cf9`.
   The one-image export was also `8-bit/color RGB` at exactly 1080 by 1920 pixels with SHA-256 `f76478af7ea1852a14fdb385401736865243484982bc0984f1f8c48ad3a82750`.

This run does not establish TalkBack reading order, switch announcements, or gesture navigation.
Those assistive-technology checks remain explicit manual follow-up in GitHub issue #9.

### 2026-08-13, post code-review fixes

> [!WARNING]
> This run predates the current product-readiness baseline and its APK is **not** the one in the baseline above.
> Use the 2026-08-24 result as the current seven-scenario manual gate.

All seven scenarios were re-run against APK SHA-256 `61a31e3783b9628923244714b0b0d9c90152cc836c908be20eb03345bd9d8a91` on the API 34 `flutter_emulator` AVD, **driven through `adb` and `uiautomator` rather than by hand**.

1. A clean install (`uninstall` then `install`) opened with zero runtime permissions in `dumpsys package` and no dialog from any package other than the app itself.
2. One image: Photo Picker reported "This app can only access the photos you select"; `Focus` and `Stack` re-rendered the preview; the `Split` button's clickable node reported `enabled=false` and tapping it changed nothing.
3. Selecting two images moved the header to `02 FRAMES` and `Split` to `enabled=true`.
4. Frame toggle, title, subtitle, `Coral`, `Violet`, `Strong`, and `Soft` each changed the preview region's pixel hash.
5. Backing out of both Photo Picker and Create Document returned the identical preview hash at a fixed scroll position.
6. Rotating to landscape kept `02 FRAMES`, the composition, and the selected `Style` tab.
7. `Export PNG` suggested `screenloom-ship-faster.png` from the typed title and wrote an 859,705-byte file that reopened as a PNG of exactly 1080 by 1920.

What this run does **not** establish: it asserts that pixels changed, not that the result looks right.
Visual quality, text legibility, and colour rendering still need a human pass before release.

### 2026-08-12, original MVP

The 2026-08-12 API 34 smoke run completed all seven scenarios against the earlier APK hash.
A clean install opened without a permission prompt; one-image and two-image imports worked through Photo Picker; `Split` changed from disabled to enabled with the second image; copy, palette, frame, shadow, and layout changes updated the preview; picker cancellation preserved the composition; and title, subtitle, images, and selected `Split` layout survived rotation while the process remained alive.
Create Document cancellation also preserved the composition.

The run exported `/sdcard/Download/review-safely.png`.
It reopened as PNG with pixel width 1080 and pixel height 1920 and SHA-256 `9172afc05fb739fb5bc95aea22c8aeb8aaceea584e82009d4dae49d34b3c1d40`.

## Visual QA Evidence

Captured 2026-08-13 against APK SHA-256 `1bf67850ad7fef5b6605c50a011279228599d96ce8b6eb4fbb104ce10fd49d96`, written to the gitignored `app/build/outputs/manual-qa/`: the empty state, the `Focus` / `Stack` / `Split` editor states with a real title and subtitle, and the poster exported from `Split`.

Mechanically confirmed from those captures: all three tabs and the three composition options render, `Split` shows its selected outline, both screenshots appear in the two-image `Split` output, the title and subtitle are legible in preview and export alike, and the exported file reopens as a PNG of exactly 1080 by 1920.

The crop finding from that pass was acted on: screenshot frames now take the source's aspect ratio, so nothing is cropped. Captures `06`–`09` show the result, rendered from synthetic 9:16 sources carrying a full-bleed border, corner markers, and a left-anchored block — all of which survive intact.

**Human visual approval: given 2026-08-13**, by the operator, running the app rather than reading these captures. Both items flagged for a deliberate look were accepted as-is:

- The accent glow rendering as a hard-edged flat disc rather than a glow, in both preview and export.
- Aspect-fitting shortening the `Split` frames from 1030 to 782 units tall for a 9:16 source, leaving more empty space below them.

The sign-off was "no problems beyond the app being a little unattractive", which closes the correctness gate and leaves visual refinement as ordinary product work rather than a release blocker.

## Repository Quality Gate

Trunk was adopted on 2026-08-13, after the MVP passed its gates.
`trunk fmt` and `trunk check` run on changed files as the pre-commit and pre-push gate; `trunk check --all` is clean across 50 files.

```bash
trunk check
```

- **ktlint is deliberately disabled**, not merely absent. Kotlin formatting is unchecked on purpose: ktlint 1.x rewrites 23 of the 26 Kotlin files, so adopting the official style is a standalone decision rather than a side effect of enabling linting.
- **A stale trunk cache imitates two unrelated bugs.** During setup it produced a plugin source that "conflicted" with the CLI's bundled definitions (`Tool has both download and package defined`), linter pins stuck years behind, `trunk upgrade` insisting "already up to date", and markdownlint failing to run on a single file while passing on its neighbour. None of that was real: clearing the cache fixed all of it at once. Suspect the cache before theorising about trunk, and re-check any conclusion drawn while it was dirty.

An unauthenticated `api.github.com` failure was also observed during setup and wrongly written up here as a property of this machine.
It was transient — the endpoint answers normally now.

## Sunlit Editorial QA and approval, 2026-08-14

The Sunlit Editorial automated and manual QA evidence was recorded against debug APK SHA-256 `a176abeee6bc9618b325d9b8761a14e472c461255a10907878eb2a82f5f653f8`.

### Automated, device, and permission evidence

- The JVM suite passed with 31 tests, 0 failures, 0 errors, and 0 skipped.
- The instrumented suite passed with 45 tests, 0 failures, 0 errors, and 0 skipped.
- `lintDebug`, including `verifyDebugManifestPermissions`, passed.
- `assembleDebug`, `trunk check`, worktree `git diff --check`, and main-checkout `git diff --check` passed.
- The device was `emulator-5556`, `flutter_emulator_2(AVD) - 14`, API 34, `sdk_gphone64_arm64`, at 1080 by 1920.
- A clean uninstall/install returned `Success`, and cold `am start -W` returned `Status: ok` with `TotalTime: 5139` ms.
- The package declared and was granted only `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- The runtime permission section was empty, and no Android platform permission was present.

### Commands used

The following observed commands were used to produce the recorded evidence.
They were not rerun for this documentation update.

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
ANDROID_SERIAL=emulator-5556 ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
trunk check
git diff --check
git -C /Volumes/dongminyu/Development/01_personal/screenloom diff --check
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 uninstall kr.donminzzi.screenloom
adb -s emulator-5556 install app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -W -n kr.donminzzi.screenloom/.MainActivity
adb -s emulator-5556 shell dumpsys package kr.donminzzi.screenloom
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-paper.png
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-cobalt.png
sips -g pixelWidth -g pixelHeight app/build/outputs/manual-qa/sunlit-editorial/export-iris.png
file app/build/outputs/manual-qa/sunlit-editorial/export-paper.png app/build/outputs/manual-qa/sunlit-editorial/export-cobalt.png app/build/outputs/manual-qa/sunlit-editorial/export-iris.png
```

### Manual scenarios

1. [PARTIAL] Clean launch reached the canonical empty state after an emulator System UI ANR dialog was dismissed with its real `Wait` action.
   The canonical capture contained no app permission dialog.
2. [PARTIAL] The real Photo Picker showed recognizable pushed fixtures, and selecting one reached `01 FRAME / 1080 x 1920`.
   This is the one-image Focus result.
   Stack was then tapped, and fresh UIAutomator reported the nearest clickable Split ancestor as `enabled=false`; a disabled-content capture was recorded.
3. [PARTIAL] The real Photo Picker showed `Add (2)` after two recognizable fixtures were selected.
   The editor then showed `02 FRAMES / 1080 × 1920`; a fresh editor dump reported the nearest clickable Split ancestor as `enabled=true`, and tapping it produced the canonical Split capture.
   UIAutomator selection was not used as selected-semantics evidence.
4. [PARTIAL] Real Copy inputs changed to `Sunli` and `Pure`.
   Real Style controls selected Paper, Cobalt, Coral, Mint, Iris, and Sunrise; Strong depth was selected and Device frame was toggled off before the changed-composition capture.
   Pixel-change and visual-quality conclusions were retained for the human review.
5. [PARTIAL] Replace opened the real Photo Picker and its Cancel action returned to `02 FRAMES`.
   Export PNG opened the real Create Document picker and Back returned to the editor composition.
   Fresh canonical dumps were taken after each return.
6. [PARTIAL] Forced landscape produced a device-original 1920 by 1080 capture with `02 FRAMES / 1080 × 1920` and the preview still present.
   Portrait was restored afterwards.
7. [PARTIAL] Real Paper, Cobalt, and Iris exports were written through Create Document, pulled, verified as 1080 by 1920 RGBA PNGs, and opened through their MediaStore content URIs in Google Photos.

### Capture inventory

The application captures are `01-empty-state.png`, `01-empty-state.xml`, `02-layout-focus.png`, `03-layout-stack.png`, `13-split-disabled.png`, `04-layout-split.png`, `05-copy-tab.png`, `frame-off-strong-sunrise.png`, `import-cancel-preserved.png`, `export-cancel-preserved.png`, and `15-rotated-state.png`.

The six palette captures are `06-style-paper.png`, `07-style-cobalt.png`, `08-style-coral.png`, `09-style-mint.png`, `10-style-iris.png`, and `11-style-sunrise.png`.

The export and reopened captures are `export-paper.png`, `reopened-paper.png`, `export-cobalt.png`, `reopened-cobalt.png`, `export-iris.png`, and `reopened-iris.png`.

### Export results

Paper, Cobalt, and Iris each exported as a 1080 by 1920, 8-bit RGBA PNG and reopened through Google Photos via their MediaStore content URIs.

The pulled Paper evidence is `export-paper.png` and its reopened capture is `reopened-paper.png`.
The pulled Cobalt evidence is `export-cobalt.png` and its reopened capture is `reopened-cobalt.png`.
The pulled Iris evidence is `export-iris.png` and its reopened capture is `reopened-iris.png`.

### Limitations and approval

- [PARTIAL] No faithful visible `12-importing.png` or `14-exporting.png` transient frame was captured, although the actual flows and lock/cancellation behavior were exercised.
- [PARTIAL] One emulator System UI ANR dialog appeared before the canonical empty-state recapture and was dismissed with its real `Wait` action.
  This was not established as an app regression.
- [PARTIAL] One UIAutomator null-root result was retried successfully.
- [PARTIAL] Seven stale completed-QA ADB command process groups caused an apparent transport stall.
  Terminating only those groups restored ADB without restarting the emulator or changing app state.
- [PARTIAL] Paper's on-device filename was `ssunlit-paper.png`.
  The pulled evidence file is `export-paper.png`, and format and dimensions were unaffected.

The operator reviewed the presented empty state, editor, six palette previews, and three export images.
The exact visual verdict on 2026-08-14 was `네 승인합니다.`
Its faithful English gloss is “Yes, I approve.”

## Sunlit Editorial final-remediation approval, 2026-08-14

This section records evidence for the remediated APK SHA-256 `2e8c4cfa8971a057e3b2fe61b98ea08d33a4cf9a433222af75b3cd73fa76d5a2`.
It is distinct from the preceding historical approval of APK SHA-256 `a176abeee6bc9618b325d9b8761a14e472c461255a10907878eb2a82f5f653f8`, whose exact verdict `네 승인합니다.` remains historical.

### Final-remediation automated, device, and permission evidence

- The JVM suite passed with 31 tests, 0 failures, 0 errors, and 0 skipped.
- The connected API 34 suite on `emulator-5556` passed with 47 tests, 0 failures, 0 errors, and 0 skipped.
- `lintDebug`, including `verifyDebugManifestPermissions`, `assembleDebug`, `trunk check`, worktree `git diff --check`, and main-checkout `git diff --check` passed.
- The package contained only the signature-protected `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- The runtime permissions section was empty.

### Affected manual QA

- On API 34 at 1080 by 1920 portrait, installation returned `Success` and cold launch returned `Status: ok` with `TotalTime: 1702` ms.
- The real Photo Picker accepted two fixtures with `Add (2)`, and the editor showed `02 FRAMES / 1080 × 1920`.
- Real Copy inputs changed to `Sunli` and `Pure`.
- Cobalt and Iris previews showed warm-white copy within the dark rounded copy zone and strengthened Ink metadata and section labels, with no observed clipping.
- Real Create Document exports `post-review-cobalt.png` and `post-review-iris.png` were each verified as 1080 by 1920, 8-bit RGBA PNGs.
- The Cobalt and Iris exports reopened in Google Photos through MediaStore URI IDs `1000000203` and `1000000195`, respectively.

### Final-remediation capture inventory

The post-review capture prefix is `post-review-`.
The captures are `post-review-01-empty.png`, `post-review-02-editor-layout.png`, `post-review-03-cobalt-preview.png`, `post-review-04-iris-preview.png`, `post-review-export-cobalt.png`, `post-review-export-iris.png`, `post-review-reopened-cobalt.png`, and `post-review-reopened-iris.png`.
XML evidence was stored alongside the first four captures where present.

### Limitations and final approval

- [PARTIAL] The uninstall command returned `Failure [DELETE_FAILED_INTERNAL_ERROR]`.
  An immediate `pm` path check showed the package absent, and the subsequent install succeeded.
- [PARTIAL] Direct comparison supports the requested affected changes but cannot prove an exhaustive global visual diff because the earlier captures used a different layout and fixture composition.
- [PARTIAL] The earlier transient Importing and Exporting capture limitation remains historical and is not a claim about this remediated APK.

The operator reviewed the remediated captures.
The exact final approval of the remediated APK on 2026-08-14 was `네 동의합니다.`
Its faithful English gloss is “Yes, I agree.”
