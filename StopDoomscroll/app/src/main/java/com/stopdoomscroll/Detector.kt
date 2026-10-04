package com.stopdoomscroll

import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.graphics.Bitmap
import android.graphics.Color
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs

/**
 * Doomscroll "AI": fuses three signals.
 *  1. foreground app is a known short-form app
 *  2. accessibility tree shows the Reels/Shorts/TikTok feed UI (view ids / selected tab)
 *  3. timestamped screenshot thumbnails: content changed a lot between samples (new video swiped in)
 * To upgrade to a real model, replace [looksLikeFeed] with a TFLite classifier over the thumbnail.
 */
object Detector {
    val APPS = setOf(
        "com.instagram.android", "com.google.android.youtube", "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill", "com.facebook.katana", "com.snapchat.android",
    )
    private val ALWAYS_FEED = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    private val STRONG_IDS = listOf("clips_viewer", "reel_recycler", "reel_player", "shorts_")
    private val TAB_WORDS = listOf("reel", "shorts")
    const val W = 24
    const val H = 48

    fun looksLikeFeed(pkg: String, root: AccessibilityNodeInfo): Boolean {
        if (pkg in ALWAYS_FEED) return true
        val q = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var seen = 0
        while (q.isNotEmpty() && seen++ < 500) {
            val n = q.removeFirst()
            val id = n.viewIdResourceName?.lowercase() ?: ""
            if (STRONG_IDS.any { it in id }) return true
            // Nav-bar "Reels"/"Shorts" tab only counts when it is the selected tab
            val label = "${n.contentDescription} ${n.text}".lowercase()
            if (n.isSelected && TAB_WORDS.any { it in label }) return true
            for (i in 0 until n.childCount) n.getChild(i)?.let(q::add)
        }
        return false
    }

    fun thumbnail(r: ScreenshotResult): IntArray {
        val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
        val soft = hw?.copy(Bitmap.Config.ARGB_8888, false)
        r.hardwareBuffer.close()
        if (soft == null) return IntArray(0)
        val small = Bitmap.createScaledBitmap(soft, W, H, true)
        val px = IntArray(W * H).also { small.getPixels(it, 0, W, 0, 0, W, H) }
        hw.recycle(); soft.recycle(); small.recycle()
        return IntArray(px.size) { (Color.red(px[it]) * 3 + Color.green(px[it]) * 6 + Color.blue(px[it])) / 10 }
    }

    /** Mean absolute grayscale difference, 0..255. Different video ~35+, same video ~10-25. */
    fun diff(a: IntArray, b: IntArray): Int {
        if (a.isEmpty() || a.size != b.size) return 255
        var s = 0L
        for (i in a.indices) s += abs(a[i] - b[i])
        return (s / a.size).toInt()
    }
}
