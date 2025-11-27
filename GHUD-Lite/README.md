# GHUD Lite - Отдельный проект

Полностью независимое приложение для Garmin HUD.

## Структура проекта

```
ghud-lite/
├── app/                        # Основное приложение
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/iMel9i/garminhud/lite/
│       │   ├── MainActivity.kt
│       │   ├── HudService.kt
│       │   └── GarminHudLite.kt
│       └── res/
├── bt-spp-library/             # Bluetooth SPP библиотека
├── build.gradle                # Корневой build файл
├── settings.gradle
├── gradlew
└── gradlew.bat
```

## Сборка

```powershell
cd C:\Users\mts88\Documents\GHUD\Garmin.apk\ghud-lite
.\gradlew assembleDebug
```

## Установка

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

## Особенности

- ✅ Полностью независимый проект
- ✅ Пакет: `iMel9i.garminhud.lite`
- ✅ Собственная Bluetooth SPP библиотека
- ✅ Не требует других приложений
