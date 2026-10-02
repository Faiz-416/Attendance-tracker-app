package com.example.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object AttendanceCalculator {

    /**
     * Calculates attendance percentage.
     * Returns null if total conducted (present + absent) is 0.
     */
    fun calculatePercentage(present: Int, absent: Int): Float? {
        val conducted = present + absent
        if (conducted <= 0) return null
        return (present.toFloat() / conducted.toFloat()) * 100f
    }

    /**
     * Calculates the minimum number of additional consecutive lectures needed
     * to reach target threshold T (0..100) when currently below target.
     * Formula: ceil((T * (P + A) - P) / (1 - T))
     */
    fun calculateRequiredConsecutive(present: Int, absent: Int, targetPercentage: Float): Int {
        val conducted = present + absent
        if (conducted == 0) return 0
        val targetFraction = (targetPercentage / 100f).coerceIn(0f, 0.999f)
        val currentFraction = present.toFloat() / conducted.toFloat()

        if (currentFraction >= targetFraction) {
            return 0
        }

        val numerator = targetFraction * conducted - present
        val denominator = 1f - targetFraction
        if (denominator <= 0f) return 0

        val needed = ceil(numerator / denominator).toInt()
        return max(0, needed)
    }

    /**
     * Calculates the number of upcoming lectures that can be safely missed (bunked)
     * without dropping below the target threshold T (0..100).
     * Formula: floor(P / T - (P + A))
     * Returns 0 if currently below target or conducted == 0.
     */
    fun calculateSafeBunks(present: Int, absent: Int, targetPercentage: Float): Int {
        val conducted = present + absent
        if (conducted == 0) return 0
        val targetFraction = (targetPercentage / 100f).coerceIn(0.001f, 1.0f)
        val currentFraction = present.toFloat() / conducted.toFloat()

        if (currentFraction < targetFraction) {
            return 0
        }

        val safe = floor(present / targetFraction - conducted).toInt()
        return max(0, safe)
    }

    /**
     * Simulates future attendance when attending `additionalPresent` and missing `additionalAbsent`.
     */
    fun simulateAttendance(
        currentPresent: Int,
        currentAbsent: Int,
        additionalPresent: Int,
        additionalAbsent: Int
    ): Float? {
        val newPresent = currentPresent + additionalPresent
        val newAbsent = currentAbsent + additionalAbsent
        val newConducted = newPresent + newAbsent
        if (newConducted <= 0) return null
        return (newPresent.toFloat() / newConducted.toFloat()) * 100f
    }
}
