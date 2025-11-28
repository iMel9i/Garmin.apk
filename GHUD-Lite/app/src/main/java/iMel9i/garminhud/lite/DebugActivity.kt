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
        findViewById<TextView>(R.id.osmSpeedLimit).text = "Speed Limit: ${HudState.speedLimit ?: "none"} km/h"
        findViewById<TextView>(R.id.osmCameras).text = "Cameras Found: ${HudService.osmDebug.camerasFound}"
        findViewById<TextView>(R.id.osmNearestCamera).text = "Nearest Camera: ${HudState.cameraDistance?.let { "${it}m" } ?: "none"}"
        findViewById<TextView>(R.id.osmUpdateTime).text = "Last Update: ${HudService.osmDebug.lastUpdateTime}"
        
        // Navigation Debug
        findViewById<TextView>(R.id.navPackage).text = "Package: ${HudState.lastPackageName}"
        
        // Display ALL raw extras
        val allExtras = HudState.rawData.entries.joinToString("\n") { (key, value) ->
            "  $key: $value"
        }
        findViewById<TextView>(R.id.navTitle).text = "All Extras:\n$allExtras"
        findViewById<TextView>(R.id.navText).text = "Parsed Instruction: ${HudService.navDebug.parsedInstruction}"
        findViewById<TextView>(R.id.navBigText).text = "Parsed Distance: ${HudState.distanceToTurn}"
        
        findViewById<TextView>(R.id.navParsedInstruction).text = "Parsed ETA: ${HudState.eta}"
        findViewById<TextView>(R.id.navParsedDistance).text = "Traffic Score: ${HudState.trafficScore ?: "none"}"
        findViewById<TextView>(R.id.navParsedEta).text = "Remaining Time: ${HudState.remainingTime ?: "none"}"
        findViewById<TextView>(R.id.navUpdateTime).text = "Last Update: ${HudService.navDebug.lastUpdateTime}"
        
        // Extracted Arrow Image
        val arrowImg = findViewById<android.widget.ImageView>(R.id.navArrowImage)
        if (HudService.navDebug.lastArrowBitmap != null) {
            arrowImg.setImageBitmap(HudService.navDebug.lastArrowBitmap)
            arrowImg.visibility = android.view.View.VISIBLE
        } else {
            arrowImg.setImageDrawable(null)
            arrowImg.visibility = android.view.View.GONE
        }
        
        // HUD Debug
        findViewById<TextView>(R.id.hudSpeed).text = "Current Speed: ${HudState.currentSpeed} km/h"
        findViewById<TextView>(R.id.hudSpeedLimit).text = "Displayed Speed Limit: ${HudState.speedLimit?.let { "${it} km/h" } ?: "none"}"
        findViewById<TextView>(R.id.hudSpeedingIcon).text = "Speeding Icon: ${if (HudState.isSpeeding) "ON" else "OFF"}"
        findViewById<TextView>(R.id.hudCameraIcon).text = "Camera Icon: ${if (HudState.cameraDistance != null) "ON" else "OFF"}"
        findViewById<TextView>(R.id.hudDirection).text = "Direction: ${HudService.hudDebug.currentDirection}"
        findViewById<TextView>(R.id.hudDistance).text = "Distance: ${HudService.hudDebug.currentDistance}"
        findViewById<TextView>(R.id.hudLastCommand).text = "Last Command: ${HudService.hudDebug.lastCommand}"
        findViewById<TextView>(R.id.hudUpdateTime).text = "Last Update: ${HudService.hudDebug.lastUpdateTime}"
        
        // === Virtual HUD Update ===
        updateVirtualHud()
    }
    
    private fun updateVirtualHud() {
        // Use the same logic as HudService to determine what to show
        // Ideally this logic should be in a shared ViewModel or Helper, but for now we duplicate/adapt it
        // to show what *would* be on the HUD based on current State + Config.
        
        val layoutManager = LayoutConfigManager(this)
        // Determine mode (simplified logic for debug)
        val mode = if (HudState.isNavigating) {
             if (HudState.lastPackageName?.contains("yandex") == true) "YANDEX" else "GOOGLE"
        } else {
            "IDLE"
        }
        val profile = layoutManager.getProfile(mode)
        
        // 1. Direction
        val vDirection = findViewById<TextView>(R.id.vHudDirection)
        val arrowType = profile.slots[HudSlot.DIRECTION_ARROW]
        if (arrowType == HudDataType.DISTANCE_TO_TURN || arrowType == HudDataType.NONE) {
             if (HudState.isNavigating) {
                 vDirection.text = "↰" // Placeholder for turn icon
                 vDirection.setTextColor(android.graphics.Color.GREEN)
             } else {
                 vDirection.text = "↑"
                 vDirection.setTextColor(android.graphics.Color.DKGRAY)
             }
        }
        
        // 2. Main Number
        val vMain = findViewById<TextView>(R.id.vHudMain)
        val mainType = profile.slots[HudSlot.MAIN_NUMBER]
        val mainValue = getValueForType(mainType)
        vMain.text = mainValue ?: "--"
        
        // 3. Secondary Number
        val vSecondary = findViewById<TextView>(R.id.vHudSecondary)
        val secType = profile.slots[HudSlot.SECONDARY_NUMBER]
        val secValue = getValueForType(secType)
        vSecondary.text = secValue ?: "--:--"
        
        // Icons
        findViewById<android.view.View>(R.id.vHudIconSpeeding).visibility = 
            if (HudState.isSpeeding) android.view.View.VISIBLE else android.view.View.INVISIBLE
            
        findViewById<android.view.View>(R.id.vHudIconCamera).visibility = 
            if (HudState.cameraDistance != null) android.view.View.VISIBLE else android.view.View.INVISIBLE
    }
    
    private fun getValueForType(type: HudDataType?): String? {
        return when (type) {
            HudDataType.CURRENT_SPEED -> "${HudState.currentSpeed}"
            HudDataType.SPEED_LIMIT -> "${HudState.speedLimit}"
            HudDataType.CURRENT_TIME -> HudState.currentTime
            HudDataType.DISTANCE_TO_TURN -> HudState.distanceToTurn
            HudDataType.ETA -> HudState.eta
            HudDataType.REMAINING_TIME -> HudState.remainingTime
            HudDataType.TRAFFIC_SCORE -> HudState.trafficScore?.toString()
            HudDataType.DISTANCE_TO_CAMERA -> "${HudState.cameraDistance}m"
            else -> null
        }
    }
}
