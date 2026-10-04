package com.stopdoomscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BackHandler {} // can't escape without answering
                Pause { q, a -> Store.add(this, q, a); finish() }
            }
        }
    }
}

@Composable
fun Pause(onDone: (String, String) -> Unit) {
    val q = rememberSaveable { Questions.list.random() }
    var a by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Pause. Wake your brain up.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Text(q, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = a, onValueChange = { a = it }, minLines = 3, label = { Text("Your answer") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
        Button(
            onClick = { onDone(q, a.trim()) }, enabled = a.trim().length >= 3,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save to taskboard") }
    }
}
