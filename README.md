# <img src="https://github.com/user-attachments/assets/ab28eba7-4b88-47b4-be10-ac4487d66e23" alt="randomizer" width="24" height="24" style="vertical-align: middle;" />andomizer-CS2

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange.svg)](https://adoptium.net/)
[![GitHub Stars](https://img.shields.io/github/stars/bsommerfeld/randomizer-cs2?style=social)](https://github.com/bsommerfeld/randomizer-cs2/stargazers)

> ⚠️ **Rewrite in progress** — this branch is a complete from-scratch rewrite (v2.0.0). The old codebase has been removed.

**Randomizer-CS2** is a desktop app for *Counter-Strike 2*. The rewrite currently covers the first building block:

- Automatically locates your CS2 config (`user_keys_default.vcfg`) via the Windows registry and Steam's `libraryfolders.vdf` — works no matter where your Steam library lives.
- Displays the config as pretty-printed JSON on startup.
- Manual file picker as fallback (the chosen path is remembered).

## Build & Run

Requirements: Windows, JDK 25, Maven.

```bash
mvn verify        # build + tests
mvn javafx:run    # start the app
```

## Team

- **Development:** [Benjamin Sommerfeld](https://github.com/bsommerfeld)
- **Design:** [Kjell Witzurke](https://github.com/bustolio)
