package com.example.srf_connect_test.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

sealed class ConnState {
    object Off : ConnState()
    object Connecting : ConnState()
    data class On(val name: String, val services: List<BluetoothGattService>) : ConnState()
}

data class NotifEntry(val time: Long, val value: String)

val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var gatt: BluetoothGatt? = null

    val scanResults = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val connState   = MutableStateFlow<ConnState>(ConnState.Off)
    val readValues  = MutableStateFlow<Map<UUID, String>>(emptyMap())
    val notifLogs   = MutableStateFlow<Map<UUID, List<NotifEntry>>>(emptyMap())
    val notifEnabled = MutableStateFlow<Set<UUID>>(emptySet())

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCb = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val d = result.device
            if (scanResults.value.none { it.address == d.address })
                scanResults.value = scanResults.value + d
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleManager", "Scan failed: error $errorCode")
        }
    }

    fun startScan() {
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BleManager", "bluetoothLeScanner is null — Bluetooth off?")
            return
        }
        scanResults.value = emptyList()
        scanner.startScan(null, scanSettings, scanCb)
    }

    fun stopScan() = adapter.bluetoothLeScanner?.stopScan(scanCb)

    fun connect(device: BluetoothDevice) {
        connState.value = ConnState.Connecting
        gatt = device.connectGatt(context, false, gattCb, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() = gatt?.disconnect()

    private fun findChar(svcUuid: UUID, charUuid: UUID): BluetoothGattCharacteristic? =
        gatt?.services?.find { it.uuid == svcUuid && it.getCharacteristic(charUuid) != null }
            ?.getCharacteristic(charUuid)

    fun read(svcUuid: UUID, charUuid: UUID) {
        val c = findChar(svcUuid, charUuid)
        if (c == null) { Log.e("BleManager", "read: characteristic not found $charUuid"); return }
        val ok = gatt?.readCharacteristic(c) ?: false
        if (!ok) Log.e("BleManager", "read: readCharacteristic returned false for $charUuid")
    }

    fun write(svcUuid: UUID, charUuid: UUID, data: ByteArray) {
        val c = findChar(svcUuid, charUuid) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(c, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            c.value = data
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(c)
        }
    }

    fun setNotify(svcUuid: UUID, charUuid: UUID, enable: Boolean) {
        val c = findChar(svcUuid, charUuid) ?: return
        gatt?.setCharacteristicNotification(c, enable)
        val desc = c.getDescriptor(CCCD) ?: return
        val v = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeDescriptor(desc, v)
        } else {
            @Suppress("DEPRECATION")
            desc.value = v
            @Suppress("DEPRECATION")
            gatt?.writeDescriptor(desc)
        }
        notifEnabled.value = if (enable) notifEnabled.value + charUuid else notifEnabled.value - charUuid
    }

    private fun toStr(b: ByteArray): String {
        val s = b.toString(Charsets.UTF_8).trim()
        return if (s.isNotEmpty() && s.none { it.isISOControl() && it != '\n' && it != '\r' }) s
        else b.joinToString(" ") { "%02X".format(it) }
    }

    private fun putRead(uuid: UUID, bytes: ByteArray) {
        readValues.value = readValues.value + (uuid to toStr(bytes))
    }

    private fun putNotif(uuid: UUID, bytes: ByteArray) {
        val s = toStr(bytes)
        val prev = notifLogs.value[uuid] ?: emptyList()
        notifLogs.value = notifLogs.value + (uuid to (listOf(NotifEntry(System.currentTimeMillis(), s)) + prev).take(20))
        readValues.value = readValues.value + (uuid to s)
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connState.value = ConnState.Off
                readValues.value = emptyMap()
                notifLogs.value = emptyMap()
                notifEnabled.value = emptySet()
                g.close()
                gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                connState.value = ConnState.On(g.device.name ?: g.device.address, g.services)
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) putRead(c.uuid, value)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && status == BluetoothGatt.GATT_SUCCESS)
                putRead(c.uuid, c.value ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            putNotif(c.uuid, value)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                putNotif(c.uuid, c.value ?: byteArrayOf())
        }
    }
}
