# Handoff — settings UX fixes and controller picture sync (2026-10-24)

Branch `kmp`. **Nothing is committed.** New files are `git add`ed; everything else is modified in the
working tree. `app/mosque_athan.realm*`, `gradle/libs.versions.toml` and `.idea/*` were already dirty
before this session — do not commit them with this work.

## State in one paragraph

Four rounds of user-reported issues were implemented. **Every round compiles and passes the unit
tests** (`:core:model :ui:design :ui:settings allTests`, then `:app:testDebugUnitTest
:app:compileDebugKotlin` — 216 app tests, 0 failures, last run after round 4; the wasm panel also
builds). **Most of it has been seen on a device only up to round 3**: the user tested rounds 1–3 on the
Xiaomi (display) and Realme (controller) themselves and reported the fixes that became rounds 2–4.
**Round 4 (presence sweep, sending pictures to a new server, saved displays) has never run on a
device.** The last APK this session installed was on the AQUA TV and predates rounds 3–4. Which build
is on which device is whatever the user installed — ask before assuming.

## Rules that applied all session (still apply)

- **No build without asking**, and permission expires after 1 hour. This includes `assemble*`, test
  runs and compiles. Ask every batch. `:app` compile also builds the wasm panel (it is a dependency).
- **Ask before installing to any device.** All are in personal use. Install with
  `adb -s <serial> install -r`, never `installDebug` (that pushes to every connected device).
- The AQUA TV needs its adb RSA prompt accepted on screen; `unauthorized` means exactly that. A
  `disconnect` + `connect` after accepting fixed a stale transport.
- Comments follow the repo style (explain *why*, name the failure being prevented). Use
  `Lang.get(en, id)` for strings and design tokens, never literals.

## What was done

### Round 1 (11 items)
1. Dashboard backgrounds list now fed by `getDashboardBackgroundsFlow()` (was a one-shot read inside a
   `combine`, so it only refreshed when something else changed). `DisplayMediaViewModel`.
2. **Sort mode for the image pager** (`ui/component/SlideshowComponent.kt`): Sort button beside Add
   (stacked on Compact); in sort mode D-pad Left/Right moves the centred item, OK confirms, Back /
   Cancel discards; on-screen move buttons for touch. Order held locally and sent once on Done.
   Covers Dashboard, Slideshow and Friday. `onReorderItems` is now nullable.
3. 3/4/5. **`RunningTextList.kt`** (new, shared) replaces the Dashboard's own list and the Adzan/Iqomah
   list: Sort mode (OK picks up, Up/Down moves, OK drops), edit dialog, **delete confirmation**
   (`MosqueConfirmDialog`, new in `:ui:design`). Adzan/Iqomah adapt strings to rows with ids =
   text + occurrence index (an index would change under a held row). Sort button is always drawn,
   disabled below two items.
6. **Preview Stop.** `PreviewKind` (core:model) + `MosqueSnapshot.activePreview` + `exit_preview`
   action. `AppViewModel` tracks `activePreview`, remembers `screenBeforePreview` and `exitPreview()`
   returns to it (it used to always land on Settings, which stranded a TV that a controller previewed
   on). Server broadcasts on change. Web panel: the pressed Preview button becomes `StopPreviewButton`
   via `LocalActivePreview`. `SettingsViewModel.lastCategory` restores the category after a local
   preview. **Android controllers still preview on their own screen**, not on the display.
7. `LocalCanServe` (`:ui:design`): web provides `false`, so the Server option is hidden there.
8. Mode selector is now a compact two-segment switch top-right (above the text on Compact).
9. Mode-change confirmation, only when something live would be dropped
   (`RemoteDevicesUiState.switchingModeDropsConnection`).
10. Remote mode: tabs removed, the Sync card shows directly.
11. Friday category moved between Display & Media and Connectivity.

### Round 2 (5 items)
- Sort button visible from the first item (Adzan/Iqomah ship with one line).
- Friday "Transition Animation" / "Transition interval" reuse `TransitionAnimationRow` and a new
  `TransitionIntervalRow` (`ThemeSettingsSegment.kt`). Friday now stores **total seconds**; the old
  seconds/minutes selector never converted anything (the display read the number as seconds).
  `FridayEvent.IntervalUnitChanged` removed; `jumatSlideIntervalUnit` is now an unused stored field.
