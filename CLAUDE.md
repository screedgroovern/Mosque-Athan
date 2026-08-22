# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MosqueAthan (`com.groovernapp.mosqueathan`) — an Android TV mosque prayer-schedule display. Gradle modules `:app`, `:core:model`, `:core:prayer` (see *KMP migration*), Kotlin + Jetpack Compose, Realm Kotlin for persistence, adhan2 for prayer-time math. Landscape-only, launcher-declared for `LEANBACK_LAUNCHER`, auto-started on boot via `BootReceiver` (TV devices only). UI strings are bilingual EN/ID at runtime — see `util/Lang.kt`.

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

`PrayerCalculator` lives in `:core:prayer` and is genuinely stateless: `now` and the Kemenag schedule source are both parameters (tests pass fakes for each). Two sources, layered:
1. `calculationMethodName == "KEMENAG"` — pre-fetched Indonesian Kemenag schedules supplied through the `KemenagScheduleSource` parameter. `:app` passes `RealmKemenagScheduleSource`, which reads `KemenagScheduleEntity` from Realm (fetched from `api.myquran.com`). If no source is passed, the Kemenag branch is skipped and adhan2 is used.
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

Tokens live in `:ui:design` and are reached through `MosqueTheme`:

- `MosqueTheme.colors` — 19 tokens. `accent`; surfaces (`surface`,
  `surfaceRaised`, `backdrop`, `runningTextBackground`); foreground
  (`textPrimary`, `textSecondary`, `textMuted`, `textDisabled`); fills and
  borders (`fillSubtle`, `fill`, `fillStrong`, `border`); status (`danger`,
  `success`, `info`); and `onAccent`, `scrim`, `transparent`.
- `MosqueTheme.typography` — 22 font sizes. Ten display sizes for the TV
  screens (`clockDisplay` 170sp down to `prayerNameCompact`), twelve for
  settings UI (`titleLarge` down to `captionSmall`). Sizes, not `TextStyle`s:
  call sites set their own weight and colour.
- `MosqueTheme.spacing` / `.radius` / `.stroke` — 24 tokens on a 4dp-ish scale.
- `MosqueTheme.size` — 7 tokens for icons and controls (`iconSm/Md/Lg`,
  `controlSm/Md/Lg/Xl`). **Kept separate from `spacing` even where the numbers
  match**: 36dp of control height and 36dp of padding are different decisions, and
  a later pass scaling spacing per window class must not drag icon sizes with it.
  A `Spacer`'s width is spacing, not a size.
- `MosqueTheme.windowClass` / `LocalWindowClass` — `Compact` / `Medium` /
  `Expanded` / `Tv`. **Read this; do not accept an `isCompact` parameter.**

**Use tokens, never literals.** `:app` contains zero colour literals and zero
`.sp` literals — verified with complete patterns, including named Compose colour
constants and fractional values. The two `android.graphics.Color.WHITE/BLACK`
uses in `WebControlSettingsSegment` are Int colours for QR bitmap generation, a
different type, and are correctly outside the design system. Spacing, radius and stroke are tokenised wherever the
call shape made the meaning unambiguous.

Known gaps:

- **Component sizes.** 246 `dp` literals survive in `size()`, `width()` and
  `height()`. There is no component-size scale yet; deriving one means deciding
  which values are icons, which are containers and which are fixed dialog
  dimensions. Match the surrounding file until that scale exists.
