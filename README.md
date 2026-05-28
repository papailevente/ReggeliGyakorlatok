# Morning Routine (Reggeli Rutin) 🏋️‍♂️☀️

A modern, professional fitness tracking application built with **Kotlin** and **Jetpack Compose**. Designed to help you track your morning exercises with a focus on simplicity, aesthetics, and motivation.

## Features 🚀

- **Custom Exercise Groups**: Organize your workout into logical segments (e.g., Upper Body, Cardio, etc.).
- **Workout Modes**: Choose between **Normal** (full day routine) and **Extra** (select a specific group to focus on).
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

## Changelog 📝

### v1.3.1
- **Feature Tour**: Added an interactive showcase to guide users through the app's features.
- **Onboarding Improvement**: Visual highlights for key components like Modes, Timers, and Groups.

### v1.3.0
- **Exercise Groups**: Organize exercises into custom categories for more focused workouts.
- **Normal & Extra Modes**: Choose between a full daily routine or selecting a specific group.
- **Improved Management**: Dedicated UI for managing groups and assigning exercises to them.
- **Database v4**: Migrated to support the new group-based data structure.

### v1.2.0
- **Weekly Planner**: Introduction of daily exercise sets. Customize your routine for each day of the week.
- **Day Selector UI**: New tab-based interface for switching between different days.
- **Smart Detection**: App automatically opens to the current day of the week.

### v1.1.0
- **New Splash Screen**: Added a modern, themed splash screen with a custom logo for a more professional app start experience.
- **Dynamic Backgrounds**: Implemented different splash backgrounds for portrait and landscape orientations to ensure the app looks great on any screen.
- **Visual Branding**: Introduced a new "Morning Routine" logo centered on the startup screen.

### v1.0.6
- **Database Fix**: Disabled destructive migration to prevent data loss during app updates. Your workout history is now safe across future versions.
- **Auto-Version UI**: The version number in the menu now automatically synchronizes with the project settings.

### v1.0.5
- Initial public release with basic history and exercise management.

## Installation 📲

### Option 1: Direct APK
Download the latest `MorningRoutine.apk` from the [Releases](https://github.com/papailevente/ReggeliGyakorlatok/releases) section and install it on your Android device.

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
