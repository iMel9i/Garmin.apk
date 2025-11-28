package iMel9i.garminhud.lite

import android.accessibilityservice.AccessibilityService
import iMel9i.garminhud.lite.DebugLog
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service для парсинга UI-элементов навигации
 * Читает данные по resource-id прямо с экрана приложений
 */
class NavigationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NavAccessibility"
        var instance: NavigationAccessibilityService? = null
    }

    private lateinit var configManager: AppConfigManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        configManager = AppConfigManager(this)
        DebugLog.i(TAG, "Service created")
        Log.d(TAG, "Navigation Accessibility Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        DebugLog.w(TAG, "Service destroyed")
        Log.d(TAG, "Navigation Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Check if this package is in our configs
        val config = configManager.getConfigs().find { 
            it.packageName == packageName && it.enabled 
        } ?: return

        // Parse UI elements based on config
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            parseUIElements(rootNode, config, packageName)
        }
    }

    private fun parseUIElements(
        rootNode: AccessibilityNodeInfo,
        config: AppConfigManager.AppConfig,
        packageName: String
    ) {
        val parsedData = mutableMapOf<String, String?>()
        var recognizedArrow: ArrowDirection? = null

        // Parse each configured field
        config.fields.forEach { (dataTypeName, resourceId) ->
            if (resourceId.isBlank()) return@forEach
            
            val node = findNodeByResourceId(rootNode, resourceId)
            
            // Check if this is an ImageView (arrow)
            if (node?.className == "android.widget.ImageView") {
                DebugLog.i(TAG, "Found ImageView: $resourceId")
                // Try to extract and recognize arrow
                recognizedArrow = recognizeArrowFrom(node)
                Log.d(TAG, "Found ImageView at $resourceId: arrow=$recognizedArrow")
            } else {
                // Regular text field
                val value = node?.text?.toString()
                parsedData[dataTypeName] = value
                
                if (value != null) {
                    Log.d(TAG, "Found $dataTypeName ($resourceId): $value")
                }
            }
        }
        
        // If we recognized an arrow, store its HUD code
        recognizedArrow?.let { arrow ->
            if (arrow != ArrowDirection.NONE) {
                HudState.turnIcon = arrow.hudCode
                Log.d(TAG, "Set turn icon: ${arrow.name} (code=${arrow.hudCode})")
            }
        }

        // Update HudState based on parsed data
        updateHudState(parsedData, packageName)
    }
    
    private var lastScreenshotTime = 0L
    private val SCREENSHOT_INTERVAL = 1000L // 1 second throttle

    private fun recognizeArrowFrom(imageNode: AccessibilityNodeInfo): ArrowDirection? {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScreenshotTime < SCREENSHOT_INTERVAL) {
            return null
        }
        lastScreenshotTime = currentTime

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            DebugLog.i(TAG, "Screenshot OK")
                            try {
                                val hardwareBuffer = screenshot.hardwareBuffer
                                val colorSpace = screenshot.colorSpace
                                val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                
                                if (bitmap != null) {
                                    // Must copy because HardwareBuffer is closed after this scope
                                    val copy = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                    hardwareBuffer.close()
                                    
                                    processScreenshot(copy, imageNode)
                                } else {
                                    hardwareBuffer.close()
                                }
                            } catch (e: Exception) {
                                DebugLog.e(TAG, "Screenshot error: ${e.message}")
                                Log.e(TAG, "Error processing screenshot", e)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            DebugLog.e(TAG, "Screenshot fail: $errorCode")
                            Log.e(TAG, "Screenshot failed with error code: $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                DebugLog.e(TAG, "Screenshot init fail: ${e.message}")
                Log.e(TAG, "Failed to initiate screenshot", e)
            }
        }
        return null
    }

    private fun processScreenshot(screenBitmap: android.graphics.Bitmap, node: AccessibilityNodeInfo) {
        try {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            
            // Validate bounds
            if (rect.width() > 0 && rect.height() > 0) {
                 // Ensure bounds are within bitmap dimensions
                 val left = rect.left.coerceAtLeast(0)
                 val top = rect.top.coerceAtLeast(0)
                 val width = rect.width().coerceAtMost(screenBitmap.width - left)
                 val height = rect.height().coerceAtMost(screenBitmap.height - top)
                 
                 if (width > 0 && height > 0) {
                     val cropped = android.graphics.Bitmap.createBitmap(screenBitmap, left, top, width, height)
                     
                     // Send to Debug UI
                     HudService.navDebug.lastArrowBitmap = cropped
                     DebugLog.i(TAG, "Arrow: ${width}x${height}")
                     
                     // Calculate Hash for future recognition
                     val arrowImg = ArrowImage(cropped)
                     val hash = arrowImg.getArrowValue()
                     DebugLog.i(TAG, "Hash: $hash")
                     Log.d(TAG, "Captured Arrow Hash: $hash")
                     
                     // Try to recognize (will likely fail until we add hash)
                     val recognized = ArrowDirection.recognize(arrowImg)
                     if (recognized != ArrowDirection.NONE) {
                         HudState.turnIcon = recognized.hudCode
                         DebugLog.i(TAG, "Recognized: ${recognized.name}")
                         Log.d(TAG, "Recognized arrow from Screenshot: $recognized")
                     } else {
                         DebugLog.w(TAG, "Not recognized")
                     }
                 }
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "Crop error: ${e.message}")
            Log.e(TAG, "Error cropping arrow", e)
        }
    }

    private fun findNodeByResourceId(
        node: AccessibilityNodeInfo,
        resourceId: String
    ): AccessibilityNodeInfo? {
        // Direct search by viewIdResourceName
        if (node.viewIdResourceName == resourceId) {
            return node
        }

        // Search children recursively
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByResourceId(child, resourceId)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }

        return null
    }

    private fun updateHudState(parsedData: Map<String, String?>, packageName: String) {
        // Update raw data
        HudState.lastPackageName = packageName
        HudState.rawData.clear()
        parsedData.forEach { (key, value) ->
            if (value != null) {
                HudState.rawData[key] = value
            }
        }

        // Map to specific HudState fields
        parsedData[HudDataType.DISTANCE_TO_TURN.name]?.let { 
            HudState.distanceToTurn = it
            // Try to parse distance to meters
            val distanceMeters = parseDistanceToMeters(it)
            if (distanceMeters != null) {
                HudState.distanceToTurnMeters = distanceMeters
            }
        }

        parsedData[HudDataType.NAVIGATION_INSTRUCTION.name]?.let {
            // Store instruction for turn icon parsing
            HudState.turnIcon = parseTurnDirection(it)
        }

        parsedData[HudDataType.ETA.name]?.let {
            HudState.eta = it
        }

        parsedData[HudDataType.REMAINING_TIME.name]?.let {
            HudState.remainingTime = it
        }

        parsedData[HudDataType.TRAFFIC_SCORE.name]?.let {
            // Try to extract number from traffic score text
            val score = it.replace(Regex("[^0-9]"), "").toIntOrNull()
            HudState.trafficScore = score
        }

        parsedData[HudDataType.SPEED_LIMIT.name]?.let {
            val limit = it.replace(Regex("[^0-9]"), "").toIntOrNull()
            if (limit != null) {
                HudState.speedLimit = limit
            }
        }

        parsedData[HudDataType.CURRENT_SPEED.name]?.let {
            val speed = it.replace(Regex("[^0-9]"), "").toIntOrNull()
            if (speed != null) {
                HudState.currentSpeed = speed
            }
        }

        // Set navigation active if we have distance or instruction
        HudState.isNavigating = !HudState.distanceToTurn.isNullOrBlank() || 
                                HudState.eta != null

        // Notify update
        HudState.notifyUpdate()
        
        // Update debug info
        HudService.navDebug.packageName = packageName
        HudService.navDebug.lastUpdateTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    }

    private fun parseDistanceToMeters(distanceText: String): Int? {
        // Parse "500 m", "1.5 km", etc.
        val regex = """(\d+(?:[.,]\d+)?)\s*(m|м|km|км)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(distanceText) ?: return null
        
        val value = match.groupValues[1].replace(",", ".").toFloatOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        
        return when (unit) {
            "km", "км" -> (value * 1000).toInt()
            "m", "м" -> value.toInt()
            else -> null
        }
    }

    private fun parseTurnDirection(instruction: String): Int {
        // Parse turn direction from instruction text
        // This is a simplified version - you may need to expand it
        return when {
            instruction.contains("лев", ignoreCase = true) || 
            instruction.contains("left", ignoreCase = true) -> 1 // Left
            
            instruction.contains("прав", ignoreCase = true) || 
            instruction.contains("right", ignoreCase = true) -> 2 // Right
            
            instruction.contains("прямо", ignoreCase = true) || 
            instruction.contains("straight", ignoreCase = true) -> 0 // Straight
            
            else -> 0
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
}
