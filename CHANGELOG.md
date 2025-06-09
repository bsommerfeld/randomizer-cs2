## Changelog

### Added

- Added setting to switch whether CS2 has to be in focus in order that the Randomizer works
- Added GitHub Repository Details to top corner of HomeView
- Added Releases View
- Added Machine Learning Settings for future updates
- Timers for each action in the Randomizer Logbook

### Changed

- Refined SettingsView layout and styling
- Refined RandomizerView history container contents
- Several Code and Performance improvements
- Refined RandomizerView layout and styles with improved action styling, icons, and time labels
- Refined builder-view styles and layout with updated CSS and image assets
- Refined logbook styles and spacing for improved layout and readability
- Enhanced HomeView with dynamic releases list, changelog display, and improved styles
- Updated alert styling with modernized CSS classes
- Simplified configuration loading logic and improved UI synchronization
- Dynamically load randomizer version from properties file and update application title
- Improved Starting/Stopping the Executor

### Fixed

- Config Sync in Settings now don't show there is no config anymore, despite there is
- Fixed button showing false positive sync success when the sync wasn't successful **but** there was a config file
  beforehand
- UIExceptionHandler is now always populating its alerts on the UI thread
- Fixed safe index calculation to prevent out-of-bound errors when updating dropIndicator position
- Fixed handling of native hook unregister failure during application shutdown
- Fixed order of executor stop and action discard during application shutdown
- Fixed injection of ApplicationContext into various components
- Fixed GitHub issues URL in UIUncaughtExceptionHandler
- Fixed skipping GitHub releases without CHANGELOG.md asset and improved related exception handling
- Fixed stopping the Randomizer interrupts the current sequence and every underlying Action
- Fixed handling of actions when execution stopped mid-sequence

### Removed

- Removed time tracking
- Removed Apply buttons in Builder settings and replaced it with live updates
