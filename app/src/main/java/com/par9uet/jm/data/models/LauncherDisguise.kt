package com.par9uet.jm.data.models

enum class LauncherDisguise(val id: String) {
    DEFAULT("default"),
    INNOCUOUS_LAUNCHER("launcher"),
    INNOCUOUS_ROUND("launcher_round"),
    ;

    companion object {
        val ids: List<String> = entries.map { it.id }

        fun fromId(id: String): LauncherDisguise =
            entries.find { it.id == id } ?: DEFAULT
    }
}