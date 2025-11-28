package iMel9i.garminhud.lite

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Служба для мониторинга уведомлений от приложений навигации
 * (Google Maps, Yandex Maps, Yandex Navigator)
 */
class NavigationNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NavNotifListener"
        
        var instance: NavigationNotificationListener? = null
        var onNavigationUpdate: ((NavigationData) -> Unit)? = null
    }
    
    data class NavigationData(
        val distance: String? = null,
        val distanceMeters: Int? = null,
        val instruction: String? = null,
        val eta: String? = null,
        val speed: Int? = null,
        val speedLimit: Int? = null,
        val isNavigating: Boolean = false
    )
    
    private lateinit var configManager: AppConfigManager
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        configManager = AppConfigManager(this)
        Log.d(TAG, "Navigation notification listener created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Navigation notification listener destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val config = configManager.getConfigs().find { it.packageName == packageName && it.enabled }
        
        if (config != null) {
            parseNotification(sbn, config)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val config = configManager.getConfigs().find { it.packageName == packageName && it.enabled }
        
        if (config != null) {
            Log.d(TAG, "Navigation notification removed from: $packageName")
            // Сообщаем, что навигация завершена
            onNavigationUpdate?.invoke(NavigationData(isNavigating = false))
        }
    }
    
    private fun parseNotification(sbn: StatusBarNotification, config: AppConfigManager.AppConfig) {
        val notification = sbn.notification
        val extras = notification.extras
        
        // LOG ALL EXTRAS KEYS for debugging
        val allExtras = mutableMapOf<String, String>()
        extras.keySet().forEach { key ->
            val value = extras.get(key)
            allExtras[key] = value?.toString() ?: "null"
            Log.d(TAG, "  Extra: $key = $value")
        }
        
        // Update Debug Raw Data with standard fields
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()
        val subText = extras.getCharSequence("android.subText")?.toString()
        val infoText = extras.getCharSequence("android.infoText")?.toString()
        
        HudService.navDebug.packageName = sbn.packageName
        HudService.navDebug.title = title ?: ""
        HudService.navDebug.text = text ?: ""
        HudService.navDebug.bigText = bigText ?: ""
        HudService.navDebug.lastUpdateTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        HudState.lastPackageName = sbn.packageName
        HudState.rawData.clear()
        HudState.rawData.putAll(allExtras) // Store ALL extras for debugging
        
        // --- Parsing based on Configured Mappings ---
        
        // 1. Distance
        val distanceKey = config.fields[HudDataType.DISTANCE_TO_TURN.name]
        var distance: String? = null
        if (distanceKey != null) {
            val rawDist = extras.getCharSequence(distanceKey)?.toString()
            // If user mapped it, try to extract distance from it
            distance = extractDistance(rawDist)
        } else {
            // Fallback: try standard fields
            distance = extractDistance(title) ?: extractDistance(text) ?: extractDistance(bigText)
        }
        
        // 2. Instruction / Direction
        val instructionKey = config.fields[HudDataType.NAVIGATION_INSTRUCTION.name]
        var instruction: String? = null
        if (instructionKey != null) {
            instruction = extras.getCharSequence(instructionKey)?.toString()
        } else {
            // Fallback
            instruction = title ?: text
        }
        
        // 3. ETA
        val etaKey = config.fields[HudDataType.ETA.name]
        var eta: String? = null
        if (etaKey != null) {
            eta = extras.getCharSequence(etaKey)?.toString()
        } else {
            // Fallback: usually text contains ETA if title contains instruction
            eta = text
        }
        
        // 4. Remaining Time
        val timeKey = config.fields[HudDataType.REMAINING_TIME.name]
        var remainingTime: String? = null
        if (timeKey != null) {
            remainingTime = extras.getCharSequence(timeKey)?.toString()
        }
        
        // 5. Traffic Score
        val trafficKey = config.fields[HudDataType.TRAFFIC_SCORE.name]
        var trafficScore: Int? = null
        if (trafficKey != null) {
            val rawTraffic = extras.getCharSequence(trafficKey)?.toString()
            trafficScore = rawTraffic?.toIntOrNull()
        }
        
        // 6. Speed Limit (if available in notif)
        val limitKey = config.fields[HudDataType.SPEED_LIMIT.name]
        var speedLimit: Int? = null
        if (limitKey != null) {
            val rawLimit = extras.getCharSequence(limitKey)?.toString()
            speedLimit = rawLimit?.toIntOrNull()
        }
        
        if (instruction == null && distance == null) return
        
        val navData = NavigationData(
            instruction = instruction,
            distance = distance,
            eta = eta,
            speedLimit = speedLimit,
            isNavigating = true
        )
        
        // Update Universal State
        HudState.isNavigating = true
        HudState.distanceToTurn = distance
        HudState.eta = eta
        HudState.remainingTime = remainingTime
        HudState.trafficScore = trafficScore
        if (speedLimit != null) HudState.speedLimit = speedLimit
        
        // 7. Arrow Image (Large Icon or Picture)
        val largeIcon = extras.getParcelable<android.graphics.Bitmap>("android.largeIcon") 
            ?: (extras.getParcelable<android.graphics.drawable.Icon>("android.largeIcon")?.loadDrawable(this)?.let { ImageUtils.drawableToBitmap(it) })
        
        val picture = extras.getParcelable<android.graphics.Bitmap>("android.picture")
        
        if (largeIcon != null) {
            Log.d(TAG, "Found LargeIcon in notification: ${largeIcon.width}x${largeIcon.height}")
            HudService.navDebug.lastArrowBitmap = largeIcon
            val arrowImage = ArrowImage(largeIcon)
            val arrow = ArrowDirection.recognize(arrowImage)
            if (arrow != ArrowDirection.NONE) {
                HudState.turnIcon = arrow.hudCode
                Log.d(TAG, "Recognized arrow from LargeIcon: $arrow")
            }
        } else if (picture != null) {
            Log.d(TAG, "Found Picture in notification: ${picture.width}x${picture.height}")
            HudService.navDebug.lastArrowBitmap = picture
            val arrowImage = ArrowImage(picture)
            val arrow = ArrowDirection.recognize(arrowImage)
            if (arrow != ArrowDirection.NONE) {
                HudState.turnIcon = arrow.hudCode
                Log.d(TAG, "Recognized arrow from Picture: $arrow")
            }
        } else {
            HudService.navDebug.lastArrowBitmap = null
            // Fallback: Parse arrow from text instruction
            if (instruction != null) {
                val arrow = parseTextToArrow(instruction)
                if (arrow != ArrowDirection.NONE) {
                    HudState.turnIcon = arrow.hudCode
                    Log.d(TAG, "Parsed arrow from text '$instruction': $arrow")
                }
            }
        }
        
        HudService.navDebug.parsedInstruction = instruction ?: ""
        HudService.navDebug.parsedDistance = distance ?: ""
        HudService.navDebug.parsedEta = eta ?: ""
        
        Log.d(TAG, "Parsed navigation data: $navData")
        onNavigationUpdate?.invoke(navData)
        HudState.notifyUpdate()
    }
    
    private fun parseTextToArrow(text: String): ArrowDirection {
        val t = text.lowercase()
        return when {
            "u-turn" in t || "разворот" in t -> ArrowDirection.SHARP_LEFT // Or specific code if available
            "sharp left" in t || "резко налево" in t -> ArrowDirection.SHARP_LEFT
            "sharp right" in t || "резко направо" in t -> ArrowDirection.SHARP_RIGHT
            "left" in t || "налево" in t -> ArrowDirection.LEFT
            "right" in t || "направо" in t -> ArrowDirection.RIGHT
            "keep left" in t || "левее" in t -> ArrowDirection.KEEP_LEFT
            "keep right" in t || "правее" in t -> ArrowDirection.KEEP_RIGHT
            "straight" in t || "прямо" in t -> ArrowDirection.STRAIGHT
            else -> ArrowDirection.NONE
        }
    }
    
    private fun extractDistance(text: String?): String? {
        if (text == null) return null
        
        // Ищем паттерны: "500 m", "500 м", "1.5 km", "1.5 км"
        // Added support for comma/dot decimal separator
        val distanceRegex = """(\d+(?:[.,]\d+)?)\s*(m|м|km|км)""".toRegex(RegexOption.IGNORE_CASE)
        val match = distanceRegex.find(text)
        
        return match?.value
    }
}
