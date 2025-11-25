package sky4s.garminhud.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages runtime permissions for the GoogleMaps HUD application.
 * Handles requesting and checking all necessary permissions including:
 * - Location (fine, coarse, background)
 * - Bluetooth (connect, scan)
 * - Notifications (post notifications)
 */
public class PermissionManager {

    public static final int REQUEST_CODE_ALL_PERMISSIONS = 101;

    private final Activity activity;
    private PermissionCallback callback;

    /**
     * Callback interface for permission results
     */
    public interface PermissionCallback {
        void onPermissionsGranted();

        void onPermissionsDenied(List<String> deniedPermissions);
    }

    public PermissionManager(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(PermissionCallback callback) {
        this.callback = callback;
    }

    /**
     * Request all necessary runtime permissions for the app
     */
    public void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Location permissions
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Background location permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= 31) { // S = 31
            if (!hasPermission("android.permission.BLUETOOTH_CONNECT")) {
                permissionsToRequest.add("android.permission.BLUETOOTH_CONNECT");
            }
            if (!hasPermission("android.permission.BLUETOOTH_SCAN")) {
                permissionsToRequest.add("android.permission.BLUETOOTH_SCAN");
            }
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) { // TIRAMISU = 33
            if (!hasPermission("android.permission.POST_NOTIFICATIONS")) {
                permissionsToRequest.add("android.permission.POST_NOTIFICATIONS");
            }
        }

        // Request all permissions if any are missing
        if (!permissionsToRequest.isEmpty()) {
            String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
            ActivityCompat.requestPermissions(activity, permissionsArray, REQUEST_CODE_ALL_PERMISSIONS);
        } else {
            // All permissions already granted
            if (callback != null) {
                callback.onPermissionsGranted();
            }
        }
    }

    /**
     * Check if a specific permission is granted
     */
    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all critical permissions are granted
     */
    public boolean hasAllCriticalPermissions() {
        boolean hasLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        boolean hasBluetooth = true;
        if (Build.VERSION.SDK_INT >= 31) { // S = 31
            hasBluetooth = hasPermission("android.permission.BLUETOOTH_CONNECT");
        }

        boolean hasNotifications = true;
        if (Build.VERSION.SDK_INT >= 33) { // TIRAMISU = 33
            hasNotifications = hasPermission("android.permission.POST_NOTIFICATIONS");
        }

        return hasLocation && hasBluetooth && hasNotifications;
    }

    /**
     * Handle permission request results
     * Call this from Activity's onRequestPermissionsResult
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_CODE_ALL_PERMISSIONS) {
            return;
        }

        List<String> deniedPermissions = new ArrayList<>();
        boolean allGranted = true;

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                deniedPermissions.add(permissions[i]);
            }
        }

        if (allGranted) {
            if (callback != null) {
                callback.onPermissionsGranted();
            }
        } else {
            if (callback != null) {
                callback.onPermissionsDenied(deniedPermissions);
            }
            showPermissionDeniedDialog(deniedPermissions);
        }
    }

    /**
     * Show dialog when permissions are denied
     */
    private void showPermissionDeniedDialog(List<String> deniedPermissions) {
        StringBuilder message = new StringBuilder();
        message.append("This app requires the following permissions to function properly:");

        for (String permission : deniedPermissions) {
            String readableName = getReadablePermissionName(permission);
            message.append("\n- ").append(readableName);
        }

        message.append("\n\nPlease grant these permissions in Settings.");

        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    /**
     * Convert permission string to readable name
     */
    private String getReadablePermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Precise Location";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Approximate Location";
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return "Background Location";
            case "android.permission.BLUETOOTH_CONNECT":
                return "Bluetooth Connection";
            case "android.permission.BLUETOOTH_SCAN":
                return "Bluetooth Scanning";
            case "android.permission.POST_NOTIFICATIONS":
                return "Notifications";
            default:
                return permission;
        }
    }

    /**
     * Open app settings page
     */
    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}
