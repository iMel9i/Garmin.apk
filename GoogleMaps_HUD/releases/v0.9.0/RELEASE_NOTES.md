# GoogleMaps HUD v0.9.0

**Release Date:** 2025-11-25

## 📦 Download
- **APK:** `GoogleMaps_HUD-v0.9.0.apk` (3.67 MB)
- **Package:** `sky4s.garminhud.app`

## ✨ What's New

### 🔧 Critical Fixes
- **Fixed app crash on startup (Android 12+)**: Bluetooth is now initialized only after permissions are granted
  - Moved HUD initialization from `onCreate()` to permission callback
  - Prevents `SecurityException` when accessing Bluetooth without `BLUETOOTH_CONNECT` permission
  
### 📉 Optimizations
- **Reduced logging**: Removed excessive debug logs to reduce APK size
  - Removed debug logs from MainActivity button handlers
  - Removed debug logs from onResume
  - Removed always-on log from BMWMessage.setTrafficDelay()
  - DEBUG flags already disabled in HUD classes

### 🔐 Permissions
The app will request the following permissions on first launch:
- 📍 **Location** (Fine, Coarse, Background) - for GPS speed and navigation
- 🔵 **Bluetooth** (Connect, Scan) - for HUD connection (Android 12+)
- 🔔 **Notifications** (Post Notifications) - for reading Google Maps notifications (Android 13+)

## 🛠️ Technical Details

### Changes
1. **MainActivity.java**
   - Moved `initializeHUD()` call from onCreate to PermissionManager callback
   - Removed debug logging calls
   
2. **BMWMessage.java**
   - Removed always-on debug log from `setTrafficDelay()`

### Build Info
- **Min SDK:** 23 (Android 6.0)
- **Target SDK:** 29 (Android 10)
- **Build Tools:** Gradle 6.1.1

## 📱 Installation

1. Enable "Install from Unknown Sources" in Android settings
2. Install the APK: `GoogleMaps_HUD-v0.9.0.apk`
3. Grant all requested permissions
4. Connect to your Garmin or BMW HUD

## 🐛 Known Issues
None reported in this version.

## 📝 Notes
- First launch will request multiple permissions - please grant all for full functionality
- HUD will auto-connect after permissions are granted
- For Garmin HUD: Bluetooth must be enabled
- For BMW HUD: Wi-Fi must be enabled

---

**Previous Version:** v0.8.x  
**Next Version:** TBD
