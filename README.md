# MosqueAthan

A prayer-schedule display for mosques, built for Android TV.

MosqueAthan turns a television on a mosque wall into a prayer clock. It shows the day's prayer
times, switches to an adzan screen and an iqomah countdown at the right moments, and rolls
announcements and a photo/video slideshow in between. The mosque's committee (takmir) changes
every setting from a phone or a web browser on the same wifi, so nobody has to stand in front of
the TV with a remote.

- **Package:** `com.groovernapp.mosqueathan`
- **Version:** 2.0 (branch `kmp`); the last release on the old line was 1.4.4
- **Platform:** Android 8.0+ (minSdk 26), landscape only, launched from the TV launcher and
  started automatically on boot
- **Languages:** the UI is bilingual, English and Indonesian

## What it shows

| Screen | When |
|---|---|
| **Dashboard** | The default. Large clock, Hijri date, the day's prayer times, running announcement text and an optional slideshow. |
| **Adzan** | A few minutes before each prayer (configurable lead time). |
| **Iqomah** | After the adzan, counting down the per-prayer iqomah wait. |
| **Friday layout** | On Fridays Dzuhur becomes Jum'at, with its own media and optional murottal (Quran recitation) playback that stops at the adzan. |
| **Standby** | Dims the screen on a schedule, for example overnight. |

Imsak and Syuruq are shown for information only. They never trigger an adzan or iqomah screen.

### Prayer times

Times come from one of two sources, with per-prayer minute offsets applied on top:

1. **Kemenag**: the Indonesian Ministry of Religious Affairs schedules, fetched ahead of time
   from `api.myquran.com` and stored on the device.
2. **adhan2**: any of its calculation methods, falling back to Singapore if the name is unknown.

## How it is controlled

- **On the TV:** press the D-pad sequence **UP UP DOWN DOWN** on the Dashboard to open settings.
  Every change saves immediately; there is no Save button.
- **From a browser:** the TV runs a small web server on port 8080 and serves the control panel at
  `/panel/`. Scan the pairing QR code on the display, or open the TV's address in any browser on
  the same network.
- **From another Android device:** the same app can run as a remote controller. It keeps a full
  copy of the settings, can be edited with no connection, and syncs later. When both sides have
  changed, a dialog lets you keep the TV's copy, this device's copy, or whichever is newer.

The display is the only device that does display duty. Phones and browsers are controllers.

## What is in the `kmp` version

The `kmp` branch is a restructure of the whole app, merged to `master` as version 2.0. The goal was
to stop maintaining the web control panel and the TV's settings screen as two separate programs.

**One settings UI, three places it runs.** Previously the browser panel was about 9,100 lines of
hand-written HTML, CSS and JavaScript kept in sync by eye with a separate Compose settings
screen. Now the settings UI is written once in Compose Multiplatform. It runs natively on the TV
and on Android controllers, and compiles to WebAssembly for the browser panel. The old panel is
deleted.

What changed:

- **Modules.** The app is split into Gradle modules so the build enforces the boundaries:

  | Module | Contents |
  |---|---|
  | `:app` | Android TV app: manifest, `MainActivity`, boot receiver, the web server, data layer, dependency injection |
  | `:core:model` | Settings and domain classes, shared by every platform |
  | `:core:prayer` | Prayer-time calculation (pure, with no database dependency) |
  | `:core:protocol` | The names of the actions shared by the TV and its controllers |
  | `:ui:design` | Design tokens, buttons, D-pad focus handling, window size classes |
  | `:ui:display` | Dashboard, Adzan and Iqomah screens |
  | `:ui:settings` | The six settings panes and their ViewModels |
  | `:webApp` | The Compose-to-wasm browser panel |
  | `build-logic` | Convention plugins for the Kotlin Multiplatform targets |

- **Storage moved from Realm to Room.** On first launch after upgrading, the old Realm database is
  copied into Room once and left untouched, with a backup beside it. Realm leaves in a later
  release. Upgrading does not wipe a device's settings.
- **Two-way sync.** Controllers can now push their edits to the display as well as pull from it,
  with a conflict dialog.
- **Browser panel served by the TV itself.** The wasm bundle ships inside the APK, so the TV
  hands the panel to any browser on the mosque wifi. Nothing else has to host it.
- **Media from any controller.** Thumbnails, uploads, replacements and deletions work from the
  browser and from Android controllers, not only from the TV.
- **Murottal playback.** The murottal setting existed before but nothing played. It now plays on
  Fridays until the adzan.
- **Design system.** Every colour and font size is a token. TV layouts are pixel-identical to
  before; phones and tablets get adaptive layouts.
- **Build tooling.** Android Gradle Plugin 9.4, Gradle 9.8, Kotlin 2.2.10, Compose Multiplatform
  1.9.3, with no opt-out flags.

Known limits of this version:

- The Android controller does not queue edits made while the display is unreachable. It reports
  them as not delivered. The browser queues them.
- Switching an announcement *off* on a controller does not cross a sync.
- Media files are not synced between devices as part of a settings sync, only uploaded one by
  one.
- A native iOS app does not exist yet. The browser panel already works on iPhones.
- Downgrading to a 1.x build after running 2.0 loses anything changed since the upgrade.

## Tech stack

Kotlin 2.2, Jetpack Compose / Compose Multiplatform, Room, Realm (migration only), Koin,
kotlinx.serialization, [adhan2](https://github.com/batoulapps/adhan-kotlin) for prayer-time
maths, and a hand-written HTTP and WebSocket server for the control panel.

## Build

Requires JDK 21.

```bash
./gradlew assembleDebug      # APK named "Mosque Athan - <versionName>.apk"
./gradlew installDebug       # install on a connected device or emulator
./gradlew test               # JVM unit tests
./gradlew lint               # Android Lint
```

The first build also compiles the browser panel to wasm, which takes a while. The version number
lives in `gradle/libs.versions.toml`.

## Documentation

- [`CLAUDE.md`](CLAUDE.md): architecture, conventions and the history of the KMP migration.
- [`CODEX.md`](CODEX.md): product and UI rules.
