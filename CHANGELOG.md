## Changelog

### Added

- Added Release View below the HomeView

### Changed

- Refined SettingsView layout and styling
- Refined history container contents
- Refined alerts on errors

### Fixed

- Config Sync in Settings now don't show there is no config anymore, despite there is
- Fixed button showing false positive sync success when the sync wasn't successful **but** there was a config file
  beforehand
- UIExceptionHandler is now always populating its alerts on the UI thread

### Removed

- Removed time tracking