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
        
        // Extract fields based on config
        // We look for keys "title", "text", "bigText" in the config fields map
        // If not found, we use default android keys
        val titleKey = config.fields["title"] ?: "android.title"
        val textKey = config.fields["text"] ?: "android.text"
        val bigTextKey = config.fields["bigText"] ?: "android.bigText"
        
        val title = extras.getCharSequence(titleKey)?.toString()
        val text = extras.getCharSequence(textKey)?.toString()
        val bigText = extras.getCharSequence(bigTextKey)?.toString()
        
        Log.d(TAG, "Notification from ${sbn.packageName}")
        Log.d(TAG, "  Title ($titleKey): $title")
        Log.d(TAG, "  Text ($textKey): $text")
        Log.d(TAG, "  BigText ($bigTextKey): $bigText")
        
        // Update debug data
        HudService.navDebug.packageName = sbn.packageName
        HudService.navDebug.title = title ?: ""
        HudService.navDebug.text = text ?: ""
        HudService.navDebug.bigText = bigText ?: ""
        HudService.navDebug.lastUpdateTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Generic parsing logic
        // 1. Instruction is usually in title
        // 2. Distance can be in title (e.g. "In 500m...") or text
        // 3. ETA is usually in text
        
        val instruction = title ?: text
        
        // Try to find distance in title first, then text, then bigText
        val distance = extractDistance(title) ?: extractDistance(text) ?: extractDistance(bigText)
        
        // ETA is usually the rest of the text
        val eta = text
        
        if (instruction == null && distance == null) return
        
        val navData = NavigationData(
            instruction = instruction,
            distance = distance,
            eta = eta,
            isNavigating = true
        )
        
        // Update debug data for parsed values
        HudService.navDebug.parsedInstruction = navData.instruction ?: ""
        HudService.navDebug.parsedDistance = navData.distance ?: ""
        HudService.navDebug.parsedEta = navData.eta ?: ""
        
        Log.d(TAG, "Parsed navigation data: $navData")
        onNavigationUpdate?.invoke(navData)
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
