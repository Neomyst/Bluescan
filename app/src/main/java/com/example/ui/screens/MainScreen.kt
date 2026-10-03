package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddVirtualDeviceDialog
import com.example.ui.components.RenameEditDialog
import com.example.ui.theme.AccentGold
import com.example.ui.viewmodel.BluetoothUiState
import com.example.ui.viewmodel.BluetoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BluetoothViewModel,
    uiState: BluetoothUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scanner, 1: Favoris, 2: Radar, 3: Analytics
    var showAddVirtualDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        } else {
            Toast.makeText(
                context,
                "Permissions Bluetooth nécessaires pour scanner les appareils réels.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Handle user toast/snackbar messages
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Bluetooth Manager",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (uiState.favoriteDevices.isNotEmpty()) {
                        BadgedBox(
                            badge = { Badge { Text(uiState.favoriteDevices.size.toString()) } },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Mes Favoris",
                                tint = AccentGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            contentDescription = null
                        )
                    },
                    label = { Text("Scanner") },
                    modifier = Modifier.testTag("tab_scanner")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.favoriteDevices.isNotEmpty()) {
                                    Badge { Text(uiState.favoriteDevices.size.toString()) }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null)
                        }
                    },
                    label = { Text("Favoris") },
                    modifier = Modifier.testTag("tab_favorites")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Radar, contentDescription = null) },
                    label = { Text("Radar") },
                    modifier = Modifier.testTag("tab_radar")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Stats") },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ScannerScreen(
                    uiState = uiState,
                    onStartScan = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    },
                    onStopScan = { viewModel.stopScan() },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                    onSortOptionSelect = { viewModel.setSortOption(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onEditDevice = { viewModel.openEditDialog(it) },
                    onSelectForRadar = {
                        viewModel.selectDeviceForRadar(it)
                        selectedTab = 2 // Switch to radar screen
                    },
                    onToggleSimulation = { viewModel.toggleSimulationMode(it) },
                    onOpenAddVirtualDialog = { showAddVirtualDialog = true }
                )

                1 -> FavoritesScreen(
                    uiState = uiState,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onEditDevice = { viewModel.openEditDialog(it) },
                    onSelectForRadar = {
                        viewModel.selectDeviceForRadar(it)
                        selectedTab = 2
                    },
                    onExportFavorites = { viewModel.exportFavoritesAsText() }
                )

                2 -> SignalRadarScreen(
                    uiState = uiState,
                    onSelectDevice = { viewModel.selectDeviceForRadar(it) },
                    onEditDevice = { viewModel.openEditDialog(it) }
                )

                3 -> AnalyticsScreen(
                    uiState = uiState,
                    onExportFavorites = { viewModel.exportFavoritesAsText() }
                )
            }
        }
    }

    // Modal Edit Device Dialog
    uiState.selectedDeviceForEdit?.let { device ->
        RenameEditDialog(
            device = device,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { name, notes, category, iconType ->
                viewModel.updateDeviceDetails(device, name, notes, category, iconType)
            },
            onDelete = {
                viewModel.deleteSavedDevice(device.macAddress)
                viewModel.closeEditDialog()
            }
        )
    }

    // Modal Add Virtual Device Dialog
    if (showAddVirtualDialog) {
        AddVirtualDeviceDialog(
            onDismiss = { showAddVirtualDialog = false },
            onAdd = { name, category ->
                viewModel.addSimulatedDevice(name, category)
                showAddVirtualDialog = false
            }
        )
    }
}
