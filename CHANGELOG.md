## Changelog

### Added

- Added Release View below the HomeView
- Added setting to switch whether CS2 has to be in focus in order that the Randomizer works

### Changed

- Refined SettingsView layout and styling
- Refined history container contents
- Refined alerts on errors
- General Alert stylings have been improved

### Fixed

- Config Sync in Settings now don't show there is no config anymore, despite there is
- Fixed button showing false positive sync success when the sync wasn't successful **but** there was a config file
  beforehand
- UIExceptionHandler is now always populating its alerts on the UI thread

### Removed

- Removed time tracking