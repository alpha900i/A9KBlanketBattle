@file:OptIn(ExperimentalMaterial3Api::class)

package com.alpha900i.a9kblanketbattle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.datastore.Settings
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.SettingsActions
import com.alpha900i.a9kblanketbattle.ui.SettingsResult

@Composable
fun SettingsScreen(
    settings: Settings,
    actions: SettingsActions
) {
    Column {
        IntSetting(
            title = stringResource(R.string.width_setting_title),
            intValue = settings.width,
            settings = settings,
            setter = actions::setWidth
        )
        IntSetting(
            title = stringResource(R.string.height_setting_title),
            intValue = settings.height,
            settings = settings,
            setter = actions::setHeight
        )
        IntSetting(
            title = stringResource(R.string.start_kitten_setting_title),
            intValue = settings.kittenStart,
            settings = settings,
            setter = actions::setKittenStart
        )
        IntSetting(
            title = stringResource(R.string.start_cat_setting_title),
            intValue = settings.catStart,
            settings = settings,
            setter = actions::setCatStart
        )
        PlayerTypeSetting(
            title = stringResource(R.string.player_1_setting_title),
            value = settings.firstPlayerType,
            setter = actions::setFirstPlayerType,
        )
        PlayerTypeSetting(
            title = stringResource(R.string.player_2_setting_title),
            value = settings.secondPlayerType,
            setter = actions::setSecondPlayerType,
        )
        Button(
            onClick = actions::restoreDefaults,
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            Text(
                text = stringResource(R.string.restore_defaults_button_title)
            )
        }
    }
}

@Composable
fun IntSetting(
    title: String,
    intValue: Int,
    settings: Settings,
    setter: (Int) -> SettingsResult
) {
    var textValue by remember(intValue) {mutableStateOf("$intValue")}
    var error by remember { mutableStateOf<SettingsResult.Failure?>(null) }

    LaunchedEffect(settings) {
        val parsed = textValue.toIntOrNull()
        if (parsed == null || parsed == intValue) {
            error = null
            return@LaunchedEffect
        }
        error = when (val r = setter(parsed)) {
            is SettingsResult.Success -> null
            is SettingsResult.Failure -> r
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(1f)
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                val parsed = newText.toIntOrNull()
                if (parsed != null) {
                    val result: SettingsResult = setter(parsed)
                    error = result as? SettingsResult.Failure
                } else {
                    error = null
                    return@OutlinedTextField
                }
            },
            label = { Text(title) },
            isError = error != null,
            supportingText = error?.let { failure ->
                {
                    Text(
                        text = stringResource(
                            failure.messageRes,
                            *failure.formatArgs.toTypedArray(),
                        ),
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PlayerTypeSetting(
    title: String,
    value: PlayerType,
    setter: (PlayerType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(1f)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(4f)
        ) {
            TextField(
                value = value.title,
                onValueChange = {},
                readOnly = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PlayerType.getAllValues().forEach { playerType ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = playerType.title,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(1f)
                            )
                        },
                        onClick = {
                            setter(playerType)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}