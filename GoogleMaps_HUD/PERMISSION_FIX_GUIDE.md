# Исправление проблем с разрешениями в GoogleMaps_HUD

## Проблемы:
1. **Заблокирован переключатель уведомлений** - приложение не запрашивает разрешение POST_NOTIFICATIONS (требуется с Android 13+)
2. **Приложение не запрашивает разрешения при первом запуске** - отсутствует код для запроса runtime-разрешений

## Решение:

### Шаг 1: Добавить импорт Uri
В файле `MainActivity.java`, после строки 18 добавьте:
```java
import android.net.Uri;
```

### Шаг 2: Добавить константу для запроса разрешений
После строки 87 (где определена константа `MY_PERMISSIONS_REQUEST_LOCATION`), добавьте:
```java
private static final int MY_PERMISSIONS_REQUEST_ALL = 101;
```

### Шаг 3: Добавить вызов requestAllPermissions() в конец метода onCreate()
В конце метода `onCreate()` (перед закрывающей скобкой `}`), добавьте:
```java
// Request all necessary runtime permissions at startup
requestAllPermissions();
```

### Шаг 4: Добавить методы для запроса и обработки разрешений
После метода `updateTextViewDebug()` (примерно на строке 764), добавьте следующие методы:

```java
/**
 * Request all necessary runtime permissions at app startup
 */
private void requestAllPermissions() {
    java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();

    // Location permissions
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    // Background location permission (Android 10+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    // Bluetooth permissions (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
    }

    // Notification permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // Request all permissions if any are missing
    if (!permissionsToRequest.isEmpty()) {
        String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissionsArray, MY_PERMISSIONS_REQUEST_ALL);
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == MY_PERMISSIONS_REQUEST_ALL) {
        boolean allGranted = true;
        StringBuilder deniedPermissions = new StringBuilder();

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                deniedPermissions.append("\n- ").append(permissions[i]);
            }
        }

        if (!allGranted) {
            // Some permissions were denied, show explanation
            new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("This app requires the following permissions to function properly:" +
                            deniedPermissions.toString() +
                            "\n\nPlease grant these permissions in Settings.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    } else if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
        // Handle location permission result from checkLocationPermission method
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, can proceed with location-based features
            log("Location permission granted");
        } else {
            log("Location permission denied");
        }
    }
}
```

## Результат:
После применения этих изменений:
1. Приложение будет автоматически запрашивать все необходимые разрешения при первом запуске
2. Переключатель уведомлений в настройках будет активен (после предоставления разрешения POST_NOTIFICATIONS)
3. Пользователь сможет легко перейти в настройки приложения, если откажет в разрешениях

## Примечание:
Разрешение POST_NOTIFICATIONS требуется только на Android 13+ (API 33). На более старых версиях Android разрешение на уведомления предоставляется автоматически.
