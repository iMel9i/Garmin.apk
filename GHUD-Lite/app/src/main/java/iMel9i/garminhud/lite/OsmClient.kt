package iMel9i.garminhud.lite

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class OsmClient {
    private val client = OkHttpClient()
    private val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    private val TOMTOM_FLOW_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json"
    private val TAG = "OsmClient"
    private var speedDataProvider: SpeedDataProvider = SpeedDataProvider.OSM
    private var tomTomApiKey: String = ""

    data class CameraLocation(val lat: Double, val lon: Double)
    data class SpeedData(val currentSpeed: Int?, val maxSpeed: Int?)

    enum class SpeedDataProvider { OSM, TOMTOM }

    fun setSpeedDataProvider(provider: SpeedDataProvider) {
        speedDataProvider = provider
    }

    fun setTomTomApiKey(apiKey: String?) {
        tomTomApiKey = apiKey?.trim().orEmpty()
    }

    fun getSpeedData(lat: Double, lon: Double, callback: (SpeedData) -> Unit) {
        when (speedDataProvider) {
            SpeedDataProvider.OSM -> getSpeedDataFromOsm(lat, lon, callback)
            SpeedDataProvider.TOMTOM -> getSpeedDataFromTomTom(lat, lon, callback)
        }
    }

    fun getSpeedLimit(lat: Double, lon: Double, callback: (Int?) -> Unit) {
        getSpeedData(lat, lon) { speedData ->
            callback(speedData.maxSpeed)
        }
    }

    private fun getSpeedDataFromOsm(lat: Double, lon: Double, callback: (SpeedData) -> Unit) {
        // Query for ways around the point with maxspeed.
        // Using a small radius (20m) to find the road the user is on.
        val query = """
            [out:json];
            way(around:20, $lat, $lon)["maxspeed"];
            out tags;
        """.trimIndent()

        val url = "$OVERPASS_URL?data=${URLEncoder.encode(query, "UTF-8")}"
        
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to get speed limit", e)
                callback(SpeedData(currentSpeed = null, maxSpeed = null))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(SpeedData(currentSpeed = null, maxSpeed = null))
                        return
                    }
                    
                    val json = it.body?.string() ?: return
                    try {
                        val jsonObj = JSONObject(json)
                        val elements = jsonObj.getJSONArray("elements")
                        if (elements.length() > 0) {
                            // Get the first one for now
                            val tags = elements.getJSONObject(0).getJSONObject("tags")
                            val maxspeed = tags.optString("maxspeed")
                            // Parse maxspeed (e.g., "50", "60 mph")
                            // Assuming km/h for now as requested
                            val limit = maxspeed.filter { it.isDigit() }.toIntOrNull()
                            callback(SpeedData(currentSpeed = null, maxSpeed = limit))
                        } else {
                            callback(SpeedData(currentSpeed = null, maxSpeed = null))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing speed limit", e)
                        callback(SpeedData(currentSpeed = null, maxSpeed = null))
                    }
                }
            }
        })
    }

    private fun getSpeedDataFromTomTom(lat: Double, lon: Double, callback: (SpeedData) -> Unit) {
        if (tomTomApiKey.isBlank()) {
            Log.w(TAG, "TomTom provider selected, but API key is empty")
            callback(SpeedData(currentSpeed = null, maxSpeed = null))
            return
        }

        val point = "$lat,$lon"
        val url = "$TOMTOM_FLOW_URL?point=${URLEncoder.encode(point, "UTF-8")}&unit=KMPH&key=${URLEncoder.encode(tomTomApiKey, "UTF-8")}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to get TomTom speed data", e)
                callback(SpeedData(currentSpeed = null, maxSpeed = null))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.w(TAG, "TomTom request failed: ${it.code}")
                        callback(SpeedData(currentSpeed = null, maxSpeed = null))
                        return
                    }

                    val json = it.body?.string() ?: return
                    try {
                        val flow = JSONObject(json).optJSONObject("flowSegmentData")
                        val current = flow?.optInt("currentSpeed")?.takeIf { value -> value > 0 }
                        val freeFlow = flow?.optInt("freeFlowSpeed")?.takeIf { value -> value > 0 }
                        callback(SpeedData(currentSpeed = current, maxSpeed = freeFlow))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing TomTom speed data", e)
                        callback(SpeedData(currentSpeed = null, maxSpeed = null))
                    }
                }
            }
        })
    }
    
    fun getCameras(lat: Double, lon: Double, radius: Int, callback: (List<CameraLocation>) -> Unit) {
        // Query for surveillance nodes or speed cameras
        val query = """
            [out:json];
            (
              node(around:$radius, $lat, $lon)["man_made"="surveillance"];
              node(around:$radius, $lat, $lon)["highway"="speed_camera"];
            );
            out body;
        """.trimIndent()

        val url = "$OVERPASS_URL?data=${URLEncoder.encode(query, "UTF-8")}"
        
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to get cameras", e)
                callback(emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(emptyList())
                        return
                    }
                    
                    val json = it.body?.string() ?: return
                    try {
                        val jsonObj = JSONObject(json)
                        val elements = jsonObj.getJSONArray("elements")
                        val cameras = mutableListOf<CameraLocation>()
                        
                        for (i in 0 until elements.length()) {
                            val el = elements.getJSONObject(i)
                            val lat = el.getDouble("lat")
                            val lon = el.getDouble("lon")
                            cameras.add(CameraLocation(lat, lon))
                        }
                        callback(cameras)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing cameras", e)
                        callback(emptyList())
                    }
                }
            }
        })
    }
}
