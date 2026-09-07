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

### Checking the layout on a phone

The settings UI is authored for a television and goes wrong on a narrow screen in ways
a screenshot does not make obvious. Two shipped examples: a heading that rendered as
`"Dashboard Background ListRandomize"`, and a fourth option clipped to `"INSTAN"`.
Neither was caught by looking at the screen. Comparing pixel bounds catches them:

```bash
adb -s <serial> shell uiautomator dump /sdcard/ui.xml
adb -s <serial> shell cat /sdcard/ui.xml > /tmp/ui.xml
python3 tools/sweep_layout.py /tmp/ui.xml 1080 "Display & Media"
```

It reports `CLIPPED` (a node reaching the screen edge) and `COLLIDE` (two nodes on one
row overlapping). Navigate between dumps yourself; it inspects one screen at a time.

Three things learned driving these devices, each of which cost time:

- **`adb shell input keyevent` does not reach the app.** Injected events carry input
  source *unknown*, which the D-pad handling ignores. Use `input dpad -d 0 keyevent
  <code>`. The `-d 0` matters whenever a second display is attached — Android Studio
  mirroring adds one, and without it key events go to the wrong display and silently do
  nothing. This is a test-harness quirk, **not** a bug in `SecretSequence`.
- **The UI is Compose, so assert on the accessibility tree, not screenshots.**
  `uiautomator dump` gives text and exact bounds; a screenshot cannot be diffed
  meaningfully against a photo background.
- **A black screenshot usually means standby, not a crash.** Check the file size: under
  ~50KB is a blank screen. Confirm with `standbyModeEnabled` from
  `GET /api/get_config`, and note that `jumatStandbyOnly` suppresses standby on
  non-Fridays, so the flag being `true` does not mean the screen will dim today.

Known false positive, already filtered: an `OutlinedTextField`'s floating label is a
separate node whose bounds nest inside the field's, so label and value always look like
they overlap.

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
- Settings exist twice on purpose: `MosqueConfigEntity` (a Realm object, in `:app`) and `domain.MosqueConfig` (a `data class` in `core:model`). The repository maps between them at its own boundary, in `data/EntityMappers.kt`, so nothing above the repository sees Realm. **Adding a config field means editing the entity, the domain class, and the mapper** — and since Room landed beside Realm, *both* entities and *both* mappers until Realm leaves in 2.1 — and `MosqueConfigRepository.updateConfig` too if the field should be part of a whole-config write. The mapper is the one the compiler will not catch on its own, because every domain field has a default: a field left out of `toDomain()` still compiles and then reads back as its default no matter what is on disk. `EntityMappersTest` fails on exactly that, so add the field and run `./gradlew :app:test`.
- `updateConfig` deliberately copies **47 of the 55** config fields. The other eight belong to dedicated setters (`updateThemeSettings`, `updateCustomBackground`, `updateShowSettingsOnTv`, the PIN), so a whole-object write would clobber a theme the caller never touched. Do not "fix" it by generating a total write.
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

### Settings UI

One ViewModel per settings category, each exposing a single `uiState: StateFlow<XUiState>`
and a single `onEvent(XEvent)`. Composables receive `state` + `onEvent`.

| ViewModel | Lines | Surface |
|---|---|---|
| `SettingsViewModel` | 236 | the shared/global settings |
| `DisplayMediaViewModel` | 243 | `uiState` + `onEvent` |
| `PrayerScheduleViewModel` | 162 | `uiState` + `onEvent` |
| `FridayViewModel` | 145 | `uiState` + `onEvent` |
| `AdzanIqomahViewModel` | 95 | `uiState` + `onEvent` |
| `RemoteDevicesViewModel` | 87 | `uiState` + `onEvent` |

Two earlier revisions of this file were both wrong about this section, in opposite
directions — the first described this pattern before it existed, the second denied it after
the restructure had begun. **Measure before editing it a third time:** count
`StateFlow` properties in `SettingsViewModel`, and count composables with six or more
parameters. Today that count is two, `AdjustmentRow` (6) and `DropdownAdjustmentRow` (7),
both generic row helpers where the parameters are the content.

`SettingsScreen` resolves the five per-category ViewModels with `koinViewModel()`. They are
registered **once**, in `settingsViewModelsModule` in `:ui:settings`, which both
`:app`'s `AppModule` and `:webApp`'s `webModule` pull in with `includes(...)`. Do not add a
registration to one platform's module: while the two lists were separate, the browser build
registered one of the six and the whole panel rendered nothing — see *Debugging wasm* for how
expensive that was to trace.

What is real and worth keeping:

- Settings auto-save: every change writes to Realm immediately, and
  `getConfigFlow()` fans it back out to all screens and web clients.
- High-frequency text input is buffered in local `remember { mutableStateOf(...) }`
  inside leaf composables and only emitted when parsed and valid, so typing does
  not cause a Realm write per keystroke.
- `LocalWindowClass` drives the phone layout; see *TV navigation*.

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

  **No raw `IconButton` or `TextButton` remains.** This file used to record 15 raw
  `IconButton`s as "the real gap ... it has not been written"; `MosqueIconButton`
  now exists and all 15 call sites use it, and `MosqueTextButton` likewise. Counted,
  not assumed — grep before editing this paragraph again.

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

- **The state pattern.** This entry has been wrong in both directions. It first
  claimed one-ViewModel-per-category existed before it did; it was then retracted as
  "1028 lines and eleven flows". The restructure has since landed, so the pattern is
  real now — see *Settings UI* above for the measured shape. Keep it.
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
   boundary is what catches it. **True across the whole settings and display layer
   as of 2026-08-26.**
4. **`expect`/`actual` only where genuinely unavoidable** — currently video playback,
   image decoding, file picking, transient messages, system back, and TV detection.
   Any other use is a design smell.
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

- ~~**One set of classes, annotated twice.**~~ **This does not compile, and the reason is the wasm
  target.** The plan was for a Room `@Entity` and the wire model to be the same `@Serializable`
  data class in `core:model`. But `core:model` targets `wasmJs` — that is the guardrail that
  catches JVM assumptions — and Room has no wasm target, so the annotations are simply not there
  to apply. The plan predates the target.

  What shipped instead: Room entities live in `:app`, replacing the Realm ones one for one, and
  `RoomMappers.kt` maps them to the domain classes exactly as `EntityMappers.kt` does for Realm.
  That is the "edit two places" arrangement the original plan wanted to avoid, and it is the price
  of `core:model` staying free of Android. The mappers were generated from the Realm ones rather
  than retyped, since the field names are identical, and a coverage test fails if a column is
  dropped in either direction.
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

**`SettingsServer` keeps `org.json`, deliberately.** It is the one file where removing it
buys nothing. The reason `org.json` has to go everywhere else is that it is Android-only
and those files are bound for `commonMain` — but the server is *also* Android-only and
is never being ported, so converting its 137 reads and 31 builders would be a large,
risky change to a 1,905-line file in exchange for consistency alone.

There is a further trap if someone tries: `org.json`'s output key order is hash-based
while `kotlinx.serialization`'s `JsonObject` preserves insertion order, so the
byte-identical check that made the other conversions safe does not apply here. Any such
change would need parsed, order-insensitive comparison instead.

It becomes worth doing when `core:protocol` grows typed messages and the server can share
them with the panel — which is the same work, done once, with a payoff.

**Pattern — unidirectional data flow.** The display screens (`AppUiState`,
`DashboardUiState`) follow it, and settings now does too: one `uiState` and one `onEvent`
per category — see *Settings UI* above. Formal MVI reducers are still not worth the
ceremony.

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
| `sendRemoteAction` builds `org.json.JSONObject` | `data/remote/RemoteActionSender.kt` | kotlinx.serialization. Note `kotlinx-serialization-json` is already on the classpath via `core:model` (see `joinFooterLines`), so step 4 no longer starts from zero |
| `HijriDateFormatter` uses `UmmalquraCalendar` + `java.util.Calendar` | `util/HijriDateFormatter.kt` | **unblocked, not solved** — it now implements `HijriDateSource` (`core:model`) and is injected, so the display screens are portable without it. A real iOS/web implementation still needs the Umm al-Qura month table: it is set by decree, not computable, and an approximation would shift the displayed date by a day |
| ~~`VideoView` through `AndroidView`~~ | ~~`ui/dashboard/DashboardScreen.kt`~~ | **done** — `expect VideoSurface` in `:ui:display` |
| ~~`LocalConfiguration` / `UiModeManager` size detection~~ | ~~three sites~~ | **done** — `LocalWindowInfo` + `expect rememberIsTv()` |
| `zxing` is JVM/Android-only | device-pairing QR | per-platform implementation |
| `BitmapFactory` | `util/ImageUtils.kt` (settings side only now) | `expect`/`actual`; the display side is done via `expect decodeImageFile` |
| `BootReceiver`, Manifest, leanback launcher | `app/` | stays Android-only, never shared |
| ~~`MosqueConfigEntity` is simultaneously the domain model and the Realm entity~~ | ~~`data/MosqueConfigEntity.kt`~~ | **done** — nine plain data classes in `core:model`, mapped in `data/EntityMappers.kt`; the entity dropped 191 → 76 LOC and the hand-written `copy(...)` is gone |

That last one is worth doing for its own sake: it is the root cause of the
"adding a config field means editing all three places" problem documented under
*Persistence*, and a plain `data class` gets `copy()` from the compiler —
deleting the hand-written `copy(...)` extension entirely.

### Step 6: moving the UI to Compose Multiplatform

**Compose Multiplatform keeps the `androidx.compose.*` package names.** CMP is built
from the AndroidX sources; only the Maven coordinates differ. So moving a module to
Compose Multiplatform changes its *dependencies* and touches *no imports at all* —
`ui:design` moved with 15 of its 17 source files byte-identical. There is no atomic
53-file import swap, and no need to sequence the migration around one.

Two things were verified rather than assumed, because both are easy to get wrong:

**Every Material3 component this app uses exists in CMP** and compiles for `wasmJs`,
`iosSimulatorArm64` and `jvm`: `SecondaryTabRow`, `DatePicker` +
`rememberDatePickerState`, `HorizontalPager`, `BadgedBox`/`Badge`,
`ModalNavigationDrawer` + `rememberDrawerState`, `AlertDialog`, `DropdownMenu`.

**Use the `compose.*` dependency accessors, never hand-written coordinates.** CMP
version numbers are not uniform across artifacts: under the 1.9.0 plugin, `runtime`
and `foundation` resolve to 1.9.1 while `material3` resolves to **1.8.2**, and
`org.jetbrains.compose.material3:material3:1.9.1` does not exist at all. Declaring the
artifacts in the version catalog against a single version therefore fails to resolve.
The plugin knows its own mapping. This is why `ui/design/build.gradle.kts` re-applies
`id("org.jetbrains.compose")` in its own `plugins` block even though the convention
plugin already applied it — the redundant application is what makes Gradle generate
the accessors.

**One exception, in the root `build.gradle.kts`: material3 is pinned to 1.9.0.** The
accessor's 1.8.2 was compiled against kotlinx-datetime **0.6.0**; we resolve 0.7.1, where
`Clock` and `Instant` have moved to `kotlin.time` and the old classes are gone. Gradle sees
no conflict and the build succeeds — then wasm throws at first use:

