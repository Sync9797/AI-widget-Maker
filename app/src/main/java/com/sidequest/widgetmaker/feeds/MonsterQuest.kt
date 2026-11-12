package com.sidequest.widgetmaker.feeds

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Side Quest (the Side Quest game) shares a small summary of your game
 * with this app: your eggs, the monsters out on adventures, steps and sleep.
 */
object MonsterQuest {
    const val PACKAGE = "com.monsterquest.app"
    private val SUMMARY = Uri.parse("content://com.monsterquest.app.summary/summary")

    enum class Status { Ready, NotInstalled, NeedsUpdate }

    fun installed(context: Context) = runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess

    fun status(context: Context): Status = when {
        !installed(context) -> Status.NotInstalled
        context.packageManager.resolveContentProvider("com.monsterquest.app.summary", PackageManager.GET_META_DATA) == null -> Status.NeedsUpdate
        else -> Status.Ready
    }

    /** The game's summary as widget values; nothing if the game isn't installed or doesn't share yet. */
    fun read(context: Context): Map<String, String> {
        val json = context.contentResolver.query(SUMMARY, arrayOf("json"), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: return emptyMap()
        return MonsterQuestMath.values(json, System.currentTimeMillis())
    }
}

/** Reads the game's summary into widget values. Pure, for tests. */
object MonsterQuestMath {
    private val json = Json { ignoreUnknownKeys = true }

