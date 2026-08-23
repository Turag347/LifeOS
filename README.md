# LifeOS — Full Android Project

This package is a substantially expanded offline-first LifeOS foundation:
dashboard, tasks, routine tracking, finance, AI feature entry points, study/Russian
modules, analytics, dark mode, local persistence, and GitHub Actions APK building.

## Build without Android Studio
1. Create a GitHub repository.
2. Upload this project.
3. Push to `main`.
4. Open Actions → LifeOS APK → run the workflow.
5. Download the `LifeOS-debug-apk` artifact.
6. Transfer the APK to Android and install it.

## Google Drive design
Use Google OAuth and the least-privilege `drive.appdata` scope for private app data.
The app should encrypt sensitive records before upload. Do not place Gemini API keys
or OAuth client secrets that must remain secret inside the APK.

## Important
This is a full project foundation, not a claim that every production integration
(Gemini backend, Google OAuth credentials, Drive sync conflict engine, licensed
Russian audio/video content, calendar providers, and release signing) is already
connected. Those require external credentials/services and testing on a real device.
