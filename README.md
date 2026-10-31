# NIKO AI — Android starter project

This is a starter Android Studio project, not a compiled APK.

## Included in this prototype
- Dark purple chat interface for NIKO
- Android speech-to-text through the system speech recognizer
- Text-to-speech replies (Persian voice depends on installed Android TTS voices)
- Optional OpenAI-compatible chat connection using a user-provided API key
- Safe call flow: opens the dialer; the user confirms the call

## Not included yet
- Always-on wake word / background microphone service
- Find-my-phone voice beacon while locked/backgrounded
- Direct contact lookup and one-tap calling
- Universal control of other apps/video players
- Cloud-synced long-term memory
- Secure backend for API key storage

## Build
Open this folder in Android Studio, allow Gradle sync, then Build > Build Bundle(s) / APK(s) > Build APK(s).
Use a current Android Studio installation with JDK 17.

## Important
For a production app, do not ship a personal API key inside the APK. Use a secure backend. This prototype keeps the key only in app memory during the current run, but it still sends requests directly to the API provider.
Speech recognition and Persian TTS availability depend on the device's installed Google/Samsung speech services and language packs.
