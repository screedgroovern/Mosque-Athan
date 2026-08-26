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
- Settings exist twice on purpose: `MosqueConfigEntity` (a Realm object, in `:app`) and `domain.MosqueConfig` (a `data class` in `core:model`). The repository maps between them at its own boundary, in `data/EntityMappers.kt`, so nothing above the repository sees Realm. **Adding a config field means editing the entity, the domain class, and the mapper** — and `MosqueConfigRepository.updateConfig` too if the field should be part of a whole-config write. The mapper is the one the compiler will not catch on its own, because every domain field has a default: a field left out of `toDomain()` still compiles and then reads back as its default no matter what is on disk. `EntityMappersTest` fails on exactly that, so add the field and run `./gradlew :app:test`.
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

### Settings UI — the pattern described here did not exist

An earlier revision of this file described one standalone ViewModel per settings
category, each exposing a single `uiState: StateFlow<XUiState>` and one
`onEvent(XEvent)`, with composables receiving "exactly `state` + `onEvent`" and
"never a long parameter list of individual values and callbacks".

**None of that is in the code.** What is actually there:

- **One `SettingsViewModel`**, 1028 lines, 70→48 public functions, exposing **11
  separate `StateFlow`s** (`configState`, `announcementsState`, `devicesState`,
  `mediaListState`, …). There are no per-category ViewModels.
- **No `XUiState` or `XEvent` types for settings at all.** The only sealed
  interfaces in `ui/` are `AppUiState`, `AppViewModel.ActiveScreen` and
  `DashboardUiState`.
- **Long parameter lists everywhere** — exactly the thing the old text forbade.
  20 composables take 6 or more parameters, 318 in total:

  | Composable | Parameters |
  |---|---|
  | `FridaySettingsPane` | 41 |
  | `ThemeSettingsSegment` | 35 |
  | `PrayerScheduleSettingsSegment` | 33 |
  | `DisplayMediaSettingsPane` | 32 |
  | `SlideshowTabContent` | 21 |
  | `AdzanIqomahSettingsPane` | 18 |

- Composables receive `viewModel: SettingsViewModel` directly, or those parameter
  lists. `SettingsScreen` does not obtain per-category ViewModels via
  `viewModel()`; there are none to obtain.

**This is not a KMP blocker** — a ViewModel with eleven flows still moves to
`commonMain`, and a 41-parameter composable compiles there fine. It is
maintenance debt, and it is the largest remaining piece of it. Anyone who was
told the state layer is "already the right shape for KMP" was told something this
file made up.

**Until it is restructured, match the surrounding file.** Do not add a
half-migrated `XEvent` to one pane; a single pane in a different style is worse
than a consistent bad style. Restructuring is a decision that has not been taken.

What *is* real and worth keeping:

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

- ~~The state pattern.~~ **Retracted.** This claimed the settings layer was
  already the KMP-idiomatic shape — one ViewModel per category, single `uiState`,
  single `onEvent`. It is not; see *Settings UI* above. One 1028-line ViewModel
  with eleven flows, and composables taking up to 41 parameters. It is still not a
  *blocker* for moving to `commonMain`, so the migration order does not change,
  but it is real debt and restructuring it is a live option rather than something
  to protect.
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

**Pattern — unidirectional data flow is the target, not the current state.** The
display screens (`AppUiState`, `DashboardUiState`) do follow it. Settings does
not — see *Settings UI* above. If settings is restructured, the display screens
are the model to copy, and formal MVI reducers are still not worth the ceremony.

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

### Step 2: the repository does not want one interface

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

**`Schedule.kt` and `SlideshowComponent.kt` stayed in `:app`** despite living in
`ui/component`. Both are used by settings and the server; the dashboard renders its
slideshow inline. They belong with `ui:settings`.

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

Seven `expect` declarations, and the module gained its first `androidMain` dependency
(`androidx.activity:activity-compose`) for two of them:

| Seam | Android | jvm |
|---|---|---|
| `rememberIsTv()` | `UiModeManager` | `false`, permanently — a desktop build is a controller |
| `decodeImageFile(path)` | `BitmapFactory` | `ImageIO` |
| `rememberMediaPicker` / `rememberMultipleMediaPicker` | `PickVisualMedia` | no-op |
| `rememberAudioFilePicker` | `OpenMultipleDocuments` | no-op |
| `MessageHost(messages)` | `Toast` | `SnackbarHost` |
| `SystemBackHandler(enabled, onBack)` | `BackHandler` | no-op — no system back button to intercept |

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

### Migration order

Each step is independently reviewable. Steps 1–6 stay Android-only and remain
shippable throughout.

| # | Step | Releasable? |
|---|---|---|
| ~~1~~ | ~~`build-logic` + `core:model` + `core:prayer`; fix `PrayerCalculator` purity~~ **done** | yes |
| ~~2~~ | ~~Extract `ConfigRepository` interface + Koin; delete `getInstance()`~~ **done** | yes |
| 3 | Realm → Room in `data`, shared entity/wire classes, `MosqueSnapshot` (see data-loss warning) | yes |
| 3a | ~~Split the Realm entities from the domain model~~ **done**, ahead of the rest of step 3 — it touches no schema, so it carries none of step 3's data-loss risk, and both UI modules were blocked on it | yes |
| 4 | `core:protocol` **started** — action names are shared constants; `org.json` → kotlinx.serialization still to do | yes |
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
