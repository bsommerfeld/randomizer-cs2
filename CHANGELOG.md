## Changelog

### Added

- Added Release View below the HomeView
- Added setting to switch whether CS2 has to be in focus in order that the Randomizer works
- Added GitHubService to interact with GitHub API and fetch repository releases
- Added ActionSequenceExecutor injection and set executor thread as daemon
- Added "Hector" button to HomeView interface (coming soon)
- Added dynamic binding for actionSequencesSection visibility based on configuration path
- Added package-info for action execution SPIs detailing interfaces and their roles

### Changed

- Refined SettingsView layout and styling
- Refined history container contents
- Refined alerts on errors
- General Alert stylings have been improved
- Several Code and Performance improvements
- Refined RandomizerView layout and styles with improved action styling, icons, and time labels
- Refined builder-view styles and layout with updated CSS and image assets
- Refined logbook styles and spacing for improved layout and readability
- Enhanced HomeView with dynamic releases list, changelog display, and improved styles
- Replaced action name logic with icon-based implementation
- Replaced ActionRepository and FocusManager with SPI interfaces and added default implementations
- Updated alert styling with modernized CSS classes
- Extracted various styles into standalone CSS files for better organization
- Simplified configuration loading logic and improved UI synchronization
- Dynamically load randomizer version from properties file and update stage title
- Migrated GitHubRelease and GitHubReleaseAsset to records

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

### Removed

- Removed time tracking
- Removed unused TimeTracker and RandomizerConfig bindings and dependencies
- Removed redundant toggle button disable logic in SettingsViewController
- Removed unused input handling logic in various controllers
- Removed unused logbookState field
- Removed redundant toString method from GitHubRelease class
- Removed updater module and associated files from project due to deprecation
