package com.abounegm.sup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar

class BalanceStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cardInfo")
        private val CARD_NUMBER_KEY = stringPreferencesKey("card_number")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
        private val BALANCE_KEY = intPreferencesKey("total_balance")
        private val REMAINING_KEY = intPreferencesKey("remaining_limit")
    }

    val getCardNumber: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CARD_NUMBER_KEY] ?: ""
    }

    val getLastUpdated: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_UPDATED_KEY] ?: SimpleDateFormat.getTimeInstance()
            .format(Calendar.getInstance().time)
    }

    val getBalance: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BALANCE_KEY] ?: 0
    }

    val getRemaining: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REMAINING_KEY] ?: 0
    }

    suspend fun saveCardNumber(cardNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[CARD_NUMBER_KEY] = cardNumber
        }
    }

    suspend fun updateValues() {
        val cardNumber = getCardNumber.first()
        val limit = getLimits(cardNumber)
        val balance = getBalance(cardNumber)
        context.dataStore.edit { preferences ->
            preferences[BALANCE_KEY] = balance.balance.availableAmount
            preferences[REMAINING_KEY] = limit.value - limit.usedValue
            preferences[LAST_UPDATED_KEY] =
                SimpleDateFormat.getTimeInstance().format(Calendar.getInstance().time)
        }
    }
}