```
IrLinkageError: Can not get instance of singleton 'System':
    No class found for symbol 'kotlinx.datetime.Clock.System'
```

Any `DatePicker` or `TimePicker` therefore killed the composition, which is what made the
browser panel render a blank canvas. **Android was never affected**: its material3 variant
uses the AndroidX calendar model and does not touch kotlinx-datetime, so this is invisible
until something renders on wasm. material3 1.9.0 declares kotlinx-datetime 0.7.1 and asks
for foundation/runtime/ui 1.9.1 — exactly what the plugin already resolves — so the pin
aligns the graph rather than forcing a mismatch. It is applied per-dependency rather than
with `force()` because each KMP target is its own module (`material3-android`,
`material3-wasm-js`). Remove it when the plugin's own mapping catches up.

This is the concrete instance of the warning under *Build & test*: a kotlinx-datetime bump
breaks things broadly. Worth knowing that it can break at **runtime linkage** and not at
compile time, so a green build proves nothing here.

### What is actually Android-only

| API | Uses | What it needs |
|---|---|---|
| ~~`ActivityResultContracts`~~ | ~~15~~ | **done** — `rememberMediaPicker` in `:ui:design` |
| ~~`rememberLauncherForActivityResult`~~ | ~~14~~ | **done** — same seam |
| ~~`android.net.Uri`~~ | ~~21~~ | **done** — `PickedFile` in `core:model` |
| ~~`BitmapFactory` / `asImageBitmap`~~ | ~~11 / 11~~ | **done** for UI — `decodeImageFile` in `:ui:design`; only `util/ImageUtils.kt` still uses it |
| ~~`LocalContext`~~ | ~~7~~ | **done** in settings — the last one read the version string, now `LocalAppVersion` |
| ~~`PickVisualMediaRequest`~~ | ~~7~~ | **done** — same seam |
| `UiModeManager` | 4 | `expect` TV detection — **done**, see below |
| `AndroidView` | 1 | `expect` video player — the `VideoView` in DashboardScreen |

`MediaFileStore` and `rememberIsTv` were built as the seams for two of these.

**`ui:design` is migrated.** It needed exactly one `expect`: `rememberIsTv()`. The
`jvmMain` actual returns `false` permanently — a desktop build is a controller, never
the mosque display — which is a decision, not a stub. Unit tests moved to `commonTest`
and were converted from JUnit to `kotlin.test` so they run on the `jvm` guardrail
target as well as Android; note that `kotlin.test` takes its assertion message **last**
where `org.junit.Assert` takes it first. Focus tests stay in `androidInstrumentedTest`,
because whether a focus ring draws depends on modifier order at runtime.

**Practical cost to plan around:** the iOS target takes about 8½ minutes to compile
from cold. Expect a much slower edit-build loop than the Android-only work so far.

### Step 2: the repository does not want one interface — it wanted three

**Written as two, and a third arrived when the store was replaced.** `ServerDataSource` extends
`ConfigRepository` with the eight members only `SettingsServer` uses — device registration,
presence, the raw Kemenag rows and the panel's announcement list. It exists because the server took
the *concrete* Realm class, and that type was the last thing pinning the app to Realm: a Room
repository could satisfy every settings member and still be unusable.

This file used to state the opposite as a decision — the server is Android-only and never being
ported, so abstracting it bought nothing. That was right until there were two stores. The interface
is not for portability; it is what lets the store be replaced at all.


Counting the call sites changed what step 2 should build. The repository has **70
public members**. Of those:

| Consumer | Members used |
|---|---|
| the display screens (Dashboard/Adzan/Iqomah + `AppViewModel`) | **9**, all reads |
| settings | 56 |
| `SettingsServer` | 56 |

So a single 70-member `ConfigRepository` interface would force the display screens to
depend on 61 members they never call, and every one of those is a mutation. Two
interfaces is the better shape, and the split falls out of the roles the app already
has: the display is a pure function of state, the controller edits it.

**`DisplayDataSource` is those nine**, in `core:model`: `getConfigFlow`, `getConfig`,
`getMediaListFlow`, `getDashboardBackgroundsFlow`, `observeFridayMedia`,
`getActiveAnnouncementsFlow`, `getStandbySchedules`, `getSlideshowSchedules`,
`getIqomahMinutes`. `MosqueConfigRepository` implements it, and Koin binds the same
singleton twice — `single<DisplayDataSource> { get<MosqueConfigRepository>() }` — so
there is no second Realm. `DashboardViewModel`, `AdzanViewModel`, `IqomahViewModel`
and `AppViewModel` now take the interface.

The payoff beyond portability: a display bug can no longer be a stray write, and a
reader can see that from the type rather than having to check.

**`ConfigRepository` covers the other 49**, extending `DisplayDataSource` — which also
states the relationship plainly: a controller does everything a display does, plus writes.

One interface rather than one per settings category, and that was measured rather than
assumed. The five category ViewModels use 3, 5, 8, 10 and 29 members, but **46 of the 55
are also used by the web server**, and configuration writes appear in every category. The
categories partition the *screens*, not the data, so per-category interfaces would mostly
duplicate one another. `DisplayDataSource` earns its separation because read-only versus
read-write is a real boundary; category boundaries are not.

`SettingsServer` still takes the concrete class deliberately — it is Android-only and
never moving, so abstracting it buys nothing.

**What still ties `ui:settings` to `:app`** is now small enough to list precisely. The
repository, the file picker, media storage, HTTP and the app version are all behind
interfaces or `expect`s. What remains is whatever `SettingsScreenShared.kt` and the
segment files still reach for — audit before moving the module, since the ViewModels and
the two big panes are clean.

Koin resolves at runtime, so `AppModuleTest.everyDependencyInTheGraphCanBeResolved`
is what catches a missing binding — verified by deleting the `DisplayDataSource` line
and watching it fail.

### `:ui:display` — what the move actually cost

Dashboard, Adzan and Iqomah are in `:ui:display` and compile for `jvm`, which is what
proves no Android API is left in `commonMain`. Packages were kept identical, so `:app`
imports did not change.

Two `expect`s, as predicted:

- **`decodeImageFile(path): ImageBitmap?`** — also de-duplicated three dashboard call
  sites. The `catch (e: Throwable)` inside it is load-bearing and must stay `Throwable`:
  `OutOfMemoryError` on an oversized image is the likely failure here, and a corrupt
  file dropped on the TV has to fall back to the default background rather than take the
  display down.
- **`VideoSurface(path, loop, onEnded)`** — keeps two behaviours that look incidental
  and are not: always muted, because the adzan comes from the building's sound system;
  and `onEnded` fires on *error* as well as completion, because otherwise one unplayable
  file freezes the display on a black rectangle.

Two things that were **not** `expect`, and should not become one:

- **Hijri dates are an interface** (`HijriDateSource` in `core:model`), because
  `ummalqura-calendar` is not a platform API — it carries the real Umm al-Qura
  month-length table. That calendar is set by decree and cannot be computed, so a port
  means finding the table for each platform, not writing arithmetic. Injecting it made
  the display screens portable without answering that question.
- **The three built-in backgrounds arrive as `Painter`s** through
  `LocalDisplayBackgrounds`, provided by `MainActivity`. `main_bg_default` cannot move:
  `SettingsServer` serves the same file to the web panel as raw bytes, so it must stay
  reachable as an Android resource. Compose Multiplatform resources are the eventual
  home, once the panel no longer needs bytes.

Also removed on the way: `org.json` was hiding in `AdzanViewModel` and
`IqomahViewModel`, parsing footer text that is stored as *either* a plain string or a
JSON array — history, not design. `joinFooterLines` in `core:model` handles both.

**`Schedule.kt` and `SlideshowComponent.kt` stayed in `:app`** at the time of the `:ui:display`
move, despite living in `ui/component`, because settings and the server both used them while the
dashboard renders its slideshow inline. They have since moved *with* `ui:settings`, which is where
that paragraph said they belonged — so look for them in
`ui/settings/src/commonMain/.../ui/component/`, not in `:app`.

### Step 4 so far: the action names are shared

`core:protocol` exists and holds `RemoteActions` — the 47 action names of the WebSocket
protocol as constants. `SettingsServer`'s `when` and the panel's JavaScript both
reference them, so a name cannot be spelled differently on the two sides.

This was not a tidiness exercise. Each name used to be typed twice, in two languages,
in two files, with nothing connecting them — and the panel's device-rename button had
been sending `register_device` while the server only ever handled `register`. The
message was dropped and the panel showed a success toast. The `when` had no `else`, so
an unhandled action left no trace; it does now.

Two things make the change safe to trust:

- **The panel the device serves is byte-identical.** Fetched `http://<tv>:8080/` from
  the Xiaomi before and after; 417,376 bytes both times, `cmp` clean. The constants hold
  the strings the protocol already used, so an older panel talking to a newer server
  still works.
- **`ActionNameDriftTest` fails if either side writes a literal again.** Mutation-tested
  three ways: a bare `action: '…'` in the JS fails it, a bare `"…" ->` branch fails it,
  and an action name appearing only in a *comment* does not — comments are stripped
  before matching, which matters because this test reads source rather than behaviour.

Reading source in a test is unusual; the justification is that the failure being guarded
against is textual, and there is no runtime moment at which an action nothing handles can
be observed. That is precisely what made the original bug invisible.

**Four action names are still literals in the JavaScript**, and must stay that way: the
schedule actions are assembled as `'update_' + type + '_schedule_days'`. They are why a
name-by-name diff of the two sides shows ten apparent mismatches that are not real.

Still to do in step 4: `org.json` itself. It remains in `SettingsServer`, the remote-sync
path in `SettingsViewModel`, and `RemoteActionSender`. That is the kotlinx.serialization
half, and `kotlinx-serialization-json` is already on the classpath via `core:model`.

### The seams `:ui:design` owns

Nine `expect` declarations. `androidMain` depends on `androidx.activity:activity-compose`
(picker, back handling) plus `zxing` and `zxing-android-embedded`; `jvmMain` depends on
`zxing`:

| Seam | Android | jvm |
|---|---|---|
| `rememberIsTv()` | `UiModeManager` | `false`, permanently — a desktop build is a controller |
| `decodeImageFile(path)` | `BitmapFactory` | `ImageIO` |
| `rememberMediaPicker` / `rememberMultipleMediaPicker` | `PickVisualMedia` | no-op |
| `rememberAudioFilePicker` | `OpenMultipleDocuments` | no-op |
| `MessageHost(messages)` | `Toast` | `SnackbarHost` |
| `SystemBackHandler(enabled, onBack)` | `BackHandler` | no-op — no system back button to intercept |
| `encodeQrCode(content, sizePx)` | zxing → `Bitmap` | zxing → `BufferedImage`, a **real** implementation |
| `rememberQrScanner(prompt)` | `ScanContract` | no-op |

**The QR pair is asymmetric on purpose, and the asymmetry is the lesson.** zxing's core is
pure Java, so encoding has a genuine jvm implementation; scanning is a camera pipeline, a
preview surface and a permission prompt, so its jvm actual is a no-op. Encoding is a
computation any platform can do; scanning is a feature. An `expect` whose only
implementation is Android would be indistinguishable from leaving the code in `:app`.

Two of these are worth understanding rather than just using:

