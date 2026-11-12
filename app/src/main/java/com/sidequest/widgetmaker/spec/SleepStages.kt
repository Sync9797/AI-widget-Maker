package com.sidequest.widgetmaker.spec

/**
 * Last night's sleep stages in order, as the widget value "health.sleep.stages":
 * "L45,D30,L20,R25,A5,..." (stage letter and minutes). Free of Android, so it can be tested.
 */
object SleepStages {
    data class Part(val stage: Char, val minutes: Int)

    /** A, L, D and R: awake, light, deep, REM. */
    const val STAGES = "ALDR"

    fun encode(parts: List<Part>): String =
        merge(parts).joinToString(",") { "${it.stage}${it.minutes}" }

    fun decode(value: String?): List<Part> = value.orEmpty().split(',').mapNotNull { piece ->
        val stage = piece.firstOrNull()?.takeIf { it in STAGES } ?: return@mapNotNull null
        piece.drop(1).toIntOrNull()?.takeIf { it > 0 }?.let { Part(stage, it) }
    }

    /** Joins neighbours in the same stage, so the value stays short. */
    private fun merge(parts: List<Part>): List<Part> = parts.filter { it.minutes > 0 }.fold(mutableListOf()) { out, p ->
        val last = out.lastOrNull()
        if (last != null && last.stage == p.stage) out[out.lastIndex] = last.copy(minutes = last.minutes + p.minutes) else out += p
        out
    }

    fun color(stage: Char): Int = when (stage) {
        'A' -> 0xFFFF9E6B.toInt()
        'L' -> 0xFF8AB4FF.toInt()
        'D' -> 0xFF4F5BD5.toInt()
        else -> 0xFFC79BFF.toInt()
    }
}
