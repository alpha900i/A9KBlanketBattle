package com.alpha900i.a9kblanketbattle.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.alpha900i.a9kblanketbattle.R
import com.alpha900i.a9kblanketbattle.ui.StartScreenActions

@Composable
fun StartScreen(
    actions: StartScreenActions
) {
    val BUTTON_WIDTH_FACTOR : Float = 0.8f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(1f)
    ) {
        Image(
            painter = painterResource(R.drawable.start_screen_icon),
            contentDescription = stringResource(R.string.start_screen_icon_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(3f)
                .fillMaxWidth(1f)
        )
        Button(
            onClick = {
                actions.startGame()
            },
            modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FACTOR)
                .weight(1f)
        ) {
            Text(stringResource(R.string.start_game_button_title))
        }
        Spacer(modifier =
            Modifier.fillMaxWidth(1f)
                .height(dimensionResource(R.dimen.start_screen_spacer_height))
        )
        Button(
            onClick = {
                actions.openSettings()
            },
            modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FACTOR)
                .weight(1f)
        ) {
            Text(stringResource(R.string.settings_button_title))
        }
        Spacer(modifier =
            Modifier.fillMaxWidth(1f)
                .height(dimensionResource(R.dimen.start_screen_spacer_height))
        )
        Button(
            onClick = {
                actions.exitGame()
            },
            modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FACTOR)
                .weight(1f)
        ) {
            Text(stringResource(R.string.exit_game_button_title))
        }
    }
}