**`MessageHost` is an `expect` rather than a shared `Snackbar`, deliberately.** A
`Snackbar` participates in focus and traversal, and this screen is driven by a D-pad —
an element that appears for four seconds and rearranges the focus graph is a navigation
bug that gets reported as "the remote stopped working". It also pins to the bottom edge
of a 55" screen, metres from where the user is looking. `Snackbar` is still right *off*
Android, which is what the jvm actual uses; the seam lets each platform be correct
instead of averaging them.

**The picker's callback is passed to `launch()`, not to `remember`.** With the callback
fixed at composition, "replace this one slide" forced each pane to park the row id in its
own `remember { mutableStateOf<String?>(null) }` and read it back later. Passing it per
launch lets the call site capture the id in the lambda, which deleted both id holders and
collapsed five launchers to four.

`PickedFile` is an interface, not an `expect class` — an `expect class` implies a
no-argument constructor and `android.net.Uri` is abstract, so a typealias can never
match. `AndroidPickedFile` lives in `core:model`'s `androidMain` because `:ui:design`
constructs it and `:app`'s `MediaFileStore` unwraps it, and those two must not depend on
each other.

### What is left before `ui:settings` can move

**`ui/settings` has no platform imports at all** — no `android.*`, no `androidx.activity`,
no `java.*`, no `org.json`. What remains are references into `:app` itself:

| Reference | What it needs |
|---|---|
| `SettingsServer.getIpAddress()` | a "what is my LAN address" seam — the next real one |
| `com.groovernapp.mosqueathan.R` (2 uses) | a `Painter` passed in, like `LocalDisplayBackgrounds` |
| `ui.theme.MosqueAthanTheme` (3 uses) | call `MosqueTheme` directly; this alias was always meant to go |
| `ui.component.Schedule`, `SlideshowComponent` | not blockers — they belong *with* `ui:settings` and move together |

Also worth knowing: `:ui:design`'s jvm test classpath cannot construct an `ImageBitmap` —
Skia's native library is absent, so `BufferedImage.toComposeImageBitmap()` throws
`ExceptionInInitializerError`. Any seam returning an `ImageBitmap` is therefore only
*null*-testable on that target, which is why `QrCodeTest` and `ImageDecoding` assert the
failure contract rather than a successful decode.

Three duplications were removed on the way there, and they are worth knowing about because
each was a *different* answer to the same question:

- **"Is this a television?"** `Schedule.kt` asked the package manager whether the leanback
  *feature* is present; `MosqueTheme` asks `UiModeManager` for the current mode *type*.
  Two definitions in one app that can disagree. Now both use `rememberIsTv()`.
- **Indonesian month names** existed three times: `DashboardViewModel`, the settings
  schedule list via `SimpleDateFormat("MMMM yyyy", Locale("id"))`, and again as a literal
  array in the panel's JavaScript. `Month.toIndonesian()` in `core:model` is now the one
  copy the Kotlin side uses; the JavaScript array is step 8's problem.
- **ISO date ↔ picker millis** was written twice with `java.util.Calendar`. Now
  `isoDateToUtcMillis` / `utcMillisToIsoDate`, tested at the boundary — `1970-01-01` must
  be exactly `0`. Getting that wrong shifts a scheduled date by a day, and on a Friday
  schedule that means the Friday layout appears on Thursday.

### Step 6 is done: what the module boundary cost, and caught

`:ui:settings` exists — 32 files, compiling for the `jvm` guardrail. `:app` is down to 56
files: the server, the Realm data layer, `MainActivity`, `BootReceiver`, DI, and the
`when (AppUiState)` state machine in `ui/navigation`.

Four things only surfaced once a real Gradle boundary enforced them, which is the argument
for module boundaries over conventions in one paragraph:

- **`koin-androidx-compose` is Android-only.** `koinViewModel()` had to become
  `org.koin.compose.viewmodel.koinViewModel` from `koin-compose-viewmodel`. `:app` still
  uses the androidx variant for its own call sites.
- **CMP's material3 marks APIs experimental that Google's 1.4.0 does not.**
  `SecondaryTabRow`, `DatePicker` and `rememberDatePickerState` needed
  `@file:OptIn(ExperimentalMaterial3Api::class)` in five files — a real difference between
  the two artifacts, not a code smell.
- **`allWarningsAsErrors` is not set on `:app`**, so a deprecated `Divider` had been warning
  quietly for however long. The module inherits the flag and the warning became a build
  failure, which is exactly what it is for.
- **Two unused imports were load-bearing evidence**: `util.ImageUtils` and
  `com.groovernapp.mosqueathan.R` looked like blockers in the audit and turned out to be
  imports nothing used. Worth checking usage, not just imports.

**`androidx.lifecycle` and `androidx.compose` are both multiplatform**, so no import in the
32 files changed for either.

### Step 7 groundwork: every shared module compiles for wasmJs

All six — `core:model`, `core:prayer`, `core:protocol`, `ui:design`, `ui:display`,
`ui:settings`. No `webApp` module yet; this is the part that had to come first, because it
turns "will the UI port?" from a question into a measurement.

**What wasmJs caught that the jvm guardrail did not.** This is the whole reason it was
added rather than trusted:

- **`String.format` in six places.** `"%02d:%02d:%02d".format(...)` resolves to
  `java.lang.String.format`. The jvm target compiles it happily; wasm does not have it.
  These formatted the *clock on the mosque display*, so `PaddingTest` now pins
  `Int.pad(2)` against `%02d`'s behaviour — including that a value wider than the pad is
  printed in full, since a three-digit minute countdown must not lose a digit.
- **`androidx.lifecycle:lifecycle-viewmodel-compose` has no wasm variant.** The
  multiplatform artifacts are JetBrains' republish,
  `org.jetbrains.androidx.lifecycle:*` — the same arrangement as `org.jetbrains.compose`.
  This file previously said "androidx.lifecycle 2.10 is multiplatform", which was true of
  the runtime and not of the compose bindings.
- **Two unused imports** (`asImageBitmap`, `painterResource`) that the Android and jvm
  builds tolerated.

**What the seams cost:** `:ui:design` failed with exactly nine errors, one per `expect`,
and nothing else. `:ui:display` needed one more (`VideoSurface`). That is the payoff for
naming every platform difference — the port is bounded by a list you can read.

**The wasmJs actuals are not all equal, and the KDoc on each says which it is.** Three
kinds:

