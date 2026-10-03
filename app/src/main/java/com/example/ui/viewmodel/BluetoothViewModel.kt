package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bluetooth.BluetoothScannerManager
import com.example.data.local.AppDatabase
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.DeviceCategory
import com.example.data.repository.BluetoothRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption(val labelFr: String) {
    FAVORITES_FIRST("Favoris en premier"),
    SIGNAL_STRENGTH("Force du signal (RSSI)"),
    NAME_AZ("Nom (A-Z)"),
    LAST_SEEN("Récemment détectés")
}

data class BluetoothUiState(
    val devices: List<BluetoothDeviceItem> = emptyList(),
    val favoriteDevices: List<BluetoothDeviceItem> = emptyList(),
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = true,
    val simulationMode: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: DeviceCategory? = null,
    val selectedSort: SortOption = SortOption.FAVORITES_FIRST,
    val selectedDeviceForEdit: BluetoothDeviceItem? = null,
    val selectedDeviceForRadar: BluetoothDeviceItem? = null,
    val userMessage: String? = null
)

class BluetoothViewModel(
    application: Application,
    private val repository: BluetoothRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<DeviceCategory?>(null)
    private val _selectedSort = MutableStateFlow(SortOption.FAVORITES_FIRST)
    private val _selectedDeviceForEdit = MutableStateFlow<BluetoothDeviceItem?>(null)
    private val _selectedDeviceForRadar = MutableStateFlow<BluetoothDeviceItem?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _filterState = combine(_searchQuery, _selectedCategory, _selectedSort) { query, category, sort ->
        Triple(query, category, sort)
    }

    private val _scannerState = combine(
        repository.isScanning,
        repository.isBluetoothEnabled,
        repository.simulationMode
    ) { scanning, btEnabled, simMode ->
        Triple(scanning, btEnabled, simMode)
    }

    val uiState: StateFlow<BluetoothUiState> = combine(
        repository.combinedDevices,
        _scannerState,
        _filterState,
        _selectedDeviceForEdit,
        _selectedDeviceForRadar
    ) { rawDevices, (scanning, btEnabled, simMode), (query, categoryFilter, sortOpt), editDevice, radarDevice ->

        val filtered = rawDevices.filter { device ->
            val matchesQuery = query.isBlank() ||
                    device.displayName.contains(query, ignoreCase = true) ||
                    device.originalName.contains(query, ignoreCase = true) ||
                    device.macAddress.contains(query, ignoreCase = true) ||
                    device.notes.contains(query, ignoreCase = true)

            val matchesCategory = categoryFilter == null || device.category == categoryFilter

            matchesQuery && matchesCategory
        }.sortedWith { d1, d2 ->
            when (sortOpt) {
                SortOption.FAVORITES_FIRST -> {
                    val favCompare = d2.isFavorite.compareTo(d1.isFavorite)
                    if (favCompare != 0) favCompare else d2.rssi.compareTo(d1.rssi)
                }
                SortOption.SIGNAL_STRENGTH -> d2.rssi.compareTo(d1.rssi)
                SortOption.NAME_AZ -> d1.displayName.lowercase().compareTo(d2.displayName.lowercase())
                SortOption.LAST_SEEN -> d2.lastSeenMs.compareTo(d1.lastSeenMs)
            }
        }

        val favs = rawDevices.filter { it.isFavorite }

        BluetoothUiState(
            devices = filtered,
            favoriteDevices = favs,
            isScanning = scanning,
            isBluetoothEnabled = btEnabled,
            simulationMode = simMode,
            searchQuery = query,
            selectedCategory = categoryFilter,
            selectedSort = sortOpt,
            selectedDeviceForEdit = editDevice,
            selectedDeviceForRadar = radarDevice ?: filtered.firstOrNull(),
            userMessage = _userMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BluetoothUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: DeviceCategory?) {
        _selectedCategory.value = category
    }

    fun setSortOption(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun startScan() {
        repository.startScan()
        _userMessage.value = "Recherche d'appareils Bluetooth lancée..."
    }

    fun stopScan() {
        repository.stopScan()
        _userMessage.value = "Recherche arrêtée"
    }

    fun toggleSimulationMode(enabled: Boolean) {
        repository.toggleSimulationMode(enabled)
        _userMessage.value = if (enabled) "Mode simulation activé" else "Mode appareils réels activé"
    }

    fun toggleFavorite(device: BluetoothDeviceItem) {
        viewModelScope.launch {
            repository.toggleFavorite(device)
            val action = if (device.isFavorite) "retiré des favoris" else "ajouté aux favoris ⭐"
            _userMessage.value = "${device.displayName} $action"
        }
    }

    fun renameDevice(device: BluetoothDeviceItem, newName: String) {
        viewModelScope.launch {
            repository.renameDevice(device.macAddress, device.originalName, newName)
            _userMessage.value = "Nom mis à jour : $newName"
            _selectedDeviceForEdit.value = null
        }
    }

    fun updateDeviceDetails(
        device: BluetoothDeviceItem,
        customName: String?,
        notes: String,
        category: DeviceCategory,
        iconType: String
    ) {
        viewModelScope.launch {
            repository.updateDeviceDetails(
                mac = device.macAddress,
                originalName = device.originalName,
                customName = customName,
                notes = notes,
                category = category,
                iconType = iconType
            )
            _userMessage.value = "Modifications enregistrées pour ${device.displayName}"
            _selectedDeviceForEdit.value = null
        }
    }

    fun deleteSavedDevice(mac: String) {
        viewModelScope.launch {
            repository.deleteSavedDevice(mac)
            _userMessage.value = "Appareil supprimé des données sauvegardées"
        }
    }

    fun addSimulatedDevice(name: String, category: DeviceCategory) {
        repository.addSimulatedDevice(name, category)
        _userMessage.value = "Appareil virtuel '$name' ajouté au scanner"
    }

    fun openEditDialog(device: BluetoothDeviceItem) {
        _selectedDeviceForEdit.value = device
    }

    fun closeEditDialog() {
        _selectedDeviceForEdit.value = null
    }

    fun selectDeviceForRadar(device: BluetoothDeviceItem) {
        _selectedDeviceForRadar.value = device
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun exportFavoritesAsText(): String {
        val favs = uiState.value.favoriteDevices
        if (favs.isEmpty()) return "Aucun appareil favori à exporter."
        val sb = StringBuilder("=== Mes Appareils Bluetooth Favoris ===\n\n")
        favs.forEachIndexed { index, device ->
            sb.append("${index + 1}. ${device.displayName}\n")
            sb.append("   - Nom d'origine : ${device.originalName}\n")
            sb.append("   - Adresse MAC : ${device.macAddress}\n")
            sb.append("   - Catégorie : ${device.category.labelFr}\n")
            if (device.notes.isNotBlank()) {
                sb.append("   - Remarques : ${device.notes}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val scannerManager = BluetoothScannerManager(application)
            val repo = BluetoothRepository(db.bluetoothDao(), scannerManager)
            return BluetoothViewModel(application, repo) as T
        }
    }
}
