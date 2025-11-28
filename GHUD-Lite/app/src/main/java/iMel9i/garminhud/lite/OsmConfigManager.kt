package iMel9i.garminhud.lite

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Менеджер настроек OpenStreetMap API
 */
class OsmConfigManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("OsmConfig", Context.MODE_PRIVATE)
    
    data class OsmApiConfig(
        val dataType: HudDataType,
        val enabled: Boolean = true,
        val apiUrl: String,
        val query: String,
        val searchRadius: Int = 20, // meters
        val parseField: String // JSON field to parse
    )
    
    companion object {
        private const val KEY_CONFIGS = "osm_configs"
        
        val DEFAULT_CONFIGS = listOf(
            OsmApiConfig(
                dataType = HudDataType.SPEED_LIMIT,
                enabled = true,
                apiUrl = "https://overpass-api.de/api/interpreter",
                query = """[out:json];
way(around:{radius}, {lat}, {lon})["maxspeed"];
out tags;""",
                searchRadius = 20,
                parseField = "elements[0].tags.maxspeed"
            ),
            OsmApiConfig(
                dataType = HudDataType.DISTANCE_TO_CAMERA,
                enabled = true,
                apiUrl = "https://overpass-api.de/api/interpreter",
                query = """[out:json];
(
  node(around:{radius}, {lat}, {lon})["man_made"="surveillance"];
  node(around:{radius}, {lat}, {lon})["highway"="speed_camera"];
);
out body;""",
                searchRadius = 1000,
                parseField = "elements"
            )
        )
    }
    
    fun getConfigs(): List<OsmApiConfig> {
        val json = prefs.getString(KEY_CONFIGS, null) ?: return DEFAULT_CONFIGS
        
        try {
            val jsonArray = JSONArray(json)
            val configs = mutableListOf<OsmApiConfig>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                configs.add(
                    OsmApiConfig(
                        dataType = HudDataType.valueOf(obj.getString("dataType")),
                        enabled = obj.getBoolean("enabled"),
                        apiUrl = obj.getString("apiUrl"),
                        query = obj.getString("query"),
                        searchRadius = obj.getInt("searchRadius"),
                        parseField = obj.getString("parseField")
                    )
                )
            }
            
            return configs
        } catch (e: Exception) {
            e.printStackTrace()
            return DEFAULT_CONFIGS
        }
    }
    
    fun saveConfigs(configs: List<OsmApiConfig>) {
        val jsonArray = JSONArray()
        
        configs.forEach { config ->
            val obj = JSONObject()
            obj.put("dataType", config.dataType.name)
            obj.put("enabled", config.enabled)
            obj.put("apiUrl", config.apiUrl)
            obj.put("query", config.query)
            obj.put("searchRadius", config.searchRadius)
            obj.put("parseField", config.parseField)
            
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_CONFIGS, jsonArray.toString()).apply()
    }
    
    fun updateConfig(dataType: HudDataType, newConfig: OsmApiConfig) {
        val configs = getConfigs().toMutableList()
        val index = configs.indexOfFirst { it.dataType == dataType }
        if (index != -1) {
            configs[index] = newConfig
            saveConfigs(configs)
        }
    }
    
    fun getEnabledConfigs(): List<OsmApiConfig> {
        return getConfigs().filter { it.enabled }
    }
}
