package com.ulsee.mower.ui.settings.mower

public enum class MowerWorkingMode(val mode: Int) {
    unknown(-1),
    learning(0),
    working(1),
    learnAndWork(2),
    gradual(3),
    explosive(4);

    companion object {
        operator fun invoke(rawValue: Int) = values().find { it.mode == rawValue } ?: MowerWorkingMode.unknown
    }
}
data class MowerSettings(
    var workingMode: MowerWorkingMode,
    var rainMode: Int,
    val knifeHeight: Int
) {
    var isWorkingOnRainlyDay: Boolean
        get() = rainMode == 1
        set(value) {
            rainMode = if(value) 1 else 0
        }
}
