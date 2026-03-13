#!/usr/bin/env bash

set -e

APP_MODULE="app"
APK_PATH="$APP_MODULE/build/outputs/apk/debug/app-debug.apk"

echo "===> Сборка debug APK..."
./gradlew :"$APP_MODULE":assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "Не найден APK по пути: $APK_PATH"
  exit 1
fi

echo "===> Установка на подключённое устройство через adb..."
adb install -r "$APK_PATH"

echo "Готово: установлен $APK_PATH"

