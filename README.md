# Coach Stopwatch (Multi-Stopwatch Android App)

**Coach Stopwatch** is a practical, ad-free multi-stopwatch utility designed for coaches and trainers tracking multiple athletes simultaneously during races, training sessions, and time trials.

## Overview & Value Proposition

In fast-paced race environments, coaches need to measure precise timing for multiple athletes without navigating complex menus or dealing with touch response lag. **Coach Stopwatch** delivers a streamlined, high-reliability interface that allows seamlessly starting, stopping, split-timing, and resetting multiple stopwatches in parallel. With instant touch-down triggering, physical volume key shortcuts, smart focus prediction, and local session persistence, it ensures timing accuracy and efficiency when every millisecond counts.

---

## Core Features

- **Multi-Stopwatch Grid**: Run and display multiple stopwatches concurrently on a single screen with clear layout, dynamic sizing, and individual label management for athletes.
- **Instant Touch-Down Control**: Timers trigger on `PointerEventType.Press` (touch-down) rather than touch-up/release to eliminate human response lag and ensure real-time reaction accuracy.
- **Hardware Volume Key Shortcuts with Smart Focus Prediction**: Use physical Volume Up and Volume Down buttons as tactile start/stop triggers. The app intelligently predicts and auto-focuses the active or next relevant timer for rapid operation.
- **Multi-Session Run Tracking**: Group timing events into distinct training sessions, runs, or races to organize history, splits, and athlete records cleanly.
- **Long-Press Reset**: Accidental resets are prevented using long-press confirmation gestures on clear/reset controls.
- **Detail View with Individual Export**: Drill down into individual stopwatch logs, review full split time breakdowns, and export session data as formatted text or CSV.
- **English & German i18n**: Full internationalization support with localized strings for English (`en`) and German (`de`).
- **Theme & Text Scaling**: Dynamic Material 3 styling with light/dark theme toggle and adjustable text scaling for clear visibility on field devices under various outdoor conditions.
- **DataStore JSON Persistence**: Reliable local storage powered by Jetpack DataStore, serializing application state and timing history into structured JSON so data is preserved across app restarts or backgrounding.

---

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3 Design](https://m3.material.io/)
- **Architecture**: MVVM with `ViewModel` and `StateFlow`
- **Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) with JSON serialization
- **Testing**: JUnit 5 (JUnit Jupiter) for unit testing application logic
- **CI/CD**: GitHub Actions CI workflow for automated builds and testing

---

## Build & Run Instructions

### Prerequisites

- **JDK**: JDK 17
- **Android Studio**: Android Studio Ladybug or newer
- **Android SDK**: SDK 35 (Android 15)

### Commands

Execute the following commands from the project root directory:

- **Assemble Debug Build**:
  ```bash
  ./gradlew assembleDebug
  ```
  *(Outputs `watches-debug.apk` in `app/build/outputs/apk/debug/`)*

- **Run Unit Tests**:
  ```bash
  ./gradlew test
  ```

- **Install Debug APK to Connected Device/Emulator**:
  ```bash
  ./gradlew installDebug
  ```

---

## License & Contributing

### License
This project is provided open-source under the GPL License.

### Contributing
Contributions, bug reports, and feature requests are welcome. Feel free to open an issue or submit a pull request!
