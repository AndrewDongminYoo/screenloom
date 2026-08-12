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

The MVP was last verified on 2026-08-12 against the API 34 `flutter_emulator` AVD at 1080 by 1920.
The automated result contained 17 passing unit tests and 26 passing instrumented tests with zero failures, errors, or skips.
`lintDebug` and `assembleDebug` both exited zero.

The verified debug APK SHA-256 is `9eec1b460c69d755216c4e866da03a83559804a6a8822189a8b4a501c23026c5`.

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

The run exported `/sdcard/Download/launch-beautiful-stories-for-every-store.png`, which reopened as PNG with pixel width 1080 and pixel height 1920 and SHA-256 `a365e8c798aa8b5c4e659b06e7dcc3a22f95ac79edef4f270b2be32e2ba9deff`.
That exported fixture had SHA-256 `41fca6bbd4414b027a758eba960218bc5eeeefa74d9b0e6eb50c8b7eba9594be`.

## Repository Quality Follow-Up

Run `$setup-trunk` only as a separately approved repository-quality task after the native Android MVP passes all gates.
