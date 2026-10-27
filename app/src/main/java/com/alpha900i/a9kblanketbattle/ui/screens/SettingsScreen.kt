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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.datastore.Settings
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants
import com.alpha900i.a9kblanketbattle.ui.SettingsActions

@Composable
fun SettingsScreen(
    settings: Settings,
    actions: SettingsActions
) {
    Column {
        IntSetting(
            title = stringResource(R.string.width_setting_title),
            intValue = settings.width,
            minValue = Constants.MIN_WIDTH,
            setter = actions::setWidth
        )
        IntSetting(
            title = stringResource(R.string.height_setting_title),
            intValue = settings.height,
            minValue = Constants.MIN_HEIGHT,
            setter = actions::setHeight
        )
        IntSetting(
            title = stringResource(R.string.start_kitten_setting_title),
            intValue = settings.kittenStart,
            minValue = Constants.MIN_KITTEN_START,
            setter = actions::setKittenStart
        )
        IntSetting(
            title = stringResource(R.string.start_cat_setting_title),
            intValue = settings.catStart,
            minValue = Constants.MIN_CAT_START,
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
    minValue: Int,
    setter: (Int) -> Boolean
) {
    var textValue by remember(intValue) {mutableStateOf("$intValue")}
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(1f)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(4f)
        )
        TextField(
            value = textValue,
            onValueChange = {
                textValue = it
                try {
                    val fieldValue = Integer.parseInt(textValue)
                    if (fieldValue >= minValue) {
                        if (!setter(fieldValue)) {
                            textValue = "$intValue"
                        }
                    } else {
                        textValue = "$minValue"
                    }
                } catch (_: Exception) {
                    //nothing. If value is bad, we just ignore it
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
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