- **Components.** Buttons live in `:ui:design`, in
  `component/MosqueButton.kt`, as six semantic variants. Use them; do not add a
  parallel set, and do not reach for raw `Button`/`OutlinedButton`/
  `FilledTonalButton`. Every variant handles D-pad focus — a 3dp border and a
  1.05 scale — which raw Material3 does not, and which is the whole reason they
  exist on a television.

  | Variant | Form | Call sites |
  |---|---|---|
  | `MosqueSecondaryButton` | neutral fill (`fillStrong`) | 24 |
  | `MosquePrimaryButton` | gold, `onAccent` content | 18 |
  | `MosqueTertiaryButton` | bordered, no fill | 4 |
  | `MosqueDangerButton` | tinted `danger` | 3 |
  | `MosquePreviewButton` | gold tint, bordered at rest | 2 |
  | `MosqueSelectableButton` | takes `selected: Boolean` | 2 |

  `mosqueTextFieldColors()` (same package) styles every `OutlinedTextField`.

  Still raw, deliberately: **15 `IconButton`** and **12 `TextButton`**. The
  `TextButton`s are dialog confirm/dismiss actions, where Material's own is
  right. The `IconButton`s are the real gap — they have no focus handling, which
  on a TV means a D-pad user cannot see which icon is selected. That is worth a
  variant; it has not been written.

  `SettingsCard` and `AdjustmentRow` are still in
  `ui/settings/SettingsScreenShared.kt` and should move to `:ui:design` too.

- **Component sizes.** 246 `dp` literals survive in `size()`, `width()` and
  `height()`. There is no component-size scale yet; deriving one means deciding
  which values are icons, which are containers and which are fixed dialog
  dimensions. Match the surrounding file until that scale exists.

Colour consolidation has landed: 39 tokens became 19, moving 61 call sites.
Font sizes have **not** been consolidated — 22 tokens remain, and that merge is
deliberately held back so it can happen in the same pass that assigns
per-window-class values, rather than moving the same 84 call sites twice. See
*Consolidation proposal*.

## Conventions

- Wrap every user-visible string in `Lang.get(en, id)`.
- No deprecated APIs, no unused imports, zero compiler warnings on touched files.
- `git add` new files as soon as they are created.
- Commit subjects follow `type: [Area] > description`, e.g. `style & refactor: [Friday] > refactor settings pane to use standalone viewmodel`.
- Feature branches are versioned (`v.1.5x`, `v.1.4.x`); `main` is the PR target.

## KMP migration (branch `kmp`)

> **Steps 1 and 5 (phase A) have landed. Steps 2, 3, 4, 6–9 have not.**
> `build-logic`, `:core:model`, `:core:prayer` and `:ui:design` exist,
> `PrayerCalculator` is pure, and every colour and font size in `:app` reads
> from a token. Everything else below is still intent.

### Why

Not iOS. The driver is that the web control panel is ~9,100 lines of
HTML/CSS/JS written as Kotlin raw strings across `server/WebPage*.kt` and
`server/*WebSegment.kt`, hand-maintained against a *separate* Compose UI that
renders the same settings. The two drift, and the drift is only detectable by
eye. Sharing one Compose Multiplatform UI makes that drift **structurally
impossible** rather than merely rare. (`SettingsServer.kt`, 1,895 lines, is the
transport and stays.)

Secondary goal: legibility. A Gradle module boundary turns a convention into a
compile error, so the build enforces the rules instead of the author remembering
them. iOS is a side effect, not the goal.

Platform roles — iOS must never become the mosque display (no boot autostart, no
kiosk, suspended in background). The TV role stays Android-only, permanently.

| Platform | Display | Controller |
|---|---|---|
| Android TV | **yes** — the only real display | n/a |
| iOS | best-effort preview only | **yes** |
| Web (wasmJs) | best-effort preview only | **yes** |

"Best-effort preview" means Dashboard/Adzan/Iqomah render so the user can check
what they just configured — not production display duty.

"Controller" means a **remote control that works fully offline**: it holds a
complete replica of the server's settings, is edited with no connection, and
syncs later. The server is usually the TV, but any Android device can serve.
Access control (`ConnectedDeviceEntity`) is the one thing a controller does not
manage.

### What is *not* being rebuilt

This branch has broad licence to restructure, but three things are assets, not
debt, and must survive unchanged:

- **The state pattern** — one standalone ViewModel per settings category, single
  `uiState: StateFlow`, single `onEvent(XEvent)`. This is the entire reason a
  shared UI is feasible; it is already the KMP-idiomatic shape. Rewriting it to
  formal MVI reducers, or to anything else, buys nothing.
- **The `when (AppUiState)` screen state machine.** Deliberate, works, portable.
- **`PrayerCalculator` and its tests.** The logic is correct; only its hidden
  repository dependency needs fixing.

