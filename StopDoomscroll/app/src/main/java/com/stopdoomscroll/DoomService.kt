package com.stopdoomscroll

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent

class DoomService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var doomMs = 0L          // accumulated doomscrolling time
    private var calmMs = 0L          // continuous time spent NOT doomscrolling
    private var lastScrollAt = 0L
    private var prevThumb: IntArray? = null

    private val tick = object : Runnable {
        override fun run() { sample(); handler.postDelayed(this, SAMPLE_MS) }
    }

    override fun onServiceConnected() { handler.post(tick) }
    override fun onInterrupt() {}
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && e.packageName?.toString() in Detector.APPS)
            lastScrollAt = SystemClock.elapsedRealtime()
    }

    private fun sample() {
        val root = rootInActiveWindow
        val pkg = root?.packageName?.toString()
        if (root == null || pkg !in Detector.APPS || !Store.on(this) || !Detector.looksLikeFeed(pkg!!, root)) {
            prevThumb = null; calm(); return
        }
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                val scrolling = SystemClock.elapsedRealtime() - lastScrollAt < 45_000
                val thumb = runCatching { Detector.thumbnail(r) }.getOrDefault(IntArray(0))
                val newContent = prevThumb?.let { Detector.diff(it, thumb) > 30 } ?: true
                prevThumb = thumb
                if (scrolling || newContent) doom() else calm()
            }
            override fun onFailure(errorCode: Int) {
                if (SystemClock.elapsedRealtime() - lastScrollAt < 45_000) doom() else calm()
            }
        })
    }

    private fun doom() {
        calmMs = 0
        doomMs += SAMPLE_MS
        if (doomMs >= Store.limit(this) * 60_000L) {
            doomMs = 0
            startActivity(Intent(this, InterventionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun calm() {
        calmMs += SAMPLE_MS
        if (calmMs >= 60_000) doomMs = 0   // a real break resets the clock
    }

    companion object { const val SAMPLE_MS = 5_000L }
}
