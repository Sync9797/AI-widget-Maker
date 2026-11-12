package com.sidequest.widgetmaker.feeds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Side Quest says the game changed (an egg hatched, an adventure ended...): refresh its values. */
class GameChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Feeds(context).settings.monsterQuest) FeedWorker.refreshSoon(context, setOf("mq"))
    }

    companion object {
        const val ACTION = "com.sidequest.widgetmaker.GAME_CHANGED"
    }
}