The visual layer is what has no system. Scope the rebuild there.

### Target structure

```
MosqueAthan/
├── build-logic/                 convention plugins (KMP targets, compose, lint)
├── gradle/libs.versions.toml
│
├── core/
│   ├── model/                   MosqueConfig, PrayerType, PrayerResult, Lang
│   ├── prayer/                  PrayerCalculator, PrayerUtils, HijriDateFormatter
│   └── protocol/                sealed RemoteAction + kotlinx.serialization
│
├── data/                        ConfigRepository (interface) + implementations
│
├── ui/
│   ├── design/                  tokens, App* components, window size class
│   ├── display/                 Dashboard / Adzan / Iqomah + their ViewModels
│   └── settings/                the six settings panes + their ViewModels
│
├── app/                         Android TV: Manifest, MainActivity,
│                                BootReceiver, SettingsServer
├── iosApp/
└── webApp/
```

`build-logic` is a single included build. Without it the same
`kotlin { androidTarget(); iosArm64(); wasmJs() … }` block is copy-pasted into
six modules and their target lists silently diverge.

Source sets per module: `commonMain`, `androidMain`, `iosMain`, `wasmJsMain`,
`commonTest`. Do not create an intermediate source set speculatively — see the
open decision on Room below.

### Hard rules

These are boundaries the *build* must enforce. If one is only a convention, it
will be broken.

1. **`core/*` must not depend on `data/*`.** Prayer-time logic cannot reach a
   repository; config arrives as a parameter.
2. **`ui/*` must not depend on Room.** UI knows `ConfigRepository`, never an
   implementation.
3. **No platform types in a ViewModel** — no `Context`, `Uri`, `Bitmap`,
   `JSONObject`, `File`. This is the rule broken accidentally; the `ui:*` module
   boundary is what catches it.
4. **`expect`/`actual` only where genuinely unavoidable** — currently video
   playback, image decoding, file paths, TV detection. Any other use is a design
   smell.
5. **State is an immutable `data class`**, updated via `copy()`. Never mutate a
   field in place.
6. Everything under **Conventions** above still applies unchanged.

### Step 5 in detail: design system + adaptive layer

The audit is in *Design system* above: no tokens, no components, 168 colour
literals, 189 `.sp`, 560 `.dp`, one inconsistent breakpoint computed three
times, `isCompact` threaded through ~86 call sites.

This must land **before** the UI moves to `commonMain` (step 6), for two reasons
that are not about taste:

- Moving 168 colour literals and 189 `.sp` literals into `commonMain` means
  moving the mess and then fixing it there — the work gets done twice.
- `LocalConfiguration` and `UiModeManager` are both Android-only, so the size
  detection **has to** be rebuilt regardless. Do it once, in the right place.

#### Hard constraint: the mockup does not change

This is a refactor, not a redesign.

- **Tokens are derived from the values already in the code, never invented.**
  Phase A is a 1:1 mapping — every existing value gets a token name, zero pixel
  change.
- **Consolidation is a separate, separately-reviewable change.** Collapsing 20
  text sizes into ~8 roles, or 8 near-blacks into ~4 surface elevations, *does*
  move pixels. It does not belong in the same commit as the extraction, and it
  needs explicit sign-off.
- **TV output must stay pixel-identical.** The TV rendering *is* the mockup.
  Phone / tablet / 15" is where new adaptive behaviour may appear, since almost
  nothing is handled there today.

#### Window size class

TV is not a width. A 15" monitor at 60 cm and a 43" TV at 3 m can both be
1920 px wide and need completely different type scales — the variable is viewing
distance. So Material3's three width classes are not enough on their own:

```
size class = f(width, isTv)      // TV is its own class at any width
```

| Class | Width | Target |
|---|---|---|
| `Compact` | < 600dp | phone |
| `Medium` | 600–839dp | 10" tablet, portrait |
| `Expanded` | ≥ 840dp | 10" tablet landscape, 15" screen |
| `Tv` | overrides width | TV — larger type, D-pad focus |

