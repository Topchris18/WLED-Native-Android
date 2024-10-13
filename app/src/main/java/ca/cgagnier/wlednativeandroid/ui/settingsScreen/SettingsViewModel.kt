package ca.cgagnier.wlednativeandroid.ui.settingsScreen

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.cgagnier.wlednativeandroid.repository.ThemeSettings
import ca.cgagnier.wlednativeandroid.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val theme = preferencesRepository.themeMode
    private val autoDiscovery = preferencesRepository.autoDiscovery
    private val showOfflineDevicesLast = preferencesRepository.showOfflineDevicesLast

    val settingsState = combine(
        autoDiscovery,
        showOfflineDevicesLast,
        theme,
    ) { autoDiscovery, showOfflineDevicesLast, theme ->
        SettingsState(
            isAutoDiscoveryEnabled = autoDiscovery,
            showOfflineLast = showOfflineDevicesLast,
            theme = theme,
        )
    }.stateIn(viewModelScope, WhileSubscribed(5000), SettingsState())

    fun setAutoDiscover(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        preferencesRepository.updateAutoDiscovery(enabled)
    }
    fun setShowOfflineDevicesLast(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        preferencesRepository.updateShowOfflineDeviceLast(enabled)
    }
    fun setTheme(theme: ThemeSettings) = viewModelScope.launch(Dispatchers.IO) {
        preferencesRepository.updateThemeMode(theme)
    }
}


@Stable
data class SettingsState(
    val isAutoDiscoveryEnabled : Boolean = true,
    val showOfflineLast : Boolean = true,
    val theme: ThemeSettings = ThemeSettings.UNRECOGNIZED,
)