    fun values(body: String, nowMs: Long): Map<String, String> {
        val root = json.parseToJsonElement(body).obj()
        val out = mutableMapOf<String, String>()
        val count = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
        root["hero"].str()?.let { out["mq.hero"] = it }
        root["level"].num()?.let { out["mq.level"] = it.toInt().toString() }
        root["steps"].num()?.let { out["mq.steps"] = count.format(it.toLong()) }
        root["torches"].num()?.let { out["mq.torches"] = it.toInt().toString() }
        root["gold"].num()?.let { out["mq.gold"] = count.format(it.toLong()) }

        val eggs = (root["eggs"] as? JsonArray).orEmpty().map { it.obj() }
        out["mq.eggs"] = eggs.size.toString()
        out["mq.eggs.ready"] = eggs.count { it["ready"].bool() }.toString()
        eggs.take(5).forEachIndexed { i, e ->
            val n = i + 1
            out["mq.egg.$n"] = e["name"].str().orEmpty()
            out["mq.egg.$n.progress"] = (e["progress"].num() ?: 0.0).roundToInt().coerceIn(0, 100).toString()
            out["mq.egg.$n.left"] = if (e["ready"].bool()) "Ready to hatch!" else "${e["left"].str().orEmpty()} to go"
            out["mq.egg.$n.link"] = "mq-open:eggs"
            // A Hatch button's tap, only while the egg is ready (empty hides it).
            out["mq.egg.$n.hatch"] = if (e["ready"].bool()) e["id"].str()?.let { "mq-action:hatch:$it" }.orEmpty() else ""
        }

        val adventures = (root["adventures"] as? JsonArray).orEmpty().map { it.obj() }
        out["mq.adventures"] = adventures.size.toString()
        out["mq.adventures.done"] = adventures.count { (it["endMs"].num()?.toLong() ?: 0L) <= nowMs }.toString()
        adventures.take(3).forEachIndexed { i, a ->
            val n = i + 1
            val start = a["startMs"].num()?.toLong() ?: nowMs
            val end = a["endMs"].num()?.toLong() ?: nowMs
            out["mq.adv.$n"] = a["title"].str().orEmpty()
            out["mq.adv.$n.place"] = a["place"].str().orEmpty()
            out["mq.adv.$n.team"] = a["team"].str().orEmpty()
            out["mq.adv.$n.left"] = if (end <= nowMs) "Done! Collect your loot" else FeedParsers.duration(((end - nowMs) / 1000).toInt()) + " left"
            out["mq.adv.$n.progress"] = if (end <= start) "100" else (((nowMs - start) * 100) / (end - start)).coerceIn(0, 100).toString()
            out["mq.adv.$n.link"] = "mq-open:adventures"
            // A Claim button's tap, only once it's back (empty hides it).
            out["mq.adv.$n.claim"] = if (end <= nowMs) a["id"].str()?.let { "mq-action:claim:$it" }.orEmpty() else ""
        }

        val quests = (root["quests"] as? JsonArray).orEmpty().map { it.obj() }
        out["mq.quests"] = (root["questsOpen"].num()?.toInt() ?: quests.size).toString()
        quests.take(5).forEachIndexed { i, q ->
            val n = i + 1
            out["mq.quest.$n"] = q["title"].str().orEmpty()
            out["mq.quest.$n.when"] = q["when"].str().orEmpty()
            out["mq.quest.$n.link"] = "mq-open:quests"
            // A check button's tap: completes the quest in the game.
            out["mq.quest.$n.done"] = q["id"].str()?.let { "mq-action:complete:$it" }.orEmpty()
        }

        (root["party"] as? JsonArray).orEmpty().map { it.obj() }.take(6).forEachIndexed { i, m ->
            val n = i + 1
            val hp = m["hp"].num()?.toInt() ?: 0
            val max = m["maxHp"].num()?.toInt()?.coerceAtLeast(1) ?: 1
            out["mq.party.$n"] = m["name"].str().orEmpty()
            out["mq.party.$n.level"] = "Lv ${m["level"].num()?.toInt() ?: 1}"
            out["mq.party.$n.hp"] = "$hp/$max"
            out["mq.party.$n.hpPct"] = (hp * 100 / max).coerceIn(0, 100).toString()
        }

        (root["bounty"] as? JsonArray).orEmpty().map { it.obj() }.take(3).forEachIndexed { i, b ->
            val n = i + 1
            val goal = b["goal"].num()?.toLong()?.coerceAtLeast(1) ?: 1
            val progress = b["progress"].num()?.toLong() ?: 0
            out["mq.bounty.$n"] = b["text"].str().orEmpty()
            out["mq.bounty.$n.progress"] = "${count.format(progress)} / ${count.format(goal)}"
            out["mq.bounty.$n.pct"] = (progress * 100 / goal).coerceIn(0, 100).toString()
        }
        if (root["bountyReady"].bool()) out["mq.bounty.ready"] = "Bounty ready to claim!"

        root["torchSteps"].num()?.let { walked ->
            val per = root["stepsPerTorch"].num()?.toInt() ?: 1000
            out["mq.torch.pct"] = (walked.toInt() * 100 / per.coerceAtLeast(1)).coerceIn(0, 100).toString()
            out["mq.torch.next"] = "${count.format(per - walked.toInt())} steps to the next torch"
        }
        root["torchCap"].num()?.let { out["mq.torchCap"] = it.toInt().toString() }

        root["focus"]?.obj()?.let { f ->
            val start = f["startMs"].num()?.toLong() ?: nowMs
            val end = f["endMs"].num()?.toLong() ?: nowMs
            if (end > nowMs) {
                out["mq.focus"] = "Phone-free · ${FeedParsers.duration(((end - nowMs) / 1000).toInt())} left"
                out["mq.focus.pct"] = if (end <= start) "100" else (((nowMs - start) * 100) / (end - start)).coerceIn(0, 100).toString()
            }
        }

        root["sleep"]?.obj()?.let { s ->
            s["minutes"].num()?.let { out["mq.sleep"] = HealthMath.hoursMinutes(it.toLong()) }
            s["deep"].num()?.let { out["mq.sleep.deep"] = HealthMath.hoursMinutes(it.toLong()) }
        }
        return out
    }

    private fun JsonElement?.obj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonElement?.num(): Double? = (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.longOrNull?.toDouble() }
    private fun JsonElement?.bool(): Boolean = (this as? JsonPrimitive)?.content == "true"
    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
}
