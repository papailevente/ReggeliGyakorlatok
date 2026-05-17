# Morning Routine (Reggeli Rutin) 🏋️‍♂️☀️

A modern, professional fitness tracking application built with **Kotlin** and **Jetpack Compose**. Designed to help you track your morning exercises with a focus on simplicity, aesthetics, and motivation.

## Features 🚀

- **Dynamic Sunrise Animation**: The background features a rising sun that tracks your workout progress. Start in the dark and finish in the bright daylight!
- **Intelligent Timers**:
    - **Total Workout Timer**: Track your overall session duration.
    - **Exercise Timer**: Independent timer for specific movements (auto-restarts on reset).
    - **Automatic Rest Timer**: Triggered automatically after each set completion.
- **Exercise Management**:
    - Add, edit, or delete your own custom exercises.
    - **Drag & Drop Reordering**: Easily change the order of your routine using up/down controls.
    - Persistent storage using **Room Database**.
- **Workout History**:
    - Track your previous sessions with timestamps and detailed set counts.
    - View total stats: number of workouts and total active minutes.
- **Material You Design**:
    - Supports **Dynamic Color** (Android 12+), adapting to your phone's wallpaper.
    - Clean, modern Material 3 interface with adjustable transparency.
- **Multilingual Support**: Available in **English**, **Deutsch**, and **Hungarian**.
- **User Experience**:
    - **Keep Screen On**: The display stays active while you work out.
    - Notification sounds and vibration when timers finish.

## Tech Stack 🛠

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture**: MVVM (ViewModel, StateFlow)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **DI/Build System**: Gradle 9.x with Version Catalogs (.toml)

## Installation 📲

### Option 1: Direct APK
Download the latest `MorningRoutin.apk` from the [Releases](https://github.com/papailevente/ReggeliGyakorlatok/releases) section and install it on your Android device.

### Option 2: Build from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/papailevente/ReggeliGyakorlatok.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and click **Run**.

## Credits ✍️

Developed by **Zsenike** with the assistance of **Grok** and **Gemini**.

---
*Stay fit, stay motivated!* 🏆
