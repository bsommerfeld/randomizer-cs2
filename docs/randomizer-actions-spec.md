# Randomizer actions

While CS2 runs, the randomizer waits a random time and then presses the key that the user's keybinds put on a random action: shoot, reload, drop, jump, crouch, move, switch weapons. One action needs no key and moves the mouse instead. The tab "Randomizer" starts and stops it and logs every key it pressed and every mouse move.

This file describes the code as of 2026-09-26 and replaces the decision log from 2026-09-20. `mvn verify` passes with 150 tests. In a live match only the shoot action has been tried so far. MOUSE1 fired and GSI recognized the AWP. The hold ranges, the other mouse buttons and the reload and drop routes are untested in play.

## The loop

`ActionRunner` runs this on one virtual thread from `start` to `stop`:

```
until stopped:
    sleep a whole number of seconds, uniform between min and max
    if the gate is closed or CS2 is not in front: skip, draw the next wait
    candidates = enabled actions with at least one route whose every key step has a sendable key
    if there is none: skip
    draw an action uniformly from the candidates
    draw one of its routes uniformly
    press the route's steps in order, one log line per step,
        and end the action once stop, the gate or the foreground says no before a key down
```

- The first action comes after one full wait, not at start.
- The runner drops an action that comes due while the gate is closed. It does not queue it. A queue would fire everything the moment freezetime ends, which is predictable. Dropped actions leave no log line.
- The draw is over actions. An action with three routes is no likelier than one with a single route.
- Min, max and the enabled actions come in through `configure(Settings)` as one record. The runner reads them again for every wait and every draw, so changes in the tab apply while it runs.
- Steps run one after another. A step's key is up before the next step's key goes down, unless the user holds that key themselves (see "The user's own keys").
- Before every step and every further click the runner asks again whether it was stopped, whether the gate is open and whether CS2 is in front. A burst or a route takes seconds, and the player can die, open the chat or switch windows in between. A hold that already runs is not cut short, the action just presses nothing more.
- A cycle that throws goes to stderr, and the next cycle runs.

## When an action may fire

`FireGate` decides from the game state that GSI reports and from the window in front. It checks in this order and stops at the first reason. The text is what the status line of the tab shows.

| Check | Status line |
|---|---|
| GSI is not running | Wartet auf GSI. Es wurde gestoppt, starte es im Tab "Live (GSI)" wieder. |
| No game state yet, `player.activity` is `Undefined` | Wartet auf Daten von CS2. |
| No payload for more than 25 s | Wartet: CS2 sendet seit über 25 s nichts mehr. |
| `player.activity` is not `Playing` | Wartet: du bist im Menü, im Chat oder in der Konsole. |
| `GameState.isLocalPlayer()` is false | Wartet: du schaust einem anderen Spieler zu. |
| `player.state.health` is 0 or less | Wartet: du bist tot. |
| `round.phase` or `phaseCountdowns.phase` is freezetime, paused or a timeout | Wartet: Freezetime, Pause oder Timeout. |
| CS2 is not the foreground window | Wartet: CS2 ist nicht im Vordergrund. |

Warmup, a live round, a planted bomb and the seconds after a round ends all leave the gate open. The gate reads both phase nodes because pauses and timeouts show up in `phaseCountdowns` and freezetime shows up in both.

The silence check guards against a game that stops sending mid-match, for example when the config file is gone, the port is blocked or a CS2 update broke GSI. Without it the gate would stay open on the last "playing" state. `GsiService.silence()` is the time since the last payload, taken from the library's `getLastGameStateTime()`. A heartbeat that repeats the state counts as a payload. The generated config file asks CS2 for a heartbeat every 10 s, so 25 s means two lost heartbeats. `GsiService` hands the listener and `silence()` the same clock. If the gate ever closes with this text while CS2 runs normally, raise `FireGate.SILENT_AFTER`.

`isLocalPlayer()` comes from cs2gsi 1.2.0 and is false when either steamId is missing. CS2 fills the player node with whoever is on screen, so without this check the randomizer would fire while the user spectates.

