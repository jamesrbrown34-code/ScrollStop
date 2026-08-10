package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** A multi-week scroll-reduction programme difficulty. */
enum class ReductionPlanDifficulty(
    val displayName: String,
    val durationWeeks: Int,
    val weeklyFractions: List<Double>,
    val pitch: String
) {
    EASY(
        displayName = "Easy",
        durationWeeks = 4,
        weeklyFractions = listOf(0.95, 0.90, 0.85, 0.80),
        pitch = "A gentle taper that trims your scrolling gradually."
    ),
    MEDIUM(
        displayName = "Medium",
        durationWeeks = 4,
        weeklyFractions = listOf(0.90, 0.75, 0.60, 0.50),
        pitch = "A steady cut, halving your scrolling by the final week."
    ),
    HARD(
        displayName = "Hard",
        durationWeeks = 3,
        weeklyFractions = listOf(0.80, 0.60, 0.40),
        pitch = "A fast cut that gets you under half your baseline in three weeks."
    ),
    EXTREME(
        displayName = "Extreme",
        durationWeeks = 3,
        weeklyFractions = listOf(0.70, 0.50, 0.30),
        pitch = "The toughest push — down to 30% of your baseline."
    );

    /** e.g. "90% → 75% → 60% → 50%" — how the target steps down each week. */
    val scheduleLabel: String
        get() = weeklyFractions.joinToString(" → ") { "${(it * 100).roundToInt()}%" }
}

data class ReductionPlanSettings(
    val difficulty: ReductionPlanDifficulty? = null,
    val startDate: LocalDate? = null,
    val baselineWeeklyScrolls: Int = 0
) {
    val isActive: Boolean
        get() = difficulty != null && startDate != null && baselineWeeklyScrolls > 0

    /** Zero-based week index since the start, clamped to the final week. */
    fun weekIndex(today: LocalDate = LocalDate.now()): Int {
        val start = startDate ?: return 0
        val elapsed = ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)
        return (elapsed / 7).toInt().coerceIn(0, (difficulty?.durationWeeks ?: 1) - 1)
    }

    /** Zero-based week index since the start, not clamped — used to detect plan completion. */
    fun weekNumber(today: LocalDate = LocalDate.now()): Int {
        val start = startDate ?: return 0
        return (ChronoUnit.DAYS.between(start, today).coerceAtLeast(0) / 7).toInt()
    }

    /** This week's target as a fraction of the baseline (1.0 when no plan is active). */
    fun currentFraction(today: LocalDate = LocalDate.now()): Double {
        if (!isActive) return 1.0
        return difficulty?.weeklyFractions?.getOrElse(weekIndex(today)) { 1.0 } ?: 1.0
    }

    /** This week's weekly scroll target, derived from the baseline and the current fraction. */
    fun targetForWeek(today: LocalDate = LocalDate.now()): Int =
        (baselineWeeklyScrolls * currentFraction(today)).roundToInt().coerceAtLeast(1)

    fun isComplete(today: LocalDate = LocalDate.now()): Boolean {
        val start = startDate ?: return false
        val duration = difficulty?.durationWeeks ?: return false
        return ChronoUnit.DAYS.between(start, today).coerceAtLeast(0) / 7 >= duration.toLong()
    }
}

private val Context.reductionPlanDataStore by preferencesDataStore("reduction_plan")

class ReductionPlanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val difficultyKey = stringPreferencesKey("difficulty")
    private val startDateKey = stringPreferencesKey("start_date")
    private val baselineKey = intPreferencesKey("baseline_weekly_scrolls")
    val settings: StateFlow<ReductionPlanSettings> = appContext.reductionPlanDataStore.data
        .map { prefs ->
            ReductionPlanSettings(
                difficulty = prefs[difficultyKey]?.let { name ->
                    ReductionPlanDifficulty.entries.firstOrNull { it.name == name }
                },
                startDate = prefs[startDateKey]?.let { serialized ->
                    runCatching { LocalDate.parse(serialized) }.getOrNull()
                },
                baselineWeeklyScrolls = (prefs[baselineKey] ?: 0).coerceAtLeast(0)
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, ReductionPlanSettings())

    fun setPlan(difficulty: ReductionPlanDifficulty, startDate: LocalDate, baselineWeeklyScrolls: Int) {
        scope.launch {
            appContext.reductionPlanDataStore.edit {
                it[difficultyKey] = difficulty.name
                it[startDateKey] = startDate.toString()
                it[baselineKey] = baselineWeeklyScrolls.coerceAtLeast(1)
            }
        }
    }

    fun clearPlan() {
        scope.launch {
            appContext.reductionPlanDataStore.edit {
                it.remove(difficultyKey)
                it.remove(startDateKey)
                it.remove(baselineKey)
            }
        }
    }

    /** The reminder cadence implied by the plan: the weekly target mapped to a daily allowance,
     *  never loosening the user's configured limit. Returns [baseLimit] when no plan is active. */
    fun currentReminderLimit(baseLimit: Int, today: LocalDate = LocalDate.now()): Int {
        val s = settings.value
        if (!s.isActive) return baseLimit
        val planDerived = (s.targetForWeek(today) / 7.0).roundToInt().coerceAtLeast(1)
        return minOf(baseLimit, planDerived)
    }
}

object ReductionPlanGraph {
    @Volatile private var repository: ReductionPlanRepository? = null

    fun repository(context: Context): ReductionPlanRepository = synchronized(this) {
        repository ?: ReductionPlanRepository(context.applicationContext).also { repository = it }
    }
}

/** Weekly baseline from the last seven complete days, or null when fewer than three days had scrolls. */
fun buildWeeklyBaseline(events: List<ScrollEvent>, today: LocalDate = LocalDate.now()): Int? {
    val window = (1L..7L).map { today.minusDays(it) }
    val counts = window.map { day -> events.firstOrNull { it.date == day }?.scrolls ?: 0 }
    if (counts.count { it > 0 } < 3) return null
    return counts.sum().coerceAtLeast(1)
}