Deliver it as a **`CompositionLocal`, not a parameter.** `isCompact` threaded
through ~86 call sites already hurts at two classes; at four it is unworkable.
Components read `LocalWindowClass` themselves, and the typography/spacing tokens
resolve per class — so `AppButton` never asks whether it is on a TV, it just
reads tokens whose values differ.

Detection: `BoxWithConstraints` at the root (portable to `commonMain`), plus
`expect`/`actual` for `isTv` only — that is the sole part needing a platform.

#### Tokens

- **Colour** — semantic roles, not hex: `accent`, `surface` at 3–4 elevations
  (replacing the 8 ad-hoc near-blacks), `textPrimary` / `textSecondary` /
  `textDisabled`, `danger`, `success`, `info`, `border`.
- **Typography** — ~8 roles (`displayLarge` for the 96sp countdown down to
  `caption`), each with a value per size class. This is what makes TV work: same
  role, different numbers, no `if (isTv)` at the call site.
- **Spacing** — the 37 `.dp` values reduce to ~6 steps.

#### Button variants

Semantic, not visual. Height, padding, radius, text style and colour all come
from tokens, so TV scaling is automatic.

| Variant | Visual | Use for |
|---|---|---|
| `Primary` | filled, accent | the main action — one per screen |
| `Secondary` | tonal | supporting actions |
| `Tertiary` | outlined | optional actions |
| `Text` | text only | navigation, cancel |
| `Danger` | filled, red | reset, delete |
| `Preview` | tonal, accent | adzan / iqomah preview |

The single existing `FilledTonalButton` against 16 other buttons is the symptom:
there is currently no rule for when tonal applies.

### Technical decisions

**Persistence — Room on Android/iOS/desktop, IndexedDB on web, one shared model.**

An earlier revision of this file claimed the controller is "a client, not a data
owner" and needs no local database. **That was wrong**, and it is worth stating
plainly because the mistake would have produced the wrong architecture: the
remote control is a *full offline replica*. A takmir pairs by scanning a QR code
on the display, takes the device home, edits every setting with no connection at
all, returns to the mosque, and syncs. It therefore needs the entire settings
model stored locally — not a handful of preferences.

The decision:

- **One set of classes, annotated twice.** A Room `@Entity` and the wire model
  are the *same* `@Serializable data class`. No mappers, and adding a config
  field touches one class rather than three. This works here because every
  existing entity is flat — only `String`/`Boolean`/`Int`, no nesting — so Room
  needs no `TypeConverter`.
- **Android / iOS / Windows:** Room (`androidx.room` KMP) over those entities.
  SQLite provides durability, so no hand-rolled atomic-write code.
- **Web:** Room has no wasm target, so the browser stores the same
  `MosqueSnapshot` in IndexedDB, whose transactions are already atomic. This is a
  second *store*, not a second *model* — the distinction that matters.
- **Sync payload:** `MosqueSnapshot` — one `@Serializable` container holding the
  config object plus a list per table. `ConnectedDeviceEntity` is excluded:
  access control stays server-authoritative.

Rejected, with reasons, so they are not relitigated:

- **SQLDelight everywhere** — the honest fallback, and strictly better than a
  split. Rejected because its web driver is experimental and historically tied
  to the `js` target, not `wasmJs`; SQLite-wasm adds 1–1.5MB to an already large
  Compose bundle; and it prices in querying this data never needs (one config
  row, a few hundred rows total, no joins, single writer). Revisit it the moment
  conflict resolution becomes row-level rather than snapshot-level.
- **A JSON blob inside a single-row SQLite table** — gets durability without
  modelling tables, but reads as a trick, and legibility is a stated goal of
  this migration.
- **Separate storage and wire models with a mapper** — reintroduces exactly the
  "edit three places" problem documented under *Persistence* above.

**Schema evolution still needs defaults.** Coupling the storage model to the
wire model is normally a smell; it is acceptable here because both ends are this
codebase, maintained by the same two people, who update devices together — so
skew *between* devices is not a real risk and should not be designed around.