| Kind | Which |
|---|---|
| Correct as null/no-op | `rememberIsTv` (a browser is never the display), `decodeImageFile` (the paths belong to the *display's* filesystem and no browser can read them), `encodeQrCode` (only the display draws a pairing QR) |
| Real gap, bounded by a product question | `SystemBackHandler` — the browser has `popstate`, but what a settings drawer should do to page history is a product decision |
| Real gap, bounded by the canvas | `rememberQrScanner`, `VideoSurface` — Compose for web renders to a canvas, so a camera preview or a `<video>` has to be *composited* with it rather than laid out inside it. One obstacle, two features |
| Real gap, bounded by upload | the three media pickers — a browser can pick a file; getting it into the *display's* storage means uploading over mosque wifi, and `POST /upload` exists but the progress UX does not |

Do not "finish" these by writing plausible implementations. The pickers and the scanner
each need a decision first, and the KDoc records which decision.

### `webApp` renders — what it is and is not

`:webApp` builds and serves. It renders the *real* `SettingsScreen` from `:ui:settings`,
the same code the television runs, which is the entire point of the migration.

Run it with `./gradlew :webApp:wasmJsBrowserDistribution`, then serve
`webApp/build/dist/wasmJs/productionExecutable`.

**It has no transport yet.** The Koin graph binds `InMemoryConfigRepository` plus three
do-nothing collaborators, all in `web/WebModule.kt` — one file, so "what the web build
cannot do yet" is a place you can read rather than a hunt. A toggle stays toggled; nothing
is sent anywhere. The next step is a `ConfigRepository` backed by the display's own
`GET /api/get_config` and `POST /api/action`, which already exist and already serve the
hand-written panel.

**IndexedDB is not needed for that**, and it is worth being precise because it is easy to
assume otherwise: a *connected* controller is a client. Local storage is only for the
offline replica (`MosqueSnapshot`) — the takmir who edits at home. That is step 3's
business, not step 7's.

**The bundle is 14MB** — an 8MB wasm, a 3.3MB wasm, 576KB of JS. Measured, not estimated.
For a panel served over mosque LAN that is acceptable, which this file already said; the
number is now real rather than "multi-MB".

Three things the build itself needed, none of them obvious:

- **`repositoriesMode` had to drop from `FAIL_ON_PROJECT_REPOS` to `PREFER_SETTINGS`.** The
  Kotlin/Wasm browser toolchain adds its own Node and Yarn download repositories from
  inside the plugin, and the strict mode rejects that outright. The declared repositories
  are still consulted first; what is given up is the guarantee that nothing else can be
  added.
- **Node, Yarn and Binaryen repositories are declared in `settings.gradle.kts`** with
  content filters, so they are only ever consulted for those three artifacts.
- **`:webApp` does not use a `mosque.kmp-*` convention plugin.** Those declare an
  `androidTarget`, and this module has no Android side. It is the one place a hand-written
  `kotlin { }` block is correct rather than drift waiting to happen.

### Step 7 is done: the browser controls the display

`:webApp` is a working controller, verified end to end on hardware: a phone's browser changed
`adzanLeadMinutes` on the television, 3 → 4 → 5 → 3, over the mosque LAN.

**One WebSocket carries everything, and that is the whole transport.** On connect `DisplayLink`
sends `register`; the display answers with a `registration_result` and then the full state, and
re-sends the full state to every client on any change. So the same socket is the first read, the
live-update channel, and the write path. `POST /api/action` exists and works, but it builds a
throwaway connection and re-authenticates per request — the socket is already registered.

**`MosqueSnapshot` was not a new format.** `getCurrentStateJson()` already served exactly this
shape, and its keys were already the domain classes' property names. Measured against a live
device before writing any parser: all seven lists decoded with **no unmatched key**, and of the
config's fields only `hasPin` had no counterpart. So the domain classes became `@Serializable` and
decode straight off the wire — which is the "one set of classes, annotated twice" arrangement step 3
wants, arrived at early and for free.

What that check also found, and is worth knowing:

- **`jumatTransitionType` and `jumatVideoWaitUntilEnd` were never in the payload** — config fields
  the display did not serve, so every controller, this one *and* the hand-written panel, showed
  their defaults while the display held something else. Now served. The write path had existed on
  both sides all along, which is what made it invisible: the field could be *changed* from a
  controller and never *read* back.

  `ConfigPayloadCoverageTest` guards the class of bug rather than these two fields: it enumerates
  `MosqueConfig`'s properties reflectively and fails on any that the server neither serves nor
  withholds for a stated reason (`id`, `pinCode`, `connectivityMode`, `remoteTvIp`). It reads
  source, like `ActionNameDriftTest`, because a missing key is indistinguishable at runtime from a
  key whose value happens to equal the default — there is no moment where the absence shows.
- **`pinCode` never crosses the wire**; the display sends `hasPin`. `MosqueSnapshotTest` asserts the
  consequence rather than trusting the habit.
- **`connectivityMode` and `remoteTvIp` are not sent either**, and must not be adopted from a
  snapshot: they describe *this* device. `InMemoryConfigRepository.apply` carries the local values
  over, because without that the panel flips to "server" on the first snapshot it receives.

**The panel never gets a secure context, so secure-context-only APIs are unavailable.** It is served
over plain `http` on a LAN, so `window.isSecureContext` is false. `crypto.randomUUID()` is therefore
*undefined*, and calling it threw a TypeError before Koin finished building the graph — blanking the
whole page for the second time in this migration. Measured on the device: `getRandomValues` is
available in an insecure context and `randomUUID` is not. The same rule retires the clipboard and
`crypto.subtle`; check `isSecureContext` before reaching for anything in that family.

**Media upload works from the browser**, and was verified end to end on hardware: a file chosen in
the phone's own chooser arrived on the display as a new dashboard background. The wasm pickers are a
real `<input type="file">` now rather than a no-op, and `WebMediaStore` posts the raw file to
`POST /upload` — raw body, not multipart, because the server reads `Content-Length` bytes straight
into a file; FormData would write the MIME boundary into the image. Every `store*` returns null on
purpose: the upload endpoint adds the database row itself, so there is nothing local to add.

**Replacing a media file works too**, through a `replacePath` parameter added to `/upload`. Three
things about it are load-bearing rather than incidental:

- **The path is validated against the database, not trusted.** It arrives from another device on the
  mosque wifi, so `isReplaceableMediaPath` requires it to match a row the display already holds —
  media, Friday media, dashboard background, or the custom background — comparing *canonical* paths
  on both sides so `..` segments and symlinks cannot walk out of the media directories. Verified by
  attack, not by reading: an unknown path and a traversal built from a real media path both return
  403 and create nothing.
- **The upload is written beside the original and renamed over it only once the whole body has
  arrived.** Writing straight to the target would mean an upload that drops halfway destroys the
  file it was replacing, and over mosque wifi halfway is a normal outcome. Verified by sending a
  declared `Content-Length` far larger than the body and cutting the socket: the original was still
  intact, byte for byte, and no temp file was left behind.
- **No row is written.** The path does not change, so the broadcast after the rename is the whole
  notification — which is why `WebMediaStore.replaceFile` returns false. As with the `store*`
  methods returning null, false here means "dispatched, nothing local to do", and that overloading
  is the cost of a synchronous interface over an asynchronous upload.

Verified end to end through the panel's own edit control on hardware: a 297,806-byte background
became the 21,747-byte file chosen in the phone's chooser, same path, same row count, then was
restored byte for byte.

**Deleting media: the leak was on the display, not the controllers.** An earlier revision of this
file said the opposite, and it was wrong — worth recording, because the wrong version would have
sent someone to fix the wrong side. `SettingsServer`'s delete actions had always removed the file,
so deleting from the web panel or an Android controller cleaned up correctly. The display's *own*
settings UI did not: `DisplayMediaViewModel` deleted the row and left the file, and of the four
places a row can be removed only Friday media also deleted its file. Media deleted by someone
standing at the television accumulated in private storage; the same action from a phone did not.

The file now follows the row inside `MosqueConfigRepository`, so every caller gets it — the display's
UI, the panel, the Android controller, the server's handlers. The duplicate deletes in those handlers
were removed at the same time: two owners of one rule is how the display's side came to be missing.

`isPathOwned` guards the delete as well as the replace — the same question from opposite sides, "does
any row still name this file". Sharing it means the delete inherits the canonical-path comparison, so
two spellings of one file cannot pass as unreferenced. It lives in `data`, not `server`, because media
paths belong to the data layer.

Also unresolved:
the Kemenag city search calls `api.myquran.com` from the page, which is cross-origin — it works only
if that API sends CORS headers. If it does not, the fix is to route the search through the display,
which can reach the API unhindered, not to fight CORS from the browser.

### Pairing by QR, driven end to end with a real camera

A takmir's actual first move — point a phone at the display — verified rather than reasoned about.
Realme camera on the Xiaomi's screen, the display in Server mode showing its pairing QR.

**The QR reads correctly off a screen at normal distance.** A plain barcode scanner decoded
`http://192.168.1.3:8080`, exactly what `TvPairing.url` produces, and the app's own scanner then
turned it into the setting: the TV IP field went from a stale `192.168.1.8` to `192.168.1.3`
without a keystroke. `TvPairing.addressFrom` doing the parsing is what makes that a host and not a
URL.

Then the controller half: Adzan preparation `+` on the Realme moved the display's
`adzanLeadMinutes` from 3 to 4 within three seconds, and back. So `REMOTE` mode works end to end on
hardware, which had never been shown in one sitting.

Two things fell out of it that were not the point.

**The pairing QR lands on the *old* panel, and step 8 deletes it.** It encodes the bare origin, so a
scan opens `/` — whose title is still `MOSQUE ATHAN - Web Control Panel`, the hand-written page.
The new panel is at `/panel/`. **Before `WebPage*.kt` can be removed, `/` has to serve the new
panel or redirect to it**, or the first thing a takmir does after the upgrade is scan a code that
leads nowhere. This is a step 8 prerequisite and nothing else records it.

**An Android controller never registers.** The display's device list holds eight `web-*` and ten
`dev_*` ids and no `remote_*`: `RemoteActionSender` only POSTs actions, and "Connect" calls the
sync GET, so nothing ever sends `register`. Harmless while no PIN is set — and squarely in the way
of `verify_pin`, since a device the display has never seen cannot be the device it approves. Worth
knowing before that work starts.

**Testing note:** the settings entry sequence must arrive back to back. `input dpad -d 0 keyevent`
is necessary but not sufficient — four presses one second apart never opened settings, and the same
four in a single `adb shell "…; …; …; …"` opened it every time.

### The QR scanner is not a parity gap — no browser can have one

Looked at as the last thing standing between the panel and step 8, and it turned out not to be
standing at all.

**The camera is unreachable from the panel, permanently.** `getUserMedia` requires a secure context;
the panel is served over plain `http` on a mosque LAN, so `window.isSecureContext` is false —
measured on the device. The hand-written panel *does* carry `html5-qrcode`, and it also carries a
guard that refuses to start for exactly this reason, so its scanner has never worked either. Nothing
is being given up.

**It would buy nothing anyway.** The scanner's whole job is `onUpdateRemoteTvIp` — telling a
controller which display to talk to. A panel served *by* the display already knows: `displayHost()`
reads the host that served the page. The Android controller is the one that genuinely needs it,
because it is installed rather than served, and it keeps a working scanner.

So `rememberQrScanner` returns `QrScanner?`, null where no camera can be reached, and
`RemoteControlSettingsSegment` does not draw the control when it is null. Before this the web panel
showed a "Pindai QR" button that silently did nothing — the same defect as the Back button that went
nowhere, and found the same way: by pressing it.

Verified on the Xiaomi: the sync section now offers only Hubungkan and Sinkronkan Sekarang.

**Step 8 had no *known* blocker left**, and then the pane-by-pane comparison found one. Media
upload, replace and delete all work from the browser, the display serves the panel itself, the
Kemenag search is allowed cross-origin (`access-control-allow-origin: *`, checked against the live
API), and murottal audio is configurable and now actually plays. What the comparison turned up is
below — and it is large enough that **step 8 must not proceed yet.**

Two smaller things noticed on the way, neither fixed:

- `SettingsScreen` has `val showAllSettings = true` hardcoded, which makes the `REMOTE_DEVICES`
  starting category and the single-category sidebar unreachable. Dead branches, and the reason the
  Connectivity pane cannot be deep-linked to.
- The display cannot serve its own panel to its own browser: opening Chrome backgrounds the server,
  and the page then dies mid-load. Testing the panel needs a second device, always.

### The display serves the panel itself

The wasm bundle used to exist only behind a static file server on a developer's machine, which
made the browser panel undeliverable: a takmir has nothing to serve it from. The display is the one
thing on the mosque wifi that can hand it over, so `:app` now carries the bundle in its assets and
`SettingsServer` serves it at **`/panel/`**.

Verified with the laptop's server switched off: `http://<display>:8080/panel/` renders the settings
UI and reports `Connected`. `displayHost()` needs no `?tv=` there — the host that served the page
*is* the display, which is what that fallback was written for.

Details that are load-bearing rather than incidental:

- **`.wasm` must be served as `application/wasm`.** `WebAssembly.instantiateStreaming` refuses any
  other type outright, and the failure surfaces as a MIME-type error with nothing to do with this
  app.
- **The source map is excluded** — 1.7 MB of debug information no takmir opens, and it would
  otherwise be among the largest things in the APK.
- **`index.html` is served no-cache while the hashed files are immutable.** The bundle's filenames
  carry a content hash, so they can be cached hard; `index.html` is what points at whichever hash
  the installed build shipped, and a stale one names files that no longer exist.
- **`panelAssetPath` is the only thing between a request and the rest of the APK's assets.**
  `AssetManager` resolves `..` happily. It decodes before checking, because `%2e%2e%2f` is a
  traversal that only looks like one afterwards — mutation-tested by removing the decode, which
  fails the suite. Refused on the device too, not just in tests.

**Cost:** the APK grows by the compressed size of the bundle — the two wasm files are 13 MB on
disk and about 5 MB inside the APK. Every `:app` build now depends on `wasmJsBrowserDistribution`,
which is up-to-date-checked and so only re-runs when `:webApp` changes.

**`copyWebPanel` is a `Sync`, and it started life as a `Copy`, which was a slow leak.** The
bundle's filenames carry a content hash, so every change to `:webApp` produces new names — and
`Copy` never removes what the source no longer has. The staging directory accumulated every bundle
ever built in that working tree: 45 MB and eight wasm files where two are live, all of it shipped.
It grows without bound and nothing fails, which is why it went unnoticed until an unrelated hash
comparison looked wrong. `Sync` deletes what the source dropped, which is the semantics a hashed
bundle needs.

Worth knowing for later: the panel is *not* what makes this APK large. Uncompressed it is 133 MB,
of which `classes.dex` is 39.5 MB, Realm's native library across four ABIs is ~31 MB, and two
default background PNGs are 7.2 MB each. The panel's 14 MB sits well below all of them.

The old panel still serves at `/`. Step 8 removes it, and this was its missing prerequisite.

### Step 8's pane-by-pane comparison: the new panel cannot write a side table

The comparison was done textually first — enumerate what each panel touches, diff it, then
photograph only where the diff points — which found in minutes what twelve panes of screenshots
would have found slowly. Three separate classes of finding came out of it, and the first one blocks
step 8 outright.

**1. Every side-table write from the new panel is silently discarded.** Two code facts with no third
path between them: `InMemoryConfigRepository`'s writes are all `= Unit`, and the settings ViewModels
call `remote.send(...)` for only *some* operations. So a change reaches the display when — and only
when — a ViewModel sends it explicitly. That covers config fields (via `update_config`), the theme,
announcements, the PIN and devices, plus file uploads, which the `/upload` endpoint completes
server-side. Everything else is inert on both sides at once: the panel does not even update its own
copy.

Proven on hardware rather than inferred: pressing **Add Schedule** in the Slideshow pane left the
display's `slideshowSchedules` at 1, before, immediately after, and ten seconds later. The button
gives no error.

What is affected — the whole list, derived from the two facts above:

| Pane | Inert |
|---|---|
| Slideshow media | delete, reorder, path update, "Use" toggle |
| Dashboard backgrounds | delete, reorder, "Use" toggle, clear custom |
| Friday media | delete, reorder, path update |
| Slideshow schedules | add, delete, time, days, dates, enable |
| Standby schedules | add, delete, time, days, dates, enable |
| Prayer schedule | the Kemenag fetch |
| System & Reset | reset to defaults |
| Connectivity | `verify_pin` |
| Several | all four previews (adzan, iqomah, slideshow, Friday slideshow) |

**This is not a web-only bug.** `ui:settings` is shared, so an Android device in `REMOTE` mode has
exactly the same hole — it writes to its own Realm and never tells the display. The old HTML panel
is currently the *only* controller that can manage media or schedules at all, which is precisely why
deleting it now would remove capability rather than duplicate it.

The fix that addresses both controllers is to add the missing `remote.send(...)` calls beside the
repository calls, matching what announcements already do — roughly thirty call sites across four
ViewModels. The tempting alternative, making the web's repository send instead, is smaller but fixes
only the browser and leaves the Android controller broken, so it is the wrong trade.

**2. Two side-table fields are never served**, the same class of bug as `jumatTransitionType` and
invisible for the same reason — a missing key is indistinguishable from a key whose value happens to
equal the default:

- **`Media.isUsed`** — so every controller shows the default, not the display's value. This is why
  toggling "Use" could not even be *observed* over the wire, let alone verified.
- **`Announcement.position`** — so a controller reorders announcements using positions that all read
  0.

`ConfigPayloadCoverageTest` did not catch these because it enumerates `MosqueConfig` only. It should
be extended over the side tables, which is the same reflective check applied to seven more classes.

**3. Layout and dead-control defects, found by looking at three panes.** Consistent with the five
the browser exposed earlier, and the same two shapes:

- **No media thumbnail renders** — every item reads "Failed to load image". Correct per
  `decodeImageFile`'s wasm contract (the paths belong to the display's filesystem), but the *panel*
  is not: the old one fetched `/media?path=` over HTTP from the display. Without thumbnails a takmir
  cannot tell which image is which, so media management is unusable in the browser even once the
  writes land. The endpoint already exists; what is missing is a URL-based image loader, since
  `decodeImageFile` is path-based and synchronous.
