package ca.cgagnier.wlednativeandroid.ui.homeScreen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.cgagnier.wlednativeandroid.R
import ca.cgagnier.wlednativeandroid.model.Device
import ca.cgagnier.wlednativeandroid.ui.homeScreen.detail.DeviceDetail
import ca.cgagnier.wlednativeandroid.ui.homeScreen.deviceEdit.DeviceEdit
import ca.cgagnier.wlednativeandroid.ui.homeScreen.list.DeviceList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DeviceListDetail(
    modifier: Modifier = Modifier,
    viewModel: DeviceListDetailViewModel = hiltViewModel(),
) {
    var firstLoad by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val defaultScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val customScaffoldDirective = defaultScaffoldDirective.copy(
        horizontalPartitionSpacerSize = 0.dp,
    )
    val navigator =
        rememberListDetailPaneScaffoldNavigator<Any>(scaffoldDirective = customScaffoldDirective)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val selectedDeviceAddress = navigator.currentDestination?.content as? String ?: ""
    val selectedDevice =
        viewModel.getDeviceByAddress(selectedDeviceAddress).collectAsStateWithLifecycle(null)

    LaunchedEffect("onStart-startDiscovery") {
        if (firstLoad) {
            firstLoad = false
            viewModel.startDiscoveryServiceTimed(2000)
            viewModel.startRefreshDevicesLoop()
        }
    }

    val navigateToDeviceDetail = { device: Device ->
        navigator.navigateTo(
            pane = ListDetailPaneScaffoldRole.Detail,
            content = device.address
        )
    }
    val navigateToDeviceEdit = { device: Device ->
        navigator.navigateTo(
            pane = ListDetailPaneScaffoldRole.Extra,
            content = device.address
        )
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    openSettings = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            NavigableListDetailPaneScaffold(
                modifier = modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                navigator = navigator,
                defaultBackBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
                listPane = {
                    AnimatedPane {
                        DeviceList(
                            selectedDevice.value,
                            isDiscovering = viewModel.isDiscovering,
                            onItemClick = navigateToDeviceDetail,
                            onRefresh = {
                                viewModel.refreshDevices(silent = false)
                                viewModel.startDiscoveryServiceTimed()
                            },
                            onItemEdit = {
                                navigateToDeviceDetail(it)
                                navigateToDeviceEdit(it)
                            },
                            openDrawer = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                }, detailPane = {
                    AnimatedPane {
                        // selectedDevice est static, pas dynamic de la database... Utiliser ID à la place
                        selectedDevice.value?.let { device ->
                            DeviceDetail(
                                device = device,
                                onItemEdit = {
                                    navigateToDeviceEdit(device)
                                },
                                canNavigateBack = navigator.canNavigateBack(),
                                navigateUp = {
                                    navigator.navigateBack()
                                }
                            )
                        }
                    }
                }, extraPane = {
                    AnimatedPane {
                        selectedDevice.value?.let { device ->
                            DeviceEdit(
                                device = device,
                                canNavigateBack = navigator.canNavigateBack(),
                                navigateUp = {
                                    navigator.navigateBack()
                                }
                            )
                        }
                    }
                }
            )

        }
    }
}

@Composable
private fun DrawerContent(
    openSettings: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        horizontalArrangement = Arrangement.Center) {
        Image(
            painter = painterResource(id = R.drawable.wled_logo_akemi),
            contentDescription = stringResource(R.string.app_logo)
        )
    }
    NavigationDrawerItem(
        label = { Text(text = stringResource(R.string.settings)) },
        icon = {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings)
            )
        },
        selected = false,
        onClick = {
            Toast.makeText(context, "Coming soon...", Toast.LENGTH_SHORT).show()
            openSettings()
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    HorizontalDivider(modifier = Modifier.padding(12.dp))

    NavigationDrawerItem(
        label = { Text(text = stringResource(R.string.help)) },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.baseline_help_24),
                contentDescription = stringResource(R.string.help)
            )
        },
        selected = false,
        onClick = {
            uriHandler.openUri("https://kno.wled.ge/")
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        label = { Text(text = stringResource(R.string.support_me)) },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.baseline_coffee_24),
                contentDescription = stringResource(R.string.support_me)
            )
        },
        selected = false,
        onClick = {
            uriHandler.openUri("https://github.com/sponsors/Moustachauve")
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    // ...other drawer items
}