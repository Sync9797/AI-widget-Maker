package com.sidequest.widgetmaker.feeds

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
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

    /** NOOP, the offline WHOOP companion (github.com/ryanbr/noop): its data is preferred when it shares to Health Connect. */
    const val NOOP = "com.noop.whoop"

    /** What the widgets read. Background reading lets widgets refresh while the app is closed. */
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
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

        val heart = if (allowed(HeartRateRecord::class)) runCatching {
            // NOOP writes heart rate in chunks that can start before midnight, so read the last 30 hours (every
            // page of it), NOOP's when it shares, and keep today's readings; early in the day, the last 24 hours.
            val records = preferNoop(readAll(client, HeartRateRecord::class, TimeRangeFilter.between(now.minus(Duration.ofHours(30)), now))) {
                it.metadata.dataOrigin.packageName
            }
            val samples = records.flatMap { r -> r.samples.map { HealthMath.Pulse(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) } }
                .filter { it.timeMs <= now.toEpochMilli() }.distinctBy { it.timeMs }.sortedBy { it.timeMs }
            val today = samples.filter { it.timeMs >= startOfDay.toEpochMilli() }
            if (today.size >= 2) today else samples.filter { it.timeMs >= now.minus(Duration.ofHours(24)).toEpochMilli() }
        }.getOrDefault(emptyList()) else emptyList()
        // The last 30 days of these, for today's value and your own baseline (NOOP's when it shares them).
        // NOOP stamps each day's numbers at noon, so read to the end of today: in the morning,
        // today's (from last night) is otherwise still "in the future" and skipped.
        val endOfToday = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
        val month = TimeRangeFilter.between(now.minus(Duration.ofDays(30)), endOfToday)
        val recent = TimeRangeFilter.between(now.minus(Duration.ofDays(3)), endOfToday)
        val restingDays = if (allowed(RestingHeartRateRecord::class)) {
            preferNoop(client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, month)).records) { it.metadata.dataOrigin.packageName }
                .sortedBy { it.time }.map { it.time to it.beatsPerMinute.toDouble() }
        } else emptyList()
        val resting = restingDays.lastOrNull()?.takeIf { Duration.between(it.first, now).toHours() < 60 }?.second?.toInt()
        val hrvDays = if (allowed(HeartRateVariabilityRmssdRecord::class)) {
            preferNoop(client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, month)).records) { it.metadata.dataOrigin.packageName }
                .sortedBy { it.time }.map { it.time to it.heartRateVariabilityMillis }
        } else emptyList()
        val spo2 = if (allowed(OxygenSaturationRecord::class)) {
            preferNoop(client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, recent)).records) { it.metadata.dataOrigin.packageName }
                .maxByOrNull { it.time }?.percentage?.value
        } else null
        val respiratory = if (allowed(RespiratoryRateRecord::class)) {
            preferNoop(client.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, recent)).records) { it.metadata.dataOrigin.packageName }
                .maxByOrNull { it.time }?.rate
        } else null
        val vo2 = if (allowed(Vo2MaxRecord::class)) {
            preferNoop(client.readRecords(ReadRecordsRequest(Vo2MaxRecord::class, month)).records) { it.metadata.dataOrigin.packageName }
                .maxByOrNull { it.time }?.vo2MillilitersPerMinuteKilogram
        } else null
        val fromNoop = (restingDays.isNotEmpty() || hrvDays.isNotEmpty()) && runCatching {
            client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, month)).records.any { it.metadata.dataOrigin.packageName == NOOP }
        }.getOrDefault(false)
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
            // The last week of nights (NOOP's when it shares them): last night, and your average.
            val nights = preferNoop(
                client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(now.minus(Duration.ofDays(8)), now))).records,
            ) { it.metadata.dataOrigin.packageName }
            fun asleep(r: SleepSessionRecord): Long {
                val awake = r.stages.filter { it.stage in AWAKE }.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                return Duration.between(r.startTime, r.endTime).toMinutes() - awake
            }
            nights.maxByOrNull { it.endTime }?.takeIf { Duration.between(it.endTime, now).toHours() < 36 }?.let { r ->
                fun mins(vararg types: Int) = r.stages.filter { it.stage in types }.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                val stages = r.stages.sortedBy { it.startTime }.map {
                    com.sidequest.widgetmaker.spec.SleepStages.Part(stageLetter(it.stage), Duration.between(it.startTime, it.endTime).toMinutes().toInt())
                }
                // A night per day: the longest, when an app split one.
                val byDay = nights.groupBy { LocalDateTime.ofInstant(it.endTime, zone).toLocalDate() }
                    .mapValues { (_, n) -> n.maxOf(::asleep) }.toSortedMap()
                val perNight = byDay.values.toList()
                HealthMath.Sleep(
                    asleep(r), mins(SleepSessionRecord.STAGE_TYPE_DEEP), mins(SleepSessionRecord.STAGE_TYPE_REM),
                    stages = stages,
                    averageMinutes = perNight.takeIf { it.size >= 3 }?.average()?.toLong(),
                    bedtime = LocalDateTime.ofInstant(r.startTime, zone),
                    wake = LocalDateTime.ofInstant(r.endTime, zone),
                    week = byDay.entries.map { it.key to it.value },
                )
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
            extras = HealthMath.Extras(
                hrvToday = hrvDays.lastOrNull()?.takeIf { Duration.between(it.first, now).toHours() < 60 }?.second,
                hrvBaseline = HealthMath.baseline(hrvDays.map { it.second }),
                restingBaseline = HealthMath.baseline(restingDays.map { it.second }),
                spo2 = spo2,
                respiratoryRate = respiratory,
                vo2max = vo2,
                source = if (fromNoop) "NOOP" else null,
                hrvAllowed = allowed(HeartRateVariabilityRmssdRecord::class),
            ),
        )
    }

    /** The data types the app can read but isn't allowed to yet (added in an update, or turned off). */
    suspend fun missing(context: Context): Set<String> = runCatching {
        if (HealthConnectClient.getSdkStatus(context, PROVIDER) != HealthConnectClient.SDK_AVAILABLE) return emptySet()
        val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        permissions - granted
    }.getOrDefault(emptySet())

    private val AWAKE = setOf(SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED)

    private fun stageLetter(stage: Int): Char = when (stage) {
        in AWAKE -> 'A'
        SleepSessionRecord.STAGE_TYPE_DEEP -> 'D'
        SleepSessionRecord.STAGE_TYPE_REM -> 'R'
        else -> 'L'
    }

    /** Every page of a read (Health Connect returns them a page at a time). */
    private suspend fun <T : androidx.health.connect.client.records.Record> readAll(
        client: HealthConnectClient,
        type: kotlin.reflect.KClass<T>,
        range: TimeRangeFilter,
    ): List<T> {
        val out = mutableListOf<T>()
        var token: String? = null
        repeat(10) {
            val page = client.readRecords(ReadRecordsRequest(type, range, pageSize = 5000, pageToken = token))
            out += page.records
            token = page.pageToken ?: return out
        }
        return out
    }

    /**
     * What Health Connect is sharing with this app, for Settings: for each kind
     * of data, whether it's allowed, how many records the last day has, and
     * which apps they come from.
     */
    suspend fun diagnose(context: Context): List<String> = runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val now = Instant.now()
        val day = TimeRangeFilter.between(now.minus(Duration.ofHours(30)), now)
        val pm = context.packageManager
        fun appName(pkg: String) = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        suspend fun <T : androidx.health.connect.client.records.Record> line(label: String, type: kotlin.reflect.KClass<T>): String {
            if (HealthPermission.getReadPermission(type) !in granted) return "$label: not allowed"
            val records = runCatching { readAll(client, type, day) }.getOrElse { return "$label: couldn't read (${it.javaClass.simpleName})" }
            if (records.isEmpty()) return "$label: nothing in the last day"
            val from = records.map { it.metadata.dataOrigin.packageName }.distinct().joinToString(", ", transform = ::appName)
            return "$label: ${records.size} in the last day, from $from"
        }
        listOf(
            line("Heart rate", HeartRateRecord::class),
            line("Resting heart rate", RestingHeartRateRecord::class),
            line("HRV", HeartRateVariabilityRmssdRecord::class),
            line("Blood oxygen", OxygenSaturationRecord::class),
            line("Breathing rate", RespiratoryRateRecord::class),
            line("Sleep", SleepSessionRecord::class),
            line("Steps", StepsRecord::class),
            line("Workouts", ExerciseSessionRecord::class),
        ) + if (PERMISSION_BACKGROUND !in granted) listOf("Background reading: not allowed, so widgets only update while the app is open") else emptyList()
    }.getOrElse { listOf("Couldn't reach Health Connect (${it.javaClass.simpleName}).") }

    private const val PERMISSION_BACKGROUND = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    /** NOOP's records when there are any, else everyone's (so two apps' numbers don't mix). */
    private fun <T> preferNoop(records: List<T>, origin: (T) -> String): List<T> =
        records.filter { origin(it) == NOOP }.ifEmpty { records }

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
