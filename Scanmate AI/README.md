# ScanMate AI Pro

A premium offline-safe Android document scanner and assistant built with Jetpack Compose, CameraX, Room, ML Kit OCR/barcode, PDF export, ZIP backup tools and optional Gemini AI.

## Important

This is not a rebuild from scratch. Existing scanner, OCR, QR/barcode, PDF, ZIP, Room, settings and AI-key architecture are preserved and hardened for Colab builds.

## Google Colab build

Open `colab-build.ipynb` or follow `COLAB_BUILD_GUIDE.md`.

Core commands:

```bash
chmod +x ./gradlew
./gradlew clean --no-daemon
./gradlew assembleDebug --no-daemon --stacktrace
./gradlew assembleRelease --no-daemon --stacktrace
./gradlew bundleRelease --no-daemon --stacktrace
```

## Output paths

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
app/build/outputs/apk/release/app-release-unsigned.apk
app/build/outputs/bundle/release/app-release.aab
```

## Release signing

Release signing is environment-based. See `SIGNING_RELEASE_GUIDE.md`.

## Android compatibility

See `ANDROID_COMPATIBILITY_NOTES.md` for Android 9–16 support and Android 17 upgrade notes.

## Privacy

See `PRIVACY_NOTICE.md`. Replace the placeholder before publishing.