- `windowClassFor(width, isTv, height)`: a window taller than wide is `Compact` (tablet portrait gets
  the phone layout). Tests added in `WindowClassTest`.
- **Picture sync, pull direction.** `ConfigRepository.applySnapshot` gained `media`, `fridayMedia`,
  `dashboardBackgrounds` (default null) in Room (`MosqueDao.replaceTables`), Realm and the web
  repository. A controller now takes the display's picture rows; it never sends its own (files live
  on the display). `CLAUDE.md` updated.
- Offline copies: `PreviewCache` (app) keeps a downscaled JPEG (max 1280 px, `filesDir/preview-cache`)
  of every picture, filled after a sync via `MediaStore.cacheForOffline` and on demand by
  `AndroidMediaImageLoader` (local file → cache → fetch). Re-fetched on every refresh, pruned of
  unlisted pictures.

### Round 3 (this is the part that is least verified)
- `SyncConflictDialog` rebuilt as one scrolling column with full-width stacked buttons (portrait /
  short landscape), plus a note that picture files are never sent to the display.
- **Android controller uploads in REMOTE mode.** `MediaFileStore` now POSTs to the display's
  `/upload` (temp file → downscale images → fixed-length body) for presentation, dashboard, Friday,
  custom background, murottal and `replacePath`; returns null so the display adds the row. Outcome is
  reported through `MediaFileStore.notices` (a second `MessageHost` in `MainActivity`) and
  `uploadsFinished`. Previously it copied locally and created a row the display never saw, which is
  why those pictures read "Failed to load image" and never reached the server.
- **Live refresh.** `SettingsViewModel.refreshMediaFromDisplay()` — pictures only, display wins —
  called by `MainActivity` every 8 s while Settings is open (`PICTURE_POLL_MS`) and right after an
  upload. Compares rows **and** the table stamps from the snapshot.
- `ServerDataSource.touchMediaFile(path)` (Room: stamps the tables naming that path; Realm: no-op),
  called by the server after a `replacePath` upload, because a replace writes no row and the stamp is
  what the poll compares.
- **Guards added after the user lost data** (see below): the refresh pauses while the sync dialog is
  open, and only applies to the *same display* (shared row id; an empty local list adopts anything).
- `RemoteActionSender.checkLink()` heartbeat on the same 8 s tick; the "Connected" indicator used to
  only change when something was sent.

### Round 4 (compiled and unit-tested, not run on a device)
- **Server "controllers online" count.** `SettingsServer` sweeps every 10 s: a device that is online,
  holds no socket and has been silent for 30 s (`PRESENCE_TIMEOUT_MS`) is marked offline. HTTP
  controllers never had a closing socket to do it, so the count only went up. The remote's 8 s
  heartbeat (`checkLink`) keeps it online while its Settings is open.
- **Pictures cross to a new server.** `SettingsViewModel.sendLocalPictures` (called from `reconcile`)
  sends the saved previews (`MediaStore.sendPicturesToDisplay`, Android impl in `MediaFileStore`) of
  pictures whose ids the display does not list, in `sortOrder`, when this device won that table;
  videos are counted and skipped; on success it force-pulls the display's list (`pullPictures(force)`).
  `refreshMediaFromDisplay` is now a thin guard around `pullPictures`.
- **Saved displays.** `KnownServersStore` / `KnownServer` (core:model, with `withVisit` and tests),
  `PrefsKnownServers` (SharedPreferences, deliberately not the database), `InMemoryKnownServers` on
  web. `RemoteDevicesViewModel` probes each address (`HttpFetcher.isReachable`, 2 s) every 10 s while
  the pane is collected, seeds the list from the current address once, and `KnownServersSection`
  renders it (stacked actions on Compact). `MosqueSnapshot.serverName` carries the display's model.
  A display is remembered after a successful sync (`syncWithTvServer`).

## Open questions / not done

