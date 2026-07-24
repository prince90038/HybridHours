# HybridHours

HybridHours is a utility Android application designed for hybrid workers to track their office hours and manage Work From Home (WFH) schedules effectively.

## Features

- **Office Hour Calculator**: Simple interface to select start and end times for office work.
- **WFH Time Display**: Automatically calculates and displays WFH time before and after office hours based on a target total work duration.
- **Custom Reminders**: Create multiple reminders (e.g., for timesheets) that trigger notifications at specific times on selected days.
- **Persistent Storage**: Saves your work schedule and reminder settings using SharedPreferences and Gson.
- **Reboot Resilience**: Reminders are automatically rescheduled after a device reboot.
- **Material 3 Design**: Features a modern, clean UI with rounded cards and intuitive pickers.

## Technical Specifications

- **Language**: Java
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Architecture**: Standard Android Activity/Receiver pattern.
- **Key Libraries**:
    - `androidx.appcompat`: For backward compatibility.
    - `com.google.android.material`: For modern UI components.
    - `androidx.work`: For background task management.
    - `com.google.code.gson`: For JSON serialization of reminder data.

## Permissions

The app requires the following permissions to function correctly:
- `POST_NOTIFICATIONS`: To display reminders.
- `RECEIVE_BOOT_COMPLETED`: To reschedule reminders after a reboot.
- `USE_EXACT_ALARM`: To ensure reminders trigger exactly at the set time.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Recommended to prevent the system from killing the background services responsible for reminders.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/prince90038/HybridHours.git
   ```
2. Open the project in **Android Studio**.
3. Build and run the app on an emulator or a physical device running Android 8.0 or higher.

## Author

**Prince Chauhan**
- GitHub: [@prince90038](https://github.com/prince90038)
