# Agent guide

Instructions for AI agents (and humans) working in this repo. Read this before building,
releasing, or adding a game. Human-oriented release background lives in
[`RELEASING.md`](RELEASING.md); this file is the short, do-exactly-this version.

## Project in one paragraph

"Game Hub": one Android app (Kotlin + Jetpack Compose, package `com.baraka.game`) that
launches many small games. Each game is its own Activity in its own package under
`app/src/main/java/com/baraka/game/<game>/`, registered in two places:
`GAME_CATALOG` in `MainActivity.kt` and `AndroidManifest.xml`. GitHub repo:
`mbaraka/Games` (branch `master`).

## Build and install locally

```bash
./gradlew :app:compileDebugKotlin      # fastest "does it compile" check
./gradlew :app:assembleDebug           # build debug APK
./gradlew :app:installDebug            # build + install on the connected phone
```

- Debug APK output: `app/build/outputs/apk/debug/baraka-<version>-debug.apk`.
- `adb` is not on PATH on the dev machine. It lives at
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (use it for `adb devices`, etc.).
- **Only install on the user's device when they ask.** Compiling is always fine.
- Local builds are version `1.0.0` (the default) - that is expected, see below.
- Prefer verifying with a build over writing unit tests (the owner asked for no tests in
  this app). If tests are ever needed, see the Gradle test-runner PATH note in the
  owner's memory notes: `testDebugUnitTest` breaks on this machine's PATH, compilation
  does not.

## Releasing a new APK on GitHub (the main thing)

A release is produced **by pushing a git tag named `vMAJOR.MINOR.PATCH`**. Nothing is
edited in a file; the tag *is* the version. The workflow
[`.github/workflows/release.yml`](.github/workflows/release.yml) then:

1. Parses the tag (`v1.4.2` -> major 1, minor 4, patch 2).
2. Builds `./gradlew assembleRelease -PappMajor=1 -PappMinor=4 -PappPatch=2`, which sets
   `versionName = "1.4.2"` and `versionCode = major*10000 + minor*100 + patch`
   (see the top of `app/build.gradle.kts`).
3. Signs the APK with the keystore stored in repo secrets.
4. Creates a GitHub Release named `v1.4.2` with `baraka-1.4.2-release.apk` attached and
   auto-generated notes.

### Version rules

- `MAJOR` stays `1`.
- `MINOR` +1 when a **new game** is added (reset `PATCH` to 0).
- `PATCH` +1 for fixes/tweaks/new features inside existing games.
- `MINOR` and `PATCH` must each stay **below 100** (they are packed into `versionCode`).
- Each new version must be **higher than the last one**, or Android will refuse to
  install it over the previous build. Never reuse or move a published tag.

### Steps (copy these)

```bash
# 1. Make sure everything you want released is committed AND pushed to master.
git status                      # working tree should be clean for the files you changed
git push origin master

# 2. Find the current latest version, decide the next one.
git fetch --tags
git tag --sort=-v:refname | head -3
gh release list --limit 3

# 3. Tag the pushed commit and push the tag (this starts the build).
git tag v1.1.0 -m "Add Train Survivor"
git push origin v1.1.0

# 4. Watch the build (about 5-6 minutes).
gh run list --workflow "Release APK" --limit 1
gh run watch                    # pick the run, or: gh run watch <run-id> --exit-status

# 5. Confirm the release and get the download link.
gh release view v1.1.0
```

Users sideload the APK from the repo's **Releases** page. Releases are signed with the
same key every time, so a newer release installs over an older one as a normal update.

### Pre-flight checklist before tagging

- [ ] `./gradlew :app:assembleDebug` succeeds on the commit you are about to tag.
- [ ] The commit is on `origin/master` (`git log origin/master..HEAD` is empty).
- [ ] The tag is exactly `v<number>.<number>.<number>` (no suffixes like `-beta`; the
      workflow trigger will not match them).
- [ ] The tag does not exist yet (`git tag -l v1.1.0`, and `git ls-remote --tags origin`).
- [ ] The user asked for a release. **Pushing a tag publishes a public release - confirm
      before doing it.**

### If the release fails

- Open the log: `gh run view <run-id> --log-failed`.
- Typical causes: a compile error on that commit; missing/rotated signing secrets
  (`gh secret list` should show `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`); `gradlew` losing its executable bit
  (the workflow runs `chmod +x gradlew` to guard against this).
- A failed run leaves a tag but no release. Fix the problem on `master`, then **delete
  and recreate the tag on the fixed commit** (only because nothing was published):

  ```bash
  git push origin :refs/tags/v1.1.0 && git tag -d v1.1.0
  git tag v1.1.0 -m "..." && git push origin v1.1.0
  ```

  If a release *was* published and is wrong, do not reuse the number - ship the next
  patch version instead.

