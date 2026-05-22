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
        Board(
            width = 6,
            height = 6
        )
    )
    val boardState: StateFlow<Board> = _boardState.asStateFlow();

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel()
            }
        }
    }
}