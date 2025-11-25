# Руководство по сборке, установке и отладке GoogleMaps HUD

## Подготовка

### 1. Установка Android SDK Platform Tools (ADB)

ADB (Android Debug Bridge) необходим для установки APK и просмотра логов.

**Вариант А: Через Android Studio**
- ADB уже установлен вместе с Android Studio
- Путь обычно: `C:\Users\<username>\AppData\Local\Android\Sdk\platform-tools`
- Добавьте этот путь в переменную PATH

**Вариант Б: Скачать отдельно**
1. Скачайте Platform Tools: https://developer.android.com/studio/releases/platform-tools
2. Распакуйте в удобную папку (например, `C:\platform-tools`)
3. Добавьте путь в переменную PATH

**Добавление в PATH:**
```powershell
# Временно (только для текущей сессии PowerShell):
$env:Path += ";C:\Users\<username>\AppData\Local\Android\Sdk\platform-tools"

# Или постоянно через System Properties > Environment Variables
```

### 2. Подготовка телефона

1. **Включите режим разработчика:**
   - Настройки → О телефоне → Нажмите 7 раз на "Номер сборки"

2. **Включите отладку по USB:**
   - Настройки → Система → Для разработчиков → Отладка по USB

3. **Подключите телефон к компьютеру через USB**

4. **Разрешите отладку:**
   - На телефоне появится запрос "Разрешить отладку по USB?" → Разрешить

## Сборка и установка

### Вариант 1: Автоматическая сборка и установка

```powershell
# Перейдите в папку проекта
cd C:\Users\mts88\Documents\GHUD\Garmin.apk\GoogleMaps_HUD

# Запустите скрипт сборки и установки
.\build_and_install.ps1
```

### Вариант 2: Ручная сборка и установка

```powershell
# 1. Соберите APK
.\gradlew assembleDebug

# 2. Проверьте подключение телефона
adb devices
# Должно показать: List of devices attached
#                  <device_id>    device

# 3. Установите APK
adb install -r gmaps_hud\build\outputs\apk\debug\gmaps_hud-debug.apk

# 4. Запустите приложение
adb shell am start -n sky4s.garminhud.app/.MainActivity
```

## Просмотр логов

### Вариант 1: Используя скрипт

```powershell
# Все логи приложения
.\view_logs.ps1

# Только логи разрешений
.\view_logs.ps1 -Permissions

# Все логи системы
.\view_logs.ps1 -All

# Очистить логи перед просмотром
.\view_logs.ps1 -Clear
```

### Вариант 2: Прямые команды ADB

```powershell
# Очистить логи
adb logcat -c

# Показать все логи
adb logcat

# Фильтр по тегу
adb logcat -s MainActivity

# Фильтр по приоритету (E=Error, W=Warning, I=Info, D=Debug)
adb logcat *:E

# Сохранить логи в файл
adb logcat > logs.txt

# Показать логи с временными метками
adb logcat -v time

# Фильтр по процессу приложения
adb logcat | findstr "garminhud"
```

## Полезные команды ADB

```powershell
# Проверить подключенные устройства
adb devices

# Перезапустить ADB сервер (если проблемы с подключением)
adb kill-server
adb start-server

# Запустить приложение
adb shell am start -n sky4s.garminhud.app/.MainActivity

# Остановить приложение
adb shell am force-stop sky4s.garminhud.app

# Удалить приложение
adb uninstall sky4s.garminhud.app

# Проверить установленные разрешения
adb shell dumpsys package sky4s.garminhud.app | findstr permission

# Предоставить разрешение вручную
adb shell pm grant sky4s.garminhud.app android.permission.POST_NOTIFICATIONS
adb shell pm grant sky4s.garminhud.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant sky4s.garminhud.app android.permission.BLUETOOTH_CONNECT

# Отозвать разрешение
adb shell pm revoke sky4s.garminhud.app android.permission.POST_NOTIFICATIONS

# Открыть настройки приложения
adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:sky4s.garminhud.app

# Получить информацию о приложении
adb shell dumpsys package sky4s.garminhud.app

# Сделать скриншот
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Записать видео экрана (макс 3 минуты)
adb shell screenrecord /sdcard/demo.mp4
# Ctrl+C для остановки
adb pull /sdcard/demo.mp4
```

## Отладка разрешений

### Проверить текущие разрешения:
```powershell
adb shell dumpsys package sky4s.garminhud.app | findstr "permission"
```

### Сбросить разрешения (для тестирования первого запуска):
```powershell
adb shell pm reset-permissions sky4s.garminhud.app
```

### Проверить логи разрешений:
```powershell
adb logcat | findstr -i "permission"
```

## Мониторинг в реальном времени

### Открыть несколько окон PowerShell:

**Окно 1 - Логи приложения:**
```powershell
adb logcat -v time | findstr "MainActivity PermissionManager"
```

**Окно 2 - Логи разрешений:**
```powershell
adb logcat -v time | findstr -i "permission"
```

**Окно 3 - Ошибки:**
```powershell
adb logcat *:E
```

## Решение проблем

### ADB не видит устройство:
1. Проверьте кабель USB (используйте кабель с поддержкой данных)
2. Попробуйте другой USB порт
3. Перезапустите ADB: `adb kill-server && adb start-server`
4. Отключите и подключите телефон заново
5. Проверьте драйверы USB для вашего телефона

### Приложение не устанавливается:
1. Удалите старую версию: `adb uninstall sky4s.garminhud.app`
2. Попробуйте снова: `adb install -r <путь к APK>`

### Не видно логов:
1. Очистите логи: `adb logcat -c`
2. Запустите приложение
3. Проверьте логи: `adb logcat`

## Тестирование разрешений

### Сценарий 1: Первый запуск
```powershell
# 1. Удалите приложение (сбросит все разрешения)
adb uninstall sky4s.garminhud.app

# 2. Установите заново
adb install -r gmaps_hud\build\outputs\apk\debug\gmaps_hud-debug.apk

# 3. Запустите с мониторингом логов
adb logcat -c
adb shell am start -n sky4s.garminhud.app/.MainActivity
adb logcat -v time | findstr "Permission MainActivity"
```

### Сценарий 2: Проверка переключателя уведомлений
```powershell
# 1. Откройте настройки приложения
adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:sky4s.garminhud.app

# 2. Проверьте, активен ли переключатель уведомлений
# 3. Проверьте логи
adb logcat | findstr "POST_NOTIFICATIONS"
```

## Создание отчета об ошибке

Если что-то не работает, соберите полный отчет:

```powershell
# Сохраните логи
adb logcat -d > full_log.txt

# Сохраните информацию о разрешениях
adb shell dumpsys package sky4s.garminhud.app > package_info.txt

# Создайте bugreport
adb bugreport bugreport.zip
```

## Примечания

- **Логи в реальном времени**: Используйте `adb logcat` для просмотра логов в реальном времени
- **Фильтрация**: Используйте `findstr` (Windows) или `grep` (Linux/Mac) для фильтрации логов
- **Разрешения**: На Android 13+ разрешение POST_NOTIFICATIONS должно запрашиваться явно
- **Сохранение логов**: Всегда сохраняйте логи в файл для анализа: `adb logcat > logs.txt`
