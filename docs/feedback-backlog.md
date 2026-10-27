# User feedback backlog (2026-10-26 playtest)

Raw feedback from a real tester's session, screenshotted/forwarded in chat. Grouped by
area/game and turned into concrete tasks. Work through these one at a time, checking
the box when done. Keep diffs scoped to the one item being fixed so this list stays
useful as a changelog of what shipped.

> **Note:** the other session's `MainActivity.kt` icon-grid work (F1) and Road Rush
> free-steering work (F12) are both done and merged into this tree.

## Hub / game selection screen (`MainActivity.kt`)

- [x] **F1 - Game order keeps shifting.** Done by the other session working on
  `MainActivity.kt`: the hub now shows an icon grid per game (see merge commit "Merge
  GameHub1: Game Hub icon grid"). Confirmed merged cleanly with all other work in this
  backlog and verified live on-device.

## Cross-game UI consistency (applies to many games)

- [ ] **F2 - Upgrade/choice screens are inconsistent.** *(First instance fixed in
  Dreadnought, see F7; Knight Survivor's level-up screen, F9, still needs the same
  treatment before this is a cross-game win rather than a one-off.)* Some "pick one of three"
  screens lay the options out side-by-side, others stack them vertically; box sizes
  don't match each other (seen on Dreadnought's refit screen and Knight Survivor's
  level-up screen). Needs one shared layout/component so every game's upgrade-choice
  screen looks and behaves the same.
- [ ] **F3 - Too much text on choice cards; no before/after or iconography.** Upgrade
  cards show raw text like "REPAIR heals 26% every 31s -> REPAIR heals 52% every 31s"
  with no visual before/after, and no icon per option. Tester's reference: a
  soccer-ball/drill-shot/molotov skill-pick screen that leads with a big icon, one
  short line, and a star-pip rarity row. Target: icon + short label + a clear
  "12 -> 14" style delta instead of full sentences, for every upgrade/refit/level-up
  screen in the app (Dreadnought refit, Knight Survivor level-up, Train Survivor
  upgrades, Number Corridor buffs, etc).
- [x] **F4 - Inconsistent back navigation.** Done: added a shared `HubBackButton`
  (`core/src/main/java/com/baraka/game/ui/components/HubBackButton.kt`, theme-aware via
  `GameColors`/`MaterialTheme`) and wired it top-left on the 9 games that had no
  in-screen way back at all - Snake, Road Rush, Brick Blitz, Star Invasion, Silo
  Defense, Rock Blaster, Number Corridor, Memory Match, Number Cruncher. Games that
  already had their own exit control (Dreadnought, Battleship, Arc Shot, Minesweeper,
  Clash Decks, Crate Crew, Tunnel Crew, Snakes & Ladders, Base Defender, Key Reader,
  Train Survivor, Knight Survivor) were left as-is - a later pass could move them onto
  the same shared component for visual consistency, but their back navigation already
  works.
- [x] **F5 - Difficulty/mode pickers are too busy.** Battleship's mode picker done via
  F13. Memory Match's 4-way difficulty picker (its clearest match for the tester's "4
  radio selections" complaint) redone as one slider with a big current-difficulty
  label and a single Play button, replacing 4 stacked buttons. Verified live - dragged
  through all 4 levels, started Expert, got the correct 6x6/18-pair board. The
  Atari-style games' 3-way Easy/Normal/Hard chip row (Snake, Road Rush, Breakout,
  etc.) was left as-is: already a single compact row, not the "4 radio buttons" shape
  the feedback called out.
- [ ] **F6 - General "skeleton" feel / movement polish.** Several games read as
  mechanically there but visually and tactilely unfinished - movement in particular
  feels slow/stiff on some (named: Knight Survivor). No single fix; track this as an
  umbrella item and attack it game-by-game (juice: hit-stop, screen shake already
  exists per the theme rules - make sure every game actually uses it, tween movement,
  add particle/number feedback).

## Dreadnought

