package com.example.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.DeviceCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class BluetoothScannerManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _scannedDevices = MutableStateFlow<Map<String, BluetoothDeviceItem>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, BluetoothDeviceItem>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _simulationMode = MutableStateFlow(true) // Default true for instant usability in emulator/preview
    val simulationMode: StateFlow<Boolean> = _simulationMode.asStateFlow()

    private var scanJob: Job? = null
    private var simulationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { handleScanResult(it) }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothScanner", "BLE Scan failed with code $errorCode")
            _isScanning.value = false
        }
    }

    init {
        // Pre-populate with initial simulation devices
        generateInitialSimulatedDevices()
    }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val connectPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scanPerm && connectPerm
        } else {
            val locationPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return locationPerm
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _simulationMode.value = enabled
        if (enabled && _scannedDevices.value.isEmpty()) {
            generateInitialSimulatedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return

        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _isScanning.value = true

        val isRealBtAvailable = hasPermissions() && bluetoothAdapter?.isEnabled == true

        if (isRealBtAvailable) {
            try {
                val scanner = bluetoothAdapter?.bluetoothLeScanner
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner?.startScan(null, settings, bleScanCallback)
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "Failed to start real BLE scan", e)
            }
        }

        // Always run simulation ticker if simulation mode is active or no real BT devices found
        startSimulationTicker()

        // Auto-stop scan after 15 seconds to save battery
        scanJob?.cancel()
        scanJob = coroutineScope.launch {
            delay(15000)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return

        if (hasPermissions() && bluetoothAdapter?.isEnabled == true) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "Failed to stop real BLE scan", e)
            }
        }

        simulationJob?.cancel()
        _isScanning.value = false
        scanJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device: BluetoothDevice = result.device ?: return
        val address = device.address ?: return
        val rawName = device.name ?: result.scanRecord?.deviceName ?: ""
        val rssi = result.rssi

        val category = autoDetectCategory(rawName, device.bluetoothClass?.majorDeviceClass)

        _scannedDevices.update { current ->
            val existing = current[address]
            val updated = existing?.copy(
                rssi = rssi,
                lastSeenMs = System.currentTimeMillis()
            ) ?: BluetoothDeviceItem(
                macAddress = address,
                originalName = if (rawName.isBlank()) "Appareil Inconnu" else rawName,
                rssi = rssi,
                category = category,
                isSimulated = false,
                lastSeenMs = System.currentTimeMillis()
            )
            current + (address to updated)
        }
    }

    private fun autoDetectCategory(name: String, majorClass: Int?): DeviceCategory {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("casque") || lowerName.contains("headphone") || lowerName.contains("airpod") || lowerName.contains("buds") || lowerName.contains("bose") || lowerName.contains("wh-") -> DeviceCategory.AUDIO
            lowerName.contains("speaker") || lowerName.contains("enceinte") || lowerName.contains("jbl") || lowerName.contains("flip") || lowerName.contains("boom") -> DeviceCategory.SPEAKER
            lowerName.contains("watch") || lowerName.contains("band") || lowerName.contains("fitbit") || lowerName.contains("garmin") -> DeviceCategory.WATCH
            lowerName.contains("tv") || lowerName.contains("smart tv") || lowerName.contains("chromecast") -> DeviceCategory.TV
            lowerName.contains("macbook") || lowerName.contains("laptop") || lowerName.contains("pc") || lowerName.contains("dell") || lowerName.contains("thinkpad") -> DeviceCategory.LAPTOP
            lowerName.contains("phone") || lowerName.contains("iphone") || lowerName.contains("galaxy") || lowerName.contains("pixel") -> DeviceCategory.PHONE
            lowerName.contains("car") || lowerName.contains("tesla") || lowerName.contains("auto") -> DeviceCategory.CAR
            else -> DeviceCategory.GENERIC
        }
    }

    private fun generateInitialSimulatedDevices() {
        val simulatedList = listOf(
            BluetoothDeviceItem(
                macAddress = "74:A2:E6:9A:12:40",
                originalName = "Bose QuietComfort 45",
                rssi = -52,
                category = DeviceCategory.AUDIO,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "F4:D4:88:B1:05:CC",
                originalName = "Apple AirPods Pro",
                rssi = -64,
                category = DeviceCategory.AUDIO,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "A8:51:5B:33:41:09",
                originalName = "Samsung Galaxy Watch 6",
                rssi = -71,
                category = DeviceCategory.WATCH,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "C0:38:96:11:00:DF",
                originalName = "Sony WH-1000XM5",
                rssi = -58,
                category = DeviceCategory.AUDIO,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "48:2C:6A:EE:89:12",
                originalName = "MacBook Pro M3",
                rssi = -76,
                category = DeviceCategory.LAPTOP,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "AC:12:4B:90:3F:82",
                originalName = "JBL Charge 5",
                rssi = -82,
                category = DeviceCategory.SPEAKER,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "00:1A:7D:DA:71:04",
                originalName = "LG 65 OLED TV Salon",
                rssi = -88,
                category = DeviceCategory.TV,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "2C:F4:32:8B:44:0E",
                originalName = "Tesla Model 3 BT",
                rssi = -60,
                category = DeviceCategory.CAR,
                isSimulated = true
            ),
            BluetoothDeviceItem(
                macAddress = "50:1E:2D:10:99:A3",
                originalName = "Xiaomi Smart Band 8",
                rssi = -67,
                category = DeviceCategory.WATCH,
                isSimulated = true
            )
        )

        val map = simulatedList.associateBy { it.macAddress }
        _scannedDevices.value = map
    }

    private fun startSimulationTicker() {
        simulationJob?.cancel()
        simulationJob = coroutineScope.launch {
            while (_isScanning.value) {
                delay(1200)
                if (_simulationMode.value) {
                    _scannedDevices.update { current ->
                        current.mapValues { (_, device) ->
                            if (device.isSimulated) {
                                val rssiDelta = Random.nextInt(-4, 5)
                                val newRssi = (device.rssi + rssiDelta).coerceIn(-95, -40)
                                device.copy(
                                    rssi = newRssi,
                                    lastSeenMs = System.currentTimeMillis()
                                )
                            } else device
                        }
                    }
                }
            }
        }
    }

    fun addSimulatedDevice(name: String, category: DeviceCategory) {
        val randomMac = String.format(
            "%02X:%02X:%02X:%02X:%02X:%02X",
            Random.nextInt(0, 256),
            Random.nextInt(0, 256),
            Random.nextInt(0, 256),
            Random.nextInt(0, 256),
            Random.nextInt(0, 256),
            Random.nextInt(0, 256)
        )
        val newItem = BluetoothDeviceItem(
            macAddress = randomMac,
            originalName = name.ifBlank { "Nouvel Appareil" },
            rssi = Random.nextInt(-75, -50),
            category = category,
            isSimulated = true,
            lastSeenMs = System.currentTimeMillis()
        )
        _scannedDevices.update { it + (randomMac to newItem) }
    }
}
