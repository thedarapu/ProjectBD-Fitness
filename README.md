# ProjectBD - Fitness

This is a dedicated Android application for tracking macros, workouts, and nutrition. It leverages modern Android development tools to provide seamless food tracking, barcode scanning, and macro calculation to help you reach your fitness goals.

> [!NOTE]
> For more detailed context intended for developers or AI assistants, please review the documentation in the [`docs/`](./docs/) directory.

## 🚀 Features

- **Macro Tracking & Diet History**: Calculate your daily macros and keep a log of what you've eaten over time.
- **Barcode Scanning**: Built-in barcode scanner to quickly pull up food nutritional information.
- **Robust Food Database**: Integrated with Edamam and FatSecret APIs to provide a massive database of foods and nutritional data.
- **Modern UI**: Built entirely with Jetpack Compose, offering a clean, responsive, and dynamic user interface with a customizable theme.

## 🛠 Tech Stack

- **Language**: Kotlin 
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture & Local Storage**: Room Database for offline data persistence
- **Camera & Machine Learning**: 
   - CameraX for scanner interface
   - Google ML Kit for reliable Barcode Scanning
- **Health Integration**: Android Health Connect
- **Network & API Integration**: 
   - Edamam API
   - FatSecret API

## ⚙️ Setup & Configuration

To run this project locally, you will need to set up your API keys. 

1. Create a file in the root directory of the project named `secrets.env`.
2. Ensure `.gitignore` is configured to ignore `secrets.env`.
3. Add your Edamam and FatSecret credentials to `secrets.env` using the following format:

```env
FATSECRET_CLIENT_ID=your_fatsecret_client_id_here
FATSECRET_CLIENT_SECRET=your_fatsecret_client_secret_here
EDAMAM_APP_ID=your_edamam_app_id_here
EDAMAM_APP_KEY=your_edamam_app_key_here
```

After adding your keys, sync the project with Gradle and you'll be able to build and run the app.

## 📱 Compatibility

- **Minimum SDK**: 34 (Android 14)
- **Target SDK**: 36