- **Two buttons clipped their labels** — "Add Image / Video to Slideshow" and "Slide show
  Schedules" each lost their second line at phone width. **Fixed**, see below.
- ~~An unlabelled `- 1 + days` stepper at the top of the Dashboard tab~~ — **not a defect.** The
  tab row is pinned while the content under it scrolls, so the screenshot that showed this was
  already scrolled and the stepper's "Transition interval" heading was above the viewport. Worth
  recording because the wrong version of this line would send someone to fix nothing.
- **"Preview Slideshow" is drawn but sends nothing**, along with the other three previews — the same
  defect as the QR button and the Back button that went nowhere, and found the same way, by pressing
  it.

A full visual pass over the remaining panes is worth doing, but *after* the write path is fixed:
that work changes these panes, and comparing them twice is the waste this method was chosen to
avoid.

#### The two gaps the comparison found are fixed; three remain

**Thumbnails: a controller asks the display.** `decodeImageFile`'s wasm KDoc had already
written this down — "media previews in the browser have to come over HTTP from the display,
which already serves them at `/media`. That is a change to the *caller*." It was also broken on
Android and had never been noticed: a controller's own `filesDir` has the same shape as the
display's, so `File(path)` is simply absent there too.

`MediaImageLoader` (`:ui:design`) is the seam, and it is an interface the host provides rather
than an `expect`. Nothing about resolving a path is platform-specific; what differs is *which
device holds the file*, and only the host knows whether it is the display or a controller. One
Android build is both roles, so `AndroidMediaImageLoader` decides per call from
`connectivityMode` — the same rule `RemoteActionSender` uses for writes. The default reads the
file directly, so the display and its tests need no provider at all.

`rememberMediaImage` returns `Loading | Loaded | Unavailable` rather than a nullable, because
over a network that distinction is the difference between a blank frame and a lie: the previous
code would have shown "Failed to load image" during every fetch.

The bytes reach wasm as base64 because a `ByteArray` cannot cross the Kotlin/Wasm JS boundary
and strings can. `FileReader.readAsDataURL` is the browser's own encoder, so nothing is paid for
the conversion, and `org.jetbrains.skia.Image.makeFromEncoded` decodes it — skiko is already in
the bundle.

**Writes: twenty-one branches now push.** Fixed the way announcements already worked — each
branch sends its action beside the repository write — rather than by making the web repository
send, which is smaller and fixes only the browser. `SettingsViewModel` gained the
`RemoteActionSink` it never had, and `AppModuleTest` is what proves the graph still resolves.

Two categories deliberately send nothing, and both are worth knowing before someone "completes"
them:

- **Adding or replacing a file.** Those go through `MediaStore`, which uploads, and `/upload`
  writes the row itself. The transfer already carried the change.
- **`connectivityMode`, `remoteTvIp`, and `SettingsViewModel`'s sync handler.** The first two
  describe *this* device; the third applies what the display just sent and must not echo it back.

Schedule adds send no `id`: the display mints one, and a controller's own row is never created,
so there is nothing to agree on — the broadcast carries the display's. `update_standby` is
partial on the display, so sending only the flag cannot blank the window times.

**Three payload gaps, and why the existing guard could not see them.** `Media.isUsed`,
`Announcement.position` and `ConnectedDevice.authenticatedAt` were stored and never sent.
`ConfigPayloadCoverageTest` scans the whole file for `put("name")`, which suits one flat config
object and is useless across seven tables — `isUsed` is served for dashboard backgrounds, so
`Media.isUsed` *looked* served while `mediaArray` never carried it.

`SideTablePayloadCoverageTest` reads each array's own `apply { }` block, brace-counted rather
than matched with a lazy regex that would stop at the first inner brace. Mutation-tested both
ways: dropping a `put` fails it, and renaming a builder variable fails it too rather than passing
vacuously — which is the failure a source-reading test is most likely to have. `authenticatedAt`
is withheld with a reason (it is what expires a paired device, and no controller reads it), and a
stale withheld entry now fails too, since it would excuse a real gap.

**`reorder_media` and `update_media_item_used` did not exist.** Not missing from the panel — from
the *protocol*. The display stored a slide order and a per-slide "Use" flag that no message could
change, so both looked editable everywhere and were display-only. They are the only two actions
the hand-written panel never had.

**Verified on hardware** by `curl` against the display rather than by driving a screen: both keys
appear in the payload (announcement positions read 0–5, not all zero), `update_media_item_used`
flipped a slide's flag, `reorder_media` reversed and restored the order.

**Previews and the Kemenag fetch are done too.** Both were written up here as needing a decision;
on inspection neither did, and that over-caution is worth recording because it nearly cost a day.

- **The four previews were an empty-lambda bug, not a design question.** `SettingsScreen` takes
  them as four parameters defaulting to `{}`. `MainActivity` fills them; `main.kt` filled none, so
  in the browser all four drew, took focus, and did nothing — the same defect as the Back button
  and the QR button. The display side was never missing: `preview_adzan` has always called the same
  `previewAdzan()` the display's own settings screen calls, and the old panel has always sent it.
  And the meaning is not ambiguous from a controller — preview means *the television* changes,
  which is the whole reason the takmir is holding the phone. Same parameter, navigation on one host
  and a message on the other, exactly like `MediaImageLoader`.
- **`fetch_kemenag_schedules` asks the display to do the whole job.** The alternative — the browser
  fetches (CORS does allow it) and posts the result back — is up to 180 `save_kemenag_schedules`
  messages for one button press, with the browser doing work the display can do. The display has
  both the API access and the storage.

Verified on hardware: `preview_adzan` switched the display to MENJELANG ADZAN / SUBUH and back
returned it; one `fetch_kemenag_schedules` produced 25 schedule rows within four seconds.

**Still open:**

- **`verify_pin` — deferred by decision, not finished.** The user's call on 2026-09-06: the mosque
  does not use the PIN yet, so this is not urgent. It is still a real hole and must not be read as
  done. `DisplayLink` already *detects* it — a `registration_result` with `isAllowed=false` becomes
  `Status.NeedsApproval`, and it deliberately stops the reconnect churn in that state — but
  `ui:settings` contains no PIN entry anywhere. So on any display with a PIN set, the new panel
  connects, says it needs approval, and offers the takmir no way forward except walking to the
  television or using the old HTML panel. Step 8 cannot remove the old panel until this exists.
~~Panel-driven verification of the thumbnails and the twenty-one sends.~~ **Done**, on the Realme
against the Xiaomi, with the panel served by the display itself:

- **Thumbnails render.** The slideshow pane shows the actual photographs where every tile used to
  read "Failed to load image".
- **Add Schedule works through the panel's own button**, which is the exact press that proved the
  bug: `slideshowSchedules` went 1 → 2 within three seconds, where before the fix it stayed at 1
  indefinitely. The new row then appeared *in the panel* — panel → display → broadcast → panel, the
  whole round trip.
- **Preview Slideshow reaches the display**, which went from the full dashboard node tree to the
  single slideshow node, and back on the display's own Back press.

Everything was restored afterwards: one slideshow schedule, four media, two standby schedules.

#### Two ways a device check lied, in one session

Both were caught, and both would have been reported as bugs by a slightly less suspicious run.

**A layout shift invalidates every coordinate below it.** This file already said to screenshot
before each tap; that is not sufficient. Adding a schedule row pushed everything under it down, and
the Preview tap — aimed with coordinates measured *before* the row existed — landed on empty space.
The button appeared not to work. **Re-measure after any action that can add or remove a row**, not
merely once per pane.

**A before/after diff that includes a clock always says "changed".** The first Preview check
compared the display's node text and reported success; the only difference was the ticking time,
and the structure was identical — the display had not moved at all. Filter time-valued nodes out
before comparing, or compare the set of *labels* rather than the whole tree.

#### `screencap` on the Xiaomi shows content the app is not drawing

Exiting a preview left the adzan screen's text — "MENJELANG ADZAN", "SUBUH", a frozen `00:18` —
painted under a live, ticking dashboard. It looked exactly like a stuck composition, and it is not.

Three instruments disagreed with the screenshot and agree with each other: `uiautomator dump`
lists only dashboard nodes, `dumpsys window` shows one window with one surface, and
`dumpsys SurfaceFlinger --list` shows one layer for the activity. Twenty-five seconds apart the
live values ticked while the ghost stayed frozen at the same numbers — a composition would have
kept updating or appeared in the tree.

So the stale pixels are in MIUI's capture path, not in the app. This sharpens the rule already in
*Checking the layout on a phone*: on this device a screenshot can show text that **exists
nowhere in the app**, indefinitely. Confirm anything surprising against the accessibility tree
before believing it, and before reporting it as a bug.


#### A pinned button height is a clipped label waiting to happen

The two clipped buttons had one cause, and it was general rather than local: `.height(controlLg)`
on a button whose label wraps. The label's second line is simply cut off, and the button also ends
up a different height from the one beside it, which is what makes the row look wrong before you
notice the missing words. Twenty-six controls across nine files were pinned that way.

The fix is `heightIn(min = …)` instead of `height(…)` — everywhere, mechanically. It preserves the
mockup exactly where a label already fits, because the minimum wins whenever the content is
shorter; it only has any effect at the widths where the old code was clipping. That is what makes
it safe to apply to all twenty-six rather than to the two that were caught by eye, and applying it
to all twenty-six is the point: the other twenty-four are the same bug at a width nobody has
opened yet. Long labels already sitting in pinned buttons include "Reset all Settings to default",
"Remote control PIN", "Until Specific Date" and every sidebar category name.

