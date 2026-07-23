package com.alpha900i.a9kblanketbattle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val DEFAULT_WIDTH = 6
        const val DEFAULT_HEIGHT = 6
        const val DEFAULT_CAT_START = 0
        const val DEFAULT_KITTEN_START = 8

        private val WIDTH_KEY = intPreferencesKey("width")
        private val HEIGHT_KEY = intPreferencesKey("height")
        private val KITTEN_START_KEY = intPreferencesKey("kitten_start")
        private val CAT_START_KEY = intPreferencesKey("cat_start")
    }

    val width: Flow<Int> =
        getIntValue(
            key = WIDTH_KEY,
            defaultValue = DEFAULT_WIDTH
        )
    suspend fun setWidth(width: Int) =
        setIntValue(
            value = width,
            key = WIDTH_KEY
        )

    val height: Flow<Int> =
        getIntValue(
            key = HEIGHT_KEY,
            defaultValue = DEFAULT_HEIGHT
        )
    suspend fun setHeight(height: Int) =
        setIntValue(
            value = height,
            key = HEIGHT_KEY
        )

    val kittenStart: Flow<Int> =
        getIntValue(
            key = KITTEN_START_KEY,
            defaultValue = DEFAULT_KITTEN_START
        )
    suspend fun setKittenStart(kittenStart: Int) =
        setIntValue(
            value = kittenStart,
            key = KITTEN_START_KEY
        )

    val catStart: Flow<Int> =
        getIntValue(
            key = CAT_START_KEY,
            defaultValue = DEFAULT_CAT_START
        )
    suspend fun setCatStart(catStart: Int) =
        setIntValue(
            value = catStart,
            key = CAT_START_KEY
        )


    fun getIntValue(key: Preferences.Key<Int>, defaultValue: Int): Flow<Int> {
        return dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                val value = preferences[key] ?: defaultValue
                value
            }
    }
    suspend fun setIntValue(value: Int, key: Preferences.Key<Int>) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}