`FireGate.closedBecause(boolean, GameState, Duration)` is the pure function behind the GSI checks. It reads no clock, and the tests call it directly. The foreground check comes last, from a `BooleanSupplier` that `RandomizerApp` wires to `JnaGameInput.isCs2Foreground`. The runner also asks `GameInput.isCs2Foreground()` itself right before it sends, so input never depends on the wiring alone.

The foreground check goes by the process behind the foreground window, not by the window title, which a browser tab could match. `JnaGameInput` remembers the answer per window handle. The raw input watcher asks on every key event, and `ProcessHandle.info()` opens the process and resolves its user on every call. The app never moves the focus. `SendInput` lands in whatever window is in front.

## Actions

`ActionCatalog.ALL`, in the order the tab lists them. Times are in milliseconds.

| Tab name | Command | How the key goes down |
|---|---|---|
| Schießen | `+attack` | by the weapon in hand, see "Weapon rules" |
| Zweitfeuer | `+attack2` | held 100 to 800, clicked for 400 to 1500 with the R8 Revolver, only with a weapon that reacts to it |
| Nachladen | `+reload` | routes, see "Reloading" |
| Waffe droppen | `drop` | routes, see "Dropping" |
| Springen | `+jump` | paced, window 500 to 3000, 750 to 900 between clicks |
| Ducken | `+duck` | held 300 to 2000 |
| Vorwärts, Rückwärts, Links, Rechts | `+forward`, `+back`, `+left`, `+right` | held 500 to 3000 |
| Schleichen | `+sprint` | held 500 to 3000 |
| Slot 1 to Slot 4 | `slot1` to `slot4` | tap, only while the player carries a weapon in that slot |
| Letzte Waffe | `lastinv` | tap |
| Mouse move | none | a turn, see "Mouse move" |

The ranges are first guesses. Tune them after playing.

### Actions, routes and steps

`Action` is `record Action(String name, String command, Function<Player, List<List<Step>>> routes)`. `name` is the German text for the tab and the log. `command` is the CS2 command whose key the tab shows. `routes` takes the GSI `Player` at the moment the action comes due and returns every way to play it right now. A route is a list of `Step`s pressed in order. An action without a route makes no sense at that moment and stays out of the draw.

`Step` is a sealed interface with two cases. `Step.OnKey(List<String> commands, Press press)` is a key press. All commands of such a step do the same job, the best one first. The runner presses the key of the first command that has a sendable key. A route counts only when every key step has one. `Step.Turn` moves the mouse and needs no key, see "Mouse move".

Most actions have one route of one step. These constructors build them:

- `new Action(name, command, minHold, maxHold)` holds the key for a random time in that range.
- `Action.tap(name, command)` taps it.
- `Action.byWeapon(name, command, Function<Weapon, Press>)` picks the press by the weapon in hand.
- `Action.byWeapon(name, command, Predicate<Weapon>, Function<Weapon, Press>)` does the same and has no route while the predicate rejects the weapon in hand.
- `Action.slot(n)` taps `slotN` and has no route while no carried weapon has `WeaponInfo.slot == n`. A weapon the library does not list has slot 0 and counts as none.
- A package-private constructor takes a fixed `Press`. Jumping uses it.

### Presses

`Press` is `record Press(boolean clicks, int minMillis, int maxMillis, int minPauseMillis, int maxPauseMillis)`.

A held press puts the key down once for a random time from min to max. A clicking press uses that time as a window. It clicks once, then clicks again as long as the next click still starts inside the window. Before each further click it waits a random pause from the pause range. Every click is down for 30 to 60 ms. Every random draw is uniform and includes both bounds.

| Factory | What it gives |
|---|---|
| `Press.held(min, max)` | one hold |
| `Press.tap()` | a hold of 30 to 60 ms, long enough for CS2's input polling |
| `Press.spammed(min, max)` | clicks with 60 to 140 ms pauses, five to eleven a second, faster than any pistol fires. CS2 drops the clicks that come too early. |
| `Press.paced(min, max, minPause, maxPause)` | clicks at the pace of what the key sets off |

