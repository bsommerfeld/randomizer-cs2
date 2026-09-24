# Changelog

## 2.0.0 (in progress)

Complete rewrite from scratch. Runs on Windows with Java 25 and JavaFX 27 and reads the game state through CS2-GSI 1.3.0 (`de.witzurke:cs2gsi` from Maven Central).

### Randomizer

- The "Randomizer" tab waits a random number of whole seconds between a min and a max, then plays one action and starts over. Min and max go from 1 to 600 s, min always stays below max, the default is 5 to 30 s.
- The draw is even over the enabled actions that can play right now. An action with several variations then draws one of them, again evenly. An action can play when at least one of its variations has a sendable key for every step.
- Each action has a check box. Changes to the wait and the check boxes apply to a running randomizer at once and are stored in `app.properties` (`randomizer.interval.min`, `randomizer.interval.max`, `randomizer.actions.disabled`).
- Each action shows the key it presses as a key cap behind its name. With several keys on one command it is the first sendable one in file order. An action with no key shows "not bound", one whose keys cannot be sent shows "Key not supported" with those keys. Both are greyed out.
- Sendable keys: A to Z, 0 to 9, F1 to F24, MOUSE1 to MOUSE5, Space, Tab and the left Ctrl, Shift and Alt. Keyboard keys go out as scancodes through `SendInput`, mouse buttons as mouse events. The mouse wheel, the arrow keys, the numpad, Enter, the right-hand modifiers and all other keys cannot be sent.
- Keybinds are read from both files on every start, defaults first and custom keybinds on top. A later bind of the same key replaces the earlier one, and `<unbound>` removes it. A key bound to several commands at once (`"+jump; +duck"`) counts for none of them.
- The randomizer fires only while all of these hold:
  - GSI runs and CS2 has sent a game state.
  - The last game state is at most 25 s old.
  - You are playing, not in the menu, the chat or the console.
  - The player in the game state is you, not someone you spectate.
  - Your health is above 0.
  - The round is not in freezetime, a pause or a timeout.
  - CS2 is the foreground window.
- The conditions are checked before every key down and every click. An action that comes due while one fails is dropped. A running action stops at its next step.
- The status line names the first condition that fails, for example "Waiting: you are dead.", and says "Running." otherwise. It shows no countdown to the next action.
- Start reads the keybinds and starts GSI. Without a keybind file, or when GSI does not start, the status line says so and the randomizer stays off.
- While it runs the randomizer reads your keyboard and mouse through Windows Raw Input. The reading is passive, with no hook, nothing blocked and no admin rights. Only input while CS2 is in front counts, and the randomizer's own `SendInput` presses never count as yours. Reading stops with the randomizer.
- When you let go of a key an action holds, the randomizer sends that key up and down again at once and the action goes on. When an action ends while you hold its key, the randomizer leaves out its key up and your hold goes on.
- Stopping the randomizer or closing the app releases a key it holds.
- The log has one line per key press: time, action, key and the milliseconds from the first key down to the last key up. Newest on top, at most 1000 lines.

### Actions

A tap is one key down for 30 to 60 ms. A hold is one key down for a random time from its range. Clicks are taps, repeated for a random time from their range with a random pause between two, and at least one. Every action needs a sendable key on its command, and every step of a variation needs one on its own command.

**Shoot** (`+attack`). Always in the draw. The press depends on the fire mode of the weapon in hand:

- Automatic, hold 200 to 1500 ms: every SMG and assault rifle, M249, Negev, CZ75-Auto, XM1014, SCAR-20, G3SG1 and the knife.
- Semi-automatic, clicks for 200 to 1500 ms, 60 to 140 ms apart: Glock-18, P2000, USP-S, P250, Dual Berettas, Five-SeveN, Tec-9, Desert Eagle, Nova, MAG-7, Sawed-Off and Zeus x27.
- Bolt action, clicks for 2 to 4.5 s, 1.5 to 1.8 s apart, two or three shots: AWP and SSG 08.
- Revolver, hold 500 to 1500 ms: R8 Revolver.
- Grenades, C4 and weapons CS2-GSI does not list, hold 200 to 1500 ms.

**Secondary fire** (`+attack2`). Only in the draw with a weapon in hand that does something on it: knife, Glock-18, USP-S, R8 Revolver, FAMAS, M4A1-S, SG 553, AUG, SSG 08, AWP, SCAR-20, G3SG1 and every grenade.

- R8 Revolver, clicks for 400 to 1500 ms, 60 to 140 ms apart.
- Every other weapon, hold 100 to 800 ms.

**Reload** (`+reload`). Depends on the magazines:

- Magazine in hand not full and reserve ammo left: `+reload` tapped. This is then the only variation.
- Otherwise one of these, drawn evenly among the ones that apply:
  - Magazine in hand full and reserve left: one shot with `+attack`, then `+reload` held 1500 to 2000 ms. The shot is a tap, with the R8 Revolver a hold of 500 to 700 ms.
  - One variation per carried weapon whose magazine is not full and that has reserve left: its slot key (`slot1` or `slot2`) tapped, then `+reload` held 1500 to 2000 ms.
