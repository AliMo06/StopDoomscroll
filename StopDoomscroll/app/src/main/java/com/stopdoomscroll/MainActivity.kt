package com.stopdoomscroll

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

fun serviceEnabled(c: Context): Boolean {
    val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val cn = ComponentName(c, DoomService::class.java)
    return s.contains(cn.flattenToString()) || s.contains(cn.flattenToShortString())
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(Store.tasks(ctx)) }
    var enabled by remember { mutableStateOf(serviceEnabled(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tasks = Store.tasks(ctx); enabled = serviceEnabled(ctx) }

    Scaffold(topBar = {
        TabRow(selectedTabIndex = tab, modifier = Modifier.statusBarsPadding()) {
            listOf("Taskboard", "Setup").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) { if (tab == 0) Board(tasks) { tasks = it } else Setup(enabled) }
    }
}

@Composable
fun Board(tasks: List<Task>, onChange: (List<Task>) -> Unit) {
    val ctx = LocalContext.current
    if (tasks.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text("Nothing here yet. Answers you give when Stop Doomscroll interrupts you land here.", textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks.sortedBy { it.done }, key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(t.done, { onChange(Store.toggle(ctx, t.id)) })
                    Column(Modifier.weight(1f)) {
                        Text(t.question, style = MaterialTheme.typography.labelMedium)
                        Text(t.answer, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                    }
                    TextButton({ onChange(Store.delete(ctx, t.id)) }) { Text("✕") }
                }
            }
        }
    }
}

@Composable
fun Setup(enabled: Boolean) {
    val ctx = LocalContext.current
    var mins by remember { mutableFloatStateOf(Store.limit(ctx).toFloat()) }
    var on by remember { mutableStateOf(Store.on(ctx)) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Interrupt me after ${mins.toInt()} min of doomscrolling", style = MaterialTheme.typography.titleMedium)
        Slider(mins, { mins = it; Store.setLimit(ctx, it.toInt()) }, valueRange = 1f..60f, steps = 58)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Monitoring", Modifier.weight(1f))
            Switch(on, { on = it; Store.setOn(ctx, it) })
        }
        Text(if (enabled) "✅ Accessibility service is on" else "⚠️ Turn on \"Stop Doomscroll\" in Accessibility settings so it can see the screen.")
        Button({ ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
        OutlinedButton({ ctx.startActivity(Intent(ctx, InterventionActivity::class.java)) }) { Text("Test an intervention now") }
    }
}
