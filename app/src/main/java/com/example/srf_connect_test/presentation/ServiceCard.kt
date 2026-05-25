package com.example.srf_connect_test.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.srf_connect_test.ble.CharCardState
import com.example.srf_connect_test.ble.NotifEntry
import com.example.srf_connect_test.ui.theme.Srf_connect_testTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ServiceCard(
    state: CharCardState,
    onRead:   (UUID) -> Unit,
    onWrite:  (UUID, String) -> Unit,
    onNotify: (UUID, Boolean) -> Unit
) {
    var writeText by remember(state.charUuid) { mutableStateOf("") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {

            // Characteristic UUID + service UUID
            Text(
                state.charUuid.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Service: ${state.serviceUuid.toString().take(8).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.canRead)   FeatureBadge("READ",   MaterialTheme.colorScheme.primary)
                if (state.canWrite)  FeatureBadge("WRITE",  MaterialTheme.colorScheme.secondary)
                if (state.canNotify) FeatureBadge("NOTIFY", MaterialTheme.colorScheme.tertiary)
            }

            if (!state.canRead && !state.canWrite && !state.canNotify) return@Column

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            // ── Read ──────────────────────────────────────────────────────────
            if (state.canRead) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.readValue ?: "—", fontFamily = FontFamily.Monospace)
                    }
                    OutlinedButton(onClick = { onRead(state.charUuid) }) { Text("Read") }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Notify ────────────────────────────────────────────────────────
            if (state.canNotify) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Notifications", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (state.notifyEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.notifyEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.notifyEnabled, onCheckedChange = { onNotify(state.charUuid, it) })
                }
                if (state.notificationLog.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(8.dp)) {
                            state.notificationLog.take(5).forEach { entry -> NotifRow(entry) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Write ─────────────────────────────────────────────────────────
            if (state.canWrite) {
                Text("Write", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = writeText,
                        onValueChange = { writeText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter value...") },
                        singleLine = true
                    )
                    Button(
                        onClick = { onWrite(state.charUuid, writeText); writeText = "" },
                        enabled = writeText.isNotBlank()
                    ) { Text("Send") }
                }
            }
        }
    }
}

@Composable
private fun NotifRow(entry: NotifEntry) {
    Row(Modifier.padding(vertical = 1.dp)) {
        Text(
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.time)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(entry.value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
internal fun FeatureBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharCardPreview() {
    val svcUuid  = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
    val charUuid = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")
    val now      = System.currentTimeMillis()
    Srf_connect_testTheme {
        ServiceCard(
            state = CharCardState(
                serviceUuid = svcUuid, charUuid = charUuid,
                canRead = true, canWrite = false, canNotify = true,
                readValue = "22.5", notifyEnabled = true,
                notificationLog = listOf(NotifEntry(now, "22.5"), NotifEntry(now - 3000, "22.3"))
            ),
            onRead = {}, onWrite = { _, _ -> }, onNotify = { _, _ -> }
        )
    }
}