### Signing key (never commit it)

- The release keystore and passwords exist only as GitHub Actions secrets and on the
  owner's machine. `*.jks` and `*.keystore` are git-ignored; never add one to git or
  print secret values.
- Local release builds without the key env vars still work but are unsigned; use debug
  builds for local testing. One-time key creation steps are in `RELEASING.md`.

## Requirements for EVERY game

These are mandatory for each game in the hub, new or existing. If a game is missing one,
adding it is part of the next change you make to that game.

### 1. A settings screen

- Every game has its own Settings screen, reachable from the game's start/garage screen
  **and** from its in-game pause menu. Changes apply immediately and are persisted.
- Typical contents: separate volume controls for each sound channel that exists (music,
  sound effects, ambience), a vibration toggle if the game vibrates, display options
  (e.g. screen shake, damage numbers), and a "Show FPS counter" toggle. Every sound or
  effect the game has must be controllable - never ship audio with no setting for it.
- Build it from the shared rows in
  `app/src/main/java/com/baraka/game/ui/components/SettingsControls.kt`
  (`SettingsSection`, `SettingsToggleRow`, `SettingsSliderRow`) so all games look alike.
- Keep a small immutable settings data class per game (see
  `trainsurvivor/data/GameSettings.kt`) stored through the game's repository.
- The screen shows the app version; tapping it 7 times unlocks developer tools (below).

### 2. A debug tool

- Every game ships a developer panel built on the shared kit in
  `app/src/main/java/com/baraka/game/ui/debug/DebugKit.kt`:
  - describe cheats as `DebugSection`s of `DebugItem.Action` / `Toggle` / `Info`;
  - show them with `DebugPanel(...)`; gate with `DebugKit.isUnlocked(context)` (true for
    debuggable builds, or after the 7-tap unlock in Settings);
  - show live frame rate with `FpsOverlay(FrameStats)` when "Show FPS counter" is on.
- Put the game's cheat lists in their own file (example: `trainsurvivor/ui/TrainDebug.kt`).
  Provide at least: economy cheats (add coins/currency), progress cheats (unlock/max
  everything, reset all progress), and, for real-time games, in-run tools (god mode, heal,
  skip ahead in difficulty, spawn each enemy type, spawn boss, trigger events, slow motion /
  fast forward). The purpose is to let anyone test balance and late-game content in minutes.
- Debug actions may bypass normal rules but must go through the game's real state/save
  code paths, so they cannot leave the game in an impossible state.

### 3. Performance is a feature

- Target a steady 60 fps on a mid-range phone. Check with the FPS overlay, not by feel.
- No allocation in per-frame code: use index loops (`for (i in list.indices)`), reusable
  scratch lists, pre-sized collections, and in-place sorting. Avoid `filter`, `map`,
  `sortedBy`, `toList`, string templates, and `listOf`/`floatArrayOf` inside the update or
  draw loops. Cache HUD strings and rebuild them only when the value changes.
- Cap every collection that gameplay can grow (enemies, particles, floating texts).
- Keep simulation and drawing separate (see `TrainGame.kt` vs `TrainRenderer.kt`).
- When you add a feature, think about its worst case (hundreds of bullets, full screen of
  enemies) before merging it.

### 4. A theme, decided before the first screen

Colour bugs (dark text on a dark card, white on white) come from relying on Material defaults, so
every game fixes its look **before** building any UI:

- Create the game's palette first, as one object (example: `tunnelcrew/ui/CrewWidgets.kt`
  `CrewColors`, plus `tunnelcrew/model/CrewTheme.kt` for in-world colours): background, card, bar,
  accent, text, muted text, good, bad, danger, and the colour that goes *on* the accent. Nothing else
  in the game hard-codes a colour.
- **Never use a Material `Button`, `Text`, `Switch` etc. on a custom background without passing
  explicit colours.** Material picks the label colour from the theme, which is black on our dark
  containers. Build one shared game button (example: `CrewButton`) with explicit `containerColor` and
  `contentColor` and use it for every button, including dialogs and overlays.
- Check contrast for every state: normal, selected, disabled, pressed, and on overlays/dialogs
  (win/lose, pause, level start). Text must be readable at a glance: light text on dark, dark text
  only on light/accent fills.
- Keep in-world art data-driven: palettes, backdrops and per-activity drawing live in a theme/registry
  so a new level, route or skin is data, not new drawing code.
- Before calling a screen done, open every dialog and overlay once (or ask the owner to) and read
  all the text.