- [x] **F7 - Refit screen card sizing + text.** Done: cards are now equal height
  (`Row(IntrinsicSize.Min)` + `fillMaxHeight`), each refit has an icon (`Refit.icon()`
  in `NavyWidgets.kt`), the blurb is capped at 3 lines with an ellipsis, the repeated
  "Lasts until this sortie ends" note now appears once below the row instead of on
  every card, and the per-stat text was trimmed from full sentences ("REPAIR heals 28%
  every 31 s") to short numeric values ("28% / 31s") shown as `Now -> Next` with an
  arrow icon and the next value in accent/bold. Verified the stat-formatting change live
  via the Harbor upgrade shop (same `Refit.stat` code path); didn't reach the in-run
  refit-overlay screen itself live (needs a full wave sunk) so its card-layout/icon
  changes are compile-verified but not eyeballed in that exact screen.
- [ ] **F8 - Forced landscape.** Investigated and implemented, not yet verified live
  (no device connected this session). Dreadnought's simulation already used free
  `(x, y)` + `heading` (not a landscape-specific "forward = right" assumption) and the
  renderer already scales its camera from the real canvas size each frame, so this
  turned out feasible: manifest flipped to `portrait`
  (`games/dreadnought/src/main/AndroidManifest.xml`), the ship now starts facing up
  (`-90°`) instead of right, the Harbor screen's side-by-side ship/tabs layout became a
  stacked column (`HarborScreen.kt`), the refit-overlay cards use `weight(1f)` instead
  of a fixed `190.dp` so they no longer assume landscape width
  (`DreadnoughtScreen.kt`'s `RefitOverlay`), and the in-run action-button cluster
  (dive/launch/torp/repair/fire) was shrunk and made horizontally scrollable since in
  the worst case (a ship with several of those at once) the old sizes could overflow a
  portrait-width screen. All compiles clean; **needs an on-device pass** to confirm the
  HUD/Harbor/refit layouts actually read well in portrait, especially that
  worst-case action-button cluster.
- [x] **F8b - Train Survivor is also landscape-only.** Investigated and decided: stays
  landscape. Per its own architecture (see AGENTS.md's Train Survivor section), the
  engine is built around a horizontal train with cars laid out left-to-right
  (`carLeft`/`gunX`/`locoLeft` all derive from `TrainShape`) - this is the core data
  model, not a camera-orientation setting like Dreadnought's, so going portrait would
  mean redesigning the train-shape system itself. Not attempted.

## Knight Survivor

- [x] **F9 - Level-up screen.** Done: cards are now equal height (same
  `Row(IntrinsicSize.Min)` + `fillMaxHeight` pattern as F7) and each shows an icon -
  extended the game's existing `VectorIcon` system to `PassiveKind` (it already
  covered `WeaponKind`) so every upgrade type gets one. Title/description are clamped
  to fixed line counts so uneven text doesn't vary card height. `UpgradeDisplay.icon`
  is nullable since the co-op DTO doesn't send it over the wire - a guest's view of the
  partner's choices just shows no icon. Verified live - played through two level-ups
  on-device, confirmed rendering and that picking an upgrade still works.
- [ ] **F10 - Polish pass inspired by a genre reference.** Tester attached a video of a
  Vampire-Survivors-style game (`feedback/1.mp4`, shared alongside the level-up and
  overhead-map screenshots) as the target level of detail/juice: large legible enemy
  hordes, visible on-screen horde shrinkage as you kill, fire/explosion VFX on kills.
  Compare against Knight Survivor's current overhead view (small uniform dots, no
  attrition feedback) and movement speed, and scope a concrete first pass (bigger/more
  readable enemy sprites, kill VFX, speed tuning) rather than trying to match it in one
  go.

## Number Corridor

- [ ] **F11 - Lane-locked movement feels restrictive.** Tester forwarded a video of a
  well-known 3-lane "crowd runner"-style mobile game with colored +1/-1 gates as a
  comparison point, saying that style of game is "missing the satisfaction factor"
  because the player is locked to 3 lanes (vs. free movement) and the horde is only
  shown as a number rather than visibly shrinking - the same shape of feedback applies
  to Number Corridor's own +/-/x/÷ buff-wall gates and squad/lane movement. Consider:
  more than 3 lanes or freer horizontal movement, and a visible squad-size
  representation instead of (or in addition to) a number counter.
  *(Best-guess mapping from the tester's screenshot - re-confirm which game they meant
  before investing real work here.)*

## Road Rush

- [x] **F12 - Lane-locked movement feels restrictive.** Done by the other session
  (commit "Road Rush: free continuous steering instead of 3 fixed lanes (F12)",
  merged via "Merge GameHub2"): Road Rush now uses free continuous horizontal steering
  instead of 3 fixed lanes. Verified live on-device - held the steering input and
  confirmed the car moves continuously rather than snapping between lane positions.

## Battleship (Fleet Duel)

- [x] **F13 - Mode-select screen UI.** Done: "Local 2 players" / "Vs computer" are now
  big icon-led cards (group/robot icon, title, short body) instead of plain text
  buttons. Verified interactively on-device.
- [x] **F14 - Battle screen is scrollable; ship names are text.** Done: the battle
  screen (own fleet + enemy waters) no longer stacks both boards - a "Your fleet" /
  "Enemy waters" tab shows one at a time, defaulting to whichever is actionable, so it
  fits without scrolling. Fleet-status chips now show a boat icon + length pips instead
  of text like "Battleship 4". Verified interactively on-device (placed a fleet, fought
  a turn, switched tabs, confirmed firing still works).

## Closing notes from the tester (context, not individual tasks)

- Feels like there are two tiers of game in the hub: simple ones that already feel
  complete (2048/Number Cruncher, Snake, Brick Blitz) and more ambitious ones (anything
  with real art/animation needs) that read as unfinished and might need real
  asset/animation work rather than incremental polish.
- Overall framed as encouraging - "a lot of work was put into them... I think it could
  take off" - the ask is to close the polish gap, not a rewrite.
