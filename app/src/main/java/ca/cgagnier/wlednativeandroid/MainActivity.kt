package ca.cgagnier.wlednativeandroid

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import ca.cgagnier.wlednativeandroid.databinding.ActivityMainBinding
import ca.cgagnier.wlednativeandroid.model.Device
import ca.cgagnier.wlednativeandroid.repository.ThemeSettings
import ca.cgagnier.wlednativeandroid.repository_v0.DataMigrationV0toV1
import ca.cgagnier.wlednativeandroid.service.DeviceApi
import ca.cgagnier.wlednativeandroid.service.DeviceDiscovery
import ca.cgagnier.wlednativeandroid.viewmodel.DeviceListViewModel
import ca.cgagnier.wlednativeandroid.viewmodel.DeviceListViewModelFactory
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import kotlinx.coroutines.launch


class MainActivity : AutoDiscoveryActivity, DeviceDiscovery.DeviceDiscoveredListener,
    AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val autoDiscoveryLoopHandler = Handler(Looper.getMainLooper())
    private val deviceListViewModel: DeviceListViewModel by viewModels {
        DeviceListViewModelFactory(
            (application as DevicesApplication).repository,
            (application as DevicesApplication).userPreferencesRepository)
    }

    private var isAutoDiscoveryEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val devicesApp = (application as DevicesApplication)
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.themeMode.collect {
                setThemeMode(it)
            }
        }
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.autoDiscovery.collect {
                isAutoDiscoveryEnabled = it
                if (isAutoDiscoveryEnabled) {
                    startAutoDiscovery()
                } else {
                    stopAutoDiscovery()
                }
            }
        }
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.sendCrashData.collect {
                Log.i(TAG, "Setting crashData to $it")
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(it)
            }
        }
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.sendCrashData.collect {
                Log.i(TAG, "Setting performance data to $it")
                Firebase.performance.isPerformanceCollectionEnabled = it
            }
        }

        super.onCreate(savedInstanceState)
        DeviceApi.setApplication(application as DevicesApplication)
        binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainerView) { insetView, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            insetView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            windowInsets
        }

        checkMigration()

        var isConnectedToWledAP: Boolean
        try {
            isConnectedToWledAP = DeviceDiscovery.isConnectedToWledAP(applicationContext)
        } catch (e: Exception) {
            isConnectedToWledAP = false
            Log.e(TAG, "Error when checking isConnectedToWledAP: " + e.message, e)
        }

        if (isConnectedToWledAP) {
            val connectionManager =
                applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?

            val request = NetworkRequest.Builder()
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

            connectionManager!!.requestNetwork(
                request.build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        try {
                            connectionManager.bindProcessToNetwork(network)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
        }

        setContentView(binding.root)
    }

    override fun onResume() {
        startAutoDiscovery()
        super.onResume()
    }

    override fun onPause() {
        stopAutoDiscovery()
        super.onPause()
    }

    override fun startAutoDiscovery() {
        if (!isAutoDiscoveryEnabled) {
            Log.i(TAG, "Auto discovery is not enabled")
            return
        }
        Log.i(TAG, "Starting auto discovery")
        (application as DevicesApplication).deviceDiscovery
            .registerDeviceDiscoveredListener(this)
        (application as DevicesApplication).deviceDiscovery.start()
        autoDiscoveryLoopHandler.postDelayed({stopAutoDiscovery()}, 10000)
    }

    override fun stopAutoDiscovery() {
        Log.i(TAG, "Stopping auto discovery")
        autoDiscoveryLoopHandler.removeCallbacksAndMessages(null)
        (application as DevicesApplication).deviceDiscovery
            .unregisterDeviceDiscoveredListener(this)
        (application as DevicesApplication).deviceDiscovery.stop()
    }

    private fun checkMigration() {
        lifecycleScope.launch {
            val devicesApp = (application as DevicesApplication)
            val userPreferences = devicesApp.userPreferencesRepository.fetchInitialPreferences()
            if (!userPreferences.hasMigratedSharedPref) {
                Log.i(TAG, "Starting devices migration from V0 to V1")
                DataMigrationV0toV1(applicationContext, devicesApp.repository).migrate()
                devicesApp.userPreferencesRepository.updateHasMigratedSharedPref(true)
                Log.i(TAG, "Migration done.")
            }
        }
    }

    override fun onDeviceDiscovered(serviceInfo: NsdServiceInfo) {
        Log.i(TAG, "Device discovered!")
        val deviceIp = serviceInfo.host.hostAddress!!
        val deviceName = serviceInfo.serviceName ?: ""
        val device = Device(deviceIp, deviceName,
            isCustomName = false,
            isHidden = false,
            macAddress = ""
        )

        if (deviceListViewModel.contains(device)) {
            Log.i(TAG, "Device already exists")
            return
        }
        Log.i(TAG, "IP: ${deviceIp}\tName: ${deviceName}\t")

        DeviceApi.update(device, true) {
            lifecycleScope.launch {
                val existingDevice = deviceListViewModel.findWithSameMacAddress(it)
                if (existingDevice != null) {
                    Log.i(TAG, "Device ${existingDevice.address} already exists with the same mac address ${existingDevice.macAddress}")
                    deviceListViewModel.delete(existingDevice)
                }
                deviceListViewModel.insert(device)
            }
        }
    }

    private fun setThemeMode(theme: ThemeSettings){
        val mode = when(theme){
            ThemeSettings.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeSettings.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }
}