Paced clicking is for keys where CS2 acts once per key down and a spam would mean some twenty clicks per shot or jump. A jump on flat ground lasts 0.76 s (`sv_jump_impulse` 302 against `sv_gravity` 800), and a click that comes while still in the air is lost. With 750 to 900 ms pauses inside 500 to 3000 ms that gives one to four jumps. After a jump off a ledge a click can come too early, and that jump is missing.

### Weapon rules

`FireModes` turns the library's `WeaponInfo.fireMode` into a press for `+attack`. It keeps no weapon lists of its own. The one weapon it names is the R8 Revolver, for secondary fire.

| Fire mode | `+attack` |
|---|---|
| `Automatic` | held 200 to 1500 |
| `SemiAutomatic` | spammed 200 to 1500 |
| `BoltAction` | paced, window 2000 to 4500, 1500 to 1800 between clicks |
| `Revolver` | held 500 to 1500, the hammer has to come back before it fires |
| `Undefined` | held 200 to 1500 |

CS2 fires most pistols and shotguns once per click, so a held `+attack` would give one shot. The AWP needs about 1.46 s between two shots and the SSG 08 1.25 s, so the bolt-action window fits two or three shots. The library lists CZ75-Auto, XM1014, SCAR-20 and G3SG1 as `Automatic`, so `FireModes` holds them for the spray. `Undefined` covers grenades, the C4 and a weapon Valve added after the library's list. A new pistol fires once per action until a library release lists it.

Secondary fire only comes up while the weapon in hand has `WeaponInfo.hasSecondaryFire` (library 1.3.0). That is the knife, Glock-18, FAMAS, USP-S, M4A1-S, R8 Revolver, the scoped rifles and all grenades. `FireModes` holds `+attack2` for 100 to 800 ms with each of them except the R8 Revolver. The revolver gets it spammed for 400 to 1500 ms to fan the hammer.

### Reloading

CS2 ignores `+reload` with a full magazine and with a weapon that has none. `Reloading.routes(Player)` returns only routes that end in a real reload. A weapon can reload when `ammoClipMax > 0`, `ammoClip < ammoClipMax` and `ammoReserve > 0`.

- If the weapon in hand can reload, the only route is `+reload` tapped.
- Otherwise there can be several routes:
  - If the weapon in hand has a full magazine and reserve ammo, one shot, then `+reload`. The shot is `+attack` tapped, or held 500 to 700 ms with the revolver.
  - For every carried weapon that can reload and that a slot command reaches, its slot key tapped, then `+reload`.

After a shot or a switch the weapon takes no reload until its cycle or draw time is over, 1.46 s with the AWP. So after a lead-in the runner holds `+reload` for 1500 to 2000 ms, and CS2 starts the reload the moment it can. Knife, grenades and C4 report no magazine, and the Zeus has no reserve, so none of them is ever shot for this. With a knife in hand only the switch routes are left.

### Dropping

CS2 drops the weapon in hand, so dropping another weapon means drawing it first. `Dropping.routes(Player)` returns:

- `drop` tapped, if the weapon in hand can be dropped
- one route for every non-empty selection of the carried weapons that can be dropped and that a slot command reaches

In a selection route each weapon adds its slot key, held 100 to 200 ms, then `drop` tapped. The route walks through the weapons in slot order: primary, pistol, Zeus, the grenades by their own slot numbers, then the bomb.

The knife never drops, CS2 keeps it in the hand whatever the player presses. An entry with an empty name is no weapon and gets skipped. The runner holds the slot key longer than a tap. A drop that reaches the server before the switch would throw the old weapon away. Each grenade gets its own slot key and its own `drop`. When two grenades share a key, the game decides which one comes out. The number of selections doubles with every weapon. A match hands out seven at most, and `Dropping` cuts the list at ten, because a mod with thirty weapons would stall the draw.

The runner draws one route uniformly. With a primary and a pistol the weapon in hand alone is one route of four, and with a full loadout it is rare. That is intended. Losing only the weapon in hand is the lucky case.

### Slots

`Slots.commandsFor(WeaponInfo)` gives the slot commands that draw a weapon, built from the library's `directSlot` and `slot`. `directSlot` draws exactly that weapon: 6 HE, 7 flashbang, 8 smoke, 9 decoy, 10 molotov or incendiary, 11 Zeus. `slot` is its group: 1 primary, 2 pistol, 3 knife and Zeus, 4 grenades, 5 bomb.

