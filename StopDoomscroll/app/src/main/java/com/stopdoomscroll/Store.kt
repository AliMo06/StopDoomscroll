package com.stopdoomscroll

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Task(val id: Long, val question: String, val answer: String, val done: Boolean)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("sd", Context.MODE_PRIVATE)

    fun limit(c: Context) = sp(c).getInt("limit", 10)
    fun setLimit(c: Context, m: Int) = sp(c).edit().putInt("limit", m).apply()
    fun on(c: Context) = sp(c).getBoolean("on", true)
    fun setOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("on", v).apply()

    fun tasks(c: Context): List<Task> {
        val a = JSONArray(sp(c).getString("tasks", "[]"))
        return (0 until a.length()).map {
            a.getJSONObject(it).let { o -> Task(o.getLong("id"), o.getString("q"), o.getString("a"), o.getBoolean("d")) }
        }
    }

    private fun save(c: Context, l: List<Task>): List<Task> {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("q", it.question).put("a", it.answer).put("d", it.done)) }
        sp(c).edit().putString("tasks", a.toString()).apply()
        return l
    }

    fun add(c: Context, q: String, a: String) =
        save(c, listOf(Task(System.currentTimeMillis(), q, a, false)) + tasks(c))

    fun toggle(c: Context, id: Long) = save(c, tasks(c).map { if (it.id == id) it.copy(done = !it.done) else it })
    fun delete(c: Context, id: Long) = save(c, tasks(c).filter { it.id != id })
}

object Questions {
    val list = listOf(
        "What are five ideas you can come up with right now?",
        "What did you learn from the last reel?",
        "What will you do five minutes from now?",
        "What is your objective for the rest of the day?",
        "What's one thing you've been avoiding that you could start in 2 minutes?",
        "Who could you message right now to make their day better?",
        "What problem is on your mind that you could think about for 60 seconds?",
        "Name three things you can see, hear and feel right now.",
        "What would future you want you to be doing instead?",
    )
}
