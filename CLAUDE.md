# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MosqueAthan (`com.groovernapp.mosqueathan`) — an Android TV mosque prayer-schedule display. Single Gradle module (`:app`), Kotlin + Jetpack Compose, Realm Kotlin for persistence, adhan2 for prayer-time math. Landscape-only, launcher-declared for `LEANBACK_LAUNCHER`, auto-started on boot via `BootReceiver` (TV devices only). UI strings are bilingual EN/ID at runtime — see `util/Lang.kt`.

Related doc already in the repo: `CODEX.md` (product/UI rules — coordinates, prayer display rules). Architecture, design-system and settings-UI rules live in this file; there is no separate `AGENTS.md`.

## Build & test

```bash
./gradlew assembleDebug                 # APK named "Mosque Athan - <versionName>.apk"
./gradlew installDebug                  # push to connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.groovernapp.mosqueathan.PrayerCalculatorTest"
./gradlew testDebugUnitTest --tests "*PrayerCalculatorTest.testNextPrayerStandardDaytime"
./gradlew connectedAndroidTest          # instrumented tests (device required)
./gradlew lint                          # Android Lint
```

JDK 21 toolchain, Gradle 9.4.1, AGP 8.12, Kotlin 2.2.10. No ktlint/detekt configured.

Version is driven from `gradle/libs.versions.toml` (`versionName`, `versionCode`) — bump there, not in `app/build.gradle.kts`.

`-opt-in=kotlin.time.ExperimentalTime` is set globally; the code uses `kotlin.time.Clock`/`Instant` together with `kotlinx.datetime` types. Note that `kotlinx-datetime` and `kotlinx-coroutines` are **not** declared in the version catalog — they arrive transitively via adhan2/Realm, so a dependency bump there can break compilation broadly.

### Testing the web panel's JavaScript

The web control panel's JS lives inside Kotlin raw strings. The root-level `test_webpage.js` extracts those `"""…"""` blocks from the `WebPageJs*.kt` files and concatenates them into `test_webpage_full.js` so the JS can be syntax-checked outside a build:

```bash
node test_webpage.js && node --check test_webpage_full.js
```

## Architecture

### Screen state machine

There is no Navigation graph in practice. `MainActivity` owns five activity-scoped ViewModels and renders a `when` over `AppUiState` (`ui/navigation/AppUiState.kt`): `Dashboard | Adzan | Iqomah | Settings`.

`ui/navigation/AppViewModel.kt` is the coordinator and the most important file to read first:
- A 1-second `while (true)` loop (`startCoordinator`) recalculates the schedule at date rollover, evaluates transitions, and updates standby.
- It owns the *only* prayer-time source of truth (`PrayerResult`) and drives the Dashboard/Adzan/Iqomah ViewModels — those screens do not compute times themselves.
- `transitionTo*` / `preview*` swap which child ViewModel's flow is collected into `appState`; preview mode is entered from Settings and exited with the back handler.
- Dashboard → Adzan fires when a prayer is within `adzanLeadMinutes`; Adzan → Iqomah follows per-prayer iqomah minutes.

Entering settings on a TV without a mouse: the D-pad secret sequence **UP UP DOWN DOWN** while on the Dashboard (`MainActivity.onKeyDown`). Standby mode dims the window to brightness 0 and swallows the first key press.

### Prayer times

`util/PrayerCalculator.kt` is a stateless object; pass `now` explicitly (tests do). Two sources, layered:
1. `calculationMethodName == "KEMENAG"` — pre-fetched Indonesian Kemenag schedules read from Realm (`KemenagScheduleEntity`), fetched from `api.myquran.com`.
2. Otherwise adhan2 `CalculationMethod.valueOf(name)`, falling back to `SINGAPORE` on an unknown name.

Per-prayer minute offsets are applied on top of whichever source wins. Imsak defaults to Subuh − 10 min when not supplied. On Fridays with `jumatEnabled`, Dzuhur uses `offsetJumat` instead of `offsetDzuhur` and `PrayerType.DZUHUR.displayName` renders as "JUM'AT"/"FRIDAY".

`PrayerType` includes `IMSAK` and `SYURUQ`, but both are informational: they are never triggered as Adzan/Iqomah transitions (`checkTransitions` only iterates SUBUH, DZUHUR, ASHAR, MAGHRIB, ISYA) and `iqomahMinutes` is 0 for them.

### Persistence

Realm, accessed only through the singleton `data.MosqueConfigRepository.getInstance()`. All settings live in one row of `MosqueConfigEntity` (`id == "config"`), plus side tables for announcements, media, Friday media, connected devices, Kemenag schedules, slideshow/standby schedules, and dashboard backgrounds.