The direct command comes first and the group command second, for a player who took the direct one off its key. Slot 3 is never used, because a press there can land on the knife. So the Zeus goes by `slot11` alone, which CS2 binds to no key by default. A weapon the library does not list has no slot command and can only be reloaded or dropped from the hand. Nobody has checked the slot numbers against Valve's code, only against community sources.

### Mouse move

`Step.Turn(maxX, maxY, minMillis, maxMillis)` moves the mouse by `dx` from -maxX to maxX and `dy` from -maxY to maxY counts, both uniform. The runner spreads the move over a random time from the range, one `GameInput.move` every 10 ms. The path is meant to look like a hand, not a ruler:

- `HandPath.at(t, dx, dy, bend)` gives the point after the share `t` of the time. The timing is minimum jerk (Flash and Hogan, 1985), the model for a person reaching for a target: slow at the start, fastest at half time, slow at the end.
- `bend` bows the path to the side, by that share of its length at the halfway point. The runner draws it uniformly from -0.15 to 0.15. Start and end have no bow.
- The runner puts every point up to 3 counts off the path in both directions, a tremor. The last point has none.

Each slice moves to its point on the path, not by a step of its own, so rounding and tremor never add up and the counts sum exactly to `dx`, `dy`. The runner asks `mayPress` before every slice, the same as before every click. The catalog uses 3000 counts sideways, 600 up or down and 150 to 600 ms.

A count turns the view by `sensitivity * 0.022` degrees, so 3000 counts are 66 degrees at sensitivity 1 and 165 at 2.5. The counts are fixed and the turn grows with the player's sensitivity. Reading `sensitivity` from the config would allow a range in degrees.

The action's command is `Action.MOUSE_MOVE`, `mouse_move`. CS2 has no such command. The tab and the settings file only need a name for the action. The user's own mouse moves add to the turn in the game, and a move has no state that could clash the way two key downs do.

The randomizer before the rewrite had the same action with `java.awt.Robot.mouseMove(x, y)`. That sets an absolute position and stops at the screen edge. CS2 reads the mouse as raw deltas, so the rewrite sends relative moves.

## Keys

### Where the keys come from

`BoundKeys.of(List<VdfObject>)` reads the keybind files the way CS2 does. `user_keys_default.vcfg` comes first and `cs2_user_keys.vcfg` on top of it, and a later bind of the same key replaces the earlier one. That is also how `"<unbound>"` in the custom file takes a default away. `BoundKeys` reads the custom file with or without its outer `"config"` node. Only a bind of exactly the command counts, so a key bound to `"+jump; +duck"` is a key for neither.

`MainController.readBoundKeys` loads both files again on every randomizer start, the remembered path first and auto-detection otherwise. So a bind changed in the game after the app started still counts. The tab shows the keys at app start and refreshes them on every randomizer start. Without any keybind file the randomizer refuses to start.

A command can sit on several keys. In one real config `+jump` is on `SPACE`, `MWHEELUP` and `MWHEELDOWN`. `pressableKeyFor` takes the first key in file order that the key table can send.

### The key table

`Keys.key(String)` maps a CS2 key name, case ignored, to a `Key(boolean mouse, int code)`:

- `a` to `z`, `0` to `9`, `F1` to `F24`, `SPACE`, `TAB` and the left-hand `CTRL`, `SHIFT` and `ALT` as PS/2 set 1 scancodes
- `MOUSE1` to `MOUSE5` as button numbers

Every other name gives empty, the mouse wheel among them. Arrow keys, the numpad and the right-hand modifiers are not in the table. Arrow keys and the right-hand modifiers would also need `KEYEVENTF_EXTENDEDKEY`, which nothing sends yet. Letter scancodes address the physical US-QWERTY position. On a German QWERTZ keyboard only y and z sit elsewhere.

### Sending

