# Changelog

## 2.0.0 (in progress)

- Complete rewrite from scratch.
- CS2 configs (`user_keys_default.vcfg` and `cs2_user_keys.vcfg`) are located automatically on startup (Windows registry + `libraryfolders.vdf` / `userdata` scan) and displayed as JSON.
- Manual file picker as fallback; chosen paths are persisted per config.
- CS2 Game State Integration (bustolio/CS2-GSI): live tab with start/stop toggle, event log and game-state JSON; the GSI config file is generated into the CS2 cfg folder.
