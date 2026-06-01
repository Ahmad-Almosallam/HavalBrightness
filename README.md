# Haval Brightness

A tiny Android app for a **Haval large-screen head unit (Android 9 / Pie, API 28)** that sets
the screen brightness **once at boot** based on the time of day: full brightness during the
day, near-minimum at night.

It does its one job in a `BroadcastReceiver` triggered by `BOOT_COMPLETED` and goes back to
sleep — no foreground service, no background polling, no UI shown after the one-time setup.

---

## Why Android 9 specifically

The Haval head unit ships with Android 9. The app is pinned to `targetSdk 28` so behavior on
the device matches what's tested. Brightness writes use `Settings.System.putInt` with the
`WRITE_SETTINGS` special permission, which has worked unchanged since API 23 and still works
on Android 9 — there's no API-level deprecation concern here, just a "don't bump
`targetSdk` without retesting on the actual head unit" rule.

---

## How it works

```
BOOT_COMPLETED ──► BootReceiver.onReceive ──► BrightnessAdjuster.apply(context)
                                                       │
                                                       ├─ Settings.System.canWrite? ── no ──► log & return
                                                       │
                                                       ├─ hour = Calendar.HOUR_OF_DAY
                                                       │
                                                       ├─ value = (hour in 6..18) ? 255 : 10
                                                       │
                                                       ├─ SCREEN_BRIGHTNESS_MODE = MANUAL
                                                       └─ SCREEN_BRIGHTNESS      = value
```

The decision is a pure function of `hour`: `[6, 19)` is daytime → 255, everything else is
night → 10. The mode is forced to MANUAL first so the OS's auto-brightness handler doesn't
race and overwrite the value.

| File | Role |
|------|------|
| `util/BrightnessAdjuster.kt` | day/night rule + the single `Settings.System` write (unit-tested) |
| `boot/BootReceiver.kt` | listens for `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED`, calls the adjuster |
| `ui/MainActivity.kt` | one-screen UI: grant permission, see current state, "Apply now" button |

---

## Build & install

### Option A — Android Studio (recommended)
1. Open the `HavalBrightness` folder in **Android Studio** (Giraffe or newer).
2. Let it sync Gradle. It will generate the Gradle wrapper JAR automatically if missing.
3. Connect the Haval head unit over ADB (or an **API 28** emulator) and click **Run**.

### Option B — command line
The Gradle **wrapper JAR is not checked in** (it's a binary). Generate it once with a local
Gradle install, then use the wrapper:

```bash
gradle wrapper            # one-time: creates gradlew + gradle-wrapper.jar
./gradlew assembleDebug   # build the APK -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run the unit tests
The day/night logic is pure JVM code — no device needed:

```bash
./gradlew testDebugUnitTest
```

---

## First-run configuration

1. **Launch** "Haval Brightness".
2. Tap **Grant permission** — this opens *Settings → Apps → Special access → Modify system
   settings*. Flip the toggle for Haval Brightness, then press back.
3. (Optional) Tap **Apply now** to verify the brightness changes as expected.
4. That's it — at every subsequent boot the app will set the brightness automatically.

### Why "Modify system settings" instead of a normal permission?
`WRITE_SETTINGS` is a **special** permission on Android 6+: it can't be granted from a
runtime-permission dialog, only from the per-app screen in system settings. There is no API
to grant it programmatically — the user has to flip the toggle. The Activity launches that
screen for you with `Settings.ACTION_MANAGE_WRITE_SETTINGS`.

---

## Tuning the thresholds

The two windows and two brightness values live as `const val`s in `BrightnessAdjuster.kt`:

```kotlin
const val DAY_START_HOUR  = 6     // inclusive
const val DAY_END_HOUR    = 19    // exclusive
const val DAY_BRIGHTNESS  = 255   // 0..255
const val NIGHT_BRIGHTNESS = 10
```

Edit, rebuild, reinstall. The unit tests cover the boundary cases, so changing the threshold
should immediately surface mistakes in `BrightnessAdjusterTest`.

---

## Limitations

- **One-shot at boot.** If the head unit stays powered on past sunset, brightness will not
  update until the next reboot. (By design — extending this to react to day/night transitions
  during use would need an `AlarmManager` or `WorkManager` job.)
- **No location / no light sensor.** The day/night decision is a fixed hour window; it does
  not follow real sunrise/sunset, season, or actual cabin lighting.
- **`WRITE_SETTINGS` is user-granted.** If the user never opens the app to grant the special
  permission, the BootReceiver will fire and log a warning but won't change anything.
