package com.alpha900i.a9kblanketbattle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alpha900i.a9kblanketbattle.data.datastore.Settings
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore access
 * Keeps game's settings - width, height, start amount of cats and kittens, player types
 */
class DataStoreRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val WIDTH_KEY = intPreferencesKey("width")
        private val HEIGHT_KEY = intPreferencesKey("height")
        private val KITTEN_START_KEY = intPreferencesKey("kitten_start")
        private val CAT_START_KEY = intPreferencesKey("cat_start")
        private val FIRST_PLAYER_TYPE_KEY = stringPreferencesKey("first_player_type")
        private val SECOND_PLAYER_TYPE_KEY = stringPreferencesKey("second_player_type")
    }


    /**
     * Getter and setter for whole Settings object
     */
    val settings: Flow<Settings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            Settings(
                width = prefs[WIDTH_KEY] ?: Constants.DEFAULT_WIDTH,
                height = prefs[HEIGHT_KEY] ?: Constants.DEFAULT_HEIGHT,
                kittenStart = prefs[KITTEN_START_KEY] ?: Constants.DEFAULT_KITTEN_START,
                catStart = prefs[CAT_START_KEY] ?: Constants.DEFAULT_CAT_START,
                firstPlayerType = PlayerType.getByName(
                    prefs[FIRST_PLAYER_TYPE_KEY] ?: Constants.DEFAULT_FIRST_PLAYER_TYPE.name
                ),
                secondPlayerType = PlayerType.getByName(
                    prefs[SECOND_PLAYER_TYPE_KEY] ?: Constants.DEFAULT_SECOND_PLAYER_TYPE.name
                ),
            )
        }

    suspend fun setSettings(settings: Settings) {
        dataStore.edit { prefs ->
            prefs[WIDTH_KEY] = settings.width
            prefs[HEIGHT_KEY] = settings.height
            prefs[KITTEN_START_KEY] = settings.kittenStart
            prefs[CAT_START_KEY] = settings.catStart
            prefs[FIRST_PLAYER_TYPE_KEY] = settings.firstPlayerType.name
            prefs[SECOND_PLAYER_TYPE_KEY] = settings.secondPlayerType.name
        }
    }

    /**
     * Set of helper methods, getting/setting values of particular type into datastore
     * Unused as of now, because we get data in whole pack
     */
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

    fun getStringValue(key: Preferences.Key<String>, defaultValue: String): Flow<String> {
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
    suspend fun setStringValue(value: String, key: Preferences.Key<String>) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}