# Быстрый старт - Настройка ADB

## Вариант 1: Автоматическая настройка (Рекомендуется)

Запустите PowerShell **от имени Администратора** и выполните:

```powershell
cd C:\Users\mts88\Documents\GHUD\Garmin.apk\GoogleMaps_HUD
.\setup_adb.ps1
```

Скрипт автоматически:
- Найдет ADB на вашем компьютере
- Добавит его в системный PATH
- Проверит работоспособность

**После этого ADB будет доступен во всех окнах PowerShell навсегда!**

## Вариант 2: Скрипты работают без настройки

Даже если вы не настроите PATH, скрипты `build_and_install.ps1` и `view_logs.ps1` **автоматически найдут ADB** в стандартных местах:
- `C:\Users\mts88\AppData\Local\Android\Sdk\platform-tools`
- `C:\Android\Sdk\platform-tools`
- И других

Просто запускайте их, и они сами найдут ADB!

## Проверка

После настройки проверьте:

```powershell
adb version
```

Должно показать версию ADB.

## Использование

Теперь можете использовать:

```powershell
# Сборка и установка
.\build_and_install.ps1

# Просмотр логов
.\view_logs.ps1
.\view_logs.ps1 -Permissions
.\view_logs.ps1 -All

# Прямые команды ADB
adb devices
adb shell am start -n sky4s.garminhud.app/.MainActivity
adb logcat
```

## Если что-то не работает

1. Убедитесь, что Android Studio установлена
2. Запустите `setup_adb.ps1` от имени Администратора
3. Перезапустите PowerShell
4. Если проблема остается, скрипты все равно будут работать, найдя ADB автоматически
