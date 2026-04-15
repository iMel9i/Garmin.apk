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
        var enabled = true // ВКЛЮЧЕНО по умолчанию, так как AccessibilityService screenshot не работает
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
        DebugLog.i(TAG, "Navigation notification listener created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        DebugLog.i(TAG, "Navigation notification listener destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Log EVERY notification from navigation apps to debug
        if (packageName.contains("yandex") || packageName.contains("maps") || packageName.contains("nav")) {
            DebugLog.i(TAG, "Notification received from: $packageName")
        }
        
        if (!enabled) return // Пропускаем если выключено
        
        // Check if Yandex notifications are disabled
        if (packageName.startsWith("ru.yandex")) {
            val prefs = getSharedPreferences("HudPrefs", MODE_PRIVATE)
            val yandexEnabled = prefs.getBoolean("yandex_notifications_enabled", true)
            if (!yandexEnabled) {
                DebugLog.d(TAG, "Yandex notifications disabled, skipping")
                return
            }
        }
        
        var config = configManager.getConfigs().find { it.packageName == packageName && it.enabled }
        
        // Fallback for Yandex if not in config
        if (config == null && packageName.startsWith("ru.yandex")) {
            config = AppConfigManager.DEFAULT_CONFIGS.find { it.packageName == packageName }
            if (config != null) {
                DebugLog.i(TAG, "Using default config for $packageName")
            }
        }
        
        if (config != null) {
            parseNotification(sbn, config)
        }
    }

    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val config = configManager.getConfigs().find { it.packageName == packageName && it.enabled }
        
        if (config != null) {
            DebugLog.i(TAG, "Navigation notification removed from: $packageName")
            // Сообщаем, что навигация завершена
            onNavigationUpdate?.invoke(NavigationData(isNavigating = false))
        }
    }
    
    private fun parseNotification(sbn: StatusBarNotification, config: AppConfigManager.AppConfig) {
        val notification = sbn.notification
        val extras = notification.extras
        
        DebugLog.i(TAG, "=== PARSING NOTIFICATION FROM ${sbn.packageName} ===")
        
        // LOG ALL EXTRAS KEYS for debugging
        val allExtras = mutableMapOf<String, String>()
        extras.keySet().forEach { key ->
            val value = extras.get(key)
            allExtras[key] = value?.toString() ?: "null"
            DebugLog.d(TAG, "  Extra: $key = $value")
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
        }
        
        if (distance == null) {
            // Fallback: try standard fields
            distance = extractDistance(title) ?: extractDistance(text) ?: extractDistance(bigText)
        }
        
        // 2. Instruction / Direction
        val instructionKey = config.fields[HudDataType.NAVIGATION_INSTRUCTION.name]
        var instruction: String? = null
        if (instructionKey != null) {
            instruction = extras.getCharSequence(instructionKey)?.toString()
        }
        
        if (instruction == null) {
            // Fallback
            instruction = title ?: text
        }
        
        // 3. ETA
        val etaKey = config.fields[HudDataType.ETA.name]
        var eta: String? = null
        if (etaKey != null) {
            eta = extras.getCharSequence(etaKey)?.toString()
        }
        
        if (eta == null) {
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
        if (distance != null) {
            val parsed = DistanceFormatter.parseDistance(distance)
            if (parsed != null) {
                HudState.distanceToTurnMeters = parsed.first
            }
        }
        HudState.eta = eta
        HudState.remainingTime = remainingTime
        HudState.trafficScore = trafficScore
        if (speedLimit != null) HudState.speedLimit = speedLimit
        
        // 7. Arrow Image (Large Icon or Picture)
        val largeIcon = extras.getParcelable<android.graphics.Bitmap>("android.largeIcon") 
            ?: (extras.getParcelable<android.graphics.drawable.Icon>("android.largeIcon")?.loadDrawable(this)?.let { ImageUtils.drawableToBitmap(it) })
        
        val picture = extras.getParcelable<android.graphics.Bitmap>("android.picture")
        
        var arrowBitmap: android.graphics.Bitmap? = null
        
        // Try to get bitmap from largeIcon first
        if (largeIcon != null) {
            Log.d(TAG, "Found LargeIcon in notification: ${largeIcon.width}x${largeIcon.height}")
            arrowBitmap = largeIcon
        } else if (picture != null) {
            Log.d(TAG, "Found Picture in notification: ${picture.width}x${picture.height}")
            arrowBitmap = picture
        } else {
            // Try to extract bitmap from RemoteViews (like old Google Maps approach)
            arrowBitmap = extractBitmapFromRemoteViews(notification, sbn.packageName)
        }
        
        if (arrowBitmap != null) {
            HudService.navDebug.lastArrowBitmap = arrowBitmap
            val arrowImage = ArrowImage(arrowBitmap)
            val hash = arrowImage.getArrowValue()
            Log.d(TAG, "Arrow Hash: $hash")
            
            val arrow = ArrowDirection.recognize(arrowImage)
            if (arrow != ArrowDirection.NONE) {
                HudState.turnIcon = arrow.hudCode
                HudService.navDebug.arrowStatus = "Recognized: ${arrow.name} ($hash)"
                Log.d(TAG, "Recognized arrow: $arrow")
            } else {
                HudService.navDebug.arrowStatus = "Not Recognized ($hash)"
                Log.d(TAG, "Arrow not recognized, hash: $hash")
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
    
    private data class ArrowCandidate(
        val bitmap: android.graphics.Bitmap,
        val bounds: android.graphics.Rect,
        val ordinal: Int
    )

    private data class ArrowMetrics(
        val whiteRatio: Double,
        val componentDominance: Double,
        val sizeScore: Double,
        val aspectPenalty: Double,
        val positionScore: Double,
        val totalScore: Double
    )

    private fun extractBitmapFromRemoteViews(
        notification: android.app.Notification,
        packageName: String
    ): android.graphics.Bitmap? {
        try {
            // Try bigContentView first (Yandex uses this), then contentView
            val views = notification.bigContentView ?: notification.contentView ?: return null

            DebugLog.i(TAG, "Attempting to extract bitmap from RemoteViews by applying to View")

            // Apply RemoteViews to actual View hierarchy
            val context = this
            val inflatedView = views.apply(context, null)

            // Collect all candidate ImageViews, then choose the whitest one
            val candidates = mutableListOf<ArrowCandidate>()
            collectArrowImageCandidates(inflatedView, candidates)

            if (candidates.isEmpty()) {
                DebugLog.w(TAG, "Could not find arrow ImageView in notification")
                return null
            }

            val scored = candidates.map { candidate ->
                candidate to calculateArrowMetrics(candidate, packageName)
            }.sortedByDescending { it.second.totalScore }

            // Debug top 3 candidates
            scored.take(3).forEachIndexed { i, (candidate, metrics) ->
                DebugLog.d(
                    TAG,
                    "Top${i + 1}: ${candidate.bitmap.width}x${candidate.bitmap.height}, " +
                        "white=${"%.3f".format(metrics.whiteRatio)}, " +
                        "comp=${"%.3f".format(metrics.componentDominance)}, " +
                        "pos=${"%.3f".format(metrics.positionScore)}, " +
                        "score=${"%.3f".format(metrics.totalScore)}"
                )
                saveCandidateDebugBitmap(candidate.bitmap, i + 1, metrics.totalScore)
            }

            val bestBitmap = scored.firstOrNull()?.first?.bitmap

            // Recycle non-selected candidates to avoid bitmap leaks
            for (candidate in candidates) {
                if (candidate.bitmap !== bestBitmap && !candidate.bitmap.isRecycled) {
                    candidate.bitmap.recycle()
                }
            }

            if (bestBitmap != null) {
                val bestMetrics = scored.first().second
                DebugLog.i(
                    TAG,
                    "Selected white arrow: ${bestBitmap.width}x${bestBitmap.height}, " +
                        "white=${"%.3f".format(bestMetrics.whiteRatio)}, score=${"%.3f".format(bestMetrics.totalScore)}"
                )
            }

            return bestBitmap
        } catch (e: Exception) {
            DebugLog.e(TAG, "Failed to extract bitmap from RemoteViews: ${e.message}")
            e.printStackTrace()
        }

        return null
    }

    private fun collectArrowImageCandidates(view: android.view.View, out: MutableList<ArrowCandidate>) {
        // If this is an ImageView, check if it contains an arrow-like icon
        if (view is android.widget.ImageView) {
            val drawable = view.drawable
            if (drawable != null) {
                val bitmap = ImageUtils.drawableToBitmap(drawable)
                if (bitmap != null) {
                    val width = bitmap.width
                    val height = bitmap.height

                    if (width > 30 && height > 30 && width < 500 && height < 500) {
                        val aspectRatio = width.toFloat() / height.toFloat()
                        if (aspectRatio in 0.5f..2.0f) {
                            val rect = android.graphics.Rect()
                            val hasRect = view.getGlobalVisibleRect(rect)
                            val bounds = if (hasRect) rect else android.graphics.Rect(0, 0, 0, 0)
                            DebugLog.d(TAG, "Found potential arrow ImageView: ${width}x${height} @ $bounds")
                            out.add(ArrowCandidate(bitmap, bounds, out.size))
                        }
                    }
                }
            }
        }

        // Recursively search children
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                collectArrowImageCandidates(view.getChildAt(i), out)
            }
        }
    }

    private fun calculateArrowMetrics(candidate: ArrowCandidate, packageName: String): ArrowMetrics {
        val bitmap = candidate.bitmap
        val (minV, maxS) = when {
            packageName.startsWith("ru.yandex") -> 0.78f to 0.26f
            packageName.contains("google") -> 0.82f to 0.20f
            else -> 0.80f to 0.22f
        }

        val step = if (bitmap.width * bitmap.height > 120_000) 2 else 1
        val whiteMask = Array((bitmap.height + step - 1) / step) { BooleanArray((bitmap.width + step - 1) / step) }

        var whitePixels = 0
        var opaquePixels = 0

        var yi = 0
        var y = 0
        while (y < bitmap.height) {
            var xi = 0
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                val a = (p ushr 24) and 0xff
                if (a > 30) {
                    opaquePixels++
                    if (isWhitePixelHsv(p, minV, maxS)) {
                        whiteMask[yi][xi] = true
                        whitePixels++
                    }
                }
                xi++
                x += step
            }
            yi++
            y += step
        }

        val whiteRatio = if (opaquePixels == 0) 0.0 else whitePixels.toDouble() / opaquePixels.toDouble()
        val componentDominance = calculateLargestComponentDominance(whiteMask, whitePixels)

        val minSide = minOf(bitmap.width, bitmap.height).toDouble()
        val sizeScore = (minSide / 220.0).coerceIn(0.0, 1.0)
        val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
        val aspectPenalty = kotlin.math.abs(aspectRatio - 1.0).coerceAtMost(1.0)

        val positionScore = calculatePositionScore(candidate.bounds, packageName)

        val totalScore =
            (whiteRatio * 0.55) +
            (componentDominance * 0.25) +
            (positionScore * 0.20) +
            (sizeScore * 0.12) -
            (aspectPenalty * 0.12)

        return ArrowMetrics(
            whiteRatio = whiteRatio,
            componentDominance = componentDominance,
            sizeScore = sizeScore,
            aspectPenalty = aspectPenalty,
            positionScore = positionScore,
            totalScore = totalScore
        )
    }

    private fun isWhitePixelHsv(pixel: Int, minV: Float, maxS: Float): Boolean {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(pixel, hsv)
        val value = hsv[2]
        val saturation = hsv[1]
        return value >= minV && saturation <= maxS
    }

    private fun calculateLargestComponentDominance(mask: Array<BooleanArray>, whitePixels: Int): Double {
        if (whitePixels <= 0) return 0.0

        val h = mask.size
        val w = if (h == 0) 0 else mask[0].size
        if (w == 0) return 0.0

        val visited = Array(h) { BooleanArray(w) }
        var largest = 0

        val qx = IntArray(h * w)
        val qy = IntArray(h * w)

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (!mask[y][x] || visited[y][x]) continue

                var head = 0
                var tail = 0
                qx[tail] = x
                qy[tail] = y
                tail++
                visited[y][x] = true

                var size = 0
                while (head < tail) {
                    val cx = qx[head]
                    val cy = qy[head]
                    head++
                    size++

                    val neighbors = arrayOf(
                        cx - 1 to cy,
                        cx + 1 to cy,
                        cx to cy - 1,
                        cx to cy + 1
                    )

                    for ((nx, ny) in neighbors) {
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                        if (!mask[ny][nx] || visited[ny][nx]) continue
                        visited[ny][nx] = true
                        qx[tail] = nx
                        qy[tail] = ny
                        tail++
                    }
                }

                if (size > largest) largest = size
            }
        }

        return largest.toDouble() / whitePixels.toDouble()
    }

    private fun calculatePositionScore(bounds: android.graphics.Rect, packageName: String): Double {
        if (bounds.width() <= 0 || bounds.height() <= 0) return 0.5

        // Soft priors by app: active maneuver icon is usually in top area.
        val centerY = bounds.exactCenterY()
        val height = bounds.bottom.toFloat().coerceAtLeast(1f)
        val yRatio = (centerY / height).coerceIn(0f, 1f)

        val preferred = when {
            packageName.startsWith("ru.yandex") -> 0.22f
            packageName.contains("google") -> 0.28f
            else -> 0.30f
        }

        val distance = kotlin.math.abs(yRatio - preferred)
        return (1.0 - (distance * 2.0)).coerceIn(0.0, 1.0)
    }

    private fun saveCandidateDebugBitmap(bitmap: android.graphics.Bitmap, rank: Int, score: Double) {
        try {
            val dir = java.io.File(getExternalFilesDir(null), "debug_arrow_candidates")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US)
                .format(java.util.Date())
            val file = java.io.File(dir, "cand_${timestamp}_r${rank}_s${"%.3f".format(score)}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
            // Ignore debug saving errors
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
