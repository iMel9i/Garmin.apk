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
        
        // Пакеты приложений навигации
        private const val PKG_GOOGLE_MAPS = "com.google.android.apps.maps"
        private const val PKG_YANDEX_MAPS = "ru.yandex.yandexmaps"
        private const val PKG_YANDEX_NAVI = "ru.yandex.yandexnavi"
        
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
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Navigation notification listener created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Navigation notification listener destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Проверяем, является ли это уведомлением от навигационного приложения
        if (packageName in listOf(PKG_GOOGLE_MAPS, PKG_YANDEX_MAPS, PKG_YANDEX_NAVI)) {
            parseNotification(sbn)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        if (packageName in listOf(PKG_GOOGLE_MAPS, PKG_YANDEX_MAPS, PKG_YANDEX_NAVI)) {
            Log.d(TAG, "Navigation notification removed from: $packageName")
            // Сообщаем, что навигация завершена
            onNavigationUpdate?.invoke(NavigationData(isNavigating = false))
        }
    }
    
    private fun parseNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()
        
        Log.d(TAG, "Notification from ${sbn.packageName}")
        Log.d(TAG, "  Title: $title")
        Log.d(TAG, "  Text: $text")
        Log.d(TAG, "  BigText: $bigText")
        
        // Парсинг зависит от приложения
        when (sbn.packageName) {
            PKG_GOOGLE_MAPS -> parseGoogleMapsNotification(title, text, bigText)
            PKG_YANDEX_MAPS, PKG_YANDEX_NAVI -> parseYandexNotification(title, text, bigText)
        }
    }
    
    private fun parseGoogleMapsNotification(title: String?, text: String?, bigText: String?) {
        // Примерный формат Google Maps:
        // Title: "In 500 m, turn left"
        // Text: "10 min (3.5 km)"
        
        if (title == null && text == null) return
        
        val navData = NavigationData(
            instruction = title,
            distance = extractDistance(title),
            eta = text,
            isNavigating = true
        )
        
        Log.d(TAG, "Google Maps navigation: $navData")
        onNavigationUpdate?.invoke(navData)
    }
    
    private fun parseYandexNotification(title: String?, text: String?, bigText: String?) {
        // Примерный формат Yandex:
        // Title: "Через 500 м поверните налево"
        // Text: "10 мин, 3.5 км"
        
        if (title == null && text == null) return
        
        val navData = NavigationData(
            instruction = title,
            distance = extractDistance(title),
            eta = text,
            isNavigating = true
        )
        
        Log.d(TAG, "Yandex navigation: $navData")
        onNavigationUpdate?.invoke(navData)
    }
    
    private fun extractDistance(text: String?): String? {
        if (text == null) return null
        
        // Ищем паттерны: "500 m", "500 м", "1.5 km", "1.5 км"
        val distanceRegex = """(\d+(?:\.\d+)?)\s*(m|м|km|км)""".toRegex(RegexOption.IGNORE_CASE)
        val match = distanceRegex.find(text)
        
        return match?.value
    }
}
