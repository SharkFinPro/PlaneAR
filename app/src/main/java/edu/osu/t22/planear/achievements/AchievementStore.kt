package edu.osu.t22.planear.achievements

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton that holds runtime achievement state and persists to SharedPreferences.
 *
 * Must call [init] from MainActivity.onCreate() before use.
 */
object AchievementStore {

    private const val PREFS_NAME = "planear_achievements"

    // Persisted keys
    private const val KEY_UNLOCKED       = "unlocked_ids"
    private const val KEY_UNIQUE_ICAOS   = "unique_icaos"
    private const val KEY_DAILY_DATES    = "daily_track_dates"
    private const val KEY_TRACKING_MS    = "total_tracking_ms"
    private const val KEY_CONTINENT_PFX  = "continental_prefixes"
    private const val KEY_ICAO_DAY_MAP   = "icao_day_map"

    private lateinit var prefs: SharedPreferences

    // --- Runtime state ---

    /** Set of achievement IDs that have been unlocked. */
    val unlockedIds: MutableSet<String> = mutableSetOf()

    /** Lifetime unique ICAO hex addresses seen. */
    val uniqueIcaosSeen: MutableSet<String> = mutableSetOf()

    /** Dates on which the user tracked at least 1 aircraft (yyyy-MM-dd). */
    val dailyTrackDates: MutableSet<String> = mutableSetOf()

    /** Cumulative in-app tracking time in milliseconds. */
    var totalTrackingMs: Long = 0L

    /** First characters of ICAO addresses for continental_divide. */
    val continentalPrefixes: MutableSet<Char> = mutableSetOf()

    /**
     * ICAO hex → set of date strings on which it was seen.
     * Used for the "frequent_flyer" achievement.
     * Only ICAOs with 2+ sighting days are persisted to save space.
     */
    val icaoDayCounts: MutableMap<String, MutableSet<String>> = mutableMapOf()

    // --- In-memory only (not persisted, resets on restart) ---

    /** True only while the AR page is the active scene. Achievement checks are gated on this. */
    @Volatile var isOnArPage: Boolean = false

    /** Queue of achievement IDs that were just unlocked and need a popup notification. */
    val pendingNotifications: MutableList<String> = mutableListOf()

    /** ICAO → list of (timestampMs, headingDeg) for sharp_turn detection. */
    val headingHistory: MutableMap<String, MutableList<Pair<Long, Double>>> = mutableMapOf()

    /** ICAO → list of (timestampMs, lat, lon) for holding_pattern detection. */
    val positionHistory: MutableMap<String, MutableList<Triple<Long, Double, Double>>> = mutableMapOf()

    // -------------------------------------------------------------------------

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
    }

    fun isUnlocked(id: String): Boolean = id in unlockedIds

    fun unlock(id: String) {
        if (unlockedIds.add(id)) {
            pendingNotifications.add(id)
            save()
        }
    }

    /** Pop the next pending notification, or null if none. */
    fun popNotification(): String? {
        return if (pendingNotifications.isNotEmpty()) pendingNotifications.removeAt(0) else null
    }

    /** Dismiss all pending notifications. */
    fun dismissNotification() {
        // Just marks that the user closed the current popup.
        // The actual removal happens via popNotification().
    }

    fun getUnlockedCount(): Int = unlockedIds.size

    /** Returns the current consecutive-day streak ending today (or yesterday). */
    fun getCurrentStreak(): Int {
        if (dailyTrackDates.isEmpty()) return 0

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedDates = dailyTrackDates
            .mapNotNull { runCatching { fmt.parse(it) }.getOrNull() }
            .sortedDescending()

        if (sortedDates.isEmpty()) return 0

        val today    = fmt.format(Date())
        val todayMs  = runCatching { fmt.parse(today)!!.time }.getOrDefault(0L)
        val latestMs = sortedDates[0].time
        val dayMs    = 86_400_000L

        // Streak must include today or yesterday
        if (todayMs - latestMs > dayMs) return 0

        var streak = 1
        for (i in 1 until sortedDates.size) {
            val diff = sortedDates[i - 1].time - sortedDates[i].time
            if (diff in (dayMs - 3_600_000)..(dayMs + 3_600_000)) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    /** Record that today the user tracked aircraft. */
    fun recordTodayTracking() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dailyTrackDates.add(today)) {
            save()
        }
    }

    /** Add tracking time and persist periodically (called every second). */
    fun addTrackingTime(ms: Long) {
        totalTrackingMs += ms
        // Persist every 30 seconds worth of accumulated time
        if (totalTrackingMs % 30_000 < ms) {
            save()
        }
    }

    /** Record a seen ICAO address for today. */
    fun recordIcao(hex: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        uniqueIcaosSeen.add(hex)

        // Continental prefix tracking
        if (hex.isNotEmpty()) {
            continentalPrefixes.add(hex[0].uppercaseChar())
        }

        // Per-ICAO day tracking (for frequent_flyer)
        val days = icaoDayCounts.getOrPut(hex) { mutableSetOf() }
        days.add(today)
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    fun save() {
        prefs.edit().apply {
            putStringSet(KEY_UNLOCKED, unlockedIds.toSet())
            putStringSet(KEY_UNIQUE_ICAOS, uniqueIcaosSeen.toSet())
            putStringSet(KEY_DAILY_DATES, dailyTrackDates.toSet())
            putLong(KEY_TRACKING_MS, totalTrackingMs)
            putStringSet(KEY_CONTINENT_PFX, continentalPrefixes.map { it.toString() }.toSet())

            // Encode icaoDayCounts as "hex:date1,date2,date3" strings
            // Only persist ICAOs with 2+ days to save space
            val encoded = icaoDayCounts
                .filter { it.value.size >= 2 }
                .map { (hex, dates) -> "$hex:${dates.joinToString(",")}" }
                .toSet()
            putStringSet(KEY_ICAO_DAY_MAP, encoded)

            apply()
        }
    }

    private fun load() {
        unlockedIds.clear()
        unlockedIds.addAll(prefs.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet())

        uniqueIcaosSeen.clear()
        uniqueIcaosSeen.addAll(prefs.getStringSet(KEY_UNIQUE_ICAOS, emptySet()) ?: emptySet())

        dailyTrackDates.clear()
        dailyTrackDates.addAll(prefs.getStringSet(KEY_DAILY_DATES, emptySet()) ?: emptySet())

        totalTrackingMs = prefs.getLong(KEY_TRACKING_MS, 0L)

        continentalPrefixes.clear()
        (prefs.getStringSet(KEY_CONTINENT_PFX, emptySet()) ?: emptySet()).forEach {
            if (it.isNotEmpty()) continentalPrefixes.add(it[0])
        }

        icaoDayCounts.clear()
        (prefs.getStringSet(KEY_ICAO_DAY_MAP, emptySet()) ?: emptySet()).forEach { entry ->
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) {
                val hex = parts[0]
                val dates = parts[1].split(",").filter { it.isNotBlank() }.toMutableSet()
                icaoDayCounts[hex] = dates
            }
        }
    }
}
