# Coach Stopwatch (Multi-Stopwatch Android App)

**Coach Stopwatch** is a practical, ad-free multi-stopwatch utility designed for coaches and trainers tracking multiple athletes simultaneously during races, training sessions, and time trials.

## Overview & Value Proposition

In fast-paced race environments, coaches need to measure precise timing for multiple athletes without navigating complex menus or dealing with touch response lag. **Coach Stopwatch** delivers a streamlined, high-reliability interface that allows seamlessly starting, stopping, split-timing, and resetting multiple stopwatches in parallel. With instant touch-down triggering, physical volume key shortcuts, smart focus prediction, and local session persistence, it ensures timing accuracy and efficiency when every millisecond counts.

---

## Core Features

- **Multi-Stopwatch Grid**: Run and display multiple stopwatches concurrently on a single screen with clear layout, dynamic sizing, and individual names and colors per athlete.
- **Instant Touch-Down Control**: Start, Stop and Lap trigger on touch-down (`PointerEventType.Press`) rather than on release, eliminating release lag.
- **Hardware Volume Keys with Smart Focus**: Volume Up starts all watches, then records a lap for the highlighted watch. Volume Down stops it; when nothing is running, *holding* Volume Down resets all watches (a short press only shows a hint). Focus automatically moves to the watch expected to finish its next lap soonest, also across staggered starts.
- **Running Notification**: While a watch runs, a foreground service keeps timing alive in the background and shows an ongoing notification with a live timer and Lap / Stop / Stop all actions. Can be switched off in the settings.
- **Persistent Timing State**: Stopwatches, laps and run history survive app restarts and process death. After a device reboot, running watches are stopped with their duration estimated from wall-clock time.
- **Multi-Session Run Tracking**: Group timing into named runs/sessions to organize history and splits. Stop and Start continue the current run; a short Reset begins the next run.
- **Long-Press Reset**: A short press on Reset clears the current time; a long press clears the full history.
- **Detail View, Sharing & CSV Export**: Per-stopwatch lap breakdown; share results as text or CSV spreadsheet.
- **Backup Export & Import**: Export all stopwatches as a `*.watches.json` file and import it again via the in-app import or by opening the file from another app.
- **Localization**: English, German and French; integrates with the Android 13+ per-app language setting.
- **Theme & Text Scaling**: Material 3 with light/dark/system theme, optional dynamic colors and adjustable text size.
- **Accessibility**: All controls are labeled and operable with TalkBack and Switch Access (including the long-press actions); tile colors meet WCAG AA contrast.

---

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) with Coroutines and Flow
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Architecture**: MVVM; a single process-wide `StopwatchViewModel` (owned by `WatchesApplication`) shared by the UI and the foreground service
- **Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) with kotlinx.serialization JSON (separate stores for settings and timing state)
- **Testing**: JUnit 5 unit tests (JVM); JUnit 4 + Compose UI instrumented tests (device/emulator)
- **CI/CD**: GitHub Actions workflow building the debug APK and running the unit tests

---

## Build & Run Instructions

### Prerequisites

- **JDK**: 17 or newer
- **Android Studio**: a version supporting Android Gradle Plugin 9.x
- **Android SDK**: compile SDK 37, target SDK 36 (Android 16), minimum SDK 30 (Android 11)

### Commands

Execute the following commands from the project root directory:

- **Assemble Debug Build**:
  ```bash
  ./gradlew assembleDebug
  ```
  *(Outputs `watches-debug.apk` in `app/build/outputs/apk/debug/`)*

- **Run Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

- **Run Instrumented Tests** (requires a connected device or emulator; note that this installs and afterwards *uninstalls* the app, deleting its data on that device):
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

- **Install Debug APK to Connected Device/Emulator**:
  ```bash
  ./gradlew installDebug
  ```

### Versioning

`versionCode` is derived from the number of commits (`git rev-list --count HEAD`), so builds from later commits always install over earlier ones. `versionName` is set manually in `app/build.gradle.kts`. CI checks out the full history for this reason.

### Release Notes for Google Play

The foreground service uses the `specialUse` type (there is no dedicated stopwatch type), which requires a short justification in the Play Console. Language splits are disabled in the app bundle so that the in-app language picker works for all bundled languages.

---

## License & Contributing

### License
This project is open-source software licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.

### Contributing
Contributions, bug reports, and feature requests are welcome. Feel free to open an issue or submit a pull request!
