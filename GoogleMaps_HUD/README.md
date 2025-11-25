# GarmAx_HUD v0.9.8

**Google Maps Navigation for Garmin HUD & Compatible Devices**

![Version](https://img.shields.io/badge/version-0.9.8-blue)
![Android](https://img.shields.io/badge/Android-6.0%2B-green)
![Status](https://img.shields.io/badge/status-stable-success)

## 🎉 What's New in v0.9.8

This release fixes **critical crashes** that prevented the app from working with Google Maps navigation.

### Fixed Issues
- ✅ **Race condition in service initialization** - App no longer crashes on startup
- ✅ **StringIndexOutOfBoundsException** - Fixed crash when parsing empty distance strings
- ✅ **IllegalStateException in bitmap processing** - Arrow icons now handled gracefully
- ✅ **Google Maps compatibility** - Works with latest Google Maps versions

[📄 Full Release Notes](RELEASE_NOTES_v0.9.8.md)

## Features

- 📍 **Real-time Navigation** - Display Google Maps turn-by-turn directions on your HUD
- 🧭 **Turn Arrows** - Visual arrow indicators for upcoming turns
- 📏 **Distance & Time** - Show distance to next turn and ETA
- 🚗 **Speed Display** - Current speed on HUD
- 🔄 **Auto-Brightness** - Adjusts to ambient light
- 📱 **Background Operation** - Works while phone is locked
- 🎯 **Multiple HUD Support** - Garmin HUD, BMW HUD, and compatible devices

## Requirements

- Android 6.0 (API 23) or higher
- Bluetooth-enabled HUD device (Garmin HUD recommended)
- Google Maps app installed
- Location and Notification permissions

## Installation

1. Download the latest APK from [Releases](https://github.com/iMel9i/Garmin.apk/releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK
4. Grant all requested permissions:
   - Location (for speed display)
   - Bluetooth (for HUD connection)
   - Notifications (for Google Maps data)

## Usage

1. **Connect HUD**
   - Open GarmAx_HUD app
   - Tap "Scan for HUD"
   - Select your HUD device from the list

2. **Enable Notification Access**
   - Go to Android Settings → Apps → Special Access → Notification Access
   - Enable GarmAx_HUD

3. **Start Navigation**
   - Open Google Maps
   - Start navigation to any destination
   - Navigation data will automatically appear on your HUD

## Troubleshooting

### App crashes when starting navigation
- **Fixed in v0.9.8!** Update to the latest version

### HUD not connecting
- Ensure Bluetooth is enabled
- Make sure HUD is powered on and in pairing mode
- Try "Reset HUD" in app settings

### No navigation data on HUD
- Check that Notification Access is enabled for GarmAx_HUD
- Verify Google Maps is actively navigating
- Check app status indicators (all should be green/ON)

### Logs
To collect diagnostic logs:
```bash
adb logcat -s NotificationMonitor:* AndroidRuntime:E > app_logs.txt
```

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/iMel9i/Garmin.apk.git
cd Garmin.apk/GoogleMaps_HUD

# Build APK
gradlew assembleDebug

# Install on connected device
gradlew installDebug
```

### Project Structure
```
GoogleMaps_HUD/
├── gmaps_hud/              # Main app module
│   ├── src/main/java/      # Java source code
│   │   └── sky4s/garminhud/
│   │       ├── app/        # App logic
│   │       └── hud/        # HUD communication
│   └── src/main/res/       # Resources
└── bt-spp-library/         # Bluetooth SPP library
```

## Technical Details

### Key Components

- **NotificationMonitor** - Captures and parses Google Maps notifications
- **HUDInterface** - Manages communication with HUD devices
- **GarminHUD** - Garmin-specific protocol implementation
- **BMWHUD** - BMW HUD protocol implementation

### Supported HUD Devices

- ✅ Garmin HUD (Primary)
- ✅ Garmin HUD+ 
- ✅ BMW Head-Up Display
- ⚠️ Other Bluetooth HUDs (experimental)

## Credits

- Original project: [GoogleMaps_HUD](https://github.com/skyforcetw/GoogleMaps_HUD) by skyforcetw
- Maintained and enhanced by iMel9i
- Critical bug fixes and Google Maps compatibility updates

## License

This project is licensed under the terms specified in the original repository.

## Support

- 🐛 [Report Issues](https://github.com/iMel9i/Garmin.apk/issues)
- 💬 [Discussions](https://github.com/iMel9i/Garmin.apk/discussions)
- 📧 Contact: via GitHub issues

## Changelog

### v0.9.8 (2025-11-25)
- Fixed critical race condition in NotificationMonitor.onCreate()
- Added null safety checks for distance parsing
- Added error handling for bitmap processing
- Improved Google Maps notification compatibility
- Renamed app to GarmAx_HUD

### v0.9.0 (Previous)
- Permission management improvements
- Foreground service implementation
- Android 14+ compatibility

---

**Made with ❤️ for safer navigation**
