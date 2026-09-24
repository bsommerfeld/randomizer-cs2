# <img src="https://github.com/user-attachments/assets/ab28eba7-4b88-47b4-be10-ac4487d66e23" alt="randomizer" width="24" height="24" style="vertical-align: middle;" />andomizer-CS2

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange.svg)](https://adoptium.net/)
[![GitHub Stars](https://img.shields.io/github/stars/bsommerfeld/randomizer-cs2?style=social)](https://github.com/bsommerfeld/randomizer-cs2/stargazers)

> ⚠️ **Rewrite in progress.** This branch is a complete rewrite from scratch (v2.0.0). The old codebase is gone.

**Randomizer-CS2** is a desktop app for *Counter-Strike 2*. While you play it presses one of your bound keys at a random moment, so you jump, shoot or drop your weapon without asking for it. It also shows your keybinds and a live view of the match.

## What it does

**Randomizer.** Waits a random time between a min and a max you set, 5 to 30 seconds by default. Then it draws one of the enabled actions and presses the key you have bound to it. There are 16 actions: shoot, secondary fire, reload, drop weapon, jump, crouch, forward, back, left, right, walk, slot 1 to 4 and last weapon. The key comes from your keybind files, so it follows your binds.

The actions look at the game state before they play. Shooting depends on the weapon in hand. A rifle gets a held key, a pistol or a shotgun a burst of clicks, an AWP or an SSG 08 one click per bolt cycle. Reload switches to a weapon whose magazine is not full, or fires one shot first when the magazine in hand is full. Drop throws away the weapon in hand, or walks through the slots and drops several weapons. An action that would do nothing right now, like a reload with nothing to reload, stays out of the draw. [CHANGELOG.md](CHANGELOG.md) lists every action with its variations and timings.

Each action has a check box and shows its key behind its name. An action whose command sits on no key, or only on keys the app cannot send like the mouse wheel or the arrow keys, is greyed out. A log lists every key press with time, action, key and duration.

The randomizer fires only while CS2 is the foreground window and GSI reports that you are alive, in control of your own player and not in the menu, the chat, freezetime, a pause or a timeout. The status line says what it is waiting for. Starting it starts GSI too.

While the randomizer runs it reads which keys and mouse buttons you hold, through Windows Raw Input. When you let go of a key an action holds, it presses that key again. When an action ends on a key you hold, it leaves that key down. It keeps only which keys are down right now, stores nothing and stops reading when you stop the randomizer.

The key presses are simulated with `SendInput`, which Windows marks as injected input. The app does not touch the game process or its memory. Whether a given anti-cheat accepts injected input is for you to check before you play there.

**Configs.** The app finds `user_keys_default.vcfg` and `cs2_user_keys.vcfg` on its own, through the Windows registry, Steam's `libraryfolders.vdf` and the `userdata` folder, wherever your Steam library lives. Both files are shown as pretty-printed JSON. A third view merges them, with your custom keybinds on top of the defaults. For the same key your entry wins, and `<unbound>` removes a default. If detection fails you can pick a file by hand, and the app remembers the path.

**Live (GSI).** Runs a local Game State Integration listener on port 4000 and writes `gamestate_integration_randomizer.cfg` into the CS2 cfg folder. The tab shows the current game state as JSON and a log of the events CS2 sends, capped at 1000 entries. GSI only starts while CS2 runs. When the app writes the file for the first time or changes it, a dialog asks you to restart CS2 once.

**Overview.** Map, mode, score, bomb status and a round and bomb countdown. The countdown uses the mode's default times: 1:55 in Competitive, 1:30 in Wingman, 2:15 in Casual, 40 seconds once the bomb is planted. Deathmatch and the other modes without a fixed round time show no timer, and a server with its own round time shows the default anyway. As a spectator (GOTV, demo, observer) you get a scoreboard for both teams with a detail card per player. In a normal match you get your own card.

The app only reads CS2's files. The GSI config is the one file it writes into the game folder. Its own settings live in `%LOCALAPPDATA%\randomizer-cs2\app.properties`.

## Build and run

Requirements: Windows, JDK 25, Maven.

```bash
mvn verify                                 # build + all tests
mvn test -DexcludedGroups=integration      # skip the tests that open a local port
mvn javafx:run                             # start the app
```