# Installs the verified official APK and captures a bounded, local diagnostic session.
# Compatible with Windows PowerShell 5.1 and PowerShell 7. No administrator/root access required.
[CmdletBinding()]
param(
    [string]$Serial,
    [string]$AdbPath,
    [string]$ApkPath,
    [ValidateRange(1, 1800)][int]$Seconds = 180,
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$Package = 'com.vibe.stashdrop'
$Version = '0.7.0'
$ExpectedHash = '955bcf2649b7b3cffe2396b614877c433e965ca91422d4c8009bbb69bd796646'
$DownloadUrl = "https://github.com/ankitchanki/StashDrop/releases/download/v$Version/StashDrop-v$Version.apk"

if (-not $AdbPath) {
    $found = Get-Command adb -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $AdbPath = $found.Source }
    elseif ($env:LOCALAPPDATA -and (Test-Path (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))) {
        $AdbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    }
    elseif ($env:ANDROID_HOME -and (Test-Path (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'))) {
        $AdbPath = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    }
    else { throw 'ADB was not found. Pass -AdbPath C:\path\to\platform-tools\adb.exe.' }
}
$AdbExe = (Resolve-Path -LiteralPath $AdbPath).Path
function Read-Adb {
    param([string[]]$Arguments, [switch]$AllowFailure, [switch]$HostCommand)
    $commandArguments = $Arguments
    if (-not $HostCommand) { $commandArguments = @('-s', $script:Serial) + $Arguments }
    # ADB can print normal daemon-startup messages to stderr. Preserve the native exit code.
    $oldPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $result = & $script:AdbExe @commandArguments 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally { $ErrorActionPreference = $oldPreference }
    $text = ($result | ForEach-Object { $_.ToString() }) -join "`n"
    if ($exitCode -ne 0 -and -not $AllowFailure) { throw "ADB failed: $text" }
    return $text.Trim()
}

$deviceText = Read-Adb -Arguments @('devices', '-l') -HostCommand
$online = @($deviceText -split "`r?`n" | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' } | ForEach-Object { ($_ -split '\s+')[0] })
if ($Serial) {
    if ($Serial -notin $online) { throw "Selected device is not authorized/online. Unlock the phone and accept USB debugging, then retry.`n$deviceText" }
}
elseif ($online.Count -eq 1) { $Serial = $online[0] }
elseif ($online.Count -eq 0) { throw "No authorized ADB device. Connect/unlock the phone and accept its USB debugging prompt.`n$deviceText" }
else { throw "Multiple devices are connected. Run again with -Serial followed by your phone's serial.`n$deviceText" }

$model = Read-Adb -Arguments @('shell', 'getprop', 'ro.product.model')
$manufacturer = Read-Adb -Arguments @('shell', 'getprop', 'ro.product.manufacturer')
$android = Read-Adb -Arguments @('shell', 'getprop', 'ro.build.version.release')
$sdkText = Read-Adb -Arguments @('shell', 'getprop', 'ro.build.version.sdk')
$sdk = 0
if (-not [int]::TryParse($sdkText, [ref]$sdk) -or $sdk -lt 28) { throw 'StashDrop requires Android 9 / API 28 or newer.' }
Write-Host "Phone: $manufacturer $model / Android $android"

if (-not $ApkPath) {
    $ApkPath = Join-Path ([IO.Path]::GetTempPath()) "StashDrop-v$Version.apk"
    if (-not (Test-Path -LiteralPath $ApkPath) -or (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $ExpectedHash) {
        Write-Host 'Downloading the official signed APK...'
        [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri $DownloadUrl -OutFile $ApkPath
    }
}
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path
if ((Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $ExpectedHash) {
    throw 'APK checksum mismatch. This helper only installs the independently verified official 0.7.0 release.'
}

if (-not $OutputDirectory) { $OutputDirectory = Join-Path (Get-Location).Path ('StashDrop-device-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
if ((Test-Path -LiteralPath $OutputDirectory) -and @(Get-ChildItem -LiteralPath $OutputDirectory -Force).Count -gt 0) {
    throw 'Choose an empty output directory so previous reports are preserved.'
}
$null = New-Item -ItemType Directory -Path $OutputDirectory -Force
$OutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path
$reportFiles = New-Object 'System.Collections.Generic.List[string]'
$logProcesses = New-Object 'System.Collections.Generic.List[System.Diagnostics.Process]'
$seenPids = New-Object 'System.Collections.Generic.HashSet[string]'
$metadata = [ordered]@{
    Started = (Get-Date).ToString('o'); AppVersion = $Version; ApkSha256 = $ExpectedHash
    Manufacturer = $manufacturer; Model = $model; Android = $android; Api = $sdk
    Install = ''; Launch = ''; SessionError = ''; Completed = ''
}
function Save-Report {
    param([string]$Name, [string]$Content)
    $path = Join-Path $script:OutputDirectory $Name
    $Content | Set-Content -LiteralPath $path -Encoding UTF8
    $script:reportFiles.Add($path)
}
function Start-AdbLog {
    param([string]$Name, [string[]]$Arguments)
    $stdout = Join-Path $script:OutputDirectory ($Name + '.log')
    $stderr = Join-Path $script:OutputDirectory ($Name + '-stderr.log')
    $process = Start-Process -FilePath $script:AdbExe -ArgumentList (@('-s', $script:Serial) + $Arguments) -NoNewWindow -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $script:logProcesses.Add($process)
    $script:reportFiles.Add($stdout); $script:reportFiles.Add($stderr)
}

try {
    Write-Host 'Installing with adb install -r (keeps existing app data; never uninstalls)...'
    $metadata.Install = Read-Adb -Arguments @('install', '-r', $ApkPath)
    if ($metadata.Install -notmatch '(?m)^Success\s*$') { throw "Installation did not report Success: $($metadata.Install)" }
    Start-AdbLog -Name 'system-crashes' -Arguments @('logcat', '-b', 'main', '-b', 'system', '-b', 'crash', '-v', 'threadtime', '-T', '1', 'AndroidRuntime:E', 'libc:F', 'DEBUG:F', 'WindowManager:W', 'ActivityManager:I', 'ActivityTaskManager:I', 'StashDrop:V', '*:S')
    $metadata.Launch = Read-Adb -Arguments @('shell', 'am', 'start', '-n', "$Package/.MainActivity")
    if ($metadata.Launch -match '(?m)^Error:') { throw $metadata.Launch }
    Write-Host "Recording for $Seconds seconds. On the phone: move the single handle between edges, open/close the panel, scroll another app, and try the transparent search strip."
    Write-Host 'Also test previews, filters, swipe-delete confirmation, and dragging a card onto a compatible app icon. Logs stay on this laptop.'
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        $currentPid = Read-Adb -Arguments @('shell', 'pidof', '-s', $Package) -AllowFailure
        if ($currentPid -match '^\d+$' -and $seenPids.Add($currentPid)) {
            Start-AdbLog -Name ("app-pid-$currentPid") -Arguments @('logcat', '--pid', $currentPid, '-b', 'main', '-v', 'threadtime', '-T', '100')
        }
        Start-Sleep -Milliseconds 750
    }
    $installed = Read-Adb -Arguments @('shell', 'dumpsys', 'package', $Package) -AllowFailure
    Save-Report -Name 'installed-version.txt' -Content (($installed -split "`r?`n" | Where-Object { $_ -match 'versionCode=|versionName=' }) -join "`n")
    Save-Report -Name 'overlay-access.txt' -Content (Read-Adb -Arguments @('shell', 'cmd', 'appops', 'get', $Package, 'SYSTEM_ALERT_WINDOW') -AllowFailure)
    Save-Report -Name 'assistant-route.txt' -Content ("Assistant: " + (Read-Adb -Arguments @('shell', 'settings', 'get', 'secure', 'assistant') -AllowFailure) + "`nVoice service: " + (Read-Adb -Arguments @('shell', 'settings', 'get', 'secure', 'voice_interaction_service') -AllowFailure))
    if ($sdk -ge 30) { Save-Report -Name 'app-exit-info.txt' -Content (Read-Adb -Arguments @('shell', 'dumpsys', 'activity', 'exit-info', $Package) -AllowFailure) }
}
catch {
    $metadata.SessionError = $_.Exception.Message
    if ($metadata.SessionError -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
        Write-Warning 'The installed app uses a different signing certificate. Nothing was uninstalled; preserve its shelf before deciding how to migrate.'
    }
    throw
}
finally {
    foreach ($process in $logProcesses) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue; $null = $process.WaitForExit(3000) }
        $process.Dispose()
    }
    $metadata.Completed = (Get-Date).ToString('o')
    Save-Report -Name 'session.json' -Content ($metadata | ConvertTo-Json)
    Save-Report -Name 'READ-ME.txt' -Content "Local StashDrop diagnostic session. Review/redact logs before sharing: app/system logs may contain private content. Nothing was uploaded. The app PID is followed when it restarts. Private app files and signing keys are not collected. An in-app crash report can be copied manually from the dashboard."
    $existing = @($reportFiles | Where-Object { Test-Path -LiteralPath $_ })
    if ($existing.Count -gt 0) {
        $zip = $OutputDirectory + '.zip'
        Compress-Archive -LiteralPath $existing -DestinationPath $zip -Force
        Write-Host "Diagnostics ZIP: $zip"
    }
}
