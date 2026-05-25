package com.example.srf_connect_test.presentation

import androidx.compose.foundation.clickable
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
import com.example.srf_connect_test.ble.ScannedDevice
import com.example.srf_connect_test.ui.theme.Srf_connect_testTheme

@Composable
fun ScanScreen(
    isScanning: Boolean,
    devices: List<ScannedDevice>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("BLE Scanner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { if (isScanning) onStopScan() else onStartScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isScanning) "Stop Scanning" else "Start Scanning")
        }

        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    if (isScanning) "Searching..." else "Tap 'Start Scanning' to find devices",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "${devices.size} device(s) found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.address }) { device ->
                    Card(Modifier.fillMaxWidth().clickable { onConnect(device.address) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("Connect →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Idle")
@Composable
private fun ScanScreenIdlePreview() {
    Srf_connect_testTheme {
        ScanScreen(isScanning = false, devices = emptyList(), onStartScan = {}, onStopScan = {}, onConnect = {})
    }
}

@Preview(showBackground = true, name = "Scanning with devices")
@Composable
private fun ScanScreenScanningPreview() {
    Srf_connect_testTheme {
        ScanScreen(
            isScanning = true,
            devices = listOf(
                ScannedDevice("GATT Server", "AA:BB:CC:DD:EE:FF"),
                ScannedDevice("Unknown Device", "11:22:33:44:55:66")
            ),
            onStartScan = {}, onStopScan = {}, onConnect = {}
        )
    }
}
