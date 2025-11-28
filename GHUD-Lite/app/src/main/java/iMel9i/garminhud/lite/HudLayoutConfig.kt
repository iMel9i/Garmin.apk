package iMel9i.garminhud.lite

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class HudSlot {
    MAIN_NUMBER,      // 1. Верхние большие цифры (обычно дистанция)
    SECONDARY_NUMBER, // 2. Нижние малые цифры (время/скорость)
    DIRECTION_ARROW,  // 5. Стрелка направления
    LANE_ASSIST,      // 4. Полосы
    CAMERA_ICON,      // Иконка камеры
    SPEEDING_ICON     // Иконка превышения
}

enum class HudDataType(val displayName: String) {
    NONE("Ничего"),
    CURRENT_SPEED("Текущая скорость"),
    SPEED_LIMIT("Ограничение скорости"),
    CURRENT_TIME("Текущее время"),
    DISTANCE_TO_TURN("Дистанция до поворота"),
    ETA("Время прибытия"),
    REMAINING_TIME("Время в пути"),
    TRAFFIC_SCORE("Баллы пробок"),
    DISTANCE_TO_CAMERA("Дистанция до камеры"),
    NAVIGATION_INSTRUCTION("Инструкция навигации"),
    DIRECTION_ARROW("Стрелка направления")
}

data class LayoutProfile(
    val name: String,
    val slots: MutableMap<HudSlot, HudDataType>
)

class LayoutConfigManager(context: Context) {
    private val prefs = context.getSharedPreferences("HudLayouts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getProfile(mode: String): LayoutProfile {
        val json = prefs.getString("profile_$mode", null)
        return if (json != null) {
            try {
                gson.fromJson(json, LayoutProfile::class.java)
            } catch (e: Exception) {
                getDefaultProfile(mode)
            }
        } else {
            getDefaultProfile(mode)
        }
    }

    fun saveProfile(mode: String, profile: LayoutProfile) {
        val json = gson.toJson(profile)
        prefs.edit().putString("profile_$mode", json).apply()
    }

    private fun getDefaultProfile(mode: String): LayoutProfile {
        return when (mode) {
            "IDLE" -> LayoutProfile("Без навигации", mutableMapOf(
                HudSlot.MAIN_NUMBER to HudDataType.CURRENT_SPEED,
                HudSlot.SECONDARY_NUMBER to HudDataType.CURRENT_TIME,
                HudSlot.DIRECTION_ARROW to HudDataType.NONE,
                HudSlot.LANE_ASSIST to HudDataType.NONE,
                HudSlot.CAMERA_ICON to HudDataType.DISTANCE_TO_CAMERA,
                HudSlot.SPEEDING_ICON to HudDataType.NONE
            ))
            else -> LayoutProfile("Навигация", mutableMapOf(
                HudSlot.MAIN_NUMBER to HudDataType.DISTANCE_TO_TURN,
                HudSlot.SECONDARY_NUMBER to HudDataType.ETA, // Или CURRENT_SPEED
                HudSlot.DIRECTION_ARROW to HudDataType.DISTANCE_TO_TURN, // Направление подразумевается
                HudSlot.LANE_ASSIST to HudDataType.NONE,
                HudSlot.CAMERA_ICON to HudDataType.DISTANCE_TO_CAMERA,
                HudSlot.SPEEDING_ICON to HudDataType.NONE
            ))
        }
    }
}
