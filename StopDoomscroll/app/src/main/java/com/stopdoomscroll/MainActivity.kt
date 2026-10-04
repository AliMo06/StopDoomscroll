package com.stopdoomscroll

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { App() } }
    }
}

fun serviceEnabled(c: Context): Boolean {
    val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val cn = ComponentName(c, DoomService::class.java)
    return s.contains(cn.flattenToString()) || s.contains(cn.flattenToShortString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(Store.tasks(ctx)) }
    var enabled by remember { mutableStateOf(serviceEnabled(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tasks = Store.tasks(ctx); enabled = serviceEnabled(ctx) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stop Doomscroll", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = "Taskboard") },
                    label = { Text("Taskboard") },
                    selected = tab == 0,
                    onClick = { tab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setup") },
                    label = { Text("Setup") },
                    selected = tab == 1,
                    onClick = { tab = 1 }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) { 
            if (tab == 0) Board(tasks) { tasks = it } else Setup(enabled) 
        }
    }
}

@Composable
fun Board(tasks: List<Task>, onChange: (List<Task>) -> Unit) {
    val ctx = LocalContext.current
    if (tasks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("You're all caught up!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Answers you give when Stop Doomscroll interrupts you land here.", 
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tasks.sortedBy { it.done }, key = { it.id }) { t ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (t.done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = t.done, 
                        onCheckedChange = { onChange(Store.toggle(ctx, t.id)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.question, 
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            t.answer, 
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (t.done) TextDecoration.LineThrough else null,
                            color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onChange(Store.delete(ctx, t.id)) }) { 
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
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
    
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(if (enabled) Icons.Default.CheckCircle else Icons.Default.SettingsAccessibility, contentDescription = null, tint = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Accessibility Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (enabled) "Running normally" else "Requires permission to monitor screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!enabled) {
                    Button(
                        onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text("Enable in Settings") 
                    }
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Monitoring Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(checked = on, onCheckedChange = { on = it; Store.setOn(ctx, it) })
                }
                Text("When enabled, we'll monitor scrolling activity across supported apps.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Intervention Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Interrupt me after ${mins.toInt()} minutes of doomscrolling", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = mins, 
                    onValueChange = { mins = it; Store.setLimit(ctx, it.toInt()) }, 
                    valueRange = 1f..60f, 
                    steps = 58
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 min", style = MaterialTheme.typography.labelSmall)
                    Text("60 min", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        OutlinedButton(
            onClick = { ctx.startActivity(Intent(ctx, InterventionActivity::class.java)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { 
            Text("Preview Intervention") 
        }
    }
}
