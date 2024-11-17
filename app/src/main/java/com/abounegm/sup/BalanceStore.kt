package com.abounegm.sup

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
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
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date

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

object CardInfoSerializer : Serializer<CardInfo> {
    override val defaultValue: CardInfo = CardInfo.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CardInfo {
        try {
            return CardInfo.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: CardInfo,
        output: OutputStream
    ) = t.writeTo(output)
}

/** A wrapper since Kotlin doesn't support type unions */
sealed class CardData {
    data class Physical(val card: PhysicalCard) : CardData()
    data class Virtual(val card: VirtualCard) : CardData()
    data object None : CardData()
}

class BalanceStore(private val context: Context) {
    companion object {
        private val Context.oldDataStore: DataStore<Preferences> by preferencesDataStore("cardInfo")
        private val Context.cardStore: DataStore<CardInfo> by dataStore(
            "card.pb",
            CardInfoSerializer,
            produceMigrations = { context ->
                listOf(object : DataMigration<CardInfo> {
                    override suspend fun shouldMigrate(currentData: CardInfo) =
                        context.oldDataStore.data.first().asMap().isNotEmpty()

                    override suspend fun migrate(currentData: CardInfo): CardInfo {
                        val oldData = context.oldDataStore.data.first().asMap()

                        return currentData.toBuilder()
                            .setPhysicalCard(
                                PhysicalCard.newBuilder()
                                    .setCardNumber(oldData[CARD_NUMBER_KEY] as String? ?: "")
                            )
                            .setTotalBalance(oldData[BALANCE_KEY] as Float? ?: 0f)
                            // It was previously assumed that all cards have limits
                            .setLimit(
                                Limit.newBuilder()
                                    .setTotalLimit(oldData[TOTAL_LIMIT_KEY] as Float? ?: 0f)
                                    .setRemainingLimit(oldData[REMAINING_KEY] as Float? ?: 0f)
                            )
                            .build()
                    }

                    override suspend fun cleanUp() {
                        context.oldDataStore.edit { it.clear() }
                    }
                })
            }
        )
        private val Context.historyStore: DataStore<History> by dataStore(
            "history.pb",
            HistorySerializer
        )
        private val CARD_NUMBER_KEY = stringPreferencesKey("card_number")
        private val BALANCE_KEY = floatPreferencesKey("total_balance")
        private val REMAINING_KEY = floatPreferencesKey("remaining_limit")
        private val TOTAL_LIMIT_KEY = floatPreferencesKey("total_limit")
    }

    val getCardInfo: Flow<CardData> = context.cardStore.data.map { card ->
        if (card.hasPhysicalCard())
            CardData.Physical(card.physicalCard)
        else if (card.hasVirtualCard())
            CardData.Virtual(card.virtualCard)
        else
            CardData.None
    }

    suspend fun setCardInfo(cardInfo: CardData) {
        context.cardStore.updateData { card ->
            val builder = card.toBuilder()

            when (cardInfo) {
                is CardData.None -> {} // impossible case
                is CardData.Physical -> {
                    builder.setPhysicalCard(
                        PhysicalCard.newBuilder().setCardNumber(cardInfo.card.cardNumber)
                    )
                }

                is CardData.Virtual -> {
                    builder.setVirtualCard(
                        VirtualCard.newBuilder()
                            .setPhoneNumber(cardInfo.card.phoneNumber)
                            .setLast4Digits(cardInfo.card.last4Digits)
                    )
                }
            }

            builder.build()
        }
    }

    val getLastUpdated: Flow<Date> = context.cardStore.data.map {
        Date.from(
            Instant.ofEpochSecond(
                it.lastUpdated.seconds,
                it.lastUpdated.nanos.toLong()
            )
        )
    }

    val getBalance: Flow<Float> = context.cardStore.data.map { it.totalBalance }
    val getLimit: Flow<Limit?> = context.cardStore.data.map {
        if (it.hasLimit())
            it.limit
        else
            null
    }

    val getHistory: Flow<History> = context.historyStore.data

    private val transactionTypeMap: Map<String, TransactionType> = mapOf(
        "5814" to TransactionType.FAST_FOOD,
        "6010" to TransactionType.INCOMING,
        "5812" to TransactionType.RESTAURANT,
    )

    suspend fun updateValues() {
        val cardNumber = getCardInfo.first()
        val limit = fetchLimits(cardNumber)
        val balance = fetchBalance(cardNumber)

        context.cardStore.updateData { preferences ->
            val builder = preferences.toBuilder()
            val now = Instant.now()

            builder
                .setTotalBalance(balance.balance.availableAmount)
                .setLastUpdated(
                    Timestamp.newBuilder()
                        .setSeconds(now.epochSecond)
                        .setNanos(now.nano)
                )

            if (limit != null) {
                builder.setLimit(
                    Limit.newBuilder()
                        .setTotalLimit(limit.value)
                        .setRemainingLimit(limit.value - limit.usedValue)
                )
            }
            builder.build()
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