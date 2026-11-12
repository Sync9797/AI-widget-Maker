package com.sidequest.widgetmaker.feeds

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Exercise and health from Health Connect, Android's shared health store:
 * Samsung Health, WHOOP, Google Fit, Fitbit, Garmin and others write there.
 */
object Health {
    const val PROVIDER = "com.google.android.apps.healthdata"

    /** What the widgets read. Background reading lets widgets refresh while the app is closed. */
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    )

    enum class Status { Ready, Partly, NeedsPermission, NeedsInstall, Unavailable }

    suspend fun status(context: Context): Status = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> {
            val granted = runCatching { HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions() }
                .getOrDefault(emptySet())
            when {
                granted.containsAll(permissions) -> Status.Ready
                granted.any { it in permissions } -> Status.Partly
                else -> Status.NeedsPermission
            }
        }
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NeedsInstall
        else -> Status.Unavailable
    }

    /** Opens the Play Store to install or update Health Connect. */
    fun installIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"))
        .setPackage("com.android.vending")

    /** Today's numbers, last night's sleep and the latest workout, as widget values. */
    suspend fun read(context: Context, stepGoal: Int): Map<String, String> {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        fun allowed(type: kotlin.reflect.KClass<out androidx.health.connect.client.records.Record>) =
            HealthPermission.getReadPermission(type) in granted
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
        val today = TimeRangeFilter.between(startOfDay, now)

        val metrics = buildSet {
            if (allowed(StepsRecord::class)) add(StepsRecord.COUNT_TOTAL)
            if (allowed(ActiveCaloriesBurnedRecord::class)) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (allowed(TotalCaloriesBurnedRecord::class)) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (allowed(ExerciseSessionRecord::class)) add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
        }
        val totals = if (metrics.isEmpty()) null else client.aggregate(AggregateRequest(metrics, today))

        val heart = if (allowed(HeartRateRecord::class)) {
            client.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startOfDay, now)))
                .records.flatMap { r -> r.samples.map { HealthMath.Pulse(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) } }
        } else emptyList()
        val resting = if (allowed(RestingHeartRateRecord::class)) {
            client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, TimeRangeFilter.between(now.minus(Duration.ofDays(3)), now)))
                .records.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
        } else null
        val workout = if (allowed(ExerciseSessionRecord::class)) {
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now)))
                .records.maxByOrNull { it.endTime }?.let {
                    HealthMath.Workout(
                        it.title?.takeIf { t -> t.isNotBlank() } ?: exerciseName(it.exerciseType),
                        LocalDateTime.ofInstant(it.startTime, zone),
                        Duration.between(it.startTime, it.endTime).toMinutes(),
                    )
                }
        } else null
        val sleep = if (allowed(SleepSessionRecord::class)) {
            client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(now.minus(Duration.ofHours(36)), now)))
                .records.maxByOrNull { it.endTime }?.let { r ->
                    fun mins(vararg types: Int) = r.stages.filter { it.stage in types }.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                    val awake = mins(SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED)
                    HealthMath.Sleep(Duration.between(r.startTime, r.endTime).toMinutes() - awake, mins(SleepSessionRecord.STAGE_TYPE_DEEP), mins(SleepSessionRecord.STAGE_TYPE_REM))
                }
        } else null

        return HealthMath.values(
            steps = totals?.get(StepsRecord.COUNT_TOTAL),
            stepGoal = stepGoal,
            activeKcal = totals?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories,
            totalKcal = totals?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
            exerciseMinutes = totals?.get(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)?.toMinutes(),
            heart = heart,
            resting = resting,
            workout = workout,
            sleep = sleep,
            now = LocalDateTime.now(),
        )
    }

    /** Readable names for the common exercise types. */
    private fun exerciseName(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Run"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Treadmill run"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Bike ride"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Indoor cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swim"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Pool swim"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hike"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength training"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weight lifting"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "Stair climbing"
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "Boot camp"
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "Basketball"
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Soccer"
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Tennis"
        else -> "Workout"
    }
}
