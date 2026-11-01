# Lift Log — Android v1.9 + Wear OS v1.8

An offline gym weight log with 121 exercises, 13 muscle groups, two photos or labeled illustrations per exercise, searchable lists, dated weight entries in lb/kg, hide/restore controls, and per-exercise progress graphs.

## Install
[Download the phone APK (v1.9)](https://github.com/unavailable3297/LiftLog/raw/refs/heads/main/downloads/LiftLog.apk). Open it on an Android phone (Android 8 or newer). If prompted, allow installation from the app you used to open this APK. No Lift Log account is needed. Phone logging works offline; optional watch sync uses Google Play services. This is a personal-use development build, not a Play Store release.

## Use
Choose a muscle group, tap an exercise, enter the weight, and tap Save weight. The latest weight and previous entries appear on that exercise. Hide exercise removes it from normal browsing; Hidden on the home screen lets you restore it. Hiding does not delete history. A mistaken entry can be deleted after confirmation.

For dumbbells, record the weight of one dumbbell. For barbells, include the bar. For bodyweight movements enter added weight, including zero. For assistance-based movements record the amount of assistance. Switching lb/kg changes the unit for the next entry; it does not convert existing entries.

Everything is stored on your phone. Uninstalling or clearing storage removes your entries. Future APK updates must retain application ID com.axs.gymtrack and the same signing key to update in place. The original signing key is excluded from this public repository. Keep it in a private backup and restore it as `development.keystore` in the project root before building an update. Both phone and watch modules must use that same key. Do not replace it when updating an existing installation.

## Edit on Windows
1. Clone or download this repository.
2. Restore your private `development.keystore` to the project root, then open this folder in Android Studio.
3. Use JDK 17 (Android Studio's bundled compatible JDK is also suitable).
4. Let Gradle sync and install SDK Platform 35 when prompted.
5. Run on a connected phone or use Build > Generate App Bundles or APKs > Generate APKs (menu wording may vary).

Command line: `gradlew.bat assembleDebug` (first build needs internet for Gradle and Android build tools).

The build expects a local keystore with alias `androiddebugkey` and the development store/key password `android`. For an entirely new installation only, create your own local key with JDK 17 `keytool`:

```powershell
keytool -genkeypair -keystore development.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Lift Log Development"
```

For existing installations, restore the original key instead. The keystore is ignored by Git; never commit it. The prebuilt APK in `downloads/` is already signed with the original key.

The UI is bundled HTML/CSS/JavaScript in app/src/main/assets, hosted by a small Java Android activity. The phone and watch now use Google Play services Wearable Data Layer 20.0.1 for optional sync. There is no Lift Log backend or analytics. SharedPreferences stores the JSON data via a local JavaScript bridge. Browser preview uses localStorage instead and has separate data from the phone.

Files:
- app/src/main/assets/app.js: interaction and logging
- app/src/main/assets/style.css: appearance
- app/src/main/assets/catalog.js: exercise catalog
- app/src/main/assets/images: bundled exercise photos
- app/src/main/java/com/axs/gymtrack/MainActivity.java: Android wrapper and local storage

For a browser preview, serve app/src/main/assets using a local HTTP server. No npm install is required.

## Build and validation
The delivered APK was compiled against Android SDK 35, with minimum Android 8 and target SDK 34. APK v2/v3 signature verification passed. Browser-level behavior tests cover logging, persistence, hide/restore, deletion, units, search, images, and narrow-screen layout. No Android emulator or physical phone was available for device testing. Version 1.6 is built with the included Gradle wrapper (AGP 8.7.3, JDK 17, compile/build tools 35). The old direct SDK build script now delegates to Gradle so dependency resources and manifests are merged correctly.

## Photos and data
Exercise names and images come from Free Exercise DB by yuhonas:
https://github.com/yuhonas/free-exercise-db
The source repository declares the dataset public domain under the Unlicense; its license is included in DATA-LICENSE.txt. Photos are identification references, not personalized exercise coaching. The curated catalog covers common gym movements and is not an exhaustive catalog of all possible exercises.

This version intentionally omits reps/sets, workout routines, automatic progression suggestions, rest timers, notifications, and cloud backup.


## Version 1.3 update
Both home-screen widgets and the in-app Add widget button have been removed. The main app still provides exercise photos, muscle groups, weight history, and hide/restore controls.

Install this APK over the existing app without uninstalling. The application ID and signing certificate are unchanged. The same local history store and data format are retained, including weights previously saved from a widget. Widget-only settings are no longer used.

Validation: clean APK build and signature checks, compiled manifest inspection (no widget receivers/configuration), and browser checks for loading existing entries, saving, reopening, and hide/restore. No physical-phone or emulator test was available.


## Version 1.4 — expanded exercise catalog
Added machine lateral raises and single-arm machine laterals; straight-bar cable overhead triceps extensions; single-arm rope pushdowns and overhead extensions; single-arm handle pushdowns in overhand and underhand variants; single-arm handle and dumbbell overhead extensions. Single-arm machine laterals apply to machines that permit independent-arm operation.

The original Triceps Pushdown entry is now labeled Triceps Pushdown - Straight Bar. Its ID and history are unchanged. The original dumbbell kickback is labeled Single-Arm for clarity. Existing rope, V-bar and other entries retain their original IDs. New attachment variations have independent histories. Some new exercises use original schematic illustrations labeled Illustration; other additions use the existing public-domain photo dataset.

Install this update over your existing app. Do not uninstall or clear app storage. Application ID and signing certificate match prior versions; all 111 original exercise IDs and the native persistence code are unchanged. Browser tests seeded every original exercise with a saved weight and verified those exact entries, units and hidden settings after adding new entries and reopening. Signature checks passed. This build was not tested on a physical Android phone.


## Version 1.5 — all logged weights
Tap the home-screen weights-logged count to see every saved weight and date, grouped by exercise with the most recently logged exercises first. Includes hidden exercises, search, and a link to each exercise to add or manage entries. The count now includes hidden exercises with history. Native storage, exercise IDs and signing key are unchanged. Install over the existing app to retain data.


## Version 1.6 — Wear OS companion
Install app/build/outputs/apk/debug/app-debug.apk on the phone as an in-place update.
Install wear/build/outputs/apk/debug/wear-debug.apk on the paired Wear OS 3+ watch.
Both use application ID com.axs.gymtrack and the original development.keystore.
The watch APK must be installed separately; it is not installed by opening it on the phone.
See watch-install/START-HERE.txt and the Windows installer scripts.
Those instructions describe the separately provided watch setup bundle. When
working from this repository, build the wear module, copy its APK to
`watch-install/LiftLog-Watch.apk`, and place the official Windows Android
Platform-Tools in `watch-install/platform-tools/` before running the installer.
Phone v1.9 works with the existing watch v1.8 companion.

The native watch UI opens on previously logged exercises, most recent first,
including saved history for hidden exercises. Tap to see last weight/date,
adjust using minus/plus or exact numeric entry, and save. The increment is
adjustable and units can be converted explicitly. Rotary events scroll lists.
The secondary all-exercises browser respects hidden flags from the phone.

The phone has a Watch companion setup/sync page. Phone history remains in the
same liftlog SharedPreferences state JSON, with the unchanged LogStore revision
guard. All 120 catalog IDs are unchanged. Watch imports are appended under the
same lock; stale phone writes are rejected and refreshed instead of overwriting
an imported watch entry. If a watch entry arrives while editing a phone form,
the typed value is kept until Save; a stale Save asks you to re-enter/save after
refreshing history.

### Sync behavior
- shared/SyncModel.java: pure validation, append, snapshot and receipt rules.
- PhoneSync accepts each watch UUID once, commits history and seen IDs atomically,
  then publishes a latest-per-exercise snapshot and a durable receipt.
- WatchStore commits to its local outbox before showing success. Data Layer
  publication is retried on opening/syncing and subsequent incoming events.
- WatchSync retains pending entries until a receipt AND a sufficiently new phone
  snapshot are stored. Out-of-order deliveries cannot clear an unsynced entry.
- A persisted cleanup queue removes acknowledged Data Layer items safely after
  local reconciliation. The phone keeps seen IDs even after history deletion,
  so retries cannot resurrect deleted watch entries.
- Snapshots include revision and phone installation epoch. Within an epoch older
  snapshots are ignored. The watch keeps only the latest weights plus pending
  entries; full history and deletion stay on the phone.
- No full history is transmitted and catalog images are not sent to the watch.
- No automatic data reset or history migration occurs. Clearing storage/uninstall
  is not supported as a synchronization or recovery workflow.

Data Layer may route data through Google's encrypted relay when Bluetooth is
unavailable. This is device synchronization, not a cloud backup.

### Build and tests
JDK 17, Android SDK Platform 35 / Build Tools 35.0.0:
```
./gradlew :app:assembleDebug :wear:assembleDebug :app:testDebugUnitTest :wear:testDebugUnitTest
```
On Windows use gradlew.bat. Install SDK licenses through Android Studio/SDK Manager.
The browser regression tests in tests/phone-ui.cjs and tests/progress.cjs require
Node, Playwright and a Chromium executable via CHROMIUM_PATH. It uses an isolated in-memory test fixture,
never user data. Robolectric tests cover phone persistence and native watch UI.
Real phone/watch pairing, Samsung hardware behavior, and the Windows installer
have not been exercised on physical devices in this environment.


## Version 1.7 — single-arm cable bicep curl
Added Single-Arm Cable Bicep Curl under Biceps on both phone and watch, with two offline photos on the phone. Search aliases include single arm, one arm, bicep and biceps. Record the weight used for one arm. All 120 previous exercise records and IDs are unchanged, as are the storage format and signing key. Install over the current phone/watch apps; do not uninstall or clear storage.


## Version 1.8 — clearer single-arm cable lateral raise
The existing Standing Low-Pulley Deltoid Raise is now named Single-Arm Cable Lateral Raise under Shoulders on both phone and watch. Its two photos and stable exercise ID are retained so existing weights stay attached to it. Search supports cable lateral raise, one arm, single arm and the original name. The single-arm weight hint is shown. No duplicate exercise or data migration is introduced; the catalog still contains 121 exercises.


## Version 1.9 — exercise progress graphs
Each phone exercise detail now shows a read-only graph below the Save weight form. It plots every saved entry chronologically, with an All time, 90 days or 30 days view. The time axis uses actual recording times and the weight axis starts at zero. Tap the graph or use Previous/Next to inspect exact dates and logged values. The First, Latest and Change summaries follow the selected date range. Graph unit selection converts values for display only; original entry values, units and IDs are untouched. Single-entry and no-entry states explain what is available. This phone-only update is compatible with the existing 1.8 watch companion; the sync protocol and watch files are unchanged.
