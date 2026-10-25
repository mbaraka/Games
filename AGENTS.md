# Agent guide

Instructions for AI agents (and humans) working in this repo. Read this before building,
releasing, or adding a game. Human-oriented release background lives in
[`RELEASING.md`](RELEASING.md); this file is the short, do-exactly-this version.

## Project in one paragraph

"Game Hub": one Android app (Kotlin + Jetpack Compose, package `com.baraka.game`) that
launches many small games. Most games are still a package under
`app/src/main/java/com/baraka/game/<game>/`, registered in two places: `GAME_CATALOG` in
`MainActivity.kt` and `AndroidManifest.xml`. A game can also be **its own Gradle module**
under `games/<game>/` (Train Survivor is; see "Splitting a game into its own module"
below) - either way the package stays `com.baraka.game.<game>` and the `GAME_CATALOG` /
manifest registration works the same, since Gradle modules are just where the source
files physically live. Code shared by multiple games (`coop`, the settings/debug/theme
UI kit) lives in the `:core` module so every game module and `:app` can use it without
depending on each other. GitHub repo: `mbaraka/Games` (branch `master`).

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
- **App version vs game version.** The app version (`1.0.0` locally; the tag on releases) belongs to the whole hub, which holds
  many games. Each game that wants one has its OWN version, computed in `app/build.gradle.kts` as the number of commits that
  touched that game's code (Train Survivor: `1.<n>`, in `BuildConfig.TRAIN_SURVIVOR_VERSION`, shown in its Settings screen; Minesweeper: `1.<n>`, in `BuildConfig.MINESWEEPER_VERSION`, also its co-op protocol number; Dreadnought: `1.<n>`, in `BuildConfig.DREADNOUGHT_VERSION`, shown in its Settings screen).
  It goes up by one with every commit to that game and never because of another game. Do not edit it by hand; to give another
  game a version add a `gameBuild(...)` line and two `buildConfigField`s there (in `app/build.gradle.kts` for a game still in
  `:app`, or in the game's own `games/<game>/build.gradle.kts` if it has its own module).
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
  `core/src/main/java/com/baraka/game/ui/components/SettingsControls.kt`
  (`SettingsSection`, `SettingsToggleRow`, `SettingsSliderRow`) so all games look alike.
- Keep a small immutable settings data class per game (see
  `trainsurvivor/data/GameSettings.kt`) stored through the game's repository.
- The screen shows the app version; tapping it 7 times unlocks developer tools (below).

### 2. A debug tool

- Every game ships a developer panel built on the shared kit in
  `core/src/main/java/com/baraka/game/ui/debug/DebugKit.kt`:
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
   screen in `GameTheme` (copy the shape of `asteroids/AsteroidsActivity.kt`). A small game
   is fine as a package under `app/src/main/java/com/baraka/game/<game>/`; a game expected to
   grow large (lots of files, its own co-op protocol) is better off started as its own module
   under `games/<game>/` straight away - see "Splitting a game into its own module" below for
   the module shape, then just create the game inside it from the start instead of moving it
   later.
2. Register the Activity: in `app/src/main/AndroidManifest.xml` if the game lives in `:app`
   (set `screenOrientation`; landscape games use `sensorLandscape`), or in the game module's
   own `games/<game>/src/main/AndroidManifest.xml` if it has one (same activity element; no
   change needed in `:app`'s manifest, it merges in automatically).
3. Add a `GameCatalogEntry` to `GAME_CATALOG` in `MainActivity.kt` and a row to the
   table in `README.md`.
3b. **Multiplayer badge:** if the game has any multiplayer (Bluetooth co-op, or 2+ local human
   players), set `multiplayer = true` on its `GameCatalogEntry`. `GameCard` then shows a group
   icon on the hub card. Add it whenever a game gains multiplayer later, too. Currently:
   Clash Decks, Snakes & Ladders, Battleship, Train Survivor.
4. Persist progress with `SharedPreferences` (pattern: `*SettingsRepository`).
5. Define the game's **theme and shared button first** (requirement 4), then add the
   **settings screen** and the **debug tool** described above, and keep to the performance rules.
6. Screen-on: any `Activity` that is not `MainActivity` or `KeyReaderActivity` keeps the
   screen awake automatically via `GameApplication` - nothing to do.
7. Verify with `./gradlew :app:assembleDebug`, then release it as a new **MINOR**.

## Splitting a game into its own module

Most games are small enough to live as a package inside `:app`. A game with a lot of code,
its own co-op protocol, or that keeps colliding with other in-progress work (merge conflicts,
one game's broken WIP blocking everyone else's build) is worth giving its own Gradle module
instead - it still builds as part of the same app, but compiles and can be reasoned about on
its own, and another game's half-finished code can't block it. Train Survivor is the first
one done this way (`games/trainsurvivor/`); Battleship, Dreadnought, Base Defender and Key
Reader followed (`games/battleship/`, `games/dreadnought/`, `games/basedefender/`,
`games/keyreader/`) - follow the same shape for the next one.

Clash Decks is **not yet split** even though it would benefit (it's one of the larger games):
it imports `VectorIcon`/`KnightDesign`/`drawVectorIcon`/`drawKnightIcon` from Knight Survivor
and `SoundToggleRow` from Road Rush, both of which are still packages inside `:app`. A game
module can't depend on another game module or on `:app` (circular), so moving Clash Decks
first needs those shared bits lifted into `:core` (they're generic icon/sound-row UI, not
really Knight Survivor/Road Rush-specific) - or Knight Survivor and Road Rush split too.

**The shape:**

- `:app` - the thin hub: `MainActivity` (`GAME_CATALOG`), `GameApplication`, signing and
  versioning, the root `AndroidManifest.xml`, and games that haven't been split out yet.
  Depends on `:core` and every `:games:*` module.
- `:core` - an Android library module holding code **shared by more than one game**:
  `com.baraka.game.coop` (Bluetooth co-op), `com.baraka.game.ui.theme` (`GameTheme`),
  `com.baraka.game.ui.components` (`SettingsSection`/`SettingsToggleRow`/`SettingsSliderRow`),
  `com.baraka.game.ui.debug` (`DebugKit`/`DebugPanel`/`FpsOverlay`), and the shared
  `Theme.Game` style (`core/src/main/res/values/themes.xml`). Nothing game-specific belongs
  here - if only one game would use it, it stays in that game's own module.
- `games/<game>/` - an Android library module (Gradle path `:games:<game>`) holding exactly
  that game's package, `com.baraka.game.<game>`. Depends on `:core`; never depends on `:app`
  or another game module directly (that would be circular, since `:app` depends on it).

**Recipe (moving an existing game from `:app` into `games/<game>/`):**

1. `git mv app/src/main/java/com/baraka/game/<game> games/<game>/src/main/java/com/baraka/game/<game>`
   (a plain `git mv` on the whole package directory; git detects it as a rename per file).
   If the game has code under a second path (e.g. a `coop/<game>` subpackage), move that too.
2. Create `games/<game>/build.gradle.kts`: `com.android.library` + `kotlin.android` +
   `kotlin.compose` plugins, `namespace = "com.baraka.game.<game>"`, matching `compileSdk`/
   `minSdk`/`compileOptions`/`kotlinOptions` from `app/build.gradle.kts`, `buildFeatures {
   compose = true; buildConfig = true }`, and `implementation(project(":core"))` plus whatever
   AndroidX/Compose libs the game actually uses (copy from `app/build.gradle.kts`'s own
   dependency list - every game module keeps its own copy of these lines, there is no shared
   convention plugin for this project).
3. Give the module its own version counter in that same `build.gradle.kts`: copy the `git()`
   and `gameBuild()` helper functions from `app/build.gradle.kts` (small enough to duplicate
   per module) and compute `val <game>Build = gameBuild(<new path>, <old path>, ...)` -
   **pass both the new and the old (pre-move) path** so `git rev-list --count` keeps counting
   the commits made before the move; dropping the old path resets the version to a small
   number, which is wrong (the version is also the co-op protocol number other phones compare
   against). If the game uses `:core`'s shared packages (most do, at least `coop`), include
   both the old and new path for those too, the same way the existing `knightSurvivorBuild`/
   `minesweeperBuild`/`clashDecksBuild` lines in `app/build.gradle.kts` do today for `coop`.
   Then add the two `buildConfigField`s (`int ..._BUILD`, `String ..._VERSION`) there instead
   of in `app/build.gradle.kts`, and remove the game's old `gameBuild(...)` line and
   `buildConfigField`s from `app/build.gradle.kts`.
4. Fix any reference to the app module's generated `BuildConfig` inside the moved code
   (`com.baraka.game.BuildConfig.<GAME>_BUILD`/`_VERSION`) to the game module's own
   `com.baraka.game.<game>.BuildConfig.<GAME>_BUILD`/`_VERSION` - a library module's
   `BuildConfig` lives in its own `namespace` package, generated from the `buildConfigField`s
   added in step 3. `grep -rn "com.baraka.game.BuildConfig"` inside the moved package finds
   every spot that needs this.
5. Create `games/<game>/src/main/AndroidManifest.xml`: just a `<manifest
   xmlns:android="http://schemas.android.com/apk/res/android"><application>` wrapping that
   game's `<activity>` element(s), moved as-is from `app/src/main/AndroidManifest.xml`
   (**use the real `android` namespace URI above, not `res-auto`** - `res-auto` is for custom
   library attributes and silently breaks every `android:` attribute, including `android:name`,
   with a confusing "missing name" manifest-merger error). Delete the matching `<activity>`
   block from `app/src/main/AndroidManifest.xml` (leave a one-line comment pointing at the new
   file) - AGP merges a library's manifest into the app's automatically, so nothing else needs
   to change, and `MainActivity.kt`'s `GAME_CATALOG` entry and import keep working unchanged
   since the package name never changes, only which module physically contains it.
6. Add `include(":games:<game>")` to `settings.gradle.kts`, and
   `implementation(project(":games:<game>"))` to `:app`'s `dependencies {}` block in
   `app/build.gradle.kts`.
7. If the game references a style only `:app` used to define (like `@style/Theme.Game`) and
   the new module can't see it, move that resource into `core/src/main/res/` instead (`:app`
   already depends on `:core`, so it still resolves there too) rather than duplicating it.
8. Verify: `./gradlew :core:compileDebugKotlin :games:<game>:compileDebugKotlin` first (fast,
   isolates the new modules from any unrelated breakage elsewhere in `:app`), then
   `./gradlew :app:assembleDebug` for the full picture.

A new game that starts life as its own module from day one just does steps 2, 3 (with a
single path, no "old path" needed), 5 and 6 - no move required.

## Train Survivor: architecture and how to extend it

Train Survivor is built so content is data plus small plug-in pieces. **Never hard-code content in the
engine, renderer or UI**: no `when (type)` over content enums in `TrainGame.kt` or `TrainRenderer.kt` (use the
behaviour flags and registries below), and no hand-maintained lists of content in screens (derive them from
`Enum.entries`). Adding content must never require editing more files than the recipe says.

Train Survivor is its own Gradle module (`games/trainsurvivor/`, see "Splitting a game into
its own module" below). Code is in
`games/trainsurvivor/src/main/java/com/baraka/game/trainsurvivor/`:

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

**Elements** (`model/Elements.kt`, used by the Elemental Wilds): every hit has a `DamageKind` (guns are PHYSICAL; Rocket/Flamethrower/Dynamite
FIRE, Laser/Tesla ELECTRIC, Cannon/Mortar EARTH: `WeaponType.kind`, `TrainGame.strikeKind`), and a `ZombieType` can have an `element`
that takes 75% more from its weak kinds and 50% less from what it resists. `weaponScore` knows the matchups, so AUTO picks the right
gun. A new element is one `Element` entry; a boss that changes element sets `shifts = true`. Set `strikeKind` around any new damage
loop that should have a kind and reset it to PHYSICAL after.

**New weapon / upgrade / elite trait / danger level** - add the enum entry. Weapons and upgrades appear in the
shop and Field Guide automatically; an upgrade also needs its effect wired where `TrainGame` reads its level;
a weapon with new behaviour needs a branch in `TrainGame.fire` plus art in `drawWeapon`.

### Co-op for any game (shared layer)

Two-phone Bluetooth co-op lives in `core/src/main/java/com/baraka/game/coop/` (package `com.baraka.game.coop`,
in the `:core` module so every game module and `:app` can use it) and is shared by every game: **do not copy it into a game.**
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
**Shared world** (a run mode, not a map flag; `coop/SharedRun.kt`): two trains in ONE simulation. `TrainGame(save, lead = ...)`
makes the second train: it shares the lead's zombies, effects and clock, follows its route/crate/horde (`syncWorld`) and spawns
its own zombies in lane 1, so the world holds double the zombies by construction. Zombies carry a `lane` and only the train
of that lane runs their AI; guns, Blast, flak and tesla reach both lanes (`laneDy` converts frames); flames, mortar and
spikes stay in their own lane. Effects are tagged with a lane (`fxLane`) and drawn in that lane's frame. The renderer draws
lane 1 `LANE_DY` above lane 0 on a viaduct (`drawTrainScene(..., ally = ...)`). `SharedRun.bridge` turns each train's
`CoopSignal`s into direct calls on the other. Any map/route works in a shared world: nothing in the content knows about
it. **Over Bluetooth** (`coop/SharedNet.kt`, `systems/RigSync.kt`, `systems/FxLog.kt`): the host runs `SharedRun`; both phones swap saves first
(`SharedHandshake`, `SaveCodec`); the host streams `CoopMsg.Snap` ~15x/s (world, both trains, every zombie, the log of effects
since the last snapshot); the guest owns a `SharedMirror` (two trains that are never simulated, only overwritten and drawn by the
same renderer, kept smooth by `visualTick`) and sends button presses as `CoopMsg.Act` (`GuestAction`): a guest's `TrainGame` has
`remote` set, so every player action method forwards instead of acting. **To sync new game state**: write/read it in
`writeRig`/`readRig` (or `writeWorld`/`readWorld`) in the same order; **a new effect**: record it with `fx.add` in `TrainGame` and
replay it in `replayFx`; **a new hazard**: implement `Hazard.writeState/readState`; **a new player action**: add a `GuestAction`
code, a `remote?.let` line at the top of the method and a case in `SharedHostSession.applyAction`. The run screen only talks to
a `SharedDriver` (`PracticeDriver` bot, `HostDriver`, `GuestDriver`, `LoopbackDriver` = host and guest in one process, which
tests the whole network path on one phone). Both players see the same scene: the host's train is the lower track.
**Maps for two trains**: a `Biome` can be `coopOnly` (needs a partner: linked or shared), `sharedOnly` (needs a shared world:
Switchyard, Fault Line), give each track its own enemies with `Spawn(..., lane = 0|1)` (lower/upper track) and its own boss
with `bossUpper`. `ZombieType.hops` lets a zombie jump to the other track and `switcher` makes it throw the track switch;
the `SWITCHES` hazard swaps the two trains' tracks (`TrainGame.swapTracks`: `lane` is a var, so always use `lane`, never
assume the lead is on track 0). `COLLAPSE` is a partner-lock hazard like the jam and the gate.
**Versions**: `COOP_PROTOCOL` is Train Survivor's build number (automatic, see the game version in Build and install), so phones on
different builds warn the player ("different version") instead of silently ignoring messages. Nothing to bump by hand.
Co-op-only paints unlock through `Livery.coopDistance` against `SaveData.coopBest`. The engine never touches the network:
add shared behaviour as a `CoopSignal` plus a `CoopMsg`, and handle it in `CoopSession`.

### Data and saving

Progress and settings are stored in `SharedPreferences` file `train_survivor` by `TrainSaveRepository`. New
persistent fields get a default in `SaveData`, load/save lines in the repository, and must tolerate old saves
(missing keys fall back to defaults).

Balance numbers live in `model/` (costs, stats, spawn tables) and `TrainGame.kt` (ramp, event timing). Use
the debug tools (Settings -> version x7 -> Developer): "Go to <route>", spawn any enemy, trigger hazards, skip
time, god mode, unlock everything.