`JnaGameInput` sends through `SendInput`. Keyboard keys go out with `KEYEVENTF_SCANCODE` and no virtual key, because CS2 ignores input that only carries a virtual key. Mouse buttons go out as `MOUSEINPUT`, with MOUSE4 and MOUSE5 through the X button flags. A mouse move goes out as `MOUSEINPUT` with `MOUSEEVENTF_MOVE`, relative counts. Windows' pointer speed and acceleration apply to the cursor only, raw input carries the counts as sent. Raw input marks the move with no device handle, so `RawInputWatcher` never takes it for the user's. `SendInput` produces a plain OS input event. The app does not touch the game process or its memory.

`GameInput.press` returns false when `SendInput` reports that Windows did not insert the event. Windows does that for a program with higher rights, for example CS2 started as admin while the app is not. The runner then ends the action without a log line and writes the reason to stderr.

## The user's own keys

CS2 reads mouse and keyboard through SDL3. Its `game/bin/win64/inputsystem.dll` refers to `SDL_EVENT_MOUSE_BUTTON_DOWN` and `SDL_EVENT_KEY_DOWN`. SDL keeps the button state of every input source apart. `SendInput` is one source, and the user's mouse and keyboard are others. Two facts follow:

- A key down from a source that already has that key down changes nothing, and SDL drops it.
- A key up from the user ends the action for the game, even while the randomizer's source still has the key down.

The runner handles both directions:

- Every hold starts with a key up and then the key down (`ActionRunner.pressAnew`). The key up makes the key down count even when an earlier hold left its key up out.
- When the user lets go of the key an action is holding, the key goes up and down again at once (`pressAgainAfterUser`). So the user's key up does not end the action. The watcher calls this on its own thread, and `keyLock` keeps it apart from the start and the end of the hold.
- When a hold ends while the user holds the same key, the randomizer leaves its key up out. Otherwise the game would stop the user's own shooting or walking. The user's own key up ends it later. The same rule applies on stop.

The runner never sends a blind key up and down in a fixed rhythm. That would reset the revolver's hammer.

To know which keys the user holds, `RawInputWatcher` reads Windows Raw Input with `RIDEV_INPUTSINK`, through a message-only window on its own platform thread. It is passive. Nothing is blocked, nothing sits between the device and the game, and it needs no admin rights. Raw input marks events from real devices with a device handle and events from `SendInput` with none, so the randomizer's own presses never count as the user's.

`PhysicalKeys` keeps the state the watcher feeds in:

- Only events while CS2 is in front count. Any event while another window is in front clears the held keys, because key ups out there never count.
- It reports every key up, also for a key that went down before the watching began, because the game saw that key up too.
- It drops keyboard events with the E0 prefix. Right Ctrl, right Alt and the arrows share their make code with a key the table sends, left Ctrl for example, but they are other keys. Taken for left Ctrl, a held right Ctrl would make the runner leave its key up out, and left Ctrl would stay down.

The watcher runs only between `ActionRunner.start` and `stop`. `stop` ends it after the runner's thread, because the last key up of a hold still asks whether the user holds the key. If the watcher cannot start, for example off Windows or when the registration fails, it prints one line to stderr and reports no key as held. The runner then releases every key.

Blocking the user's input is off the table. `BlockInput` locks everything and needs admin rights. A low-level hook sits in the path of every mouse event and probably does not stop CS2's raw mouse input anyway.

## The tab

"Randomizer" is the first tab of the main window. `RandomizerTabController` drives it.

The top row has the start/stop button and the status line. While the randomizer runs, a 100 ms poll writes the gate's reason or "Läuft." into the status line. There is no countdown to the next action. It would give the surprise away.

The left side has two sections:

- Two spinners set the wait in seconds, min from 1 to 599 and max from 2 to 600, with 5 and 30 as defaults. They push each other so min stays below max. They are not editable, because a JavaFX spinner throws on commit when the typed text is no number. Holding the arrow is quick enough.
- Every action has a checkbox. Its label shows the key, "(nicht gebunden)" or "(Taste nicht unterstützt: <keys>)". The tab greys out an action whose own command has no sendable key. The mouse move shows no key and is never greyed out. The keys of the other steps in a route, such as a slot key or `+attack` before a reload, are checked only when the action comes due.

