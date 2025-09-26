package com.example.wifidirecthotspot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Panel
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.wifidirecthotspot.ui.theme.WIFIDirectHotspotTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Define DataStore at the file level
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val WIFI_PASSWORD_KEY = stringPreferencesKey("password")
private const val DEFAULT_PASSWORD = "123456789"

class MainActivity : ComponentActivity() {

    private val wifiP2pManager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    private lateinit var channel: WifiP2pManager.Channel

    // Required permissions based on Android version
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize channel
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        setContent {
            WIFIDirectHotspotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WifiDirectScreen(
                        createGroup = { password -> createWifiDirectGroup(password) },
                        removeGroup = { removeWifiDirectGroup() }
                    )
                }
            }
        }
    }

    @Composable
    fun WifiDirectScreen(
        createGroup: (String) -> Unit,
        removeGroup: () -> Unit
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current

        // State for password
        var wifiPassword by remember { mutableStateOf("") }

        // Load saved password when the composable is first created
        LaunchedEffect(key1 = true) {
            wifiPassword = readPassword(context) ?: DEFAULT_PASSWORD
        }

        // Permission request launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                createGroup(wifiPassword)
            } else {
                Toast.makeText(context, "Permissions required to create WiFi Direct", Toast.LENGTH_LONG).show()
            }
        }

        // Request permissions when needed
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Check if we already have permissions
                    val allGranted = requiredPermissions.all { permission ->
                        ActivityCompat.checkSelfPermission(context, permission) ==
                                PackageManager.PERMISSION_GRANTED
                    }

                    if (!allGranted) {
                        // Show rationale if needed
                        val shouldShowRationale = requiredPermissions.any { permission ->
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                this@MainActivity, permission
                            )
                        }

                        if (shouldShowRationale) {
                            Toast.makeText(
                                context,
                                "Location permission is needed to create WiFi Direct connections",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DIRECT-WIFI-Hotspot",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TextField(
                value = wifiPassword,
                onValueChange = { newValue ->
                    wifiPassword = newValue
                    coroutineScope.launch {
                        storePassword(context, newValue)
                    }
                },
                label = { Text("WIFI Direct Password (Length - between 8 and 63)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (wifiPassword.length in 8..63) {
                        // Request permissions if needed, then create the group
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        Toast.makeText(
                            context,
                            "Password must be between 8 and 63 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Create WIFI Direct")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { removeGroup() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Remove WIFI Direct")
            }
        }
    }

    private fun createWifiDirectGroup(password: String) {
        if (password.length !in 8..63) {
            Toast.makeText(
                this,
                "Password must be between 8 and 63 characters",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val config = WifiP2pConfig.Builder().apply {
            setNetworkName("DIRECT-WIFI-Hotspot")
            setPassphrase(password)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "Required permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        wifiP2pManager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Successfully Created", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                val errorMsg = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "WiFi Direct is not supported on this device"
                    WifiP2pManager.BUSY -> "System is busy, try again later"
                    WifiP2pManager.ERROR -> "Internal error occurred"
                    else -> "Failed to create group (error code: $reason)"
                }

                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()

                // Open WiFi settings if needed
                Intent(Panel.ACTION_WIFI).also { startActivity(it) }
            }
        })
    }

    private fun removeWifiDirectGroup() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Successfully Removed", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                val errorMsg = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "WiFi Direct is not supported on this device"
                    WifiP2pManager.BUSY -> "System is busy, try again later"
                    WifiP2pManager.ERROR -> "Internal error occurred"
                    else -> "Failed to remove group (error code: $reason)"
                }

                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // DataStore utility functions
    private suspend fun readPassword(context: Context): String? {
        val preferences = context.dataStore.data.first()
        return preferences[WIFI_PASSWORD_KEY] ?: DEFAULT_PASSWORD
    }

    private suspend fun storePassword(context: Context, password: String) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_PASSWORD_KEY] = password
        }
    }
}