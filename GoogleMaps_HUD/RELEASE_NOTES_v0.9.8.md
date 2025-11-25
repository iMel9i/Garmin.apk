# GarmAx_HUD v0.9.8 - Release Notes

## 🎉 Critical Bug Fixes - Google Maps Compatibility Restored

This release fixes critical crashes that occurred when using Google Maps navigation with the HUD.

### Fixed Issues

#### 1. **Race Condition in Service Initialization** ✅
- **Problem**: App crashed immediately when Google Maps started navigation
- **Cause**: `NotificationMonitor` service exited early if `sHud` wasn't initialized yet
- **Fix**: Service now initializes regardless of `sHud` state, using default queue size (10) when needed
- **Impact**: Service components (`mExecutor`, `mMsgReceiver`, `mArrowBitmaps`) are now always initialized

#### 2. **StringIndexOutOfBoundsException in Distance Parsing** ✅
- **Problem**: Crash when parsing empty distance strings from Google Maps notifications
- **Location**: `NotificationMonitor.java:885`
- **Fix**: Added `isEmpty()` check before calling `charAt(0)` on distance string
- **Impact**: App handles empty or malformed distance data gracefully

#### 3. **IllegalStateException in Bitmap Processing** ✅
- **Problem**: Crash when processing arrow icons from Google Maps
- **Location**: `NotificationMonitor.java:911` → `ArrowImage` → `ImageUtils.toBinaryImage`
- **Fix**: Wrapped arrow bitmap processing in try-catch block
- **Impact**: App continues working even if arrow icon can't be processed

### Technical Details

**Modified Files:**
- `gmaps_hud/src/main/java/sky4s/garminhud/app/NotificationMonitor.java`
  - Lines 148-149: Fixed onCreate() race condition
  - Lines 885-886: Added isEmpty() check for distance parsing
  - Lines 919-945: Added try-catch for bitmap processing

**Testing:**
- ✅ App remains stable during Google Maps navigation
- ✅ No crashes when notification capture starts
- ✅ HUD displays navigation data correctly
- ✅ Bluetooth connection maintained throughout navigation

### Compatibility

- **Android Version**: 6.0 (API 23) and above
- **Google Maps**: Tested with latest version (as of Nov 2025)
- **HUD Devices**: Garmin HUD, compatible Bluetooth HUD devices

### Known Limitations

- Arrow icons may not display if bitmap format is incompatible (app continues without crash)
- Some Google Maps notification formats may not be fully parsed (graceful degradation)

### Upgrade Notes

This is a **critical update** for anyone experiencing crashes with Google Maps. The app now handles edge cases and malformed data from Google Maps notifications without crashing.

---

**Version**: 0.9.8  
**Build**: 10  
**Release Date**: November 25, 2025  
**Previous Version**: 0.9.0
