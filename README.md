# Game Hub

A free, ad-free collection of small Android games in a single app — one home screen
("Game Hub") launches into whichever game you pick, with a "Most Played" section
that tracks your top 5 games and everything else sorted alphabetically below it.

No ads, no accounts, no tracking. Just simple, fun games.

## Games

| Game | Description |
|---|---|
| Road Rush | Atari-style vertical racer — shift lanes to dodge oncoming traffic before you run out of lives. |
| Knight Survivor | Arena survivor — auto-attack hordes, level up, and outlast the clock! |
| Number Corridor | Forward-scrolling shooter — blast obstacle walls, grab +/-/x/÷ buff walls for damage, fire rate, and squad size. |
| Clash Decks | Card duel vs computer — Attack, Defense, Support, and status-effect tactics. |
| Memory Match | Find matching pairs of gaming cards. |
| Number Cruncher | Combine numbers to reach the target. |
| Snakes & Ladders | 2D board — 2–6 players, each human or CPU. |
| Base Defender | Defend your base — aim and shoot at incoming enemies. |
| Pixel Collector | Collect green pixels while avoiding red obstacles. |
| Battleship | Local 2 players or vs computer — place fleets, then take turns firing. |
| Rhythm Runner | Run, jump, and collect to the beat. |
| Train Survivor | Side-view train defense — fight ground and flying zombies and bosses, pick routes, earn coins, upgrade the train and its named survivors. Two-phone Bluetooth co-op: two trains, double zombies, lend passengers across a Gangway. |
| Tunnel Crew | Lemmings-style puzzler — hand skills (dig, build, bash, mine, climb, float, block, bomb, pour cement, lay decks, stack stairs) to a walking crew and guide them to the exit across 22 levels with medals. |
| Minesweeper | Two-phone Bluetooth minefield — co-op (shared lives) or versus (race for cells) on one shared board, with a practice bot. |
| Key Reader | Scan NFC keys and manage saved entries. |

The full list lives in [`GAME_CATALOG`](app/src/main/java/com/baraka/game/MainActivity.kt) —
adding a new game to the hub is a single entry there.

## Tech stack

- Kotlin + [Jetpack Compose](https://developer.android.com/jetpack/compose) for UI
- Per-game modules under `app/src/main/java/com/baraka/game/<game>/`, each a self-contained
  Activity (some using Compose, some Canvas/custom rendering, one on LibGDX for 3D)
- Min SDK 27, target/compile SDK 35

## Building

```bash
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Getting the app

Signed release APKs are published on the repo's
[Releases](../../releases) page — download and sideload the latest one directly,
no Play Store needed.

## Releasing a new version

See [RELEASING.md](RELEASING.md) for the versioning scheme and how pushing a
version tag builds and publishes a signed release automatically.
