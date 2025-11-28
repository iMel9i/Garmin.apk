package iMel9i.garminhud.lite

/**
 * Утилиты для конвертации расстояний (как в оригинальном GoogleMaps HUD)
 */
object DistanceFormatter {
    
    /**
     * Конвертирует строку расстояния в метры и определяет единицы
     * Примеры: "500 m" -> (500, METRES), "1.5 km" -> (1500, KILOMETRES)
     */
    fun parseDistance(distanceStr: String?): Pair<Int, DistanceUnit>? {
        if (distanceStr.isNullOrBlank()) return null
        
        // Парсим паттерны типа "500 m", "1.5 km", "0.3 mi"
        val regex = """(\d+(?:[.,]\d+)?)\s*(m|м|km|км|mi|ft)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(distanceStr) ?: return null
        
        val value = match.groupValues[1].replace(",", ".").toFloatOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        
        return when (unit) {
            "m", "м" -> Pair(value.toInt(), DistanceUnit.METRES)
            "km", "км" -> Pair((value * 1000).toInt(), DistanceUnit.KILOMETRES)
            "mi" -> Pair((value * 1609.34).toInt(), DistanceUnit.MILES)
            "ft" -> Pair((value * 0.3048).toInt(), DistanceUnit.FEET)
            else -> null
        }
    }
    
    /**
     * Форматирует расстояние для отображения (как в оригинале)
     * Автоматически выбирает км/м в зависимости от расстояния
     */
    fun formatDistance(meters: Int): Pair<Int, DistanceUnit> {
        return when {
            meters >= 1000 -> {
                // Используем километры для расстояний >= 1 км
                val km = (meters / 100).toFloat() / 10 // Округление до 0.1 км
                if (km >= 10) {
                    Pair(km.toInt(), DistanceUnit.KILOMETRES)
                } else {
                    Pair((km * 10).toInt(), DistanceUnit.KILOMETRES) // Передаем как km*10 для отображения 1.5 км
                }
            }
            else -> Pair(meters, DistanceUnit.METRES)
        }
    }
}

enum class DistanceUnit(val hudValue: Int) {
    NONE(0),
    METRES(1),
    KILOMETRES(3),
    MILES(5),
    FEET(8)
}
