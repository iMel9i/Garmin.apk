package iMel9i.garminhud.lite

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class HudService : Service(), LocationListener {
    
    companion object {
        private const val TAG = "HudService"
        private const val CHANNEL_ID = "HudServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "HudPrefs"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val OK_DISPLAY_DURATION_MS = 2000L
        private const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        
        // Debug data for UI
        data class OsmDebugData(
            var lastQuery: String = "",
            var lastResponse: String = "",
            var currentSpeedLimit: Int? = null,
            var camerasFound: Int = 0,
            var nearestCameraDistance: Int? = null,
            var lastUpdateTime: String = "",
            var lastLocation: String = ""
        )
        
        data class NavigationDebugData(
            var packageName: String = "",
            var title: String = "",
            var text: String = "",
            var bigText: String = "",
            var parsedInstruction: String = "",
            var parsedDistance: String = "",
            var parsedEta: String = "",
            var lastUpdateTime: String = ""
        )
        
        data class HudDebugData(
            var lastCommand: String = "",
            var currentSpeed: Int = 0,
            var displayedSpeedLimit: Int? = null,
            var showingSpeedingIcon: Boolean = false,
            var showingCameraIcon: Boolean = false,
            var currentDirection: String = "",
            var currentDistance: String = "",
            var lastUpdateTime: String = ""
        )
        
        val osmDebug = OsmDebugData()
        val navDebug = NavigationDebugData()
        val hudDebug = HudDebugData()
    }
    
    private lateinit var hud: GarminHudLite
    private lateinit var locationManager: LocationManager
    private var currentSpeed: Float = 0f
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var updateTimer: Timer? = null
    private val reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isServiceRunning = false
    private var isNavigating = false
    private var currentNavigationData: NavigationNotificationListener.NavigationData? = null
    
    private val reconnectRunnable = Runnable {
        if (isServiceRunning && !hud.isConnected()) {
            autoConnectToSavedDevice()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        isServiceRunning = true
        
        createNotificationChannel()
        
        val notification = createNotification("Инициализация...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            try {
                startForeground(
                    NOTIFICATION_ID, 
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service with types", e)
                startForeground(NOTIFICATION_ID, notification)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-11
            startForeground(
                NOTIFICATION_ID, 
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            // Android 9 and below
            startForeground(NOTIFICATION_ID, notification)
        }
        
        hud = GarminHudLite(this)
        hud.onConnectionStateChanged = { connected, deviceName ->
            if (connected) {
                updateNotification("Подключено: $deviceName")
                saveDeviceInfo(hud.getConnectedDeviceName(), hud.getConnectedDeviceAddress())
                
                // Stop any pending reconnection attempts
                reconnectHandler.removeCallbacks(reconnectRunnable)
                
                // Immediately start updates
                startUpdates()
                applyBrightness()
                
                // Force an immediate update to clear "OK" or default state
                updateHud()
                
            } else {
                updateNotification("Отключено. Попытка подключения...")
                stopUpdates()
                
                // Start reconnection loop if service is still running
                if (isServiceRunning) {
                    reconnectHandler.removeCallbacks(reconnectRunnable) // Ensure we don't have multiple callbacks
                    reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS)
                }
            }
        }
        
        setupLocationManager()
        setupNavigationListener()
        autoConnectToSavedDevice()
    }
    
    private fun setupNavigationListener() {
        // Подключаемся к NavigationNotificationListener
        NavigationNotificationListener.onNavigationUpdate = { navData ->
            currentNavigationData = navData
            isNavigating = navData.isNavigating
            
            if (isNavigating) {
                Log.d(TAG, "Navigation active: ${navData.instruction}, ${navData.distance}")
            } else {
                Log.d(TAG, "Navigation inactive")
            }
        }
    }
    
    private fun applyBrightness() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoBrightness = prefs.getBoolean("auto_brightness", true)
        if (autoBrightness) {
            hud.setBrightness(0) // Auto
        } else {
            hud.setBrightness(10) // Max manual
        }
    }
    
    private fun setupLocationManager() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 second
                0f,   // 0 meters
                this
            )
        }
    }
    
    private fun autoConnectToSavedDevice() {
        if (hud.isConnected()) return
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceAddress = prefs.getString(KEY_DEVICE_ADDRESS, null)
        val deviceName = prefs.getString(KEY_DEVICE_NAME, null)
        
        if (deviceAddress != null) {
            Log.d(TAG, "Connecting to saved device: $deviceName ($deviceAddress)")
            hud.connectToDevice(deviceAddress)
        } else {
            updateNotification("Нет сохраненного устройства")
        }
    }
    
    private fun saveDeviceInfo(name: String?, address: String?) {
        if (name != null && address != null) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_DEVICE_NAME, name)
                .putString(KEY_DEVICE_ADDRESS, address)
                .apply()
            Log.d(TAG, "Saved device: $name ($address)")
        }
    }
    
    private fun startUpdates() {
        updateTimer = Timer()
        updateTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateHud()
            }
        }, 0, 1000) // Update every second
    }
    
    private fun stopUpdates() {
        updateTimer?.cancel()
        updateTimer = null
    }
    
    private fun updateHud() {
        if (!hud.isConnected()) return
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val showTime = prefs.getBoolean("show_time", true)
        val showSpeed = prefs.getBoolean("show_speed", true)
        val gmapsEnabled = prefs.getBoolean("gmaps_integration", false)
        
        if (gmapsEnabled && isNavigating && currentNavigationData != null) {
            // Режим навигации: показываем направление и расстояние
            updateNavigationMode()
        } else {
            // Обычный режим: показываем время и скорость
            updateNormalMode(showTime, showSpeed)
        }
    }
    
    private fun updateNavigationMode() {
        val navData = currentNavigationData ?: return
        
        // Отображаем направление (пока используем straight, нужно парсить из instruction)
        // TODO: Распознавать направление из текста инструкции
        val direction = parseDirection(navData.instruction)
        hud.setDirection(direction)
        
        // Отображаем расстояние
        navData.distance?.let { distStr ->
            val (value, unit) = parseDistanceToMeters(distStr)
            if (value != null) {
                hud.setDistance(value, unit)
            }
        }
        
        // Показываем скорость (если есть данные от навигации или GPS)
        val speedKmh = navData.speed ?: (currentSpeed * 3.6f).toInt()
        if (speedKmh > 0) {
            hud.setSpeed(speedKmh, showIcon = false)
        }
    }
    
    private val osmClient = OsmClient()
    private var currentOsmSpeedLimit: Int? = null
    private var nearbyCameras: List<OsmClient.CameraLocation> = emptyList()
    private var lastOsmUpdateLocation: Location? = null
    private val OSM_UPDATE_DISTANCE_METERS = 500f
    private var distanceToCamera: Int? = null

    private fun updateNormalMode(showTime: Boolean, showSpeed: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Отправляем текущее время
        if (showTime) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            hud.setTime(hour, minute)
        }
        
        // Отправляем скорость с предупреждением о превышении лимита
        if (showSpeed) {
            val speedKmh = (currentSpeed * 3.6f).toInt() // m/s to km/h
            
            val manualLimitEnabled = prefs.getBoolean("manual_speed_limit_enabled", false)
            val manualLimit = prefs.getInt("speed_limit", 71)
            val speedingThreshold = prefs.getInt("speeding_threshold", 10) // Порог превышения (по умолчанию +10 км/ч)
            
            // Priority: OSM -> Manual -> Default (71)
            // If OSM limit is available, use it. Otherwise fallback to manual or default.
            val effectiveLimit = currentOsmSpeedLimit ?: (if (manualLimitEnabled) manualLimit else 71)
            
            // Show limit if speed >= limit (as before, to keep "display as it is now")
            val showLimit = speedKmh >= effectiveLimit
            
            // Show speeding icon if speed >= limit + threshold
            val isSpeeding = speedKmh >= (effectiveLimit + speedingThreshold)
            
            val showCamera = distanceToCamera != null
            
            // Используем новый метод для отображения скорости с лимитом и иконкой
            hud.setSpeedWithLimit(
                currentSpeed = speedKmh,
                speedLimit = if (showLimit) effectiveLimit else null,
                showSpeedingIcon = isSpeeding,
                showCameraIcon = showCamera
            )
            
            // Update debug data
            hudDebug.currentSpeed = speedKmh
            hudDebug.displayedSpeedLimit = if (showLimit) effectiveLimit else null
            hudDebug.showingSpeedingIcon = isSpeeding
            hudDebug.showingCameraIcon = showCamera
            hudDebug.lastCommand = "Speed: $speedKmh km/h, Limit: ${if (showLimit) effectiveLimit else "none"}, Speeding: $isSpeeding, Camera: $showCamera"
            hudDebug.lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            
            // Если есть камера, показываем расстояние вместо времени
            if (showCamera && distanceToCamera != null) {
                hud.setDistance(distanceToCamera!!, 2) // meters
                hudDebug.currentDistance = "${distanceToCamera}m"
            } else {
                hud.clearDistance()
                hudDebug.currentDistance = ""
            }
        }
    }
    
    private fun updateOsmData(location: Location) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        osmDebug.lastLocation = String.format("%.6f, %.6f", location.latitude, location.longitude)
        osmDebug.lastUpdateTime = timeFormat.format(Date())
        
        osmClient.getSpeedLimit(location.latitude, location.longitude) { limit ->
            currentOsmSpeedLimit = limit
            osmDebug.currentSpeedLimit = limit
        }
        
        osmClient.getCameras(location.latitude, location.longitude, 1000) { cameras ->
            nearbyCameras = cameras
            osmDebug.camerasFound = cameras.size
        }
    }
    
    private fun checkCameras(location: Location) {
        var minDist: Float? = null
        
        for (cam in nearbyCameras) {
            val res = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, cam.lat, cam.lon, res)
            val dist = res[0]
            if (minDist == null || dist < minDist) {
                minDist = dist
            }
        }
        
        // Показываем предупреждение о камере если расстояние <= 300м (было 100м)
        distanceToCamera = if (minDist != null && minDist <= 300) minDist.toInt() else null
        osmDebug.nearestCameraDistance = distanceToCamera
    }
    
    private fun parseDirection(instruction: String?): Int {
        if (instruction == null) return 0x10 // Straight
        
        val instr = instruction.lowercase()
        return when {
            // Sharp Left
            "sharp left" in instr || "резко налево" in instr || "круто налево" in instr -> 0x80
            // Left
            "turn left" in instr || "поверните налево" in instr || "налево" in instr -> 0x40
            // Easy Left
            "keep left" in instr || "bear left" in instr || "держитесь левее" in instr || "левее" in instr || "немного налево" in instr -> 0x20
            
            // Sharp Right
            "sharp right" in instr || "резко направо" in instr || "круто направо" in instr -> 0x02
            // Right
            "turn right" in instr || "поверните направо" in instr || "направо" in instr -> 0x04
            // Easy Right
            "keep right" in instr || "bear right" in instr || "держитесь правее" in instr || "правее" in instr || "немного направо" in instr -> 0x08
            
            else -> 0x10 // Straight
        }
    }
    
    private fun parseDistanceToMeters(distStr: String): Pair<Float?, Int> {
        // Парсим строки типа "500 m", "1.5 km"
        val regex = """(\d+(?:\.\d+)?)\s*(m|м|km|км)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(distStr) ?: return Pair(null, 1)
        
        val value = match.groupValues[1].toFloatOrNull() ?: return Pair(null, 1)
        val unitStr = match.groupValues[2].lowercase()
        
        return when (unitStr) {
            "km", "км" -> Pair(value, 1) // Return as is (e.g. 1.2), unit=1 (km)
            else -> Pair(value, 2) // Return as is (e.g. 500), unit=2 (m)
        }
    }
    
    override fun onLocationChanged(location: Location) {
        currentSpeed = location.speed
        
        val lastLoc = lastOsmUpdateLocation
        if (lastLoc == null || location.distanceTo(lastLoc) > OSM_UPDATE_DISTANCE_METERS) {
            lastOsmUpdateLocation = location
            updateOsmData(location)
        }
        
        checkCameras(location)
    }
    
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HUD Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый сервис для Garmin HUD"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        val stopIntent = Intent(this, HudService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GHUD Lite")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Выход", stopPendingIntent)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "RESTART_HUD") {
            restartHud()
        } else if (intent?.action == ACTION_STOP_SERVICE) {
            // Broadcast to close MainActivity
            val closeIntent = Intent("CLOSE_APP")
            closeIntent.setPackage(packageName)
            sendBroadcast(closeIntent)
            
            // Stop service
            stopSelf()
        }
        return START_STICKY
    }
    
    private fun restartHud() {
        if (hud.isConnected()) {
            hud.disconnect()
            // Reconnection will be handled by the onConnectionStateChanged -> reconnectRunnable logic
            Toast.makeText(this, "HUD перезагружается...", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isServiceRunning = false
        stopUpdates()
        reconnectHandler.removeCallbacks(reconnectRunnable)
        locationManager.removeUpdates(this)
        NavigationNotificationListener.onNavigationUpdate = null
        hud.disconnect()
    }
}
