package com.alpha900i.a9kblanketbattle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.data.repository.DataStoreRepository
import com.alpha900i.a9kblanketbattle.ui.SettingsAction
import kotlinx.coroutines.flow.Flow

@Composable
fun SettingsScreen(
    settingsAction: SettingsAction
) {
    Column() {
        IntSetting(
            title = stringResource(R.string.width_setting_title),
            getter = settingsAction.getWidth(),
            defaultValue = DataStoreRepository.DEFAULT_WIDTH,
            setter = settingsAction::setWidth
        )
        IntSetting(
            title = stringResource(R.string.height_setting_title),
            getter = settingsAction.getHeight(),
            defaultValue = DataStoreRepository.DEFAULT_HEIGHT,
            setter = settingsAction::setHeight
        )
        IntSetting(
            title = stringResource(R.string.start_kitten_setting_title),
            getter = settingsAction.getKittenStart(),
            defaultValue = DataStoreRepository.DEFAULT_KITTEN_START,
            setter = settingsAction::setKittenStart
        )
        IntSetting(
            title = stringResource(R.string.start_cat_setting_title),
            getter = settingsAction.getCatStart(),
            defaultValue = DataStoreRepository.DEFAULT_CAT_START,
            setter = settingsAction::setCatStart
        )
    }
}

@Composable
fun IntSetting(
    title: String,
    getter: Flow<Int>,
    defaultValue: Int,
    setter: (Int) -> Unit
) {
    val value by getter.collectAsStateWithLifecycle(defaultValue)
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
            value = value.toString(),
            onValueChange = {
                try {
                    setter(Integer.parseInt(it).coerceAtLeast(defaultValue))
                } catch (_: Exception) {
                    setter(defaultValue)
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