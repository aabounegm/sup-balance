package com.abounegm.sup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar

class BalanceStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cardInfo")
        private val CARD_NUMBER_KEY = stringPreferencesKey("card_number")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
        private val BALANCE_KEY = floatPreferencesKey("total_balance")
        private val REMAINING_KEY = floatPreferencesKey("remaining_limit")
    }

    val getCardNumber: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CARD_NUMBER_KEY] ?: ""
    }

    val getLastUpdated: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_UPDATED_KEY] ?: SimpleDateFormat.getTimeInstance()
            .format(Calendar.getInstance().time)
    }

    val getBalance: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[BALANCE_KEY] ?: 0f
    }

    val getRemaining: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[REMAINING_KEY] ?: 0f
    }

    suspend fun saveCardNumber(cardNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[CARD_NUMBER_KEY] = cardNumber
        }
    }

    suspend fun updateValues() {
        val cardNumber = getCardNumber.first()
        withContext(Dispatchers.IO) {
            try {
                val limit = fetchLimits(cardNumber)
                val balance = fetchBalance(cardNumber)
                context.dataStore.edit { preferences ->
                    preferences[BALANCE_KEY] = balance.balance.availableAmount
                    preferences[REMAINING_KEY] = limit.value - limit.usedValue
                    preferences[LAST_UPDATED_KEY] =
                        SimpleDateFormat.getTimeInstance().format(Calendar.getInstance().time)
                }
            } catch (e: Exception) {
                println("An error occurred: $e")
            }
        }
    }
}