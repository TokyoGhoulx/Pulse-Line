# PulseLine (Android app, Java)

Smarter Machines. Fewer Surprises. A working prototype of the PulseLine retrofit kit: it learns what normal looks like for each machine, warns the team in plain language before a failure, learns from operator feedback, and shows the ROI.

## How the app maps to your slides

| Slide idea | Where it lives in the app |
|---|---|
| 1 Sense (temperature, vibration, current) | Simulated sensor kit per machine (`engine/MachineSimulator`). **"Use this phone as a machine"** uses the phone's real accelerometer and battery temperature (`PhoneSensorSource`). |
| 2 Process (edge gateway spots abnormal patterns) | `engine/AnomalyDetector`: learns a 30 s baseline, then scores each reading. `HealthTrend` estimates time to failure. |
| 3 Alert (plain-language message to the operator's phone) | `engine/AlertWriter` + Android notifications (`Notifier`). Every alert says what is wrong, why, and what to do. |
| 4 Learn (outcomes feed back) | "Useful" / "False alarm" buttons change that machine's alert sensitivity. |
| Pain point: unplanned downtime | Predictive alerts, "Schedule repair" before the machine stops. |
| Pain point: steep training curve | Guided setup dialog, plain-language alerts. |
| Pain point: upfront cost / proving ROI | **ROI tab**: downtime avoided, cost of breakdowns, pilot cost, net value, editable assumptions. |
| Roadmap (Pilot, Refine, Scale) and safeguards | **Roadmap tab** with checklists and a live pilot summary. |

## Demo script (2 minutes)

1. Open the app. Three machines start by learning for about 30 seconds.
2. Tap **Motor 1**, press **Bearing wear**. Health falls, a "Check soon" alert appears, then "Act now".
3. In the alert press **Schedule repair**. The fault is cleared and the ROI tab shows downtime avoided.
4. Do the same on **Pump A** but do nothing. After about two minutes it stops (unplanned breakdown).
5. Mark an alert **False alarm**, then rerun the same fault: it alerts later (less sensitive).
6. Optional: **Use this phone as a machine**, leave it on a table for 30 s, then shake it.

Demo speed: 1 second = 1 minute of plant time.

## Make the APK

### Option A: Android Studio (easiest)
1. Install Android Studio (free) and choose **Open**, then select this `PulseLine` folder. Let Gradle sync finish.
2. Plug in a phone with USB debugging, or start an emulator, and press **Run** to test.
3. **Build > Build Bundle(s) / APK(s) > Build APK(s)**. When it finishes click **locate**.
   The file is `app/build/outputs/apk/debug/app-debug.apk`.

### Option B: command line
Needs JDK 17 and the Android SDK (`ANDROID_HOME` set; platform 35 and build-tools installed):

    ./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug

### Option C: no installation, build in the cloud
1. Create a free GitHub repository and upload this whole folder (keep the `.github` folder).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push).
3. Open the finished run and download the **PulseLine-debug-apk** artifact (a zip containing `app-debug.apk`).

### Install on a phone
Copy `app-debug.apk` to the phone, open it, and allow "Install unknown apps" when asked. Or with USB debugging: `adb install app-debug.apk`.

### A signed release APK (to share widely)
Android Studio: **Build > Generate Signed App Bundle / APK > APK**, create a keystore (keep the file and passwords safe), choose **release**, finish. Output: `app/build/outputs/apk/release/app-release.apk`.

## Check the detection logic without Android

    sh engine-check/run.sh

Runs 240 healthy machines (expects zero false alarms), injects each fault type, and checks early warning, escalation, feedback learning, phone mode and ROI maths.

## Honest limits of this prototype
- The kit is simulated (plus the phone's own sensors). A real deployment would read an ESP32 or similar clip-on sensor over Bluetooth/Wi-Fi; only `MachineSimulator` and `PhoneSensorSource` would change, the rest of the app stays the same.
- Monitoring runs while the app process is alive. For all-day background monitoring you would add a foreground service or a gateway that pushes alerts.
- Anomaly detection is a statistical baseline (smoothed z-score), not a trained machine-learning model. It is explainable, light enough for an edge device, and tuned by operator feedback.
- ROI defaults (rupee amounts) are illustrative. Pricing is not fixed in the slides, so edit them in the ROI tab.
- Data is stored only on the phone. No network access is used.