The right side is the log, in a monospaced font. A line reads `21:14:03  Schießen       MOUSE1  840 ms`, with time, action, key and the time from first key down to last key up. There is no click count. A mouse move logs the counts it sent in the key column, `+812,-140`, and the time it took. Every step of a route gets its own line under the action's name. The newest line is on top. The log keeps 1000 lines in memory and nothing on disk, so it survives stop and start and is empty after an app restart.

The settings live in `%LOCALAPPDATA%\randomizer-cs2\app.properties`:

| Key | Value |
|---|---|
| `randomizer.interval.min` | seconds |
| `randomizer.interval.max` | seconds |
| `randomizer.actions.disabled` | the commands of the unchecked actions, comma separated. A new action is on until someone turns it off. |

The tab clamps a stored value outside the spinner range and uses the default for one that is no number. Every change reaches the running randomizer at once, and the tab writes it to the file right away. A failed write shows "Einstellung nicht gespeichert: <reason>" in the status line.

Starting the randomizer takes three steps:

1. Read the keybinds. Without any the status line says "Keine Keybind-Config gefunden - bitte im Tab "Configs" laden." and nothing starts.
2. Start GSI unless it runs, through `GsiTabController.ensureStarted`. That is the GSI tab's own start flow: CS2 check, listener on port 4000, `gamestate_integration_randomizer.cfg` in the CS2 cfg folder, and a restart dialog when that file was created or changed. If GSI does not run afterwards, the status line says "GSI ließ sich nicht starten - der Tab "Live (GSI)" nennt den Grund." and the GSI tab names the reason.
3. Start the runner, which starts the watcher.

Stopping the randomizer leaves GSI on. Stopping GSI while the randomizer runs closes the gate, and the status line says so. On app exit `RandomizerApp.stop()` stops the runner before it closes GSI. `ActionRunner.stop()` interrupts the thread and waits up to one second for it, so a held key is up before the JVM exits.

## Code layout

Package `action` holds the logic, without JavaFX or JNA:

| Class | Job |
|---|---|
| `Action`, `Step`, `Press` | what an action presses, see "Actions" |
| `ActionCatalog` | the 17 actions |
| `FireModes`, `Reloading`, `Dropping`, `Slots` | pure functions of the GSI `Weapon` or `Player` |
| `HandPath` | the path of a mouse move, a pure function of time |
| `BoundKeys` | from command to keys, read from the keybind files |
| `FireGate` | the gate over `GsiService` and the window in front |
| `ActionRunner` | the loop, holds and clicks, the reaction to the user's key ups |
| `PlayedAction` | one log line |

Package `input` is the only place that sends or reads input:

| Class | Job |
|---|---|
| `Keys` | from CS2 key name to scancode or mouse button |
| `GameInput`, `JnaGameInput` | press, release, mouse move, and whether CS2 is in front |
| `UserKeys`, `RawInputWatcher`, `PhysicalKeys` | the user's own keys: the interface, the Raw Input reader and the state it feeds |

`GameInput` and `UserKeys` are interfaces because tests cannot call `SendInput` or read real devices. `ActionRunnerTest` puts fakes behind both.

`RandomizerApp` builds `JnaGameInput`, `RawInputWatcher`, `FireGate` and `ActionRunner`. `ControllerFactory` hands the runner and the gate to `RandomizerTabController`. `MainController` gives the tab `GsiTabController::ensureStarted` as a `BooleanSupplier` and its own `readBoundKeys`.

The runner takes a `RandomGenerator`, a sleep function and a `Clock` as constructor parameters, so tests run without waiting, with a fixed seed and a fixed time.

Threads:

- The runner runs on one virtual thread per start. `PlayedAction`s reach the tab on that thread, and the tab moves them to the JavaFX thread with `Platform.runLater`.
- The watcher runs on a platform daemon thread named `raw-input-watcher`, which sits in `GetMessage` for its whole life. It calls `pressAgainAfterUser` on that thread.
- `keyLock` in `ActionRunner` guards the held key and the key events around it. `UserKeys.holds` inside that lock is a lock-free read.

## Adding an action

An action of one key that presses the same way with every weapon is one line in `ActionCatalog`, with `new Action(name, command, min, max)` or `Action.tap`.

