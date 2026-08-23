# AI Vision Assistant

Native Android multimodal assistant built with Kotlin, Jetpack Compose, MVVM, and clean
domain/repository boundaries.

## Pipeline

1. Android `SpeechRecognizer` captures a spoken question locally.
2. ML Kit Document Scanner captures or imports a one-page image.
3. ML Kit Text Recognition V2 extracts text locally and returns numbered block bounding boxes.
   The overlay can be hidden, and the recognized text remains selectable in the UI.
4. LiteRT runs the bundled quantized MobileNet V2 model on-device. It attempts NPU, then GPU,
   and falls back to CPU if an accelerator cannot compile or execute the model.
5. The image, spoken/typed question, local OCR blocks, and classification hint are sent to
   `gemini-3.6-flash` through Firebase AI Logic, protected by Firebase App Check.

The result card reports which LiteRT accelerator actually completed inference. Public LiteRT
support for Google Tensor NPUs varies by device/runtime; when the NPU provider is unavailable,
the app tries GPU and then CPU.

## Configure and run

1. Open the [Firebase console](https://console.firebase.google.com/) and create or select a
   Firebase project.
2. Open **AI Services > AI Logic**, select **Get started**, and choose the Gemini Developer API.
3. Register an Android app whose package name is exactly
   `com.example.multimodalassistant`.
4. Download its `google-services.json` file and place it here:

```text
app/google-services.json
```

The old `GEMINI_API_KEY` entry in `local.properties` is no longer used. Build with Android Studio
or:

```shell
./gradlew :app:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 8+ device with Google Play
services. On the first use, grant camera and microphone permissions. The ML Kit scanner may
download its scanner module before opening for the first time.

For a debug build, run the app and submit a query once, then find the `DebugAppCheckProvider`
registration token in Logcat. In the Firebase console, open **App Check**, select the Android app,
open the overflow menu, choose **Manage debug tokens**, and register that token. The first request
may be rejected; retry it after registering the token.

If Firebase reports `Firebase App Check token is invalid`, verify that the token was registered in
the same project as `app/google-services.json`. This configuration currently points to
`aivisionassistant-7f722`. If the Firebase project was changed, clear the app's storage or uninstall
it, run the debug build again to generate a new secret, register that new secret, and retry.

Release builds use the Play Integrity App Check provider. Configure Play Integrity and register
the release signing certificate SHA-256 in Firebase before distributing the app. Never ship the
debug App Check provider in a release build.

## Verified versions

- Android Gradle Plugin 9.3.1, compile SDK 37, target SDK 35
- Kotlin / Compose compiler 2.4.10, Compose BOM 2026.08.00
- LiteRT 2.2.0 (`litert` + `litert-api`)
- ML Kit Document Scanner 16.0.0
- ML Kit Text Recognition 16.0.1 (bundled Latin-script model)
- Firebase BoM 34.18.0 (`firebase-ai`, App Check Debug, and Play Integrity)
- Google Services Gradle plugin 4.5.0
- AndroidX Activity 1.13.0, Lifecycle 2.11.0, Core KTX 1.19.0

The LiteRT 2.2.0 runtime and Kotlin API artifacts currently publish the same Android namespace.
`android.uniquePackageNames=false` is intentionally set in `gradle.properties` until Google ships
those artifacts with unique namespaces.
