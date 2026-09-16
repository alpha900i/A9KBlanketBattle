@file:OptIn(ExperimentalMaterial3Api::class)

package com.alpha900i.a9kblanketbattle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuAnchorType
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants
import com.alpha900i.a9kblanketbattle.ui.SettingsActions
import kotlinx.coroutines.flow.Flow

@Composable
fun SettingsScreen(
    actions: SettingsActions
) {
    Column() {
        IntSetting(
            title = stringResource(R.string.width_setting_title),
            getter = actions.getWidth(),
            defaultValue = Constants.DEFAULT_WIDTH,
            minValue = Constants.MIN_WIDTH,
            setter = actions::setWidth
        )
        IntSetting(
            title = stringResource(R.string.height_setting_title),
            getter = actions.getHeight(),
            defaultValue = Constants.DEFAULT_HEIGHT,
            minValue = Constants.MIN_HEIGHT,
            setter = actions::setHeight
        )
        IntSetting(
            title = stringResource(R.string.start_kitten_setting_title),
            getter = actions.getKittenStart(),
            defaultValue = Constants.DEFAULT_KITTEN_START,
            minValue = Constants.MIN_KITTEN_START,
            setter = actions::setKittenStart
        )
        IntSetting(
            title = stringResource(R.string.start_cat_setting_title),
            getter = actions.getCatStart(),
            defaultValue = Constants.DEFAULT_CAT_START,
            minValue = Constants.MIN_CAT_START,
            setter = actions::setCatStart
        )
        PlayerTypeSetting(
            title = stringResource(R.string.player_1_setting_title),
            getter = actions.getFirstPlayerType(),
            defaultValue = Constants.DEFAULT_FIRST_PLAYER_TYPE,
            setter = actions::setFirstPlayerType,
        )
        PlayerTypeSetting(
            title = stringResource(R.string.player_2_setting_title),
            getter = actions.getSecondPlayerType(),
            defaultValue = Constants.DEFAULT_SECOND_PLAYER_TYPE,
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
    getter: Flow<Int>,
    defaultValue: Int,
    minValue: Int,
    setter: (Int) -> Unit
) {
    val intValue by getter.collectAsStateWithLifecycle(defaultValue)
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
                        setter(fieldValue)
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
    getter: Flow<PlayerType>,
    defaultValue: PlayerType,
    setter: (PlayerType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val value by getter.collectAsStateWithLifecycle(defaultValue)
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
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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