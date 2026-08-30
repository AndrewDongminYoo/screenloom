# Screenloom Post-Export Reuse Design

## Status

The operator approved this design for implementation on 2026-08-30.

## Summary

Screenloom will add a small post-export reuse loop without adding projects, accounts, network access, a database, or permissions.
The loop exposes an explicit Share action after a successful PNG export, starts a new composition with the current visual style, persists only that style, and allows one Undo action after Reset.

## Goals

- Let the user share a successfully exported PNG through the Android Sharesheet.
- Let the user start another composition without reselecting layout, palette, frame, or shadow settings.
- Restore the last visual style after a process restart.
- Let the user undo one Reset action while the reset snackbar remains available.

## Non-Goals

- Do not persist source images, source URIs, titles, subtitles, exported URIs, or project history.
- Do not add saved projects, batch output, cloud sync, analytics, accounts, billing, a database, dependency injection, a navigation framework, or a platform permission.
- Do not delete or overwrite a selected output document after an export failure.
- Do not change preview or export rendering behavior.

## State Boundaries

`EditorStyle` is a pure Kotlin value that contains `LayoutMode`, `PaletteId`, frame state, and `ShadowLevel`.
It does not contain copy, images, URIs, or export state.

The application stores `EditorStyle` in `SharedPreferences` under the `screenloom_editor_style` name.
The store uses one key for each style value and falls back to the default style when a value is absent, invalid, or has an incompatible type.

`EditorUiState.lastExportUri` exists only in memory.
The ViewModel sets it only after `PosterWriter.export()` returns `ExportResult.Success`.
The ViewModel clears it when the composition changes, when import succeeds, when the user starts another composition, and before a new export begins.

The ViewModel keeps one pre-reset `EditorDocument` only in memory.
Reset continues to preserve the currently imported images.
Undo restores that document once and clears the snapshot.
Dismissal of the reset snackbar also clears the snapshot.

## User Flow

After a successful export, the editor shows explicit `Share PNG` and `Create another` actions.
Share sends the successful `content://` output URI with `ACTION_SEND`, `EXTRA_STREAM`, `ClipData`, MIME type `image/png`, and `FLAG_GRANT_READ_URI_PERMISSION`.
The app opens the system chooser and does not query or persist target applications.

Create another recycles the current imported bitmaps and returns to the empty state.
It clears the title and subtitle.
It preserves the current layout, palette, frame, and shadow values.
An empty document may retain `Split` as a style preference because it has no visible layout controls or exportable content.
The existing reducer still falls back to `Focus` when the next import contains only one image.

Reset keeps its existing default-style behavior.
The reset snackbar includes a single `Undo` action.
Only the document state immediately before Reset can be restored.

## Architecture

`EditorStyle` and document-to-style conversion remain Android-free in `editor/EditorModels.kt`.
`EditorStylePreferences` is the small Android `SharedPreferences` adapter.
`MainActivity` constructs the adapter with the application context and passes its loaded style and save callback to `EditorViewModel`.

`EditorViewModel` owns style-save decisions, the transient successful export URI, the single reset snapshot, and bitmap recycling for Create another.
`EditorScreen` renders the post-export actions and maps snackbar Undo to the ViewModel.
`MainActivity` creates the Sharesheet intent from the successful URI.

## Error Handling

Export failure and picker cancellation do not expose a Share action and preserve the active composition.
The app never deletes the destination URI on export failure because it can reference a pre-existing user document.
Invalid stored style values recover to defaults without showing an error.
The Sharesheet is launched only from an explicit tap after a successful export.

## Verification

Instrumented ViewModel tests must cover successful-export URI state, style-only persistence requests, Create another bitmap recycling, and one-step Reset Undo.
Compose tests must cover the visible post-export actions and the Undo snackbar action.
An Android intent test must assert the share action, MIME type, stream URI, `ClipData`, and read-grant flag.

The manual emulator flow must verify Share cancellation, Create another style retention, one-step Reset Undo, and style restoration after a process restart.
The existing unit test, lint, debug assembly, instrumented test, and manual emulator gates remain required.

## Decision Basis

The advisor confirmed that GitHub Actions issue #13 can use its stated headless scope and that issue #10 needs a new spec and plan before implementation.
The Oracle precedent in `wiki/sources/screenloom--agents.md` confirms the offline single-module and permission constraints.
The Oracle precedent in `wiki/sources/screenloom--claude.md` confirms the editor-state boundary and Reset invariant.
The Oracle precedent in `wiki/sources/claude--projects---volumes-dongminyu-development-01-personal-screenloom--memory--export-delete-on-failure-rejected.md` confirms that failed output documents must not be deleted.
