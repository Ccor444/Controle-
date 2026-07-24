package com.example.model

enum class TvBrand(val displayName: String, val defaultPort: Int) {
    SAMSUNG("Samsung Smart TV", 8001),
    LG("LG webOS TV", 3000),
    ANDROID_TV("Android / Google TV", 8008),
    ROKU("Roku TV", 8060),
    GENERIC("Smart TV Universal", 80);

    companion object {
        fun fromString(value: String): TvBrand {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: GENERIC
        }
    }
}