- Out of the draw when none applies, for example with the knife in hand and only full magazines, or with no reserve ammo.

**Drop weapon** (`drop`). Variations:

- The weapon in hand: `drop` tapped. Not with the knife in hand.
- Every selection of the carried weapons, from one of them to all of them. For each selected weapon in slot order the randomizer draws it with its slot key, held 100 to 200 ms, then taps `drop`. The weapon in hand counts among the carried ones.
- All variations are equally likely. With a rifle in hand, a pistol and two grenades there are 16 variations, and one of them drops the rifle alone.
- Slot order and keys: primary `slot1`, pistol `slot2`, Zeus x27 `slot11`, HE grenade `slot6`, flashbang `slot7`, smoke `slot8`, decoy `slot9`, molotov or incendiary `slot10`, C4 `slot5`. A grenade whose own key is unbound is drawn with `slot4`. `slot11` has no key by default, so a holstered Zeus is only dropped once you bind one.
- The knife is never dropped. A weapon CS2-GSI does not list is only dropped while in hand.
- Out of the draw when you carry nothing but the knife.

The other actions have one variation each:

| Action | Command | In the draw | Press |
|---|---|---|---|
| Jump | `+jump` | always | clicks for 0.5 to 3 s, 750 to 900 ms apart, one to four jumps |
| Crouch | `+duck` | always | hold 300 to 2000 ms |
| Forward | `+forward` | always | hold 500 to 3000 ms |
| Back | `+back` | always | hold 500 to 3000 ms |
| Left | `+left` | always | hold 500 to 3000 ms |
| Right | `+right` | always | hold 500 to 3000 ms |
| Walk | `+sprint` | always | hold 500 to 3000 ms |
| Slot 1 | `slot1` | with a primary weapon | tap |
| Slot 2 | `slot2` | with a pistol | tap |
| Slot 3 | `slot3` | with the knife or Zeus x27 | tap |
| Slot 4 | `slot4` | with a grenade | tap |
| Last weapon | `lastinv` | always | tap |

### Configs

- `user_keys_default.vcfg` and `cs2_user_keys.vcfg` are found on startup. The app reads the Steam folder from the Windows registry and every library from `steamapps/libraryfolders.vdf`, picks the library that holds CS2 (app 730) by its app manifest, and looks for the custom keybinds under `userdata/<account>/730/remote/`. With several Steam accounts the most recently changed file wins.
- The "Configs" tab has three sub tabs. "Default keybinds" and "Custom keybinds" show one file each as pretty-printed JSON, with a path field, "Browse…", "Load" and "Reload". "Reload" runs the detection again. A loaded path is remembered in `app.properties` and used on the next start while the file exists.
- "Merged" shows the defaults with the custom keybinds on top. For the same key the custom entry wins. With one file missing the other one stands alone and the status line says so. The view is rebuilt each time the tab opens.
- The app never writes these files.
- `app.properties` is read anew on every access, so a hand edit applies without a restart. A file that cannot be read is not overwritten. A wait time outside 1 to 600 s is clamped, one that is no number falls back to the default.

### Live (GSI)

- The "Live (GSI)" tab starts and stops a local GSI listener on port 4000. It only starts while CS2 runs, and says so when the port is taken.
- Starting it writes `gamestate_integration_randomizer.cfg` into the CS2 cfg folder, with a 10 s heartbeat. A file with the right content is left alone. A new or changed file brings up a dialog that asks for one CS2 restart.
- The tab shows the current game state as JSON and a log of the events CS2 sends, newest at the bottom, at most 1000. The log follows new events while its last row is in view. A selected event shows its details below the log.

### Overview

- Map, mode, CT and T score, and the bomb status with CS2's bomb countdown when it sends one.
- A round countdown starts when a round goes live, with the mode's default time: 1:55 in Competitive, 1:30 in Wingman, 2:15 in Casual. A bomb plant switches it to a 0:40 bomb countdown, and it stops when the round ends. Deathmatch and the other modes without a fixed round time show no timer. Casual hostage maps and servers with their own round time show the default.
- As a spectator, in GOTV or in a demo there is a scoreboard per team and a detail card for the selected player. The selection survives game state updates.
- In a normal match there is your own card, hidden until CS2 has sent a player.
- Weapons show their slot, the name the game uses ("AK-47", "USP-S"), magazine and reserve, and whether they are in hand, holstered or reloading.

### App

- A banner above the tabs says when CS2 is not running. The app checks the process list for `cs2.exe` every 3 s.
- Dark theme. Saturated color only marks game state: blue for CT, amber for T, red for the bomb and a missing CS2, green for health.
- Own title bar through JavaFX `HeaderBar`, 31 px high and one shade darker than the background. The title dims when the window loses focus. Minimize, maximize and close are the Windows buttons.
- The main buttons of the Randomizer and GSI tabs are light. Split views cannot be dragged shut. The randomizer settings keep their width when the window grows, and the log takes the extra space.
- Every text in the UI is in English.
