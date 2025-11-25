# Интеграция PermissionManager в MainActivity

## Изменения в MainActivity.java:

### 1. Добавить поле класса (после строки ~140, где объявлены другие поля):
```java
private PermissionManager permissionManager;
```

### 2. Инициализировать PermissionManager в onCreate() (после setContentView, примерно строка 400):
```java
// Initialize permission manager
permissionManager = new PermissionManager(this);
permissionManager.setCallback(new PermissionManager.PermissionCallback() {
    @Override
    public void onPermissionsGranted() {
        // All permissions granted, continue with initialization
        log("All permissions granted");
    }

    @Override
    public void onPermissionsDenied(List<String> deniedPermissions) {
        // Some permissions denied
        log("Permissions denied: " + deniedPermissions.toString());
    }
});
```

### 3. Запросить разрешения в конце onCreate() (перед закрывающей скобкой метода):
```java
// Request all necessary runtime permissions at startup
permissionManager.requestAllPermissions();
```

### 4. Добавить обработчик результатов разрешений (после метода onActivityResult):
```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    // Delegate to permission manager
    if (permissionManager != null) {
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
    }
}
```

## Преимущества модульного подхода:

1. **Разделение ответственности** - вся логика разрешений в отдельном классе
2. **Переиспользование** - PermissionManager можно использовать в других Activity
3. **Тестируемость** - легче тестировать отдельный модуль
4. **Читаемость** - MainActivity остается чистым и понятным
5. **Расширяемость** - легко добавлять новые разрешения или изменять логику

## Дополнительно:

Вы можете проверить наличие разрешений в любом месте приложения:
```java
if (permissionManager.hasAllCriticalPermissions()) {
    // Proceed with operation
} else {
    permissionManager.requestAllPermissions();
}
```
