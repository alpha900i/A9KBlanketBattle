package com.alpha900i.a9kblanketbattle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alpha900i.a9kblanketbattle.domain.PlayerType
import com.alpha900i.a9kblanketbattle.ui.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

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

    val width: Flow<Int> =
        getIntValue(
            key = WIDTH_KEY,
            defaultValue = Constants.DEFAULT_WIDTH
        )
    suspend fun setWidth(width: Int) =
        setIntValue(
            value = width,
            key = WIDTH_KEY
        )

    val height: Flow<Int> =
        getIntValue(
            key = HEIGHT_KEY,
            defaultValue = Constants.DEFAULT_HEIGHT
        )
    suspend fun setHeight(height: Int) =
        setIntValue(
            value = height,
            key = HEIGHT_KEY
        )

    val kittenStart: Flow<Int> =
        getIntValue(
            key = KITTEN_START_KEY,
            defaultValue = Constants.DEFAULT_KITTEN_START
        )
    suspend fun setKittenStart(kittenStart: Int) =
        setIntValue(
            value = kittenStart,
            key = KITTEN_START_KEY
        )

    val catStart: Flow<Int> =
        getIntValue(
            key = CAT_START_KEY,
            defaultValue = Constants.DEFAULT_CAT_START
        )
    suspend fun setCatStart(catStart: Int) =
        setIntValue(
            value = catStart,
            key = CAT_START_KEY
        )

    val firstPlayerType: Flow<PlayerType> =
        getStringValue(
            key = FIRST_PLAYER_TYPE_KEY,
            defaultValue = Constants.DEFAULT_FIRST_PLAYER_TYPE.name
        ).map { PlayerType.getByName(it) }
    suspend fun setFirstPlayerType(playerType: PlayerType) =
        setStringValue(
            value = playerType.name,
            key = FIRST_PLAYER_TYPE_KEY
        )

    val secondPlayerType: Flow<PlayerType> =
        getStringValue(
            key = SECOND_PLAYER_TYPE_KEY,
            defaultValue = Constants.DEFAULT_SECOND_PLAYER_TYPE.name
        ).map { PlayerType.getByName(it) }
    suspend fun setSecondPlayerType(playerType: PlayerType) =
        setStringValue(
            value = playerType.name,
            key = SECOND_PLAYER_TYPE_KEY
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