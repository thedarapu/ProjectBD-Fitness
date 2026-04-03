# Architecture Overview

ProjectBD - Fitness is a modern Android application built entirely with Kotlin. It relies on the Model-View-ViewModel (MVVM) architectural pattern to ensure clean separation of concerns.

## Tech Stack Overview
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Local Persistence:** Room Database, SharedPreferences
- **Dependency Injection / Injection Equivalent:** Manual Dependency Injection via Repository instantiations and contextual resolution.
- **External APIs:** Edamam API, FatSecret API
- **On-device AI / Scanning:** CameraX and ML Kit

## Key Components

### UI Layer (`ui/screens`)
Organized by feature modules such as `home`, `diet`, `workout`, `config`, and `history`.
- Contains Jetpack Compose views holding local view states.
- Utilizes `ViewModel`s (e.g. `DietViewModel`, `WorkoutViewModel`, `HomeViewModel`) which coordinate data from repositories and dispatch UI states.

### Domain Layer (`domain`)
Contains the core business logic of the application.
- **Models:** Definitions for structural data including `ActivityLevel`, `WorkoutModels`, and `DietModels` representing core concepts.
- **Use Cases:** Executable classes like `CalculateMacrosUseCase` and `WorkoutPlanGenerator`. These classes hold independent, reusable business logic decoupled from the UI.

### Data Layer (`data`)
Responsible for persistence and fetching from external boundaries.
- **Local:** Defined inside `data/local`. Uses `Room` database (`AppDatabase`, `DailyDatabase`, `DailyActivityDao`) to persist macro history, logging, and historical fitness traces.
- **Repository:** Unified data sources inside `data/repository/SettingsRepository.kt` acting as the bridge between persistent data and ViewModels.

## Navigation flow
Navigation is driven by the `NavHost` compose setup in `MainActivity.kt`.

1. **Onboarding Screen:** Triggers if standard profile data does not exist in `SharedPreferences`.
2. **Main Application:** A Bottom Navigation structure supporting Home, Diet, Workout, and Config. State scopes are isolated per Route.

## Important Architectural Notes
- The app handles **API Keys** dynamically via `secrets.env` properties piped directly into the `BuildConfig` using Gradle scripts.
- The **App Theme** observes changes passively using a `DisposableEffect` registered right at the top scope of `setContent` in `MainActivity.kt`.
- Automation boundaries have effectively removed any specific background `Service` structures, preventing unneeded overhead.
