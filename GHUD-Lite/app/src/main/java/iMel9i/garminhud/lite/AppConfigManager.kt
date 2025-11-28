package iMel9i.garminhud.lite

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Менеджер для управления настройками приложений навигации
 */
class AppConfigManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)
    
    data class AppConfig(
        val packageName: String,
        val appName: String,
        val enabled: Boolean = true,
        val fields: Map<String, String> = emptyMap() // field name -> resource ID or key
    )
    
    companion object {
        private const val KEY_CONFIGS = "configs"
        
        // Default configurations
        val DEFAULT_CONFIGS = listOf(
            AppConfig(
                packageName = "ru.yandex.yandexmaps",
                appName = "Yandex Maps",
                enabled = true,
                fields = mapOf(
                    HudDataType.DISTANCE_TO_TURN.name to "ru.yandex.yandexmaps:id/distance_to_turn",
                    HudDataType.NAVIGATION_INSTRUCTION.name to "ru.yandex.yandexmaps:id/instruction_text",
                    HudDataType.ETA.name to "ru.yandex.yandexmaps:id/eta_time",
                    HudDataType.REMAINING_TIME.name to "ru.yandex.yandexmaps:id/remaining_time",
                    HudDataType.TRAFFIC_SCORE.name to "ru.yandex.yandexmaps:id/city_traffic_index",
                    HudDataType.DIRECTION_ARROW.name to "ru.yandex.yandexmaps:id/image_maneuverballoon_maneuver"
                )
            ),
            AppConfig(
                packageName = "ru.yandex.yandexnavi",
                appName = "Yandex Navigator",
                enabled = true,
                fields = mapOf(
                    HudDataType.DISTANCE_TO_TURN.name to "ru.yandex.yandexnavi:id/distance_to_maneuver",
                    HudDataType.NAVIGATION_INSTRUCTION.name to "ru.yandex.yandexnavi:id/maneuver_text",
                    HudDataType.ETA.name to "ru.yandex.yandexnavi:id/arrival_time",
                    HudDataType.REMAINING_TIME.name to "ru.yandex.yandexnavi:id/time_left",
                    HudDataType.TRAFFIC_SCORE.name to "ru.yandex.yandexnavi:id/traffic_index",
                    HudDataType.CURRENT_SPEED.name to "ru.yandex.yandexnavi:id/current_speed",
                    HudDataType.DIRECTION_ARROW.name to "ru.yandex.yandexnavi:id/image_maneuverballoon_maneuver"
                )
            )
        )
    }
    
    fun getConfigs(): List<AppConfig> {
        val json = prefs.getString(KEY_CONFIGS, null) ?: return DEFAULT_CONFIGS
        
        try {
            val jsonArray = JSONArray(json)
            val configs = mutableListOf<AppConfig>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val fieldsObj = obj.getJSONObject("fields")
                val fields = mutableMapOf<String, String>()
                
                fieldsObj.keys().forEach { key ->
                    fields[key] = fieldsObj.getString(key)
                }
                
                configs.add(
                    AppConfig(
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        enabled = obj.getBoolean("enabled"),
                        fields = fields
                    )
                )
            }
            
            return configs
        } catch (e: Exception) {
            e.printStackTrace()
            return DEFAULT_CONFIGS
        }
    }
    
    fun saveConfigs(configs: List<AppConfig>) {
        val jsonArray = JSONArray()
        
        configs.forEach { config ->
            val obj = JSONObject()
            obj.put("packageName", config.packageName)
            obj.put("appName", config.appName)
            obj.put("enabled", config.enabled)
            
            val fieldsObj = JSONObject()
            config.fields.forEach { (key, value) ->
                fieldsObj.put(key, value)
            }
            obj.put("fields", fieldsObj)
            
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_CONFIGS, jsonArray.toString()).apply()
    }
    
    fun addConfig(config: AppConfig) {
        val configs = getConfigs().toMutableList()
        configs.add(config)
        saveConfigs(configs)
    }
    
    fun updateConfig(packageName: String, newConfig: AppConfig) {
        val configs = getConfigs().toMutableList()
        val index = configs.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            configs[index] = newConfig
            saveConfigs(configs)
        }
    }
    
    fun deleteConfig(packageName: String) {
        val configs = getConfigs().toMutableList()
        configs.removeAll { it.packageName == packageName }
        saveConfigs(configs)
    }
    
    fun getEnabledPackages(): List<String> {
        return getConfigs().filter { it.enabled }.map { it.packageName }
    }
}