What remains real regardless: the snapshot already written to disk is always
from the previous version of the code. After an app update, a takmir's phone
still holds yesterday's JSON. So configure `Json { ignoreUnknownKeys = true;
encodeDefaults = true }` and give every newly added field a default. That is one
line plus a habit, and it is about reading your own older data, not about
tolerating a mismatched peer.

**Media files are not in the snapshot.** It carries `filePath` and `isVideo`
only. A takmir who adds a video at home has a file that exists nowhere else, so
sync has to upload it — potentially hundreds of megabytes over mosque wifi. This
is a separate problem from settings sync and needs its own design; it is the part
most likely to be discovered late.

**Serialization — kotlinx.serialization.** `org.json` is Android-only and must
go. The ~50 WebSocket actions become a sealed hierarchy in `core:protocol`
shared by server and every client, replacing today's arrangement where each
action name is retyped in both Kotlin and JavaScript.

**DI — Koin.** `MosqueConfigRepository.getInstance()` must be removed; Compose
Multiplatform needs a ViewModel factory in common code regardless.

**Server — Android target only.** Browsers cannot open listening sockets and iOS
suspends background sockets. Do not attempt to port `SettingsServer`.

**Pattern — MVVM with unidirectional data flow (MVI-shaped).** Keep the existing
single `uiState` + `onEvent` per settings category exactly as documented above.
**Do not introduce formal reducers** — the ceremony is not worth it here.

**Version catalog.** Declare `kotlinx-datetime` and `kotlinx-coroutines`
explicitly (see the warning under *Build & test* — the current transitive
arrangement actively breaks under a KMP setup).

### Already portable

The starting point is better than it looks:

- `adhan2` is itself a KMP library.
- `androidx.lifecycle` 2.10 — already in use — is multiplatform, so `ViewModel`
  and `viewModelScope` move as-is.
- 80 of 88 source files have no `android.*` import at all.
- Everything in `ui/component/` is pure Compose and ports for free.

### Known blockers

Line references are against `v.1.4.4`; re-check them before acting.

| Blocker | Where | Fix |
|---|---|---|
| `sendRemoteAction` builds `org.json.JSONObject` | `ui/settings/SettingsViewModel.kt:449` | kotlinx.serialization |
| `HijriDateFormatter` uses `UmmalquraCalendar` + `java.util.Calendar` — **deliberately left in `:app`** during step 1 | `util/HijriDateFormatter.kt` | needs the real Umm al-Qura month table; an arithmetic approximation would change displayed dates, so it must be ported exactly |
| `VideoView` through `AndroidView` | `ui/dashboard/DashboardScreen.kt:293` | `expect`/`actual` player |
| `LocalConfiguration` / `UiModeManager` size detection | three sites, see *Design system* | `BoxWithConstraints` + `expect`/`actual` `isTv` |
| `zxing` is JVM/Android-only | device-pairing QR | per-platform implementation |
| `BitmapFactory` | `util/ImageUtils.kt` | `expect`/`actual` |
| `BootReceiver`, Manifest, leanback launcher | `app/` | stays Android-only, never shared |
| `MosqueConfigEntity` is simultaneously the domain model and the Realm entity | `data/MosqueConfigEntity.kt` (191 LOC) | split into a `MosqueConfig` `data class` in `core:model` + a Room entity + a mapper |

That last one is worth doing for its own sake: it is the root cause of the
"adding a config field means editing all three places" problem documented under
*Persistence*, and a plain `data class` gets `copy()` from the compiler —
deleting the hand-written `copy(...)` extension entirely.

### Migration order

Each step is independently reviewable. Steps 1–6 stay Android-only and remain
shippable throughout.

| # | Step | Releasable? |
|---|---|---|
| ~~1~~ | ~~`build-logic` + `core:model` + `core:prayer`; fix `PrayerCalculator` purity~~ **done** | yes |
| 2 | Extract `ConfigRepository` interface + Koin; delete `getInstance()` | yes |
| 3 | Realm → Room in `data`, shared entity/wire classes, `MosqueSnapshot` (see data-loss warning) | yes |
| 4 | `core:protocol`; `org.json` → kotlinx.serialization on both sides | yes |
| ~~5~~ | ~~`ui:design` — tokens, window size class~~ **phase A done**; `App*` components pending sign-off | yes |
| 6 | `ui:settings` + `ui:display` into `commonMain` | yes |
| 7 | `webApp` (wasmJs) — runs *alongside* the existing HTML panel | yes |
| 8 | Delete `WebPage*.kt` + `*WebSegment.kt` (−9.1k LOC) | only after 7 reaches parity |
| 9 | `iosApp` — controller + preview | yes |

**Step 3 wipes device data.** `RealmConfiguration` already uses
`deleteRealmIfMigrationNeeded()` and there are no migrations, but this app runs
in production in a real mosque — the live install's prayer offsets, media,
announcements and schedules would be lost. Export the config through the
existing web panel before upgrading that device, or write a one-off export step.
Do not discover this on the mosque's TV.

**Step 3 can be deferred until after step 7.** Room is not a prerequisite for
the web panel — the panel is a client and needs no database. Running
1 → 2 → 4 → 5 → 6 → 7 → 3 reaches the visible payoff sooner and keeps the only
risky step off the critical path, at the cost of carrying Realm longer.

### TV navigation — the D-pad rules

The display is driven by four arrow keys and Enter, so **focus visibility is not
polish here, it is whether a control is reachable at all.** Material3 indicates
*press* with a ripple, which a D-pad hover never fires — so a stock Material
control on this app looks identical whether or not the remote has landed on it.
That was the reported discomfort in 1.4.x.

Rules for anything interactive you add:

1. **Use a `Mosque*Button` variant.** All eight carry the focus treatment.
2. **Anything else focusable gets `Modifier.focusHighlight(shape, scaleUp)`**
   (`:ui:design`). Pass the element's own shape or the ring will not sit on its
   edge. Set `scaleUp = false` inside lists and tight rows, where a growing item
   shoves its neighbours.
3. **Every dialog needs `Modifier.initialFocus()`** on its confirm action.
   Without it the dialog opens with nothing focused, so the first arrow press and
   Enter both do nothing.
4. `OutlinedTextField` needs neither — `mosqueTextFieldColors` already turns the
   border gold on focus.
5. The dashboard and countdown screens are exempt. Their taps are a full-screen
   dismiss and direct actions, and the dashboard reserves D-pad up/down for the
   settings entry sequence, so there is no traversal to indicate.

Current coverage: 100 elements show focus; all 8 dialogs start focused.

**Focus rings do not appear when you tap the screen, and that is correct.**
Compose keeps `clickable` nodes out of the focus system while in *touch* input
mode, so that tapping does not leave rings behind. A television is always in
non-touch mode. Consequences worth knowing:

- Installing on a phone and tapping shows no rings. Use a remote, a D-pad, or an
  attached keyboard to see them.
- Any instrumented test that asserts focus must call `RequestDpadInputMode()` at
  the top of `setContent`, or it fails on a phone while the shipped code is
  correct. `InputModeFocusTest` exists to make that failure legible — it isolates
  plain focusability from `clickable` participation, so a future focus failure can
  be placed in one run.

**Known gaps, wanting a real remote rather than more reading:**

- **Traversal order is undeclared.** No `focusGroup` or `focusProperties`
  anywhere, so moving between the settings sidebar and the content pane is
  whatever Compose's default 2D focus search decides.
- **Dialogs do not trap focus**, so arrows can in principle walk out to the
  screen behind them.
- One clickable row in `ui/component/Schedule.kt` still has no ring.

Covered by tests: `FocusHighlightTest` captures rendered pixels and asserts the
ring appears on focus and leaves with it; `InitialFocusTest` asserts a dialog's
confirm action holds focus, including when the dialog is composed after the
screen behind it. Run with `./gradlew :ui:design:connectedDebugAndroidTest` — the
Xiaomi test device rejects the APK with `INSTALL_FAILED_USER_RESTRICTED` on
roughly every other attempt; re-running is enough.

### Consolidation proposal (phase B — awaiting sign-off)

Phase A was a 1:1 extraction, so the token set is deliberately redundant. Phase B
proposes what to merge. **Every merge here moves pixels**, which is why it is a
proposal rather than a commit, and why it is the one part of step 5 that cannot be
machine-verified as neutral.

Visual version, with swatches and type specimens at true size:
<https://claude.ai/code/artifact/4b371afb-8e98-4345-92b6-b160caa03e0d>

| Family | Now | Proposed | Call sites moved |
|---|---|---|---|
| Colour | 39 | 19 | 61 |
| Font sizes | 22 | 17 (12 display kept, 10 settings → 5) | 84 |
| Component sizes | 0 tokens, 246 literals | 6 tokens covering 90 | 0 (naming only) |

- **Colour.** One gold carries 135 of 138 accent uses; the other three appear once
  each. Ten near-blacks across 19 uses collapse to four. Ten white-emphasis levels
  collapse to four — full white and 0.6 alone cover 185 of 228 uses. Eight
  fill/border alphas collapse to four. Two reds become one.
- **Font sizes.** The consequential one. 13/14/15/16sp carry 118 of 189 uses while
  differing by a single point. Surviving settings scale: **24 · 20 · 16 · 14 · 12**,
  each then holding a value per window class — that is what gives the TV larger type
  with no `if (isTv)` at a call site, and retires the hand-written
  `fontSize = if (isCompact) 13.sp else 15.sp` conditionals. 84 sites shift by
  1–2sp, mostly upward, so a phone-layout pass should follow.
- **Still unresolved:** `10.dp`, used 11 times as spacing, sits between
  `spacing.md` (8) and `spacing.lg` (12); nine off-scale component sizes (6, 10,
  14, 18, 28, 40, 44, 56, 64) each need an individual call.
- **Buttons: audited, and smaller than feared.** The variants already exist as
  `TvFocusable*` wrappers (see *Design system*), and the raw call sites are
  consistent about which look they use: accent+onAccent+`radius.md` for primary,
  `fill`+`textPrimary` for secondary, `fill`+accent for preview, bordered for
  tertiary. So the work is not "design six variants" but "move four wrappers to
  `:ui:design`, add a tertiary, and fold in the 13 sites that bypass them". The
  only pixel movement is button height: `RemoteControlSettingsSegment` pins
  `48.dp` while the wrappers use the Material default.

### Do not

- **Do not delete the `WebPage*`/`*WebSegment*` files before step 7 reaches
  parity.** The app is in production; keep the old panel serving until the
  replacement is proven.
- **Do not port the server** to iOS or web.
- **Do not add a Navigation graph.** The `when (AppUiState)` state machine is
  deliberate, works, and is trivially portable.
- **Do not rebuild the ViewModel/state layer.** See *What is not being rebuilt*.
- **Do not change the mockup.** See the constraint under step 5.
- **Do not run the steps as one branch-wide rewrite.** Each is a separate,
  reviewable commit on `kmp`.

### Open decisions

**How conflicts are resolved.** Storage is settled; the merge model is not.
Comparing only local-versus-server fires the confirmation dialog even when just
one side changed, which trains people to dismiss it — and then they dismiss the
one that mattered. Keep **three** snapshots instead:

| State | Action |
|---|---|
| `local == base` | server wins silently |
| `server == base` | local is pushed silently |
| both differ from `base` | *now* ask the user |

where `base` is the snapshot as of the last successful sync. Without `base` the
feature works technically and loses data in practice. Whether resolution should
ever become per-field rather than whole-snapshot is a product decision, and the
answer changes the storage choice — see the SQLDelight note above.

**Compose for web renders to a canvas, not the DOM.** No native text selection,
no browser zoom semantics, and a multi-MB bundle. For a control panel served
over the mosque LAN this is an acceptable trade — but it is a decision, not a
surprise to be discovered later.

**`v.1.4.x` still carries the false design-system documentation.** That branch's
CLAUDE.md tells any agent to use `MosqueTheme.colors` and `App*` components that
do not exist. Fixing it there is docs-only and touches no code, but the user has
deliberately scoped changes to this branch.
