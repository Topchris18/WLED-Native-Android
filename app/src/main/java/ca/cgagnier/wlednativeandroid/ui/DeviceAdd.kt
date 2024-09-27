package ca.cgagnier.wlednativeandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ca.cgagnier.wlednativeandroid.R


@Composable
fun DeviceAdd(
    modifier: Modifier = Modifier,
    deviceAdded: () -> Unit,
    viewModel: DeviceAddViewModel = hiltViewModel(),
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .height(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
        ) {
            // TODO: set focus on open
            val focusRequester = remember {
                FocusRequester()
            }
            val focusManager = LocalFocusManager.current

            LaunchedEffect("initialFocus") {
                focusRequester.requestFocus()
            }
            Text(
                text = stringResource(R.string.add_a_device),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = viewModel.address,
                onValueChange = {
                    viewModel.address = it
                },
                label = { Text(stringResource(R.string.ip_address_or_url)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(Modifier.padding(4.dp))
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = {
                    viewModel.name = it
                },
                label = { Text(stringResource(R.string.custom_name)) },
                supportingText = { Text(stringResource(R.string.leave_this_empty_to_use_the_device_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.createDevice()
                        deviceAdded()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(
                Modifier
                    .height(24.dp)
                    .weight(1f)
            )
            Button(
                onClick = {
                    viewModel.createDevice()
                    deviceAdded()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.description_back_button)
                )
                Text(stringResource(R.string.add))
            }
        }
    }
}