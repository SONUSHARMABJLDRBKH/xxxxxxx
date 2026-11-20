# StudyWin India – Setup, Firebase, AdMob & Release Build Guide

## 1. Firebase Configuration
- **Provisioned Project**: `gen-lang-client-0007292792`
- **Firestore Database ID**: `ai-studio-android-studywin-e88f72bc-8d27-49e5-a72c-a6735d5e50b9`
- **Google Sign-In**: Enabled via Jetpack Credential Manager (`GetSignInWithGoogleOption`).
- **Optional Console Steps**:
  - Enable **Phone** sign-in provider in Firebase Console -> Authentication -> Sign-in method for live SMS OTPs.
  - Deploy Cloud Functions (`functions/`) on Blaze plan for AdMob SSV webhooks (`firebase deploy --only functions`).
  - Deploy Firestore security rules (`firebase deploy --only firestore:rules`).

## 2. AdMob Production Setup
- **Configured Banner Ad Unit ID**: `ca-app-pub-6545218519929279/1965994726`
- **Configured Rewarded Ad Unit ID**: `ca-app-pub-6545218519929279/9899043803`
- **Development Mode**: By default `useTestAds = true` uses Google's official Test Ad Unit IDs so you never generate invalid traffic during testing.
- **Switching to Production Ads**:
  1. Replace the sample `com.google.android.gms.ads.APPLICATION_ID` meta-data value in `app/src/main/AndroidManifest.xml` with your actual AdMob App ID (`ca-app-pub-6545218519929279~...`).
  2. Open **Admin Panel -> Ads & Config** inside the app and toggle off **Use Official Google Test Ads** (or set `useTestAds: false` in `app_config/settings`).

## 3. Building Debug APK
```bash
gradle :app:assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## 4. Building Release-Ready AAB (Google Play)
Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables (or Secrets), then run:
```bash
gradle :app:bundleRelease
```
Output AAB: `app/build/outputs/bundle/release/app-release.aab`