- **User never said which direction "Settings differ → choose device data: image not sent to the
  remote" meant.** Read as "Use the display's didn't bring pictures to the remote" and fixed by the
  offline copies. If they meant remote→server: that cannot work today (only rows sync, not files).
  Offered, not built: upload the `PreviewCache` copies to the new server as pictures (≈1280 px, no
  videos).
- **"All data cleared" after choosing "Use this device's" on a second server.** Diagnosed as the live
  refresh replacing local picture rows with the new server's. If what vanished was *settings /
  schedules / running text*, no cause was found — ask which device lost what.
- Android remote does not live-refresh the custom background path or murottal paths (only pictures);
  they still need a manual Sync.
- Videos upload at full size with no progress, and a video cannot be copied to a new server at all.
- Pictures sent to a new server arrive as ~1280 px previews, and their "Use" flag resets to on.
- Pictures that exist only on the display's side and were *deleted all at once* will not auto-clear
  on a controller (no id overlap → treated as a different display); manual Sync handles it.
- `PreviewCache`, the upload path, the refresh and the heartbeat have **no unit tests** (they need
  Android bitmap / network). A Room test for `applySnapshot` with the new media params is also missing
  (`RoomApplySnapshotTest`); the existing tests pass only because the params default to null.
- Unverified on hardware: D-pad sort focus staying on the held item (pager and lists), the web Stop
  Preview round trip, the mode switch layout on a phone, portrait tablet layout, the new dialog.

## How to verify next (needs the user's go-ahead for builds and installs)

1. Install the current build on the Xiaomi (display) **and** the Realme (controller) — both are
   needed: the display for `touchMediaFile` and the preview field, the controller for upload/refresh.
2. Realme in Remote mode pointed at the Xiaomi, Settings open. Add a picture on the Xiaomi → it
   appears on the Realme within ~8 s. Add one from the Realme → "Sent to the display." and it shows on
   both. Replace one on the Xiaomi → the Realme's thumbnail refreshes.
3. Close the app on the Xiaomi → the Realme's indicator reads Offline within ~13 s and pictures still
   show. Point the Realme at a second server and choose "Use this device's" → its pictures stay.
4. Web panel (`/panel/` on the display): start a preview, press the button again (Stop), confirm the
   display returns to the clock.
5. Round 4: on the Xiaomi watch the "controllers online" count while the Realme has Settings open,
   then leaves it (drops after ~30 s). On the Realme, "Saved displays" shows the Xiaomi with a dot that
   goes grey within ~10 s of closing the Xiaomi's app — check portrait **and** landscape. Connect the
   Realme to a second display, choose "Use this device's", and confirm settings **and** pictures
   arrive there (pictures as ~1280 px previews, videos skipped with a message).

## Suggested next steps

1. Ask which devices to install the current build on, and for permission to run the unit tests
   again if anything changes. Install the **display and the controller together**.
2. Run the hardware checks above, round 4 first (least verified).
3. Add the missing tests: `RoomApplySnapshotTest` for `applySnapshot` with media rows;
   `SettingsViewModel` pull/send logic with a fake `HttpFetcher` and `MediaStore`.
4. Ask the user the two still-open questions below (item-3 direction; what exactly vanished).
5. Nothing is committed. When asked to commit, split by round, exclude the Realm files /
   `libs.versions.toml` / `.idea/*`, and use the repo's `type: [Area] > description` subjects.

## Files worth reading first

`ui/settings/.../RunningTextList.kt`, `ui/settings/.../ui/component/SlideshowComponent.kt`,
`ui/settings/.../SettingsViewModel.kt` (`reconcile`, `refreshMediaFromDisplay`,
`keepPicturesForOffline`), `app/.../data/media/MediaFileStore.kt`,
`app/.../data/remote/{PreviewCache,AndroidMediaImageLoader,RemoteActionSender}.kt`,
`app/.../ui/navigation/AppViewModel.kt` (preview tracking), `app/.../MainActivity.kt` (poll loop),
`ui/settings/.../remotedevices/{RemoteDevicesSettingsPane,RemoteDevicesViewModel,KnownServersSection}.kt`,
`core/model/.../domain/{KnownServer,MediaStore,PreviewKind}.kt`,
`app/.../server/SettingsServer.kt` (presence sweep, `touchMediaFile`, `serverName`, `/upload`).
