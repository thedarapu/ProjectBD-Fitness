# Feature Overview

ProjectBD - Fitness focuses strictly on physical health tracking by offering powerful nutrition and workout calculation and history logging mechanisms.

## Core Features

### 1. Diet & Nutrition
- **Macro Calculation:** Dynamically calculates protein, carbs, calories, and fats required to maintain, cut, or bulk based on height, weight, activity level, and gender parameters. 
- **Edamam & FatSecret Integration:** Allows for pulling wide sets of food databases securely on the fly.
- **Barcode Scanner:** Powered by Google's ML Kit and CameraX, allowing users to scan barcode tags on foods directly.
- **Daily Dashboard:** View caloric expenditure relative to goal targets.

### 2. Workout Generation
- **Target Setting:** Users input exercise minute goals, move goals (active calories), step goals, and stand hour goals.
- **Automated Planning:** Uses workout generation use-cases located in the domain layer to orchestrate weekly routines based on selected goals (e.g., Stamina, Builds Muscle, Flexibility).

### 3. Progressive History
- **Activity Logging:** Historical snapshotting backed by the Room Database mechanism storing end-of-day stats. 
- **History Viewer:** Features a localized view state `HistoryContent` triggered through the top App Bar, converting the navigation host directly to a robust history table.

### 4. Personalization & Setup
- **Onboarding Interface:** Ensures all core personal data is collected before pushing users into the core navigation state route.
- **Dynamic Configuration:** Access profile statistics, goal modification, and app thematic styling directly out of a consolidated Config Screen.

## Future Context
*For any AI tools or new development:*
Note that a legacy `Automation` context once heavily resided in this project, which dealt with auto HotSpot toggling, Bluetooth detection, and Wifi Accessibility. It has been stripped to purely align with physical Fitness tracking.
