#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
bash gradlew :app:assembleDebug :wear:assembleDebug
mkdir -p build
cp app/build/outputs/apk/debug/app-debug.apk build/LiftLog.apk
cp wear/build/outputs/apk/debug/wear-debug.apk build/LiftLog-Watch.apk
