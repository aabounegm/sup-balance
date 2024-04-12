package com.abounegm.sup

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import androidx.glance.appwidget.updateAll
import com.google.protobuf.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Calendar

object HistorySerializer : Serializer<History> {
    override val defaultValue: History = History.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): History {
        try {
            return History.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: History,
        output: OutputStream
    ) = t.writeTo(output)
}

class BalanceStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cardInfo")
        private val Context.historyStore: DataStore<History> by dataStore(
            "history.pb",
            HistorySerializer
        )
        private val CARD_NUMBER_KEY = stringPreferencesKey("card_number")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
        private val BALANCE_KEY = floatPreferencesKey("total_balance")
        private val REMAINING_KEY = floatPreferencesKey("remaining_limit")
        private val TOTAL_LIMIT_KEY = floatPreferencesKey("total_limit")
    }

    val getCardNumber: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CARD_NUMBER_KEY] ?: ""
    }

    val getLastUpdated: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_UPDATED_KEY] ?: "Never"
    }

    val getBalance: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[BALANCE_KEY] ?: 0f
    }

    val getTotalLimit: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_LIMIT_KEY] ?: 0f
    }

    val getRemaining: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[REMAINING_KEY] ?: 0f
    }

    val getHistory: Flow<History> = context.historyStore.data

    suspend fun saveCardNumber(cardNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[CARD_NUMBER_KEY] = cardNumber
        }
    }

    private val transactionTypeMap: Map<String, TransactionType> = mapOf(
        "5814" to TransactionType.FAST_FOOD,
        "6010" to TransactionType.INCOMING,
        "5812" to TransactionType.RESTAURANT,
    )

    suspend fun updateValues() {
        val cardNumber = getCardNumber.first()
        val limit = fetchLimits(cardNumber)
        val balance = fetchBalance(cardNumber)
        context.dataStore.edit { preferences ->
            preferences[BALANCE_KEY] = balance.balance.availableAmount
            preferences[TOTAL_LIMIT_KEY] = limit.value
            preferences[REMAINING_KEY] = limit.value - limit.usedValue
            preferences[LAST_UPDATED_KEY] =
                SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Calendar.getInstance().time)
        }
        context.historyStore.updateData {
            val items = balance.history.map {
                val time = OffsetDateTime.parse(it.time)
                val timestamp = Timestamp.newBuilder().setSeconds(time.toEpochSecond()).build()

                HistoryItem.newBuilder()
                    .setName(it.locationName.firstOrNull() ?: "Unknown name")
                    .setAmount(it.amount)
                    .setTime(timestamp)
                    .setType(
                        if (it.reversal)
                            TransactionType.REFUND
                        else
                            transactionTypeMap.getOrDefault(it.mcc, TransactionType.GENERIC)
                    )
                    .build()
            }
            History.newBuilder().addAllTransactions(items).build()
        }
        BalanceWidget().updateAll(context)
    }
}