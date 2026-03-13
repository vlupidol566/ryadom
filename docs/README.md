# РоманАлександров — Помощник слепым

## Идеология
Зрячий **ведущий** видит карту Yandex + видео с камеры **подопечного** (слепого) и даёт голосовые инструкции реального времени через WebRTC.

Приложение **необходимое** для слепых: TTS-приветствие при запуске, полная озвучка TalkBack, простая навигация ролями.

## Сборка и тест
```
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

**Тест TalkBack+TTS:**
1. Включи TalkBack в настройках.
2. Запуск → TTS: "Добро пожаловать. Выберите роль..."
3. Карточки ролей: "Помогайте слепому", "Получите помощь".

## Структура (рефакторинг завершён)
```
app/src/main/java/roman/alex/
├── MainActivity.kt (навигация + TTS)
├── RoleSelectionScreen.kt
├── LeadingScreen.kt
├── FollowerScreen.kt
├── WebRtcClient.kt
└── contacts/
```

## Фичи MVP
- 🎥 WebRTC видео/звонки
- 🗺️ Yandex Maps
- 📱 Compose UI + Material3
- ♿ Accessibility (TalkBack, contentDescription)
- 🗣️ TTS-приветствие

## Следующие шаги
1. Доработать WebRTC-коннект.
2. Интеграция AI-распознавание объектов.
3. Пуш FCM для уведомлений.

**Для слепых:** Протестируйте на реальном устройстве!