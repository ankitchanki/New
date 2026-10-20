# Install and test on your Windows-connected phone

The Codex cloud workspace is a separate machine. A phone connected to your laptop's USB/ADB is available to the laptop, not automatically to this cloud session. This session has no USB passthrough or reachable local ADB server; it cannot directly install on that phone.

## Windows helper

Download [Test-StashDrop.ps1](https://raw.githubusercontent.com/ankitchanki/StashDrop/main/scripts/Test-StashDrop.ps1?v=0.7.0) to a folder on the laptop with ADB. Open PowerShell in that folder and run:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\Test-StashDrop.ps1
```

The execution policy applies to that PowerShell process only. No administrator access or permanent policy change is required. The helper uses ADB from PATH or the standard Android Studio SDK location. Unlock the phone and approve USB debugging if prompted, then rerun if it is not yet authorized.

It downloads the official signed **0.7.0 APK**, checks its pinned SHA-256, installs with `adb install -r`, launches StashDrop, and records a **three-minute** test session. Approve any USB-install confirmation shown on the phone. It never uninstalls the app, clears data, clears the phone's log buffers, enables privileges, changes your assistant, opens an ADB network listener, or uploads logs.

Examples for a custom ADB location or multiple devices:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\Test-StashDrop.ps1 -AdbPath 'C:\platform-tools\adb.exe'

adb devices -l
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\Test-StashDrop.ps1 -Serial 'YOUR_PHONE_SERIAL' -Seconds 300
```

To use an already downloaded official APK:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\Test-StashDrop.ps1 -ApkPath "$env:USERPROFILE\Downloads\StashDrop-v0.7.0.apk"
```

The helper pins the independently verified published APK hash. Custom/debug APKs are deliberately rejected. An installation signed with a different key cannot be updated in place; the helper stops rather than uninstalling it and deleting its shelf.

## What to test during recording

1. Show the edge shelf. Tap or pull inward from the single handle to open its rectangular panel. Close with Back/Close/outside touch, and verify scrolling/tapping another app works with the handle enabled.
2. Import distinct images, stickers and videos; inspect previews and Images/Videos/Stickers/URLs/OTP filters. Drag a card onto a compatible app icon and verify the receiving composer. Swipe a disposable card for the red trash affordance, then test cancel/confirm deletion.
3. Choose Google as digital assistant, then test the search button, handle hold and notification. Record whether Google shows Circle to Search, Gemini or a rejection. No internal capture screen should appear.
4. Enable the transparent strip and start above system navigation. Test native corner gestures through assistant settings. Check central upward swipe, rotation, keyboard and lock/unlock. Physical bottom-edge swipes remain Android Home/Recents gestures.
5. If Shizuku is in use, verify reconnect behavior and record whether connecting the bridge changes the Google request result.

See the [full device checklist](DEVICE_VALIDATION.md) for additional cases. If the app displays its own crash report, copy it manually too; release-app private files are not accessible to normal ADB.

## Logs and next steps

A timestamped `StashDrop-device-...` folder and ZIP appear in the current directory. They contain session/device model and Android version, app process logs, selected crash/system tags, installed version, overlay access, assistant route, and available app exit information. The logger follows a changed app PID if the app restarts.

Review/redact logs before sharing: diagnostic messages can contain private content. Nothing is sent automatically. Share the ZIP with the exact action that failed and expected/actual behavior so the issue can be diagnosed. No signing credentials or private shelf files are collected.

## Manual ADB alternative

After downloading the [official signed APK](https://github.com/ankitchanki/StashDrop/releases/download/v0.7.0/StashDrop-v0.7.0.apk):

```powershell
adb devices -l
adb install -r "$env:USERPROFILE\Downloads\StashDrop-v0.7.0.apk"
adb shell am start -n com.vibe.stashdrop/.MainActivity
$appProcessId = [string](adb shell pidof -s com.vibe.stashdrop)
$appProcessId = $appProcessId.Trim()
if ($appProcessId -notmatch '^\d+$') { throw 'StashDrop is not running. Check the phone for a crash report.' }
adb logcat --pid=$appProcessId -v threadtime > stashdrop-app.log
```

Use the phone, then press Ctrl+C to stop manual logging. This manual PID filter does not follow a process restart; the helper records crashes separately and reattaches when a new PID appears.

The helper was syntax-checked with PowerShell and exercised against a simulated ADB endpoint for successful install/capture, missing/multiple devices, checksum mismatch, and incompatible signing. Those checks validate the helper; an actual Windows/phone session remains necessary.
