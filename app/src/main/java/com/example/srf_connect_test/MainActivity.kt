package com.example.srf_connect_test

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.srf_connect_test.ble.BleVM
import com.example.srf_connect_test.ble.ConnState
import com.example.srf_connect_test.presentation.ConnectingScreen
import com.example.srf_connect_test.presentation.DeviceScreen
import com.example.srf_connect_test.presentation.ScanScreen
import com.example.srf_connect_test.ui.theme.Srf_connect_testTheme

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permLauncher.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            else
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        )
        enableEdgeToEdge()
        setContent {
            Srf_connect_testTheme {
                val vm: BleVM = viewModel()
                val connState  by vm.ble.connState.collectAsState()
                val isScanning by vm.isScanning.collectAsState()
                val devices    by vm.devices.collectAsState()
                val characteristics by vm.characteristics.collectAsState()

                Scaffold(Modifier.fillMaxSize()) { pad ->
                    when (connState) {
                        is ConnState.On -> DeviceScreen(
                            deviceName      = (connState as ConnState.On).name,
                            characteristics = characteristics,
                            onDisconnect = { vm.disconnect() },
                            onRead   = { svc, chr       -> vm.read(svc, chr) },
                            onWrite  = { svc, chr, text -> vm.write(svc, chr, text) },
                            onNotify = { svc, chr, on   -> vm.setNotify(svc, chr, on) },
                            modifier = Modifier.padding(pad)
                        )
                        is ConnState.Connecting -> ConnectingScreen(Modifier.padding(pad))
                        else -> ScanScreen(
                            isScanning  = isScanning,
                            devices     = devices,
                            onStartScan = { vm.startScan() },
                            onStopScan  = { vm.stopScan() },
                            onConnect   = { vm.connect(it) },
                            modifier    = Modifier.padding(pad)
                        )
                    }
                }
            }
        }
    }
}
