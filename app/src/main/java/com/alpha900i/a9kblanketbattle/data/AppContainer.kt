package com.alpha900i.a9kblanketbattle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.alpha900i.a9kblanketbattle.data.repository.DataStoreRepository

interface AppContainer {
    val dataStoreRepository: DataStoreRepository
}

class DefaultAppContainer(
    private val dataStore: DataStore<Preferences>
): AppContainer {
    override val dataStoreRepository: DataStoreRepository by lazy {
        DataStoreRepository(dataStore)
    }
}
