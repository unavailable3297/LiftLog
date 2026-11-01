$ErrorActionPreference = 'Stop'
$adb = Join-Path $PSScriptRoot 'platform-tools\adb.exe'
$apk = Join-Path $PSScriptRoot 'LiftLog-Watch.apk'
function Run-Adb {
    param([string[]]$Arguments)
    & $adb @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'ADB command failed. Check the watch connection and try again.' }
}
function Endpoint {
    param([string]$Prompt)
    $value = (Read-Host $Prompt).Trim()
    if ($value -notmatch '^(\d{1,3}\.){3}\d{1,3}:\d{1,5}$') { throw 'Enter the IP address and port shown on your watch, like 192.168.1.20:38901.' }
    return $value
}
try {
    Write-Host 'LIFT LOG - WATCH INSTALLER' -ForegroundColor Green
    Write-Host 'First install LiftLog.apk 1.8 on your PHONE over the existing app.'
    Write-Host 'This installer installs LiftLog-Watch.apk only on a Wear OS WATCH.'
    Write-Host 'Connect this Windows PC and the watch to the same Wi-Fi.'
    Write-Host 'On the watch, enable Developer options, ADB debugging and Wireless debugging.'
    Write-Host 'See START-HERE.txt for the full steps.'
    if (!(Test-Path $adb) -or !(Test-Path $apk)) { throw 'Extract the entire setup ZIP before running this installer.' }
    $pair = Read-Host 'Does your watch show Pair new device? Enter Y for yes, N for older Debug over Wi-Fi'
    if ($pair -match '^[Yy]') {
        Write-Host 'On the watch choose Wireless debugging > Pair new device.'
        $pairEndpoint = Endpoint 'Pairing IP:port from that screen'
        Write-Host 'Enter the pairing code when ADB asks below.'
        Run-Adb -Arguments @('pair', $pairEndpoint)
        Write-Host 'Now go BACK to the main Wireless debugging screen.'
        Write-Host 'Use the connection port there, which is DIFFERENT from the pairing port.'
    }
    $target = Endpoint 'Watch connection IP:port'
    Run-Adb -Arguments @('connect', $target)
    Write-Host 'If the watch asks to allow debugging, allow it now.'
    Read-Host 'Press Enter once the watch has allowed the connection'
    $features = & $adb -s $target shell pm list features 2>&1
    if ($LASTEXITCODE -ne 0 -or !($features -match 'feature:android.hardware.type.watch')) {
        throw 'The connected device was not verified as a watch. Installation stopped to protect the phone app.'
    }
    Run-Adb -Arguments @('-s', $target, 'install', '-r', $apk)
    Run-Adb -Arguments @('-s', $target, 'shell', 'am', 'start', '-n', 'com.axs.gymtrack/.WatchActivity')
    Write-Host 'Installed. Open Lift Log on your PHONE, then tap Sync with phone on the WATCH.' -ForegroundColor Green
    Write-Host 'After your exercises appear, turn off ADB and Wireless debugging on the watch. Normal sync does not need debugging.'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
    Read-Host 'Press Enter to close'
}
