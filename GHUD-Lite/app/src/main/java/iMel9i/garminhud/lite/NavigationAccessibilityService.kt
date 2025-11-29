package iMel9i.garminhud.lite

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import iMel9i.garminhud.lite.DebugLog

class NavigationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NavAccessibility"
        var instance: NavigationAccessibilityService? = null
        var debugDumpMode = true
        var debugToastsEnabled = false
    }

    private lateinit var configManager: AppConfigManager
    private var lastDumpTime = 0L
    private val DUMP_INTERVAL = 5000L

    override fun onCreate() {
        super.onCreate()
        instance = this
        configManager = AppConfigManager(this)
        DebugLog.i(TAG, "Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        DebugLog.i(TAG, "Service connected, flags updated to include not important views")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        DebugLog.w(TAG, "Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val config = configManager.getConfigs().find { it.packageName == packageName && it.enabled } ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            // Try to get the correct root node for the target package
            val rootNode = getRootNode(event, packageName) ?: return
            
            if (debugDumpMode) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDumpTime > DUMP_INTERVAL) {
                    lastDumpTime = currentTime
                    dumpAllUIElements(rootNode, packageName)
                }
            }
            
            parseUIElements(rootNode, config, packageName)
        }
    }
    
    private fun getRootNode(event: AccessibilityEvent, targetPackage: String): AccessibilityNodeInfo? {
        // 1. Try rootInActiveWindow
        val activeRoot = rootInActiveWindow
        if (activeRoot != null && activeRoot.packageName == targetPackage) {
            return activeRoot
        }
        
        // 2. If active window is mismatch (e.g. we are debugging), try to climb up from event source
        var source = event.source ?: return null
        
        while (true) {
            val parent = source.parent
            if (parent == null) {
                if (source.packageName == targetPackage) {
                    return source
                } else {
                    source.recycle()
                    return null
                }
            }
            source.recycle()
            source = parent
        }
        
        return null
    }

    private fun dumpAllUIElements(rootNode: AccessibilityNodeInfo, packageName: String) {
        DebugLog.i(TAG, "=== UI DUMP START ($packageName) ===")
        dumpNodeRecursive(rootNode, 0)
        DebugLog.i(TAG, "=== UI DUMP END ===")
    }
    
    private fun dumpNodeRecursive(node: AccessibilityNodeInfo, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = node.className?.toString() ?: "null"
        val resourceId = node.viewIdResourceName ?: "null"
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (resourceId != "null" || text.isNotEmpty() || contentDesc.isNotEmpty() || className.contains("ImageView") || className.contains("TextView")) {
            DebugLog.d(TAG, "$indent[$className] id=$resourceId text='$text' desc='$contentDesc'")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpNodeRecursive(child, depth + 1)
                child.recycle()
            }
        }
    }

    private fun parseUIElements(rootNode: AccessibilityNodeInfo, config: AppConfigManager.AppConfig, packageName: String) {
        // Double check package to avoid parsing wrong windows
        if (rootNode.packageName != packageName) {
             return
        }
        
        DebugLog.i(TAG, "Parsing UI for ${config.appName}")
        val parsedData = mutableMapOf<String, String?>()
        var recognizedArrow: ArrowDirection? = null
        var arrowFoundNode = false

        config.fields.forEach { (dataTypeName, resourceId) ->
            if (resourceId.isBlank()) return@forEach
            
            // Try exact match first, then partial
            var node = findNodeByResourceId(rootNode, resourceId)
            if (node == null) {
                // Try finding by just the ID part (e.g. "image_maneuverballoon_maneuver")
                val idPart = resourceId.substringAfter(":id/")
                if (idPart.isNotEmpty() && idPart != resourceId) {
                    node = findNodeByResourceId(rootNode, idPart, partial = true)
                }
            }
            
            // Ultimate fallback: Full tree traversal (if still not found and it's important)
            if (node == null && dataTypeName == HudDataType.DIRECTION_ARROW.name) {
                 // DebugLog.w(TAG, "Trying fallback search for $resourceId")
                 node = findNodeRecursiveFallback(rootNode, resourceId)
            }
            
            if (node == null) {
                // DebugLog.w(TAG, "NOT FOUND: $dataTypeName at $resourceId")
                if (dataTypeName == HudDataType.DIRECTION_ARROW.name) {
                    HudService.navDebug.arrowStatus = "Node Not Found"
                }
                return@forEach
            }
            
            // DebugLog.i(TAG, "FOUND: $dataTypeName at $resourceId (class=${node.className})")
            
            val isArrowField = dataTypeName == HudDataType.DIRECTION_ARROW.name
            val isImageView = node.className?.toString()?.contains("ImageView") == true
            
            if (isArrowField || isImageView) {
                arrowFoundNode = true
                DebugLog.i(TAG, "Processing as Image: $resourceId")
                val arrow = recognizeArrowFrom(node)
                if (arrow != null) {
                    recognizedArrow = arrow
                }
            } else {
                val value = node.text?.toString()
                parsedData[dataTypeName] = value
                if (value != null) {
                    // DebugLog.i(TAG, "$dataTypeName = '$value'")
                }
            }
        }
        
        if (debugToastsEnabled) {
            val status = if (recognizedArrow != null) "Arrow: ${recognizedArrow?.name}" 
                         else if (arrowFoundNode) "Arrow: Found Node, No Recog"
                         else "Arrow: Not Found"
            showDebugToast(status)
        }
        
        recognizedArrow?.let { arrow ->
            if (arrow != ArrowDirection.NONE) {
                HudState.turnIcon = arrow.hudCode
                DebugLog.i(TAG, "Set turn icon: ${arrow.name}")
            }
        }

        updateHudState(parsedData, packageName)
    }
    
    private var lastToastTime = 0L
    private var lastArrowCheckTime = 0L
    private val ARROW_CHECK_INTERVAL = 1000L
    private var isProcessingArrow = false

    private fun recognizeArrowFrom(imageNode: AccessibilityNodeInfo): ArrowDirection? {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastArrowCheckTime < ARROW_CHECK_INTERVAL) {
            HudService.navDebug.arrowStatus = "Throttled"
            return null
        }
        
        if (isProcessingArrow) {
            HudService.navDebug.arrowStatus = "Busy"
            return null
        }
        
        lastArrowCheckTime = currentTime
        isProcessingArrow = true

        try {
            // Get bounds of the arrow ImageView
            val rect = android.graphics.Rect()
            imageNode.getBoundsInScreen(rect)
            
            if (rect.width() <= 0 || rect.height() <= 0) {
                HudService.navDebug.arrowStatus = "Invalid bounds"
                isProcessingArrow = false
                return null
            }
            
            DebugLog.i(TAG, "Arrow bounds: $rect")
            
            // Use takeScreenshot API (Android 11+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                            try {
                                val hardwareBuffer = screenshot.hardwareBuffer
                                val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                                
                                if (bitmap != null) {
                                    // Crop to arrow bounds
                                    val left = rect.left.coerceAtLeast(0)
                                    val top = rect.top.coerceAtLeast(0)
                                    val width = rect.width().coerceAtMost(bitmap.width - left)
                                    val height = rect.height().coerceAtMost(bitmap.height - top)
                                    
                                    if (width > 0 && height > 0) {
                                        val cropped = android.graphics.Bitmap.createBitmap(bitmap, left, top, width, height)
                                        bitmap.recycle()
                                        hardwareBuffer.close()
                                        
                                        processArrowBitmap(cropped)
                                    } else {
                                        bitmap.recycle()
                                        hardwareBuffer.close()
                                        HudService.navDebug.arrowStatus = "Invalid crop"
                                        isProcessingArrow = false
                                    }
                                } else {
                                    hardwareBuffer.close()
                                    HudService.navDebug.arrowStatus = "Bitmap null"
                                    isProcessingArrow = false
                                }
                            } catch (e: Exception) {
                                DebugLog.e(TAG, "Screenshot processing error: ${e.message}")
                                HudService.navDebug.arrowStatus = "Error: ${e.message}"
                                isProcessingArrow = false
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            DebugLog.e(TAG, "Screenshot failed: $errorCode")
                            HudService.navDebug.arrowStatus = "Screenshot failed: $errorCode"
                            isProcessingArrow = false
                        }
                    }
                )
            } else {
                HudService.navDebug.arrowStatus = "Android 11+ required"
                isProcessingArrow = false
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "recognizeArrowFrom error: ${e.message}")
            HudService.navDebug.arrowStatus = "Error: ${e.message}"
            isProcessingArrow = false
        }
        
        return null
    }
    
    private fun processArrowBitmap(bitmap: android.graphics.Bitmap) {
        try {
            HudService.navDebug.lastArrowBitmap?.recycle()
            HudService.navDebug.lastArrowBitmap = bitmap
            
            val arrowImg = ArrowImage(bitmap)
            val hash = arrowImg.getArrowValue()
            DebugLog.i(TAG, "Arrow Hash: $hash")
            
            val recognized = ArrowDirection.recognize(arrowImg)
            if (recognized != ArrowDirection.NONE) {
                HudState.turnIcon = recognized.hudCode
                HudService.navDebug.arrowStatus = "Recognized: ${recognized.name} ($hash)"
                HudState.notifyUpdate()
            } else {
                HudService.navDebug.arrowStatus = "Not Recognized ($hash)"
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "processArrowBitmap error: ${e.message}")
            HudService.navDebug.arrowStatus = "Process error"
        } finally {
            isProcessingArrow = false
        }
    }

    private fun showDebugToast(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > 2000) { // Throttle toasts
            lastToastTime = now
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    DebugLog.e(TAG, "Toast error: ${e.message}")
                }
            }
        }
    }

    private fun findNodeByResourceId(node: AccessibilityNodeInfo, resourceId: String, partial: Boolean = false): AccessibilityNodeInfo? {
        val nodeId = node.viewIdResourceName
        if (nodeId != null) {
            if (partial) {
                if (nodeId.endsWith(":id/$resourceId") || nodeId.endsWith("/$resourceId")) return node // Don't recycle node if returning it
            } else {
                if (nodeId == resourceId) return node // Don't recycle node if returning it
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByResourceId(child, resourceId, partial)
            if (result != null) {
                // Found it!
                // If result is NOT child, we can recycle child.
                // If result IS child, we must NOT recycle child.
                if (result != child) {
                    child.recycle()
                }
                return result
            }
            child.recycle()
        }
        return null
    }
    
    // Fallback: Traverse entire tree to find node (slower but more reliable for debugging)
    private fun findNodeRecursiveFallback(node: AccessibilityNodeInfo, resourceId: String): AccessibilityNodeInfo? {
        val nodeId = node.viewIdResourceName
        if (nodeId != null && nodeId == resourceId) {
            return node // Return without recycling
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeRecursiveFallback(child, resourceId)
            if (result != null) {
                if (result != child) {
                    child.recycle()
                }
                return result
            }
            child.recycle()
        }
        return null
    }

    private fun updateHudState(parsedData: Map<String, String?>, packageName: String) {
        HudState.lastPackageName = packageName
        HudState.rawData.clear()
        parsedData.forEach { (key, value) -> if (value != null) HudState.rawData[key] = value }

        parsedData[HudDataType.DISTANCE_TO_TURN.name]?.let { 
            HudState.distanceToTurn = it
            HudState.distanceToTurnMeters = parseDistanceToMeters(it)
        }

        parsedData[HudDataType.NAVIGATION_INSTRUCTION.name]?.let {
            HudState.turnIcon = parseTurnDirection(it)
        }

        parsedData[HudDataType.ETA.name]?.let { HudState.eta = it }
        parsedData[HudDataType.REMAINING_TIME.name]?.let { HudState.remainingTime = it }
        
        parsedData[HudDataType.TRAFFIC_SCORE.name]?.let {
            HudState.trafficScore = it.replace(Regex("[^0-9]"), "").toIntOrNull()
        }

        parsedData[HudDataType.SPEED_LIMIT.name]?.let {
            HudState.speedLimit = it.replace(Regex("[^0-9]"), "").toIntOrNull()
        }

        parsedData[HudDataType.CURRENT_SPEED.name]?.let {
            HudState.currentSpeed = it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        }

        HudState.isNavigating = !HudState.distanceToTurn.isNullOrBlank() || HudState.eta != null
        HudState.notifyUpdate()
        
        HudService.navDebug.packageName = packageName
        HudService.navDebug.lastUpdateTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        HudService.navDebug.parsedInstruction = HudState.rawData[HudDataType.NAVIGATION_INSTRUCTION.name] ?: ""
        HudService.navDebug.parsedDistance = HudState.distanceToTurn ?: ""
        HudService.navDebug.parsedEta = HudState.eta ?: ""
    }

    private fun parseDistanceToMeters(distanceText: String): Int? {
        return DistanceFormatter.parseDistance(distanceText)?.first
    }

    private fun parseTurnDirection(instruction: String): Int {
        val instr = instruction.lowercase()
        return when {
            "sharp left" in instr || "резко налево" in instr -> 7
            "sharp right" in instr || "резко направо" in instr -> 8
            "turn left" in instr || "поверните налево" in instr || "налево" in instr -> 1
            "turn right" in instr || "поверните направо" in instr || "направо" in instr -> 6
            "keep left" in instr || "левее" in instr || "держаться левее" in instr -> 4
            "keep right" in instr || "правее" in instr || "держаться правее" in instr -> 5
            "easy left" in instr || "плавно налево" in instr -> 2
            "easy right" in instr || "плавно направо" in instr -> 3
            "straight" in instr || "прямо" in instr -> 0
            else -> 0
        }
    }
    
    override fun onInterrupt() {}
}
