package com.example.srf_connect_test.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.srf_connect_test.ble.CharCardState
import com.example.srf_connect_test.ble.NotifEntry
import com.example.srf_connect_test.ui.theme.Srf_connect_testTheme
import java.util.UUID

@Composable
fun DeviceScreen(
    deviceName: String,
    characteristics: List<CharCardState>,
    onDisconnect: () -> Unit,
    onRead:   (serviceUuid: UUID, charUuid: UUID) -> Unit,
    onWrite:  (serviceUuid: UUID, charUuid: UUID, text: String) -> Unit,
    onNotify: (serviceUuid: UUID, charUuid: UUID, enable: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column {
                Text(deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Connected  •  ${characteristics.size} characteristic(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Disconnect") }
        }
        HorizontalDivider()

        // Characteristic list
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(characteristics.size) { i ->
                val card = characteristics[i]
                ServiceCard(
                    state    = card,
                    onRead   = { chr       -> onRead(card.serviceUuid, chr) },
                    onWrite  = { chr, text -> onWrite(card.serviceUuid, chr, text) },
                    onNotify = { chr, on   -> onNotify(card.serviceUuid, chr, on) }
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Device Screen")
@Composable
private fun DeviceScreenPreview() {
    val svcUuid = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
    val now     = System.currentTimeMillis()

    Srf_connect_testTheme {
        DeviceScreen(
            deviceName = "GATT Server",
            characteristics = listOf(
                CharCardState(
                    serviceUuid = svcUuid,
                    charUuid = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb"),
                    canRead = true, canWrite = false, canNotify = false,
                    readValue = "22.5"
                ),
                CharCardState(
                    serviceUuid = svcUuid,
                    charUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"),
                    canRead = true, canWrite = false, canNotify = true,
                    readValue = "85%", notifyEnabled = true,
                    notificationLog = listOf(NotifEntry(now, "85%"), NotifEntry(now - 3_000, "84%"))
                )
            ),
            onDisconnect = {},
            onRead   = { _, _ -> },
            onWrite  = { _, _, _ -> },
            onNotify = { _, _, _ -> }
        )
    }
}
