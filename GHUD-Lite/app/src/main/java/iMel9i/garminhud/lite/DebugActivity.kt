package iMel9i.garminhud.lite

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DebugActivity : AppCompatActivity() {
    
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateDebugInfo()
            updateHandler.postDelayed(this, 1000) // Update every second
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        
        title = "Debug Information"
        
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            updateDebugInfo()
        }
        
        findViewById<Button>(R.id.btnAppSettings).setOnClickListener {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        
        updateDebugInfo()
    }
    
    override fun onResume() {
        super.onResume()
        updateHandler.post(updateRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
    }
    
    private fun updateDebugInfo() {
        // OSM Debug
        findViewById<TextView>(R.id.osmLocation).text = "Location: ${HudService.osmDebug.lastLocation}"
        findViewById<TextView>(R.id.osmSpeedLimit).text = "Speed Limit: ${HudService.osmDebug.currentSpeedLimit ?: "none"} km/h"
        findViewById<TextView>(R.id.osmCameras).text = "Cameras Found: ${HudService.osmDebug.camerasFound}"
        findViewById<TextView>(R.id.osmNearestCamera).text = "Nearest Camera: ${HudService.osmDebug.nearestCameraDistance?.let { "${it}m" } ?: "none"}"
        findViewById<TextView>(R.id.osmUpdateTime).text = "Last Update: ${HudService.osmDebug.lastUpdateTime}"
        
        // Navigation Debug
        findViewById<TextView>(R.id.navPackage).text = "Package: ${HudService.navDebug.packageName}"
        findViewById<TextView>(R.id.navTitle).text = "Title: ${HudService.navDebug.title}"
        findViewById<TextView>(R.id.navText).text = "Text: ${HudService.navDebug.text}"
        findViewById<TextView>(R.id.navBigText).text = "BigText: ${HudService.navDebug.bigText}"
        findViewById<TextView>(R.id.navParsedInstruction).text = "Parsed Instruction: ${HudService.navDebug.parsedInstruction}"
        findViewById<TextView>(R.id.navParsedDistance).text = "Parsed Distance: ${HudService.navDebug.parsedDistance}"
        findViewById<TextView>(R.id.navParsedEta).text = "Parsed ETA: ${HudService.navDebug.parsedEta}"
        findViewById<TextView>(R.id.navUpdateTime).text = "Last Update: ${HudService.navDebug.lastUpdateTime}"
        
        // HUD Debug
        findViewById<TextView>(R.id.hudSpeed).text = "Current Speed: ${HudService.hudDebug.currentSpeed} km/h"
        findViewById<TextView>(R.id.hudSpeedLimit).text = "Displayed Speed Limit: ${HudService.hudDebug.displayedSpeedLimit?.let { "${it} km/h" } ?: "none"}"
        findViewById<TextView>(R.id.hudSpeedingIcon).text = "Speeding Icon: ${if (HudService.hudDebug.showingSpeedingIcon) "ON" else "OFF"}"
        findViewById<TextView>(R.id.hudCameraIcon).text = "Camera Icon: ${if (HudService.hudDebug.showingCameraIcon) "ON" else "OFF"}"
        findViewById<TextView>(R.id.hudDirection).text = "Direction: ${HudService.hudDebug.currentDirection}"
        findViewById<TextView>(R.id.hudDistance).text = "Distance: ${HudService.hudDebug.currentDistance}"
        findViewById<TextView>(R.id.hudLastCommand).text = "Last Command: ${HudService.hudDebug.lastCommand}"
        findViewById<TextView>(R.id.hudUpdateTime).text = "Last Update: ${HudService.hudDebug.lastUpdateTime}"
    }
}