## Adding a new game (checklist)

1. Create the package `com.baraka.game.<game>` with an `Activity` that wraps a Compose
   screen in `GameTheme` (copy the shape of `asteroids/AsteroidsActivity.kt`).
2. Register the Activity in `app/src/main/AndroidManifest.xml` (set
   `screenOrientation`; landscape games use `sensorLandscape`).
3. Add a `GameCatalogEntry` to `GAME_CATALOG` in `MainActivity.kt` and a row to the
   table in `README.md`.
4. Persist progress with `SharedPreferences` (pattern: `*SettingsRepository`).
5. Define the game's **theme and shared button first** (requirement 4), then add the
   **settings screen** and the **debug tool** described above, and keep to the performance rules.
6. Screen-on: any `Activity` that is not `MainActivity` or `KeyReaderActivity` keeps the
   screen awake automatically via `GameApplication` - nothing to do.
7. Verify with `./gradlew :app:assembleDebug`, then release it as a new **MINOR**.

## Train Survivor: architecture and how to extend it

Train Survivor is built so content is data plus small plug-in pieces. **Never hard-code content in the
engine, renderer or UI**: no `when (type)` over content enums in `TrainGame.kt` or `TrainRenderer.kt` (use the
behaviour flags and registries below), and no hand-maintained lists of content in screens (derive them from
`Enum.entries`). Adding content must never require editing more files than the recipe says.

Code is in `app/src/main/java/com/baraka/game/trainsurvivor/`:

- `model/` - pure data, one file per concept: `Enemies.kt` (ZombieType, EliteTrait), `Routes.kt` (Biome, Spawn,
  HazardKind), `Survivors.kt` (crew, avatar looks, perks), `TrainStyle.kt` (TrainShape, Livery), `Upgrades.kt`
  (weapons, train upgrades, danger levels), `TrainModels.kt` (save data and shop rules), `Progression.kt`
  (goals, achievements, records, prestige).
- `systems/TrainGame.kt` - the simulation. It reads behaviour from the data flags (flying, leaper, ranged,
  stationary, armor, summons...) and the train geometry from `TrainShape`. `systems/Hazards.kt` - pluggable
  route hazards.
- `render/` - `TrainRenderer.kt` (scene loop, HUD, the classic steam train and original six routes),
  `Registries.kt` (the plug-in interfaces), `MobArts.kt`, `RouteArts.kt`, `TrainShapes.kt`, `HazardArt.kt`.
- `ui/` - screens (`TrainSurvivorScreen.kt` run + audio/settings owner, `GarageScreen.kt`, `ProgressScreen.kt`,
  `FieldGuideScreen.kt`, `TrainSettingsScreen.kt`, `TrainDebug.kt`).
- `data/` - save repository, settings, audio, haptics.

### Recipes

**New enemy** - (1) add an entry to `ZombieType` using the flags (`flying`, `leaper`, `ranged`, `stationary`,
`armor`, `fireImmune`, `lunges`, `erratic`, `packSize`, `specialEvery/summon/smash` for bosses). To make it a
route's boss, set `boss = ...` on that `Biome` entry (every route names one); a boss with no unique art can be
`alias`ed to a smaller cousin's art in `BossArts.kt`. (2) Put it in a route's spawn table (`signature(...)` in `Biome`). (3) Give it
colours in `registerMobLooks()` (`MobArts.kt`) and, if it isn't a standard humanoid size, a Field Guide
portrait size in `PortraitSizes`; if it needs accessories register a `HumanoidExtra`, if it is not a
humanoid write a `MobArt` and register it in `registerMobArts()`. (4) Add its `ZombieNote` in
`FieldGuideScreen.kt` (the guide looks notes up by type). New AI behaviour that no flag covers goes in
`TrainGame.updateZombies` behind a new flag on `ZombieType`.

**New route** - (1) add a `Biome` entry: stats, its `boss`, `commonSpawns(...) + signature(...)`, optional
`gravityMult/rangeMult/speedMult/repairMult/flatRegen` and `hazards`. (2) Write a `RouteArt` object in
`RouteArts.kt` (`backdrop` required; `foreground`, `weather`, `darkness`, `glowNight`, `trainKit` optional) and
register it in `registerRouteArts()`. Junction cards, the Field Guide, debug "Go to route" and the spawn logic
need nothing else. Routes without art fall back to the plains look instead of crashing.

