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
    }
    
    private lateinit var hud: GarminHudLite
    private lateinit var locationManager: LocationManager
    private var currentSpeed: Float = 0f
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var updateTimer: Timer? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Инициализация..."))
        
        hud = GarminHudLite(this)
        hud.onConnectionStateChanged = { connected, deviceName ->
            if (connected) {
                updateNotification("Подключено: $deviceName")
                saveDeviceInfo(hud.getConnectedDeviceName(), hud.getConnectedDeviceAddress())
                startUpdates()
            } else {
                updateNotification("Отключено")
                stopUpdates()
            }
        }
        
        setupLocationManager()
        autoConnectToSavedDevice()
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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceAddress = prefs.getString(KEY_DEVICE_ADDRESS, null)
        val deviceName = prefs.getString(KEY_DEVICE_NAME, null)
        
        if (deviceAddress != null && deviceName != null) {
            Log.d(TAG, "Auto-connecting to saved device: $deviceName ($deviceAddress)")
            updateNotification("Подключение к $deviceName...")
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
        
        // Отправляем текущее время
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        hud.setTime(hour, minute)
        
        // Отправляем скорость
        val speedKmh = (currentSpeed * 3.6f).toInt() // m/s to km/h
        if (speedKmh > 0) {
            hud.setSpeed(speedKmh, showIcon = false)
        }
        
        Log.d(TAG, "Updated HUD: time=$hour:$minute, speed=$speedKmh km/h")
    }
    
    override fun onLocationChanged(location: Location) {
        currentSpeed = location.speed
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GHUD Lite")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopUpdates()
        locationManager.removeUpdates(this)
        hud.disconnect()
    }
}
