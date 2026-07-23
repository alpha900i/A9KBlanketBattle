package com.alpha900i.a9kblanketbattle

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.alpha900i.a9kblanketbattle.data.AppContainer
import com.alpha900i.a9kblanketbattle.data.DefaultAppContainer


private const val DATA_STORE_NAME = "main_data_store"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME
)
class MainApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(
            dataStore
        )
    }
}