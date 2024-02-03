package com.abounegm.sup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BalanceStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cardInfo")
        private val CARD_NUMBER_KEY = stringPreferencesKey("card_number")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
    }

    val getCardNumber: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CARD_NUMBER_KEY] ?: ""
    }

    suspend fun saveCardNumber(token: String) {
        context.dataStore.edit { preferences ->
            preferences[CARD_NUMBER_KEY] = token
        }
    }
}