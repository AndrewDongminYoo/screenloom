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

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Verification

```bash
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew testDebugUnitTest
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew lintDebug
ANDROID_SDK_ROOT=/Volumes/dongminyu/Android/sdk ./gradlew connectedDebugAndroidTest
```

See [docs/testing.md](docs/testing.md) for the complete testing contract.

The last verified debug APK is `app/build/outputs/apk/debug/app-debug.apk` with SHA-256 `7f97a62b6e5476f202ded8ea042be086b3343fefcb12668d3b718a7ddb2c4434`.

## Commercial Scope

The repository builds the complete product experience without Billing, ads, analytics, signing credentials, or Play Console configuration.
Pricing, release signing, and store publication remain separate release tasks.