Two things to know before touching the schema:
- The config is a wide, flat Realm object, so `MosqueConfigEntity.kt` also hand-writes a `copy(...)` extension (Realm objects are not data classes) and `MosqueConfigRepository.updateConfig` copies field-by-field. **Adding a config field means editing all three places** — the entity, the `copy` extension, and the update path — or the value silently never persists.
- `RealmConfiguration` uses `deleteRealmIfMigrationNeeded()` with an explicit `schemaVersion`. Bump the version when the schema changes and expect existing device data to be wiped; there are no migrations.

Settings auto-save — there is no Save button. Every event writes to Realm immediately, and `getConfigFlow()` fans the change back out to all screens and web clients.

### Web control panel + remote-TV pairing (`server/`)

`SettingsServer` is a hand-rolled HTTP + WebSocket server on port **8080**, started in `MainActivity.onCreate` and stopped in `onDestroy`. There is no Ktor/NanoHTTPD — sockets, headers, and WebSocket framing are written by hand, so adding a route means extending the `if (path == ...)` chain in the request handler.

- Routes: `/` (panel HTML), `/manifest.json`, `/sw.js` (PWA), `/background`, `/media*`, `/logs`, `GET /api/get_config`, `POST /api/action`, `POST /upload`.
- The WebSocket carries a JSON `{"action": ...}` protocol handled by `handleWebSocketMessage` (~50 actions: `update_config`, `preview_adzan`, media/slideshow/standby CRUD, device `register`/`verify_pin`/`set_master`, `reset`, …). `POST /api/action` reuses that same handler through a fake connection object, so **HTTP and WebSocket share one implementation** — add an action once.
- Realm flows are collected at startup and `broadcastState()` pushes the full state to every connected client on any change.
- `connectivityMode` is `"SERVER"` (this device is the display, serving the panel) or `"REMOTE"` (this device is a controller; sub-ViewModels additionally POST to `http://<remoteTvIp>:8080/api/action` — see `sendRemoteAction` in the settings ViewModels).

The panel's HTML/CSS/JS is generated as Kotlin raw strings, split across files purely to keep them manageable: `WebPage.kt` + `*WebSegment.kt` (markup per settings tab), `WebPageCss.kt` → `WebPageCssBase/HeaderSidebar/CardsForms/MediaList`, `WebPageJs.kt` → `WebPageJsCore/Connection/Offline/UI/Actions`. The `WebPageCss`/`WebPageJs` aggregators define concatenation order — order matters for both. Note these strings are Kotlin templates: a literal `$` in CSS/JS must be escaped.

External CDN assets (Leaflet, flatpickr, html5-qrcode, Google Fonts) are referenced by URL, so parts of the panel degrade without internet; `WebPageJsOffline.kt` handles that path.

### Settings UI pattern (follow this for new settings)

Consistently applied across all six panes — follow it exactly:

- One **standalone ViewModel per settings category**, not additions to `SettingsViewModel`. Each exposes a single `uiState: StateFlow<XUiState>` and one `onEvent(XEvent)` entry point, with `XEvent` a sealed interface. It is obtained inside `SettingsScreen`'s `when (selectedCategory)` via `viewModel()` — so it is scoped to the composable, and adding a category means adding a `SettingsCategory` enum entry plus a branch there.
- Composables receive exactly `state` + `onEvent` (+ `isCompact`). Never a long parameter list of individual values and callbacks.
- Layers: `XSettingsPane` (tabs via `AppTabbedLayout`) → `XSettingsSegment` (form content) → shared components.
- High-frequency text input is buffered in local `remember { mutableStateOf(...) }` inside the leaf composable and only emitted upward when parsed/valid, to avoid a Realm write per keystroke.
- `isCompact` (screen width < 600dp and not a TV) drives the phone layout; `isTv` is detected via `UiModeManager`.

### Design system

Custom tokens via `MosqueTheme` (`ui/theme/Theme.kt`), provided over Material3's `darkColorScheme`. Use `MosqueTheme.colors` (`gold`, `textPrimary`, `textSecondary`, `cardBg`, `border`, `danger`, `success`, `backgroundBrush`), `MosqueTheme.spacing`, `MosqueTheme.typography` — not raw `Color(...)`/`dp`/`sp` literals.

Reuse from `ui/component/` rather than re-styling: `AppCard`, `AppButton`, `AppPrimaryButton`, `AppPreviewButton`, `AppDangerButton`, `AppTabbedLayout`, `AppAdjustmentRow`, `appTextFieldColors()`, plus the `Schedule` component for time/day/date schedule lists.

## Conventions

- Wrap every user-visible string in `Lang.get(en, id)`.
- No deprecated APIs, no unused imports, zero compiler warnings on touched files.
- `git add` new files as soon as they are created.
- Commit subjects follow `type: [Area] > description`, e.g. `style & refactor: [Friday] > refactor settings pane to use standalone viewmodel`.
- Feature branches are versioned (`v.1.5x`, `v.1.4.x`); `main` is the PR target.