Two of the sites are a `Box` rather than a button — the sidebar category item and the Kemenag
duration dropdown's anchor. Both hold text that can wrap, so both grow now too.

Labels that genuinely wrap also got `textAlign = TextAlign.Center`, so two lines read as intended
rather than as an accident. And "Slide show Schedules" became "Slideshow Schedules" — that spelling
appeared nowhere else in the app.

Verified on the Realme against the display: both buttons render their full label, centred, and the
two in a row are now the same height.

**One thing the comparison cleared rather than flagged.** `standbyStartTime` and `standbyEndTime`
are the only two `MosqueConfig` fields the old panel edits and the new one does not, and
`updateStandbyStatus` reads **neither** — it iterates `standbySchedules` alone. They are stored,
served, and editable from the old panel while driving nothing. So the new panel is right to omit
them, and the old panel has been offering a control that does nothing; that is an argument *for*
step 8, once the rest is fixed.

### The link's behaviour, verified on hardware

Reconnection and queueing are the parts most likely to be assumed working, so they were driven
directly rather than reasoned about. Realme as the controller, Xiaomi as the display, production
bundle:

| Tested | Result |
|---|---|
| Write round trip | `adzanLeadMinutes` 3 → 4 → 5 → 3 from the browser |
| Display stopped | header switches to `Disconnected` within ~10s |
| Display restarted | reconnects **on its own**, no user action, back to `Connected` |
| **Edits made while the display was down** | both arrived after reconnection: 3 → 5 |

That last row is the takmir's real case — a brief wifi drop mid-edit — and it works because
`DisplayLink` holds unsent actions and flushes them once a snapshot proves the display accepted the
device again. Note the ordering it depends on: the display answers `register` with its *current*
state, so the queued action is applied after that snapshot lands, not before, or the display's older
value would win.

**Testing gotcha that costs time:** the display app is killed when it loses the foreground on a
phone or tablet, so `GET /api/get_config` starts answering "connection refused" mid-session and it
looks like a network fault. It is not — check `pidof com.groovernapp.mosqueathan` first. On the
mosque's television this does not arise, since the app is the launcher. It also means the controller
and the display must be *different* devices when testing: opening the panel in a browser on the
display itself backgrounds the very server the panel is trying to reach.

### Murottal audio: now actually plays

**Until this change the feature had no playback at all.** `murottalEnabled`, `murottalStartDelay`
and `murottalAudioPaths` were stored, synced and editable from every panel, and nothing read them —
both panels let a takmir configure a feature that did not exist. The only `MediaPlayer` in the
codebase was inside `VideoSurface`, deliberately muted because the adzan comes from the building's
sound system.

`MurottalPlayer` (`app/audio/`) is now that missing half, and it is **the one sound this app
deliberately makes.** Notes on its shape, because each was a decision:

- **Driven by `AppViewModel`'s coordinator, not its own timer.** There is already a clock ticking
  once a second that knows the date, the prayer times and the Friday window. A second timer would
  be a second opinion about when Friday starts.
- **Written as "should it be sounding right now", not as start/stop edges.** An edge-triggered
  version has to be right about every way the window is entered or left — date rollover, the config
  changing underneath, `BootReceiver` restarting the app mid-window — and being wrong about any of
  them leaves a television reciting through the khutbah. Verified: killing and restarting the app
  mid-window resumed playback on its own.
- **The adzan is the cutoff, read from the calculated schedule** — not `jumatEndTime`, which is when
  the Friday *layout* stops, over an hour later. `transitionToAdzan` also stops it directly rather
  than waiting for the next tick, because the adzan screen appears `adzanLeadMinutes` *before* the
  time the comparison uses.
- **A missing or unplayable file is skipped, not fatal**, and a whole pass that plays nothing stops
  the player rather than retrying once a second all morning. A mosque with five recitations should
  not fall silent because one was deleted.
- **The list loops.** The window is defined by the clock, not by the audio.
- **Losing audio focus stops it** rather than ducking: if something else on the display should be
  heard, the recitation should not be playing under it.

`shouldPlayMurottal` is a pure function in its own file with ten tests, and the adzan cutoff is
mutation-tested — swapping it for `jumatEndTime` fails the suite. That separation exists because the
failure being guarded against is public and expensive, and it is invisible to any test that cannot
call the decision directly.

Verified on hardware, in `dumpsys audio`: focus granted to `MurottalPlayer`, an
`android.media.MediaPlayer` in `state:started` with `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC` at 44.1 kHz;
turning murottal off released the player and abandoned focus; moving the Friday adzan into the past
stopped it, which is the cutoff proven in the real app rather than only in a unit test.

A controller can also add a recitation now, which it could not before.

- **`AudioFilePicker` hands back `PickedFile`, not paths.** It used to return the platform's own
  reference — a content URI — to be stored as the setting verbatim. That reference is only openable
  on the device that produced it, so a file picked on a *controller* named a file on the wrong
  machine. The display reads `murottalAudioPaths`, so the file has to get there first.
- **`MediaStore.storeMurottalAudio` decides how.** On the display it copies into private storage and
  returns the path; on a controller it uploads to `/upload?isAudio=true` and returns null, and the
  display appends the path it chose and broadcasts. Same shape as the media uploads.
- The display's own behaviour changed too — from URI-as-setting to a copied file. That is an
  improvement (a URI grant can be revoked; a copy cannot) and it broke nothing, because nothing
  reads the setting yet. It will not be so free later.

Verified with a 49 MB recitation: uploaded in ~10s over the LAN, byte-for-byte identical on the
display, extension and the spaces in its name intact, and the path recorded by the display rather
than the client.

**Two platform traps found doing it, both worth knowing:**

- **A wildcard audio `accept` makes Chrome on Android open the microphone.** The input was treated as
  a media-capture request: it went straight to a recording-permission prompt instead of a file
  chooser. Naming the formats explicitly — extensions *and* MIME types — routes to the document
  picker, where the music library is one of the sources offered. Measured on the device, twice,
  because the first retest was served a cached bundle and looked like the fix had failed.
- **Kotlin nests block comments, so a slash-star sequence inside a KDoc opens a comment that never
  closes.** Writing the wildcard filter's literal value in a doc comment made every declaration after
  it vanish; the compiler reported unresolved references at the *top* of the file and
  `Unclosed comment` at EOF. If a file suddenly cannot see its own private functions, look for that.

Also: **byte-searching a Kotlin/Wasm bundle for a string does not work.** Not one known UI string
appears as plain bytes. Checking whether a build contains a change has to be done by running it.

### A backgrounded page has a dead socket that still reads OPEN

Found by testing the file picker, and it invalidated a green light: uploading a background from the
browser put the file on the display and left the panel's own list unchanged, under a status chip
reading `Connected`. Pressing Sinkron did nothing. A page reload showed the new item immediately.

The cause is that choosing a file puts the browser's picker in front, and while the page is hidden
the socket stops delivering — but `WebSocket.readyState` still reports `OPEN`, so nothing in
`DisplayLink` noticed. `resync()` was re-sending `register` into it, which "succeeded" silently.

Two changes, and the first is the general rule:

- **`resync()` replaces the socket instead of reusing it.** Closing and reconnecting is one round
  trip on a LAN — `register` comes back with a full snapshot — and unlike a re-register it cannot
  appear to work against a dead connection. `closingDeliberately` keeps that close from also
  scheduling the backoff reconnect.
- **Returning to the foreground triggers a resync**, via `visibilitychange`. `focus` is not the
  event: on Android the picker and the app switcher hide the page without a focus change the page
  can rely on, and `pageshow` only fires for navigations.

Verified the way the bug was found: with the panel backgrounded, a background was deleted on the
display through the API; bringing the panel forward showed four items where it had shown five, with
no reload. Any tab switch or screen lock takes the same path.

### The offline queue is safe for a value and dangerous for a row

`DisplayLink` holds unsent actions while the display is unreachable and flushes them once a
snapshot proves the display accepted the device again. That is recorded above as the takmir's real
case, verified on hardware — and the verification used `adzanLeadMinutes` 3 → 5, which is why the
other half went unseen for so long.

**Setting a value is idempotent; appending a row is not.** Delivering "set adzanLeadMinutes to 5"
twice leaves 5. Delivering "add a standby schedule" eight times leaves eight schedules.

That is not hypothetical. During a pane sweep the display lost the foreground to another app and
its server stopped answering, so the panel showed defaults and **every button became silently
inert**: the web repository writes nothing locally, so a press produces no row, no error and no
movement. The natural response is to press again. Eight presses later the link recovered, the queue
flushed, and the display gained eight identical `22:00–04:00 Monday` rows — `createdAt` values
250 ms apart, which is what identified the cause. They were deleted; the count is back to two.

**The first half is fixed.** `DisplayLink` now emits a notice when it holds an action, and a
second when the link returns saying how many it sent. The second half of that matters as much as
the first: it is the moment a takmir who pressed a silent button repeatedly finds out how many
times it counted. It reaches the screen through a second `MessageHost` in `main.kt` — shared code
cannot see `DisplayLink` without `ui:settings` learning about the transport, so each layer keeps
the host for what it has to say.

**The real answer is still step 3.** An offline replica writes the row locally, shows it
immediately, and reconciles later — at which point the press is visibly *not* a no-op and there is
nothing to repeat. The notice is honesty in the meantime, not a substitute.

**The Android controller fails the opposite way, and neither failure is visible.**
`RemoteActionSender.post` catches, logs, and returns: no queue, no retry, nothing on screen. So on
a phone in `REMOTE` mode with the display unreachable an edit is *lost entirely*, where the browser
at least holds it. Web over-delivers, Android under-delivers, and a takmir sees the same thing in
both cases — nothing. Not fixed; recorded because the two halves want one answer rather than two.

Worth knowing when reading the hardware table above: "edits made while the display was down both
arrived after reconnection" is true, and it is a description of the same mechanism that produced
this.

### The header bar: role first, connection second

Settings now opens with a bar naming what this device *is* — `Server` or `Remote` — because one
build runs on the mosque's display and on a takmir's phone and nothing on screen said which. A
controller adds whether it is reaching the display; a display instead shows how many controllers
are attached, since a display is the source of truth and syncs nowhere.

Two things this deliberately does not do:

- **No permanent "Sinkron" button on a display.** There is nothing to sync to. The control is
  hidden rather than disabled.
- **No green light that means "we did not check".** `LinkStatus` is provided by the host through
  `LocalLinkStatus` (`:ui:design`), the same arrangement as `LocalAppVersion`, and defaults to
  `Unknown` — which renders *nothing*. The browser holds a socket and knows; the Android controller
  posts each action and forgets, so it has nothing to report and shows no status chip at all.

`LinkStatus` already carries `NeedsApproval` separately from `Offline`, because reconnecting cannot
fix the first — a person has to approve the device. When the offline replica lands, "pending
changes" and "conflict" join it there; that is why the UI enum is separate from
`DisplayLink.Status`, which only ever describes a socket.

### What the browser exposed that Android never did

