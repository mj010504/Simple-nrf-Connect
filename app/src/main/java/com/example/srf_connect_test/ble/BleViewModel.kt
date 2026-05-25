package com.example.srf_connect_test.ble

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ── UI models ─────────────────────────────────────────────────────────────────

data class ScannedDevice(val name: String, val address: String)

data class CharCardState(
    val serviceUuid: UUID,
    val charUuid: UUID,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canNotify: Boolean,
    val readValue: String? = null,
    val notifyEnabled: Boolean = false,
    val notificationLog: List<NotifEntry> = emptyList()
)


// ── ViewModel ─────────────────────────────────────────────────────────────────

class BleVM(app: Application) : AndroidViewModel(app) {

    val ble = BleManager(app)

    private val deviceMap = mutableMapOf<String, BluetoothDevice>()
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    @SuppressLint("MissingPermission")
    val devices: StateFlow<List<ScannedDevice>> = ble.scanResults.map { list ->
        list.map { device ->
            deviceMap[device.address] = device
            ScannedDevice(device.name ?: device.address, device.address)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characteristics: StateFlow<List<CharCardState>> = combine(
        ble.connState, ble.readValues, ble.notifLogs, ble.notifEnabled
    ) { connState, readValues, notifLogs, notifEnabled ->
        val on = connState as? ConnState.On ?: return@combine emptyList()
        buildCards(on.services, readValues, notifLogs, notifEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startScan() {
        _isScanning.value = true
        ble.startScan()
        viewModelScope.launch { delay(10_000); stopScan() }
    }

    fun stopScan() { _isScanning.value = false; ble.stopScan() }

    fun connect(address: String) { deviceMap[address]?.let { stopScan(); ble.connect(it) } }
    fun disconnect() = ble.disconnect()
    fun read(svc: UUID, chr: UUID) = ble.read(svc, chr)
    fun write(svc: UUID, chr: UUID, text: String) = ble.write(svc, chr, text.toByteArray())
    fun setNotify(svc: UUID, chr: UUID, on: Boolean) = ble.setNotify(svc, chr, on)

    override fun onCleared() { super.onCleared(); ble.disconnect() }

    private fun BluetoothGattCharacteristic.has(flag: Int) = properties and flag != 0

    private fun buildCards(
        gattServices: List<BluetoothGattService>,
        readValues: Map<UUID, String>,
        notifLogs: Map<UUID, List<NotifEntry>>,
        notifEnabled: Set<UUID>
    ): List<CharCardState> = gattServices.flatMap { svc ->
        svc.characteristics.map { c ->
            CharCardState(
                serviceUuid     = svc.uuid,
                charUuid        = c.uuid,
                canRead         = c.has(BluetoothGattCharacteristic.PROPERTY_READ),
                canWrite        = c.has(BluetoothGattCharacteristic.PROPERTY_WRITE),
                canNotify       = c.has(BluetoothGattCharacteristic.PROPERTY_NOTIFY),
                readValue       = readValues[c.uuid],
                notifyEnabled   = c.uuid in notifEnabled,
                notificationLog = notifLogs[c.uuid] ?: emptyList()
            )
        }
    }
}
