package com.alpha900i.a9kblanketbattle.ui.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.ui.StartScreenActions

@Composable
fun StartScreen(
    actions: StartScreenActions
) {
    Column() {
        Button(
            onClick = {
                actions.startGame()
            },
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            Text(stringResource(R.string.start_game_button_title))
        }
        Button(
            onClick = {
                actions.openSettings()
            },
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            Text(stringResource(R.string.settings_button_title))
        }
        Button(
            onClick = {
                actions.exitGame()
            },
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            Text(stringResource(R.string.exit_game_button_title))
        }
    }
}