**New hazard** - add a `HazardKind` (name + description), write a `Hazard` subclass in `systems/Hazards.kt`
(`update`, optional `onTap`, `shootDown`, `trigger`), add it to `HazardRegistry`, and draw it in
`HazardArt.kt` (the `when` there is exhaustive on purpose so a hazard can't ship invisible). List the kind in
a route's `hazards`.

**New survivor / avatar** - add a `Survivor` to `SURVIVORS` (name, role, perk, `AvatarLook`). The train gets
another seat; saving, seat prices, garage cards, the in-run switcher and debug tools follow `SURVIVORS.size`.
A new `Perk` needs one line in `TrainGame` where perks are resolved. A new `HatStyle` needs a branch in
`drawAvatarHat`; a new `AvatarBody` or `AvatarExtra` needs a branch in `drawAvatarBody`. Give each survivor a
different silhouette (`width`/`height`), gear and colours so the crew is easy to tell apart.

**New train shape** - add a `TrainShape` entry (sizes, nose, driver position, price, optional `hpMult` /
`speedMult`), write a `TrainShapeArt` (`paintLoco`, `paintCar`, `paintWindows`) in `TrainShapes.kt` using
`palette` (the current livery) and register it in `registerTrainShapes()`. All car positions in the engine and
renderer come from the shape (`carLeft`, `gunX`, `locoLeft`...), so nothing else changes.

**New livery** - add a `Livery` entry (colours, price, `glow`, and a small `perk` with its multiplier: damage, speed, health,
repair, coins or ability cooldowns). Paint is never cosmetic-only. It appears in the Garage's Look tab.

**New weapon / upgrade / elite trait / danger level** - add the enum entry. Weapons and upgrades appear in the
shop and Field Guide automatically; an upgrade also needs its effect wired where `TrainGame` reads its level;
a weapon with new behaviour needs a branch in `TrainGame.fire` plus art in `drawWeapon`.

### Co-op for any game (shared layer)

Two-phone Bluetooth co-op lives in `com.baraka.game.coop` and is shared by every game: **do not copy it into a game.**
It has `CoopLink<M>` (non-blocking send/poll), `BluetoothLink`/`BluetoothHost`/`CoopBluetooth` (classic RFCOMM between
paired phones), `SimulatedPartner<M>` (base for a pretend partner so a game can be tried on one phone), `CoopLaunch`
(connected link + `CoopSide` HOST/GUEST) and the generic `CoopLobbyScreen` (pair, host, join, practice, permissions).

To add co-op to a game: (1) define your message class `M` with `write`/`read`; (2) create a `CoopSpec<M>` with a **new
random service UUID** (that is what stops two games connecting to each other), a service name, a `CoopCodec` and an
optional `bye` message; (3) write a session class that sends messages from game events and applies received ones
(host-authoritative streaming, as in `clashdecks/coop`, or two simulations sharing events, as in `trainsurvivor/coop`);
(4) call `CoopLobbyScreen(spec, heading, intro, practiceNote, simulate = { YourSimulatedPartner() }, onReady, onClose)`.
The manifest already holds the Bluetooth permissions. Only the real Bluetooth link needs two phones to test.

Train Survivor specifics: `systems/Coop.kt` (`CoopRole`, `CoopSignal`), `coop/CoopSession.kt`. Each phone simulates its own
train with double spawns (`coopSpawnMult`); the host owns junctions and boss arrivals, boss damage is forwarded as a
fraction of boss health, Blast also goes off on the partner's train, and the **Gangway** upgrade lends a passenger to the
partner's guest seat and unlocks Send Repair. Also in co-op: Give Coins, **Link Up** (damage) and **Emergency Tow**
upgrades, combo shots when both trains hit the shared boss within 2 s, rescue events (a stranded survivor only a lent
passenger can fetch), and **co-op-only content**: a `Biome` with `coopOnly = true` (Twin Bridge, Echo Mine) only appears
at junctions while a partner is connected; `PartnerLock` hazards (`SIGNAL_JAM`, `GATE_LOCK`) can only be cleared by the
partner; a `ZombieType` with `partnerOnly = true` takes 10% from your own guns and full damage from your partner's.
Co-op-only paints unlock through `Livery.coopDistance` against `SaveData.coopBest`. The engine never touches the network:
add shared behaviour as a `CoopSignal` plus a `CoopMsg`, and handle it in `CoopSession`.

### Data and saving

Progress and settings are stored in `SharedPreferences` file `train_survivor` by `TrainSaveRepository`. New
persistent fields get a default in `SaveData`, load/save lines in the repository, and must tolerate old saves
(missing keys fall back to defaults).

Balance numbers live in `model/` (costs, stats, spawn tables) and `TrainGame.kt` (ramp, event timing). Use
the debug tools (Settings -> version x7 -> Developer): "Go to <route>", spawn any enemy, trigger hazards, skip
time, god mode, unlock everything.
