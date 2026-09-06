# IISER Auto Login

An Android application that automates authentication with the IISER Thiruvananthapuram Wi-Fi gateway.

The app monitors the device's Wi-Fi connection and attempts authentication when the IISER gateway becomes available.

## Features

- Automatic IISER Wi-Fi gateway detection
- Automatic authentication after connecting to Wi-Fi
- Stops after successful authentication
- Foreground service for background operation
- Gateway availability checking
- XML response parsing
- TLS certificate validation using a bundled CA certificate
- Start/Stop controls from the application

## How It Works

```text
Wi-Fi Connected
      │
      ▼
Check IISER Gateway
      │
      ├── Unavailable ──► Wait for gateway
      │
      ▼
Attempt Authentication
      │
      ├── Failed ──► Returns: Limit Exceded
      │
      ▼
Authentication Successful
      │
      ▼
Stop Further Login Attempts
```

## Requirements

- Android 7.0 (API 24) or newer
- Android Studio
- JDK 17
- Android SDK 35
- Access to the IISER Thiruvananthapuram Wi-Fi network

## Technology

- Kotlin
- AndroidX
- OkHttp
- Android Foreground Service
- Android Connectivity APIs
- XML parsing

## Account Configuration

The application supports username/password pair and attempts them.

Example:

```kotlin
private val ACCOUNTS = listOf(
    Pair("YOUR_USERNAME", "YOUR_PASSWORD"),
)
```


## TLS Certificate

The application uses a CA certificate to establish trusted TLS communication with the IISER gateway.

The certificate is stored at:

```text
app/src/main/res/raw/iiser_ca.crt
```

The application does not disable TLS certificate validation and does not use a trust-all SSL configuration.

## Building

1. Clone the repository.
2. Open the project in Android Studio.
3. Allow Gradle to synchronize.
4. Connect an Android device or start an emulator.
5. Build and run the application.

## Usage

1. Connect the Android device to the IISER Wi-Fi network.
2. Open **IISER Auto Login**.
3. Tap **Start Auto Login**.
4. The application monitors the network and attempts authentication automatically.
5. Tap **Stop Auto Login** when automatic authentication is no longer required.

## Project Structure

```text
iiserautologin/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/iiserautologin/
│           │       ├── MainActivity.kt
│           │       └── IISERAuthService.kt
│           └── res/
│               └── raw/
│                   └── iiser_ca.crt
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

## Security and Privacy

This project is intended for authorized use on the IISER Thiruvananthapuram network.

Do not commit:

- Passwords
- API keys
- Access tokens
- Private keys
- Android signing keystores
- Other confidential credentials

## Disclaimer

This project is provided for personal and educational use.

The author is responsible for ensuring that the application is used only where they are authorized to automate authentication.

Changes to the IISER gateway or network authentication system may require corresponding changes to the application.

## License

This project is available under the MIT License.

See `LICENSE` for details.
