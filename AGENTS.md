# Screenloom Contributor Guide

## Scope

Follow the approved design in `docs/specs/2026-08-12-screenloom-design.md` and the active plan in `docs/plans/2026-08-12-screenloom-implementation.md`.
Keep Screenloom a single-module, offline native Android application.
Do not add accounts, networking, analytics, ads, billing, a database, dependency injection, or a navigation framework without an approved specification change.

## Implementation

Use Kotlin, Jetpack Compose, Material 3, immutable editor state, and explicit manual construction of Android services.
Keep pure editor and layout behavior separate from Android bitmap and URI code.
Use string resources for user-facing copy.
Do not add broad media-library permissions; import and export must remain system-picker driven.

## Verification

Run the commands documented in `docs/testing.md`.
The minimum completion gate is unit tests, Android lint, debug assembly, instrumented UI tests, and the manual emulator flow.
Never claim Play publication, release signing, or pricing is complete from local app verification.