`:webApp` renders the same `SettingsScreen` the television runs, which is the point of the
migration — and the first real use of it on three engines turned up five defects. Verified on
Chrome/Android 16 (tablet, 1280x800dp), Chrome/Android 11 (phone, portrait) and Safari 26.5 on
macOS; all three render, so WebKit is not a special case.

**Two of these were only visible in a browser, and one was never web-specific at all:**

- **A `remember` with no key captured the wrong window class.**
  `var isSideSheetExpanded by remember { mutableStateOf(!isCompact) }` runs once, and in a browser
  the first composition happens *before* the canvas reports its size — so the window looks
  `Compact`, the settings sidebar starts collapsed, and it stays a strip of unlabelled icons on a
  3420px display. Android's first frame already has the real size, which is why this survived.
  **Any `remember` seeded from `LocalWindowClass` needs that value as its key.**
- **Controls wired to a no-op looked alive.** The browser has nowhere to go back to, so `onBack`
  was `{}` — and both the compact top bar's close icon and the sidebar's Back button rendered, took
  focus, showed a focus ring, and did nothing. `onBack` is now `(() -> Unit)?` and those two
  controls are not drawn when it is null. Prefer that over an empty lambda: a control that cannot
  act should not exist.
- **`calculationMethodOptions` in `core:model`** replaced a hard-coded
  `listOf("SINGAPORE", "UMM_AL_QURA", ...)` that was rendered *as the label*. The hand-written panel
  had always shown "Muslim World League (MWL)"; the Compose UI showed the identifier — **on the
  television too**, so this was never a web bug, it just took a phone-width row filled by
  `MUSLIM_WORLD_LEAGUE` to notice. `CalculationMethodOptionsTest` pins each id against
  `CalculationMethod.entries`, because `PrayerCalculator` silently falls back to SINGAPORE for a
  name adhan2 rejects — a typo there would shift a mosque's prayer times with nothing reporting it.
- **`LocalAppVersion` had no provider on web**, so the sidebar read "Versi: vunknown". `:webApp`
  now generates `WEB_APP_VERSION` from `gradle/libs.versions.toml` at build time, so it cannot
  drift from the APK's.
- **A scrolling list with a pinned block under it reads as finished.** Six categories overflowed
  the sidebar's scroll area while the version line and Back button stayed pinned below, so
  "Sistem & Reset" appeared not to exist. The rail now fades at its bottom edge, but **only while
  `canScrollForward`** — where all six fit, nothing about the sidebar changes, so the mockup is
  untouched wherever it was already right.

### iOS: the panel already works, without an iosApp

Verified on a physical **iPhone 17 Pro** (iOS Safari, via iPhone Mirroring): the shared Compose
settings UI renders, registers with the display over the WebSocket, and reports `Terhubung` —
Indonesian, because it follows the device locale. So a takmir with an iPhone has a working
controller **today**, with no iOS target, no Xcode, and no signing.

That reorders step 9. A native `iosApp` is no longer what unblocks iPhone users; it is a
nicer-feeling shell around a UI they can already reach. Worth knowing before spending the ~8½
minute cold iOS compile on it.

WebKit is now proven twice — Safari on macOS and Safari on iOS — so the four engines that matter
(Chromium/Android, Chromium/desktop, WebKit/macOS, WebKit/iOS) all render this UI.

### Driving the panel from a script: what works where

The Compose canvas takes **real** input events only, and that cuts differently on each platform.
Learned the hard way, twice:

| Harness | Canvas taps | Keyboard |
|---|---|---|
| `adb shell input tap` (Android) | **works** — a real event at the driver level | works |
| macOS `System Events click at` | **does not work** — it presses an *accessibility element*, and a canvas has none. It drives the old DOM panel fine, which is what makes the failure confusing | n/a |
| macOS `System Events keystroke` | n/a | **works** — `Cmd+L` focused Safari's address bar, including through iPhone Mirroring |
| iPhone Mirroring | **does not work** — the mirroring stream forwards keyboard but not synthetic mouse | **works** |

Two consequences worth planning around:

- **Android is the only platform where a script can drive this UI end to end.** Verify behaviour
  there; use the browser and the iPhone for rendering, layout and connection state.
- **Tab traversal cannot be tested this way either.** The canvas needs a real click before it holds
  keyboard focus, so `Tab` goes nowhere from a cold load — a focus ring failing to appear under a
  script is not evidence that focus traversal is broken.

### Debugging wasm — which instruments actually work

Finding why the browser panel rendered nothing took far longer than the fix, entirely
because of bad instruments. What was learned, so it is not re-learned:

**A Kotlin exception on wasm arrives in JS as `Uncaught (in promise) #<Exception>` with no
message and no stack.** That single opaque line is all the console gives you.

| Instrument | Verdict |
|---|---|
| Headless Chrome console via `--enable-logging=stderr` | **Unreliable.** Drops lines silently — one run captured 2 of 8 `println`s, another captured none at all. It produced two false conclusions before being distrusted. |
| `--dump-dom` | Produced no output at all, on the app and on a trivial static page. |
| `--screenshot` | Cannot decide rendering. A plain `Text("hello")` is equally blank headlessly. Only file **size** is a usable signal — ~2–4 KB blank versus ~61 KB rendered. |
| `-Xwasm-attach-js-exception` | Surfaced nothing; `e.reason.message` stayed undefined. The stdlib throw path is precompiled, so the flag does not reach it. Removed again. |
| **`fetch("/R/<marker>")` to the dev server** | **Reliable.** The server logs every path it serves, so no marker is ever lost. This is the instrument to reach for. |
| A `nodejs()` target for plain `println` | Works and gives real stdout, but the run task then fails fetching the browser-shaped wasm. Add it temporarily if needed; do not leave it in the build. |
| Calling the **non-composable** factory | The one way to get a real message. `try`/`catch` around a composable is a compile error, but `DatePickerState(locale)` does what `rememberDatePickerState()` does and is catchable — which is how `IrLinkageError` finally became visible. |

Two traps specific to composition:

