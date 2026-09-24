package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "Not scheduled"
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDurationHms(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatRemainingTime(dueTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): String {
        val diffMillis = dueTimestamp - currentTimestamp
        if (diffMillis <= 0) return "Overdue"
        val totalSeconds = diffMillis / 1000
        return formatDurationHms(totalSeconds)
    }

    fun addHours(timestamp: Long, hours: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.add(Calendar.HOUR_OF_DAY, hours)
        return cal.timeInMillis
    }

    /**
     * Validates if target timestamp is strictly in the future.
     * Prevents same-day date/time bugs: current 08:27 and user selects 08:30 is VALID.
     * Only times earlier than current instant are rejected.
     */
    fun isStrictlyFuture(targetTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        return targetTimestamp > currentTimestamp
    }

    fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()
        target.timeInMillis = timestamp
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    fun isWithinNextHours(targetTimestamp: Long, hours: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (targetTimestamp <= now) return false
        val maxTime = now + TimeUnit.HOURS.toMillis(hours.toLong())
        return targetTimestamp <= maxTime
    }

    fun calculateStreak(completedTimestamps: List<Long>): Int {
        if (completedTimestamps.isEmpty()) return 0

        val calNow = Calendar.getInstance()
        val todayYear = calNow.get(Calendar.YEAR)
        val todayDay = calNow.get(Calendar.DAY_OF_YEAR)

        // Group timestamps by day (year * 1000 + dayOfYear)
        val daysWithCompletions = completedTimestamps.map { ts ->
            val c = Calendar.getInstance().apply { timeInMillis = ts }
            c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }.toSet()

        val todayKey = todayYear * 1000 + todayDay
        // Streak requires completion today
        if (!daysWithCompletions.contains(todayKey)) {
            return 0
        }

        var streak = 1
        val checkCal = Calendar.getInstance()
        while (true) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val prevKey = checkCal.get(Calendar.YEAR) * 1000 + checkCal.get(Calendar.DAY_OF_YEAR)
            if (daysWithCompletions.contains(prevKey)) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
