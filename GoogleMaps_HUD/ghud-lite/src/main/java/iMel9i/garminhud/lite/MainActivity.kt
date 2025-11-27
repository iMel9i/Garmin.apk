package iMel9i.garminhud.lite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.akexorcist.bluetotohspp.library.BluetoothState

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private lateinit var statusText: TextView
    private lateinit var btnSelectHud: Button
    private lateinit var btnStartService: Button
    private lateinit var hud: GarminHudLite
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        btnSelectHud = findViewById(R.id.btnSelectHud)
        btnStartService = findViewById(R.id.btnStartService)
        
        hud = GarminHudLite(this)
        
        btnSelectHud.setOnClickListener {
            if (checkPermissions()) {
                hud.scanForDevice()
            } else {
                requestPermissions()
            }
        }
        
        btnStartService.setOnClickListener {
            if (checkPermissions()) {
                startHudService()
            } else {
                requestPermissions()
            }
        }
        
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopHudService()
        }
        
        // Проверяем разрешения при запуске
        if (!checkPermissions()) {
            requestPermissions()
        }
        
        updateStatus()
    }
    
    private fun updateStatus() {
        val prefs = getSharedPreferences("HudPrefs", MODE_PRIVATE)
        val deviceName = prefs.getString("device_name", null)
        
        if (deviceName != null) {
            statusText.text = "Сохранено: $deviceName"
        } else {
            statusText.text = "HUD не выбран"
        }
    }
    
    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= 31) { // Android 12 (S)
            permissions.add("android.permission.BLUETOOTH_CONNECT")
            permissions.add("android.permission.BLUETOOTH_SCAN")
        }
        
        return permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= 31) { // Android 12 (S)
            permissions.add("android.permission.BLUETOOTH_CONNECT")
            permissions.add("android.permission.BLUETOOTH_SCAN")
        }
        
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                statusText.text = "Разрешения получены"
            } else {
                statusText.text = "Необходимы все разрешения для работы"
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (hud.handleActivityResult(requestCode, resultCode, data)) {
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
                // Устройство выбрано, сохраняем
                val prefs = getSharedPreferences("HudPrefs", MODE_PRIVATE)
                val deviceName = data?.getStringExtra(BluetoothState.EXTRA_DEVICE_NAME)
                val deviceAddress = data?.getStringExtra(BluetoothState.EXTRA_DEVICE_ADDRESS)
                
                if (deviceName != null && deviceAddress != null) {
                    prefs.edit()
                        .putString("device_name", deviceName)
                        .putString("device_address", deviceAddress)
                        .apply()
                    
                    statusText.text = "Выбрано: $deviceName"
                }
            }
        }
    }
    
    private fun startHudService() {
        val prefs = getSharedPreferences("HudPrefs", MODE_PRIVATE)
        val deviceAddress = prefs.getString("device_address", null)
        
        if (deviceAddress == null) {
            statusText.text = "Сначала выберите HUD"
            return
        }
        
        val intent = Intent(this, HudService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Сервис запущен"
    }
    
    private fun stopHudService() {
        val intent = Intent(this, HudService::class.java)
        stopService(intent)
        statusText.text = "Сервис остановлен"
    }
}