An action of one key whose press depends on the weapon uses `Action.byWeapon` with a function from `Weapon` to `Press`, and a predicate when some weapons ignore the key. That function belongs next to the rules in `FireModes`.

An action with a condition or a lead-in gets its own class with a pure `routes(Player)` function and a test, like `Reloading` and `Dropping`. The randomness stays in the runner, which draws the route.

A new key name goes into `Keys`. A key that needs the extended flag also needs `KEYEVENTF_EXTENDEDKEY` in `JnaGameInput`, and `PhysicalKeys` has to stop dropping its E0 events.

## Tests

JUnit 5. Only the main view smoke test starts JavaFX.

| Test | What it covers |
|---|---|
| `ActionRunnerTest` | wait, press and release; key down again after the user's key up; key up left out while the user holds the key; spammed and paced clicking; press by weapon; routes pressed in order with one log line per step; a step's first command with a key; an action without a route or key stays out of the draw; a closed gate or CS2 in the background drops the action; the rest of a route and of a burst is left out once the gate closes; a stop between two steps sends nothing more; a blocked key down leaves no log line; a failed cycle does not end the loop; disabled actions; a stop during a hold releases the key; equal bounds; stop and start again; a mouse move glides in 10 ms slices that add up to the logged counts and needs no key; a closed gate ends a mouse move |
| `FireGateTest` | every closed reason and the open case, from GSI JSON payloads |
| `HandPathTest` | slow start and end, fastest at half time, the bow and its side |
| `FireModesTest`, `ReloadingTest`, `DroppingTest` | the weapon rules and the routes, with knife, Zeus, grenades, bomb and weapons the library does not list |
| `BoundKeysTest` | first sendable key, later file wins, custom file without `"config"`, a command only on the wheel |
| `KeysTest` | letters, digits, function keys, named keys in any case, mouse buttons, unknown names |
| `PhysicalKeysTest` | foreground filter, mouse and keyboard down and up, key repeats, the key up of a key pressed before the watching began, right Ctrl is not left Ctrl |
| `RandomizerTabTest` | key labels, stored seconds from a hand-edited file |
| `OverviewViewTest.mainViewLoadsWithAllIncludedTabs` | the main view loads with the randomizer tab |

## Known gaps

- `ConfigRepository` drops a custom keybind file that does not parse without any message, and the randomizer presses the default keys. Worth fixing once it happens to someone, CS2 writes that file itself.
- The library keeps its last state and its time across a GSI stop and start. After a restart within 25 s the old state counts as fresh until 25 s after it came in. The fix belongs in CS2-GSI, a `start()` that forgets both.
- A failed cycle and a key down that Windows blocked only reach stderr. The tab keeps saying "Läuft.". If either shows up in play, the tab should show it.
- A key up that never arrives leaves that key marked as held until the user taps it again. That takes a switch to the lock screen or a UAC prompt with a key down.

## Not yet seen in a live match

- How far apart two payloads get, freezetime and a long death included. The silence check assumes at most 10 s. The event log of the GSI tab hides the heartbeat, but the game-state JSON there shows `provider.timestamp` changing with every payload.
- Whether pauses and timeouts reach a playing client in `round.phase` or in `phaseCountdowns.phase`. The gate reads both, so either works, but nobody has seen either one in a match yet.
- The mouse buttons other than MOUSE1, all hold ranges, the reload and drop routes, and the slot numbers from the library.
- The mouse move. Whether CS2 turns the view on `SendInput` moves at all, and whether 3000 counts are too much.

## Out of scope, with the way in

- **Actions triggered by GSI events**, such as a drop after a kill. A second trigger next to the timer. `GsiService` would need typed event subscriptions, because today it hands out formatted `GsiEvent`s only.
- **A global start and stop hotkey.** `RegisterHotKey` through JNA, calling the same `start()` and `stop()`.
- **Weights and a repeat guard for the draw.** The draw lives in `ActionRunner.candidates` and `pick`, nowhere else.
- **Ping, scoreboard, inspect and mouse wheel binds as actions.** The first three are one catalog line each. The wheel needs a scroll event in `GameInput` and names for `MWHEELUP` and `MWHEELDOWN` in `Keys`.
