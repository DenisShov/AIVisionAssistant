# AI Vision Assistant

Native Android multimodal assistant built with Kotlin, Jetpack Compose, MVVM, Hilt dependency
injection, and clean domain/repository boundaries.

## Architecture

- `domain/model` contains the application models and configuration values.
- `domain/repository` defines the capabilities required by the application without referencing
  concrete ML Kit, LiteRT, Firebase, or speech implementations.
- `domain/usecase` coordinates local analysis and cloud processing. Dispatchers are supplied from
  the composition root so the use cases remain deterministic and testable.
- `data` contains Android, ML Kit, LiteRT, and Firebase adapters.
- `ui` contains the activity, lifecycle-aware ViewModel, mutually exclusive loading/result/error
  state, and focused Compose files for the screen, result panels, image preview, and Markdown.
- `di` is the Hilt composition root. ViewModel-owned ML resources and activity-owned speech and
  scanner resources use matching scopes.

## Pipeline

1. Android `SpeechRecognizer` captures spoken instructions locally.
2. ML Kit Document Scanner captures a one-page image, or Android's system Photo Picker imports an
   image from the gallery without broad media/storage permission.
3. As soon as an image is available, LiteRT classification and ML Kit Text Recognition V2 run
   concurrently on-device. The LiteRT result appears immediately below the image and is cached for
   the later Gemini request. OCR returns numbered block bounding boxes; the overlay can be hidden,
   and recognized text remains selectable.
4. When OCR finds enough text, bundled ML Kit Language Identification runs locally and displays the
   most likely language with confidence. A confident non-English result adds a suggestion such as
   **Translate the Ukrainian text to English**. If detection is uncertain, the app keeps a generic
   English-translation suggestion. Language identification depends on OCR producing usable text.
5. LiteRT uses the bundled quantized MobileNet V2 model. It attempts NPU, then GPU, and falls back
   to CPU if an accelerator cannot compile or execute the model.
6. The app derives up to six instruction suggestions from local OCR, detected language, and LiteRT
   results. It detects
   likely invoices, receipts, dates, contact details, tables, long documents, or general images.
   Suggestions are generated on-device and do not make another cloud request. Tapping one or more
   bubbles adds them to the editable instructions field; users can also type or speak anything.
7. The image, user instructions, local OCR blocks, and classification hint are sent to
   `gemini-3.6-flash` through Firebase AI Logic, protected by Firebase App Check.

Free-form Gemini responses render selectable Markdown, including headings, emphasis, lists,
quotes, links, inline code, and fenced code blocks. Prompt keystrokes remain local to the Compose
text editor and are committed to the ViewModel only when processing starts, avoiding whole-screen
state updates while typing.

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
- ML Kit Language Identification 17.0.6 (bundled on-device model)
- Firebase BoM 34.18.0 (`firebase-ai`, App Check Debug, and Play Integrity)
- Google Services Gradle plugin 4.5.0
- Hilt 2.60.1 with KSP 2.3.10
- AndroidX Activity 1.13.0, Lifecycle 2.11.0, Core KTX 1.19.0

The LiteRT 2.2.0 runtime and Kotlin API artifacts currently publish the same Android namespace.
`android.uniquePackageNames=false` is intentionally set in `gradle.properties` until Google ships
those artifacts with unique namespaces.
