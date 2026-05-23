package com.alpha900i.a9kblanketbattle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpha900i.a9kblanketbattle.data.Board
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class AppViewModel() : ViewModel() {
    private val _boardState = MutableStateFlow(
        Board.emptyBoard(BOARD_DEFAULT_WIDTH, BOARD_DEFAULT_HEIGHT)
    )
    val boardState: StateFlow<Board> = _boardState.asStateFlow();

    companion object {
        const val BOARD_DEFAULT_WIDTH = 6
        const val BOARD_DEFAULT_HEIGHT = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel()
            }
        }
    }
}