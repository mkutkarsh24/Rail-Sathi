# Building a signed Android App Bundle (AAB) for Google Play

Google Play requires an `.aab`. Use **Play App Signing**: Google holds the app-signing key and you
sign uploads with an *upload key* that can be reset if lost.

## 1. Before you build

- [ ] `RAIL_API_BASE_URL` points at your production HTTPS data service (see `PROVIDER_SETUP.md`).
      A release built with it blank would ship in **sample-data mode** — not acceptable for a store listing.
      `bundleRelease` / `assembleRelease` therefore fail when it is blank (override with `-PallowSampleRelease`).
- [ ] Replace the placeholder launcher icon (`app/src/main/res/drawable/ic_launcher_foreground.xml`
      and `res/values/colors.xml`).
- [ ] Bump `versionCode` (must increase on every upload) and `versionName` in `app/build.gradle.kts`.
- [ ] Run `./gradlew :app:testDebugUnitTest` and, on a device, `./gradlew :app:connectedDebugAndroidTest`.
- [ ] Fill in the effective date and contact in `PRIVACY_POLICY.md` and host it at a public URL.
- [ ] Check Play's current `targetSdk` requirement. The project targets API 35.

## 2. Create an upload key (once)

```bash
keytool -genkeypair -v \
  -keystore ~/keys/railsathi-upload.jks \
  -alias railsathi-upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Back the keystore and its passwords up somewhere safe and **outside the repository**
(`.gitignore` already excludes `*.jks`, `*.keystore`, `keystore.properties`).

## 3. Tell Gradle about it

Put this in `local.properties` (git-ignored), or export the same names as environment variables
(useful in CI secrets):

```properties
RELEASE_STORE_FILE=/home/you/keys/railsathi-upload.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=railsathi-upload
RELEASE_KEY_PASSWORD=...
```

`app/build.gradle.kts` creates the `release` signing config only when `RELEASE_STORE_FILE` is set.
Without it, `bundleRelease` still builds but produces an **unsigned** bundle that Play rejects.

## 4. Build

```bash
./gradlew clean :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

The release build enables R8 minification and resource shrinking. Keep rules for Gson DTOs and
Retrofit are in `app/proguard-rules.pro`. **Smoke-test the minified build** on a device before
uploading (install it via bundletool below) — missing keep rules show up as crashes or empty data
only in release builds.

Optional checks with [bundletool](https://developer.android.com/tools/bundletool):

```bash
# Build an installable APK set from the AAB, signed with your upload key, and install it
java -jar bundletool.jar build-apks --bundle=app-release.aab --output=app.apks \
  --ks=$RELEASE_STORE_FILE --ks-alias=$RELEASE_KEY_ALIAS \
  --ks-pass=pass:$RELEASE_STORE_PASSWORD --key-pass=pass:$RELEASE_KEY_PASSWORD
java -jar bundletool.jar install-apks --apks=app.apks

# Confirm the bundle is signed
jarsigner -verify -verbose app-release.aab | head
```

## 5. Upload to Play Console

1. **Create app**, choose *App bundle*, enrol in **Play App Signing** when prompted.
2. Upload `app-release.aab` to an *Internal testing* track first.
3. Upload `app/build/outputs/mapping/release/mapping.txt` (R8 mapping) so crash reports are readable.
4. Complete the store listing: description, screenshots, icon (512×512), feature graphic.
   State clearly that RailSathi is **not affiliated with Indian Railways, IRCTC or CRIS**.
5. **App content** declarations:
   * **Privacy policy URL** — where you hosted `PRIVACY_POLICY.md`.
   * **Data safety** — the app itself stores favourites / history / cached status on-device and sends
     train numbers, station codes, dates and PNRs to your backend. Declare what your backend and
     provider do with them (logging, retention). The in-app code collects no account, location,
     contacts, advertising ID or analytics.
   * Content rating questionnaire, target audience (not directed at children), ads (none).
6. Roll out to internal testers, verify on real devices, then promote to production.

## 6. Every later release

Increase `versionCode`, rebuild, upload the new `.aab` and its `mapping.txt`. Keep the same upload key.
If you lose the upload key, Play support can reset it because you enrolled in Play App Signing.
