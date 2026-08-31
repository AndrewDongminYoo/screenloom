# Screenloom

Screenloom is an offline native Android utility that turns one or two app screenshots into polished 9:16 promotional posters.
It is designed as a focused paid-download product for independent developers and creators.
The Android application ID is `kr.donminzzi.screenloom`.

## Product Contract

- Import one or two screenshots through the Android system photo picker.
- Compose `Focus`, `Stack`, and `Split` poster layouts.
- Edit concise copy, an optional device frame, three shadow strengths, and the `Ink`, `Cobalt`, `Coral`, `Moss`, `Violet`, and `Sunrise` palettes.
- Export an exact 1080 by 1920 PNG through the system document picker.
- Request no sensitive, storage, or network permissions.

The approved design is in [docs/specs/2026-08-12-screenloom-design.md](docs/specs/2026-08-12-screenloom-design.md).
The implementation plan is in [docs/plans/2026-08-12-screenloom-implementation.md](docs/plans/2026-08-12-screenloom-implementation.md).

## Build

Use JDK 17 and the local Android SDK.
`local.properties` is gitignored, so a fresh clone must export `ANDROID_SDK_ROOT` first.
See [docs/testing.md](docs/testing.md) for that path; do not restate it here.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

See [docs/testing.md](docs/testing.md) for the complete testing contract, including the last verified APK hash and the run that produced it.
Do not restate that hash here — the copy that used to live in this file went two revisions stale.

## Commercial Scope

The repository builds the complete product experience without Billing, ads, analytics, signing credentials, or Play Console configuration.
Pricing, release signing, and store publication remain separate release tasks.