- `try`/`catch` cannot wrap a composable call ("Try catch is not supported around composable
  function invocations"), so bisect with markers between calls instead.
- Some failures are JS-side throws that **`catch (Throwable)` does not catch at all** —
  removing the `@js-joda/timezone` import makes `TimeZone.currentSystemDefault()` fail this
  way, killing `main()` before the first statement after it. A silent probe is not a passing
  probe; always emit a marker *before* the call as well as after.

**Measured, not assumed:** the `@js-joda/timezone` import in `JsJodaTimeZone.kt` is required —
without it `TimeZone.currentSystemDefault()` throws in the browser (it works in Node, which is
why a Node-only check is not sufficient evidence). An `Intl.Locale.prototype.weekInfo` polyfill
was written when Chrome 151's removal of that property looked like the cause; it is **not**
needed with material3 1.9.0, and was removed rather than kept as a patch for an API nothing
asks for.

### Step 3, most of the way: Room beside Realm, and what the port cost

Room, a DAO, a database at version 1 with its schema exported, a data migration, a full
`ConfigRepository` over Room and the eight server-only members. **The Koin binding still chooses
Realm** — one block in `AppModule` picks the store and everything else resolves from it, so
switching is two lines and reverting is the same two. What is missing is not code; it is having
driven the switch on hardware, and `KemenagScheduleSource` is the binding whose being wrong would
be wrong on a television at adzan.

**There is no bridge release and none is needed.** A version that wrote to both stores, or exported
to a file, would only prepare data 2.0 can read directly. `RealmToRoomMigration` opens the old
database on first launch, copies it, and Realm leaves the classpath in 2.1. Three properties, in
the order they matter:

- It opens Realm **without `deleteRealmIfMigrationNeeded`**. That flag is on the live 1.x config and
  is the most dangerous thing that could have been copied into the migration: it deletes the
  database when the schema does not match, which is precisely the case a rescue has to survive.
- It copies the `.realm` file aside first and never deletes it.
- One `@Transaction`. It used to write as it read, and because the config row doubles as the
  "already migrated" marker, a throw between it and the side tables lost them **permanently** — the
  next launch saw the marker and skipped. Found by the tests written against it, not by reading.

Verified on the Xiaomi against its real database: every row count matching what the display's API
reports, Realm untouched, backup written beside it.

**`lastModified` is on every table, and cannot be forgotten.** Config writes funnel through
`editConfig` and side-table writes through nine private helpers, each applying the stamp to the
*result* of the caller's change. No path can supply its own value or omit one, because the coming
conflict dialog is only as honest as this column and "remember to set it" is not a design.

**Deletions needed their own table.** `MAX(lastModified)` over the rows cannot see a removal:
delete the newest running text and the maximum falls back to the row beneath it, so two devices
comparing notes conclude nothing happened and the deleted row comes back from the other side.
`table_stamps` holds one row per table, written on every mutation including deletes.

**Two corrections made because a new schema is the cheapest moment for them.**
`kemenag_schedules` is keyed on `(dateString, cityId)` where Realm keyed on the date alone, so two
cities could never hold a schedule for the same day and changing city overwrote the previous one
row at a time. And `ConnectedDeviceEntity` is not migrated at all — eighteen rows of access-control
bookkeeping the display rebuilds as devices register.

**Five regressions the port introduced, all caught by tests rather than by review**, and each
invisible until it bites a mosque: announcements served unfiltered so switched-off ones returned to
the running text; `updatePinCode` de-authorised the master, locking the mosque's own tablet out of
its own display; `resetToDefaults` left every paired device paired against a blank PIN and seeded
no announcement; `updateConfig` stopped triggering the Kemenag fetch. Worth knowing before the next
port: a repository rewrite compiles perfectly while quietly dropping behaviour, and the compiler
has nothing to say about any of it.

**`KemenagFetcher` came out of the repository on the way.** Deciding *which months* a duration
covers is date arithmetic that can be wrong by a whole month, and it sat interleaved with an HTTP
call and a database write where no test could reach it. `monthsFor` is now that arithmetic alone.

### Sync has a second direction now

**Until this change sync only ran server → local, and it was silently destructive.**
`syncWithTvServer` did one GET of `/api/get_config` and wrote the display's values over this
device: 23 of the config's 51 syncable fields read key by key, both schedule tables ignored
entirely, and `clearAnnouncements()` called unconditionally. A takmir who spent an evening editing
at home and pressed Sinkronkan lost all of it without being asked, and the only way to get those
edits onto the television was to retype them standing in front of it.

`apply_snapshot` is the direction that never existed. **One mechanism serves all three of the
dialog's options**, which is the whole design: a push applies only the tables its payload names, so
"use the display's copy" sends nothing, "use this device's" sends every table it can, and "keep
whichever is newer" sends the tables this device wrote later. `SyncConflict.tablesToPush` is that
decision, pure and tested; no branch in the server or the ViewModel knows which of the three it is
carrying out.

Four properties are load-bearing rather than incidental, and each is a way this could quietly
destroy a mosque's settings:

- **Null means "leave that table alone"; an empty list means "empty it there too".** Conflating
  them is how an additive sync restores a running text the takmir deleted at home — the deletion
  simply comes back from the other side on the next sync, forever.
- **The media tables cannot cross at all** (`SyncConflict.PUSHABLE_TABLES` is four of the seven
  compared tables). Those rows name *files*, and a path on a phone names nothing on the display —
  a display that accepted them would read "failed to load image" for every slide, having discarded
  the rows for files it does hold. So choosing "use this device's copy" leaves the display's media
  as it was. That is the honest outcome: the alternative is not "media syncs too", it is "media
  breaks". Getting the files themselves across is the upload problem and still has no design.
- **Which side owns a file *path* depends on the direction of travel**, so that rule lives at the
  boundary in `SnapshotPush.sanitisedConfig` rather than inside either store. A display strips
  `customBackgroundPath` and `murottalAudioPaths` from an incoming push; a controller receiving the
  display's snapshot adopts them, which is what lets it show the right thumbnails. Withholding them
  in the store would have broken the download direction in order to protect the push direction.
  The four genuinely device-local fields — `id`, `pinCode`, `connectivityMode`, `remoteTvIp` — are
  wrong to adopt whichever way the snapshot travels, and those *are* withheld in the store.
- **Only the announcements a sender could have seen are replaced.** The state payload carries
  active announcements alone (see `ServerDataSource.getActiveAnnouncements`), so a switched-off one
  is invisible to every controller and cannot have been meant for deletion. `replaceTables` calls
  `clearActiveAnnouncements`, not `clearAnnouncements`. The consequence, worth knowing: switching
  an announcement *off* does not cross a sync — the same shape of gap as the media tables, and
  losing a takmir's archive of old running texts would be worse.

**`applySnapshot` is on `ConfigRepository`, not `ServerDataSource`**, and that placement is the
point rather than a convenience: receiving a whole table is not a server's privilege, it is what
either end of a sync does. A display applies what a controller pushed; a controller applies the
snapshot the display just served. One `@Transaction` on Room, one `realm.write` on Realm — a
half-applied sync leaves a display showing a mixture of two devices' settings with nothing on
screen to say so.

The pull side decodes as `MosqueSnapshot` now — the class the display already serves — so the field
list *is* the class rather than a list to keep in step, and both schedule tables reach a controller
for the first time.

**A real bug the tests found, not review:** `core:protocol` carried the serialization *library* and
not the compiler plugin, so `SyncConflict.Stamps` was `@Serializable` in name only and the first
encode threw `Serializer for class 'SnapshotPush' is not found` at runtime. Exactly the trap
`core:model`'s build file already records in a comment; it had simply never been exercised in
`core:protocol`, because nothing there had been serialized yet. **An `@Serializable` class in a
module without `alias(libs.plugins.kotlin.serialization)` compiles and then throws.**

**Still not driven on hardware.** Everything above is unit-tested — 51 config fields carried on
both stores and mutation-tested by dropping one, null versus empty, media unreachable, the archived
announcement surviving, the four device-local fields kept, every written table stamped and no other
— but nobody has yet pressed Sinkronkan on a phone and watched a television take its settings. That
is the next thing to do, and the user's stated case for it is the Realme's data overwriting the AQUA
TV.

### Migration order

Each step is independently reviewable. Steps 1–6 stay Android-only and remain
shippable throughout.

| # | Step | Releasable? |
|---|---|---|
| ~~1~~ | ~~`build-logic` + `core:model` + `core:prayer`; fix `PrayerCalculator` purity~~ **done** | yes |
| ~~2~~ | ~~Extract `ConfigRepository` interface + Koin; delete `getInstance()`~~ **done** | yes |
| 3 | Realm → Room in `data`, shared entity/wire classes, `MosqueSnapshot` | **mostly done** — Room, the DAO, a tested data migration, a Room `ConfigRepository` and the push direction have all landed, and Koin binds Room. What remains is IndexedDB for the browser's offline replica, and driving a sync on hardware. **No longer a data-loss step; see below.** |
| 3a | ~~Split the Realm entities from the domain model~~ **done**, ahead of the rest of step 3 — it touches no schema, so it carries none of step 3's data-loss risk, and both UI modules were blocked on it | yes |
| 4 | `core:protocol` **started** — action names are shared constants; `org.json` → kotlinx.serialization still to do | yes |
| ~~5~~ | ~~`ui:design` — tokens, window size class~~ **phase A done**; `App*` components pending sign-off | yes |
| ~~6~~ | ~~`ui:settings` + `ui:display` into `commonMain`~~ **done** | yes |
| ~~7~~ | ~~`webApp` (wasmJs)~~ **done** — the shared Compose settings UI renders in a browser and controls the display over one WebSocket. Media file handling is the remaining stub. | yes |
| 8 | Delete `WebPage*.kt` + `*WebSegment.kt` (−9.1k LOC) | **blocked on `verify_pin` alone** — deferred by decision, not done. Side-table writes, thumbnails, the two missing actions, the four previews and the Kemenag fetch have all landed |
| 9 | `iosApp` — controller + preview | yes |

~~**Step 3 wipes device data.**~~ **It does not, and this warning was wrong in the dangerous
direction — it said data would be lost when a migration now carries it across.** An agent asked to
install a build on the mosque-adjacent television read this section and correctly refused, which is
how it was caught.

`deleteRealmIfMigrationNeeded()` is about Realm's *own* schema versions and has nothing to do with
moving to Room. `RealmToRoomMigration` reads the old database once on the first launch after the
upgrade and writes it into Room — verified on the Xiaomi against its real data, every row count
matching what the display's API reports, with the Realm file untouched and a `.pre-room-backup`
copy written beside it. There is no bridge release and no export step: 2.0 carries Realm on the
classpath for exactly this, and drops it in 2.1.

What still deserves care is the *reverse* direction. A device that has run 2.0 and then goes back
to a 1.x build sees only the Realm file as it was at the upgrade, so anything changed since is not
there. Downgrading is the lossy move now, not upgrading.

**Step 3 can be deferred until after step 7.** Room is not a prerequisite for
the web panel — the panel is a client and needs no database. Running
1 → 2 → 4 → 5 → 6 → 7 → 3 reaches the visible payoff sooner and keeps the only
risky step off the critical path, at the cost of carrying Realm longer.

### Display screens on a short window

The dashboard, adzan and iqomah screens are built for a television and overflow on
a phone. The app is locked to landscape, so a phone gives **360dp of height** — and
`ClockSection` alone stacks 170sp + 32sp + 22sp + 22sp, around 250dp of text. The
clock filled the screen, the Hijri date was clipped, and the prayer times row
disappeared entirely. Reported from use, not found by reading.

Two things scale with window height, both provided by `MosqueTheme`:

- **`MosqueTheme.typography`** resolves through a `CompositionLocal`. Display roles
  (`clockDisplay`, `adzanTitle`, `prayerName`, `prayerLabel`, …) are multiplied by
  the height factor; settings roles are **not** — they are 11–24sp already, and a
  phone is held closer, so shrinking body text makes it worse.
- **`MosqueTheme.displayScale`** is the same factor, for padding and fixed heights
  on display screens. Scaling type alone was not enough: fixed padding and the
  running-text bar's `height(controlXl)` came to 136dp of the 360.

**A television is never scaled, and otherwise the factor is 1.0 at 600dp and above.**
The `isTv` half is not a refinement, it is a correction: this file used to claim "the
tablet is 711dp and a television more", and the mosque's television is **540dp**. See
below. `DisplayTypeScaleTest` asserts the unscaled sizes with `assertEquals` on the
`TextUnit` rather than a tolerance — "approximately unchanged" is not good enough for a
display already in a mosque.

**Measured, tablet vs phone (2026-08-26).** Xiaomi 1137×711dp, Realme 800×360dp, both
on the dashboard, block heights as a percentage of screen height taken from the
accessibility tree rather than by eye:

| Block | Xiaomi | Realme | Relative excess |
|---|---|---|---|
| clock | 31.8% | 33.3% | +5% |
| prayer-times row | 9.9% | 14.4% | +45% |
| running-text bar | 5.6% | 6.7% | +20% |

The clock is proportional. The bottom band is not, and **it cannot be made
proportional** — that is the finding, not a to-do. The phone's effective
`displayScale` is 0.531 (its *window* is ~319dp, not the screen's 360dp), which puts
`prayerLabel` at **10.6sp** and `prayerName` at 14.9sp. Making the row proportional
means shrinking it a further 31%, landing the label at **7.3sp**. So the phone's small
type has hit a legibility floor while the clock still had headroom, and uniform
scaling has nothing left to give.

Anything further is a *layout* change rather than a scale change — putting the label
and time on one line on a short window would roughly halve the row. That is a mockup
change and needs sign-off; do not do it as part of a scaling pass.

Do not "fix" the remaining gap by steepening `displayScaleFor`, and do not raise
`FULL_HEIGHT` — see the television note below.

**A television, measured at last (2026-08-26).** An AQUA/Haier Android TV, model
`AQUA Android TV DVB`, Android 11: **1920x1080 at density 320, so 960x540dp**. That is
*below* the 600dp knee, so it had been rendering display type at **0.9** ever since the
short-window fix landed, and nobody could see it because there was nothing on screen to
compare against.

This was the developer's own television, **not the mosque's** — that one has still never
been measured. It does not change the fix, which now holds for any television at any
height, but it does mean the mosque's screen has not been confirmed either way. 540dp is
what a 1080p Android TV at density 320 reports and is the common case, so the mosque's
display was very likely shrunk too; that is an inference, not a measurement.

Measured on the device itself, both ways: with the old behaviour the clock node was
360px; with `isTv` short-circuiting the scale it is 398px. 360/398 = 0.905, which is the
0.9 the arithmetic predicted.

`displayScaleFor` now returns 1.0 for a television at any height. That is the right rule
rather than a patch for one device: shrinking exists so a phone held at arm's length can
still show the prayer times, and a television is the one screen where the opposite
holds — it is read from the back of a room.

Reaching the TV at all: **its USB port is host-only**, so a cable will never enumerate it.
Use `adb connect <ip>:5555`; it advertises itself as `_adb._tcp` and `adb mdns services`
finds it. The RSA dialog has to be accepted on the TV screen, and while it says
`unauthorized` every command — including one to wake it — answers `device offline`.

Rules for display-screen work:

1. Read sizes from `MosqueTheme.typography`; never hard-code `sp` on these screens.
2. Multiply padding and fixed heights by `MosqueTheme.displayScale`.
3. A label on a display screen needs a **display** role. `PrayerItem` used
   `titleMedium`, a settings role, so it stayed at 20sp on a phone and helped push
   the running text off screen. `prayerLabel` exists for that reason and holds the
   same 20sp.
4. Where scaling is not enough, tighten that component on `isShort` specifically
   rather than steepening the global factor — the clock size is right on a phone,
   and making everything smaller to fit one row is the wrong trade.

**Known unverified:** the prayer row's tightened padding on a short window has not
been seen on hardware. The phone is in use as a hotspot and the tablet was in
standby (02:12, inside the 22:00–04:00 window) when it was last checked.

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

**How conflicts are resolved — a two-snapshot approximation has shipped; the third
snapshot has not.** `SyncConflict` compares per-table timestamps and
`SyncConflictDialog` offers the three choices, so the feature works. But it is
still local-versus-server, and the concern below is unaddressed rather than
answered: `needsAsking` suppresses the dialog only when *one side has never
written at all*, which spares a fresh controller and nothing else. Two devices
that both write regularly will see the dialog on most syncs, which is precisely
the reflex that gets it dismissed on the one occasion it mattered.

The fix is unchanged and is a storage question, not a UI one — nothing records
the state as of the last successful sync. Keep **three** snapshots instead:

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
