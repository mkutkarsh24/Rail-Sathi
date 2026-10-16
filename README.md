# RailSathi — Indian Railway Companion

A native Android app (Kotlin, Jetpack Compose, Material 3) for live train running status, seat
availability, PNR status, favourite trains and search history. MVVM + Room + Retrofit + Hilt.

> **Read this first — what works today**
>
> * **Sample-data mode works out of the box.** With no configuration the app runs against a
>   built-in, clearly labelled sample data source. Nothing it shows is real railway data.
> * **Live mode needs a backend you provide.** No railway data provider I could find documents
>   *all* of running status, seat availability and PNR with a published response schema, and the
>   providers' terms rule out shipping their key inside an APK. So the app talks to a small data
>   service **you** run, using the contract in [`docs/PROVIDER_SETUP.md`](docs/PROVIDER_SETUP.md).
>   No third-party provider is wired in, and none of the live-mode network path has been exercised
>   against a real service.
> * **Build status.** See [Verification status](#verification-status) — the Android build itself has
>   not been run by the author of this project scaffold.

## Features

| Area | What it does |
|---|---|
| Home | Logo, search by 5-digit train number or train name, shortcuts to status / seats / PNR, recent searches, favourites, loading / empty / error states |
| Live running status | Train name & number, status text, last reported station, previous / current / upcoming stations, scheduled and expected times, delay, expected destination arrival, journey date, last-updated time, refresh, stale-data warning |
| Seat availability | Source / destination station codes, date, class, quota; per-train departure, arrival, duration, per-class status text **exactly as the provider reports it**, fare when supplied |
| PNR status | 10-digit PNR; status, train, date, boarding / destination, per-passenger booking and current status, coach / berth, chart status. Never stored, never logged |
| Favourites & history | Room-backed. Add / remove favourites, clear history, last successful refresh shown per favourite |
| Offline | Last successful running status is cached in Room and shown when the network fails, marked "Saved data from <time>" |

The app never estimates or fabricates positions, arrival times or seat counts. A field the data
service does not send is shown as "Not reported".

## Requirements

* Android Studio Ladybug (2024.2.1) or newer, using the bundled JDK 17
* Android SDK Platform 35 (Android Studio offers to install it on first sync)
* An emulator or device on Android 8.0 (API 26) or newer

## Open and run

1. **File ▸ Open…** and choose this folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync. Android Studio creates `local.properties` with your SDK path.
3. Pick the `app` configuration and press **Run**. The app starts in sample-data mode.

Command line (needs `ANDROID_HOME` pointing at an SDK):

```bash
./gradlew :app:assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:installDebug           # install on a connected device / emulator
```

## Configuration

Copy `local.properties.example` to `local.properties` (git-ignored) or use environment variables:

| Key | Purpose |
|---|---|
| `RAIL_API_BASE_URL` | `https://` base URL of your data service. Blank → sample-data mode. Anything that is not `https://` is ignored and also falls back to sample mode, so the app can never silently use cleartext |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Release signing, see [`docs/RELEASE_AAB.md`](docs/RELEASE_AAB.md) |

**Never put a railway provider's secret key in `local.properties` or `BuildConfig`.** Anything in an
APK can be extracted. Keep provider keys on your backend.

### Sample-data mode shortcuts

| Input | Result |
|---|---|
| Train `12951`, `12301`, `12002`, `12627`, `12723` | Sample running status |
| Train `00000` / `99999` | "Not found" / "Rate limited" error states |
| PNR starting with `0` / `9999999999` | "Not found" / "Rate limited" |
| Seat class `1A` | Empty result ("No trains found") |
| Any other PNR / stations | Sample result |

## Connecting a real data provider

See [`docs/PROVIDER_SETUP.md`](docs/PROVIDER_SETUP.md): the exact endpoint contract, JSON schemas,
error-code mapping, a survey of the providers I looked at (with what their public docs do and do
not state), security guidance, and a checklist to complete before publishing.

## Project layout

```
app/src/main/java/com/railsathi/app/
  data/
    local/{dao,database,entity}   Room: SavedTrainEntity, TrainSearchHistoryEntity, CachedTrainStatusEntity
    remote/{api,dto}              Retrofit RailApi, MockRailApi (sample data), RetryInterceptor, DTOs + mappers
    repository/                   Repository implementations (network + cache coordination)
    network/                      ConnectivityObserver (offline banner)
  domain/{model,repository,usecase}   Pure Kotlin models, interfaces, use cases, Validators
  di/                             Hilt modules (database, network, repositories)
  ui/{home,runningstatus,availability,pnr,favourites,history,privacy,components,navigation,theme}
  util/                           Formatters, TimeProvider (testable clock; journeys use IST)
app/src/test/                     JVM unit tests + JSON fixtures in src/test/resources/mock
app/src/androidTest/              Room, offline-cache, Compose screen and navigation tests
```

Design notes:

* Screens are split into a `…Route` (wires the Hilt ViewModel) and a stateless `…Screen` that takes
  immutable UI state, which is what the Compose tests render.
* ViewModels expose `StateFlow`; screens collect with `collectAsStateWithLifecycle`.
* Room schema is exported to `app/schemas/` (commit it). Version 1 has no migrations yet; see
  `Migrations` for how to add one. Destructive migration is deliberately not enabled.
* HTTP: HTTPS only (network security config), 10 s connect / 15 s read / 30 s call timeouts, GET
  retries on connection errors and 502/503/504 only, no logging interceptor. Live data is not
  HTTP-cached; the Room cache is the single offline source.
* Seat availability is never cached; stale "AVAILABLE" would mislead. PNR results are held in
  memory only.

## Tests

```bash
./gradlew :app:testDebugUnitTest            # JVM: validators, DTO parsing, repositories, ViewModels, Retrofit/OkHttp via MockWebServer
./gradlew :app:connectedDebugAndroidTest    # needs an emulator/device: Room DAOs, offline cache, Compose screens, navigation
```

Instrumented tests replace the network and database with the sample API and an in-memory database
(`androidTest/.../di/TestModules.kt`), so they need no credentials and never touch real saved data.

## Verification status

What has and has not been verified for this project:

* **Not run:** `./gradlew assembleDebug`, `bundleRelease`, the Gradle unit-test task, and every
  instrumented test. The environment this was prepared in could not reach Google Maven, Maven
  Central or the Android SDK downloads, so no Android build was possible.
* **Compiled and run (JVM only):** the domain, mapping, repository, sample-API and ViewModel code
  plus the 64 unit tests in `app/src/test` (all except `RailApiNetworkTest`, which needs
  MockWebServer), compiled with Kotlin 2.0.21 and run under JUnit 4 against small stand-in stubs for
  Room, Retrofit, OkHttp, Hilt and `kotlinx-coroutines-test`. This catches type and logic errors in
  those layers but is **not** a substitute for the real build. This pass found and fixed a
  null-safety compile error and four incorrect tests.
* **Never compiled:** all Jetpack Compose UI, Hilt wiring, Room generated code, Gradle scripts and
  the instrumented tests. Expect to fix a few small errors on first sync — please report or fix them.
* **Library versions** (AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.10.01,
  Hilt 2.52, Room 2.6.1, Retrofit 2.11.0, OkHttp 4.12.0) were chosen as a mutually compatible set
  from memory and could not be resolved against the repositories. Update them in
  `gradle/libs.versions.toml` as you like; keep Kotlin and KSP in lock-step.

## Known limitations

* No railway data provider is integrated; live mode needs your own backend (see above).
* Station search / autocomplete is not implemented: stations are entered as codes (e.g. `NDLS`).
  Train **name** search needs the backend's `GET /v1/trains/search`.
* Class and quota options are static lists; the app does not discover what a provider supports.
* Fares are shown only when the data service sends them.
* The launcher icon and wordmark are placeholders; replace them before publishing.
* English only; strings are in Kotlin source rather than `strings.xml`, which should be moved
  before localising.

## Privacy and legal

* [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) — publish it at a public URL for Play Console.
* RailSathi is not affiliated with Indian Railways, IRCTC or CRIS. Say so in the store listing.
* Check your data provider's terms (commercial use, caching / retention, redistribution) before
  publishing. I could not read every provider's full terms; see `docs/PROVIDER_SETUP.md`.

## Releasing

[`docs/RELEASE_AAB.md`](docs/RELEASE_AAB.md): create an upload key, sign, build the Android App
Bundle, and the Play Console checklist.
