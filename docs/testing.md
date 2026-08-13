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

The automated gate was last run on 2026-08-13 against the API 34 `flutter_emulator` AVD at 1080 by 1920, on the audit-remediation branch at `a5e4cbb`.
The result contained 24 passing unit tests and 42 passing instrumented tests with zero failures, errors, or skips.
`lintDebug` and `assembleDebug` both exited zero, and `lintDebug` carried `verifyDebugManifestPermissions` with it.

The debug APK SHA-256 for that run is `1bf67850ad7fef5b6605c50a011279228599d96ce8b6eb4fbb104ce10fd49d96`.

The 2026-08-12 automated baseline, for reference, was 17 unit and 29 instrumented tests against APK SHA-256 `a9f99a93cb5c4fb25bf7ab98dca335bc4a745d20df0b9d359e89ae986db246da`.

## Manifest Privacy Boundary

The merged manifest requests no Android platform, network, storage, camera, microphone, location, contacts, or advertising permissions.
AndroidX Core 1.18.0 contributes only `kr.donminzzi.screenloom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-protected permission scoped to this application for compatibility on older Android versions.
It produces no runtime permission prompt.
`lintDebug` runs `verifyDebugManifestPermissions` and enforces this exact merged-manifest allowlist.

Verify that exact allowlist after manifest merging:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew verifyDebugManifestPermissions
```

Use the following standalone merged-manifest inspection when diagnosing the verifier.
Read `intermediates/merged_manifest/debug`, the directory `processDebugMainManifest` writes — the similarly named plural `merged_manifests` tree belongs to `processDebugManifest` and goes stale when only the main manifest task runs:

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew processDebugMainManifest
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
7. Export a PNG, reopen it, and confirm its dimensions are 1080 by 1920 pixels.

## Manual Smoke Result

### 2026-08-13, post code-review fixes

> [!WARNING]
> This run predates the audit-remediation work and its APK is **not** the one in the baseline above.
> Serialized imports and the animated preview placements landed afterwards and are user-visible, so the seven scenarios need another pass before release.

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

`[PARTIAL]` — human visual review is still required. Typography, colour, spacing, and polish are not established by any of the above. Two things are worth a deliberate look:

- The accent glow renders as a hard-edged flat disc rather than a glow, in both preview and export. It is the most prominent non-screenshot element on the poster.
- `centerCrop` trims a tall source to the narrower `Split` frame, which clips edge-anchored overlay text in the sample screenshots (`Score: 0` reads as `core: 0`). That is the documented behaviour of the crop, not a defect, but it decides how usable `Split` is for screenshots with left-aligned HUD.

## Repository Quality Gate

Trunk was adopted on 2026-08-13, after the MVP passed its gates.
`trunk fmt` and `trunk check` run on changed files as the pre-commit and pre-push gate; `trunk check --all` is clean across 48 files.

```bash
trunk check
```

Two things about this repository's trunk setup are not obvious, and `.trunk/trunk.yaml` carries the full reasoning:

- **Linter and runtime versions are pinned by hand.** `trunk upgrade` answers "already up to date" even when a pin is years old, because it resolves versions through unauthenticated `api.github.com`, which this machine cannot reach. Do not read that answer as current. Adding a plugins source does not fix it and breaks config parsing.
- **ktlint is deliberately not enabled.** Kotlin formatting is unchecked on purpose; adopting the official style is a standalone decision.
