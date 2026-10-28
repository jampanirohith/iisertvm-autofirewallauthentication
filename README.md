# IISER Auto Login

IISER Auto Login is an Android app that automates authentication to the IISER Thiruvananthapuram Wi-Fi gateway. Start the service once, and it checks the Wi-Fi connection and gateway before attempting to sign in with the IISER account saved in Settings.

## Features

### Automatic Wi-Fi authentication
- Monitors Wi-Fi network availability while the auto-login service is running.
- Checks the current active network when the service starts, so it can act even if Wi-Fi was already connected.
- Checks whether the IISER gateway is reachable before attempting authentication.
- Sends the configured email address and password to the IISER gateway's XML login endpoint.
- Parses the gateway's XML response and reports the authentication result.
- Prevents overlapping authentication attempts.
- Updates the foreground notification with service and authentication status, including gateway availability, missing account configuration, Wi-Fi disconnection, and login results.

### Account settings
- Configure one IISER account from the in-app **Settings** screen.
- Save an email address and password without editing or rebuilding source code.
- Show or hide the password while entering it.
- Load the saved account when Settings is opened again.
- Remove the saved account. Removing it also stops the authentication service if it is running.
- Saving updated credentials while the service is running triggers an immediate authentication attempt.

### Secure local credential storage
- Stores the account on the device rather than hard-coding credentials in the source.
- Encrypts the email address and password together using AES-GCM.
- Uses a key held by Android Keystore and stores the encrypted value in private app preferences.
- Uses the configured IISER CA certificate for HTTPS connections to the gateway; TLS certificate validation is not disabled.

### Controls and status
- Start or stop auto login from the main screen.
- View whether the service is running and which account is configured.
- Access Settings from the main screen.
- Use the **IISER Login** Quick Settings tile to start or stop the service from Android's Quick Settings panel.
- The Quick Settings tile indicates whether auto login is active or inactive and prompts you to configure an account if none is saved.

### Background operation
- Runs as an Android foreground service with a persistent notification while active.
- Uses Android connectivity callbacks to react to Wi-Fi becoming available or being lost.
- Uses coroutine-based background work and OkHttp for gateway requests.

## How It Works

```text
Start Auto Login
       |
       v
Check current Wi-Fi and monitor Wi-Fi changes
       |
       v
Check IISER gateway availability
       |
       +---- Gateway unavailable ----> Update notification / wait for another trigger
       |
       v
Read saved account
       |
       +---- No account configured --> Show status in notification
       |
       v
Submit login request over HTTPS
       |
       v
Parse XML status and message
       |
       v
Update notification with the result
```

The service is event-driven by Wi-Fi availability and checks the active Wi-Fi network when it starts. It is not described as a continuous polling loop.

## Requirements

- Android 7.0 (API 24) or newer.
- Android Studio.
- JDK 17.
- Android SDK 35.
- Access to the IISER Thiruvananthapuram Wi-Fi network and its authentication gateway.

## Technology Stack

- **Language:** Kotlin
- **Android UI:** Android Activities and native Android views
- **Networking:** OkHttp 4.12.0
- **Background tasks:** Kotlin Coroutines for Android
- **Android libraries:** AndroidX Core KTX and AppCompat
- **Connectivity:** Android `ConnectivityManager` and network callbacks
- **Service:** Android foreground service
- **Credential encryption:** AES/GCM/NoPadding with Android Keystore
- **Response handling:** Android XML Pull Parser
- **Build:** Gradle Kotlin DSL, Java 17, compile SDK 35

## Setup and Usage

1. Build and install the app on an Android device.
2. Open **IISER Auto Login**.
3. Open **Settings** using the gear icon.
4. Enter your IISER account email address and password, then tap **Save Account**.
5. Return to the main screen and tap **Start Auto Login**.
6. If prompted, allow notifications so Android can display the foreground service notification.
7. Connect to the IISER Wi-Fi network. The service checks the gateway and attempts authentication when Wi-Fi is available.
8. Check the main screen or the service notification for status.
9. Tap **Stop Auto Login** on the main screen, or toggle the **IISER Login** Quick Settings tile, to stop the service.

To change accounts, open Settings and save the updated credentials. To remove the account, choose **Remove Saved Account**.

### Quick Settings tile

If the tile is not already present, add **IISER Login** to the device's Quick Settings panel using Android's tile editor. Tap the tile to toggle the service. An account must be configured first.

## Gateway and TLS

The app communicates with the IISER gateway at:

- Gateway availability page: `https://gateway.iisertvm.ac.in:8090/httpclient.html`
- Login endpoint: `https://gateway.iisertvm.ac.in:8090/login.xml`

The CA certificate is bundled at:

```text
app/src/main/res/raw/iiser_ca.crt
```

The app builds a TLS trust configuration using this certificate. If the gateway certificate or institutional trust setup changes, the bundled certificate may need to be updated. Only use the app with an account and network you are authorized to access.

## Build Instructions

1. Open the project in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Connect an Android device running Android 7.0 or newer, or configure an appropriate emulator.
4. Build and run the `app` configuration.

The project uses `compileSdk 35`, `minSdk 24`, `targetSdk 34`, and Java/Kotlin JVM target 17.

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/iiserautologin/
│       │   ├── MainActivity.kt
│       │   ├── SettingsActivity.kt
│       │   ├── IISERAuthService.kt
│       │   ├── IISERQuickTileService.kt
│       │   └── CredentialStore.kt
│       ├── res/
│       │   ├── drawable/
│       │   ├── mipmap-*/
│       │   ├── raw/iiser_ca.crt
│       │   ├── values/
│       │   └── xml/
│       └── keepRules/
├── gradle/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

## Permissions

The app declares permissions for:
- Internet access for gateway requests.
- Reading network state to detect Wi-Fi connectivity.
- Running a foreground service, including the data-sync service type.
- Posting notifications on Android versions where notification permission is required.

## Security and Privacy Notes

- Configure only an account you are authorized to use.
- Credentials are encrypted locally using a key held by Android Keystore; do not put real credentials in source code or commit them to version control.
- The app communicates with the gateway over HTTPS and uses a bundled CA certificate rather than a trust-all TLS configuration.
- Removing the saved account clears the encrypted credential entry from app preferences.
- Review Android backup behavior and your device's security settings according to your privacy needs.

## Troubleshooting

- **No account configured:** Open Settings, enter the account details, and save them.
- **Gateway unavailable:** Confirm that the device is connected to the IISER Wi-Fi network and that the gateway is reachable.
- **Login failed:** Check the account details and whether the institutional gateway or authentication flow has changed.
- **Notification not visible:** Check notification permission and Android notification settings.
- **Quick Settings tile does nothing:** Make sure an account is saved and that the tile has been added to Quick Settings.

## Limitations

- The app is designed for the IISER Thiruvananthapuram gateway and its current authentication protocol; it is not a general-purpose Wi-Fi login manager.
- Authentication depends on gateway availability, valid credentials, and the gateway's response format.
- The service reacts to Wi-Fi availability and service start; it does not implement an always-running periodic retry loop.
- Android may restrict background work or foreground services depending on OS version and device policy.

## Disclaimer

This project is intended for personal and educational use on networks where you are authorized to automate authentication. The gateway's behavior and institutional policies may change; use the application responsibly and update it as needed.

## License

No `LICENSE` file was present in the supplied project snapshot. Add the project's intended license file before claiming a specific license.
