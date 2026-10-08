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

## Adding a new game (checklist)

1. Create the package `com.baraka.game.<game>` with an `Activity` that wraps a Compose
   screen in `GameTheme` (copy the shape of `asteroids/AsteroidsActivity.kt`).
2. Register the Activity in `app/src/main/AndroidManifest.xml` (set
   `screenOrientation`; landscape games use `sensorLandscape`).
3. Add a `GameCatalogEntry` to `GAME_CATALOG` in `MainActivity.kt` and a row to the
   table in `README.md`.
4. Persist progress with `SharedPreferences` (pattern: `*SettingsRepository`).
5. Add the **settings screen** and the **debug tool** described above, and keep to the
   performance rules.
6. Screen-on: any `Activity` that is not `MainActivity` or `KeyReaderActivity` keeps the
   screen awake automatically via `GameApplication` - nothing to do.
7. Verify with `./gradlew :app:assembleDebug`, then release it as a new **MINOR**.

## Train Survivor pointers

Code is in `app/src/main/java/com/baraka/game/trainsurvivor/`:

- `model/TrainModels.kt` - weapons, zombies, upgrades, survivors/perks, shop rules.
  `model/Progression.kt` - daily goals, achievements, records, prestige.
- `systems/TrainGame.kt` - the whole simulation (per-car health, perks, abilities, events,
  bosses, train weapons). Follows the performance rules above.
- `render/TrainRenderer.kt` - all drawing (Canvas), including the HUD.
- `ui/` - `TrainSurvivorScreen.kt` (owns audio/settings, run screen), `GarageScreen.kt`,
  `TrainSettingsScreen.kt`, `ProgressScreen.kt` (goals/achievements/records/prestige),
  `FieldGuideScreen.kt`, `TrainDebug.kt` (this game's debug tools).
- `data/` - `TrainSaveRepository.kt` (progress + settings in `SharedPreferences` file
  `train_survivor`), `GameSettings.kt`, `TrainAudio.kt` (procedural music + ambience),
  `TrainSoundEngine.kt` (effects), `Haptics.kt`.

Balance numbers live in `TrainModels.kt` (costs, zombie stats) and `TrainGame.kt` (spawn
rates, difficulty ramp, boss schedule). Use the debug tools to test late-game content.
