package com.sidequest.widgetmaker.ai

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The free way to design: hand the request to the Claude app (or claude.ai in
 * the browser), where a free account works, and read Claude's reply back from
 * the clipboard. No API key needed.
 */
object ClaudeApp {
    const val PACKAGE = "com.anthropic.claude"
    private const val WEB = "https://claude.ai/new"

    enum class Opened { App, Web }

    /**
     * Copies [text] and opens Claude with it: shared straight into the Claude
     * app when it's installed, otherwise claude.ai in the browser (paste it there).
     */
    fun send(context: Context, text: String): Opened {
        copy(context, text)
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
            .setPackage(PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, share)) return Opened.App
        return open(context)
    }

    /** Opens Claude without new text (to paste a follow-up into the same chat). */
    fun open(context: Context): Opened {
        context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { if (tryStart(context, it)) return Opened.App }
        tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(WEB)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return Opened.Web
    }

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Widget request", text))
    }

    /** The clipboard's text, or null. Only readable while this app is on screen. */
    fun pasted(context: Context): String? {
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    private fun tryStart(context: Context, intent: Intent) = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
