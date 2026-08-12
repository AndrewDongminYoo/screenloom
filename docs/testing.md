# Testing Screenloom

## Environment

- JDK 17.
- Android SDK at `/Volumes/dongminyu/Android/sdk`.
- Compile and target SDK 36.
- Instrumented test AVD `flutter_emulator`, API 34, 1080 by 1920.

## Automated Checks

Run each heavy Android command sequentially.

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

Unit tests protect editor normalization and deterministic layout geometry.
Instrumented tests protect Android image decoding, bitmap export, ViewModel coordination, and Compose semantics.

## Verified Baseline

The automated gate was last run on 2026-08-13 against the API 34 `flutter_emulator` AVD at 1080 by 1920, after the code-review fixes.
The result contained 17 passing unit tests and 32 passing instrumented tests with zero failures, errors, or skips.
`lintDebug` and `assembleDebug` both exited zero.

The debug APK SHA-256 for that run is `d0cb505f014fc8284f5865be8a32c61ec462513167172880a7b0e93aab8d1460`.

> [!WARNING]
> The **manual emulator flow below has not been re-run since 2026-08-12**, so it does not cover the code-review fixes.
> Two of them are user-visible and land squarely in that flow: EXIF orientation now rotates camera photos on import, and the selected control tab survives rotation (step 6).
> Re-run the seven scenarios before any release.

The 2026-08-12 automated baseline, for reference, was 17 unit and 29 instrumented tests against APK SHA-256 `a9f99a93cb5c4fb25bf7ab98dca335bc4a745d20df0b9d359e89ae986db246da`.

## Manifest Privacy Boundary

The merged manifest requests no Android platform, network, storage, camera, microphone, location, contacts, or advertising permissions.
AndroidX Core 1.18.0 contributes only `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-protected permission scoped to this application for compatibility on older Android versions.
It produces no runtime permission prompt.

Verify that exact allowlist after manifest merging:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew processDebugMainManifest
merged_manifest=$(find app/build/intermediates/merged_manifests/debug -name AndroidManifest.xml -print -quit)
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
7. Export a PNG, reopen it, and confirm its dimensions are 1080 by 1920 pixels.

## Manual Smoke Result

The 2026-08-12 API 34 smoke run completed all seven scenarios against the APK hash above.
A clean install opened without a permission prompt; one-image and two-image imports worked through Photo Picker; `Split` changed from disabled to enabled with the second image; copy, palette, frame, shadow, and layout changes updated the preview; picker cancellation preserved the composition; and title, subtitle, images, and selected `Split` layout survived rotation while the process remained alive.
Create Document cancellation also preserved the composition.

The run exported `/sdcard/Download/review-safely.png`.
It reopened as PNG with pixel width 1080 and pixel height 1920 and SHA-256 `9172afc05fb739fb5bc95aea22c8aeb8aaceea584e82009d4dae49d34b3c1d40`.

## Repository Quality Follow-Up

Run `$setup-trunk` only as a separately approved repository-quality task after the native Android MVP passes all gates.
