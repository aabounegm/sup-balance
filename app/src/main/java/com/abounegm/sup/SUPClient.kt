package com.abounegm.sup

import com.google.gson.Gson
import java.net.URL


data class LimitsResponse(
    val status: String,
    val data: LimitsData,
) {
    data class LimitsData(
        val limits: Array<Limit>,
        // schedule
        // timePeriods
    ) {
        data class Limit(
            val cycle: String,
            val value: Float,
            val usedValue: Float,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LimitsData

            return limits.contentEquals(other.limits)
        }

        override fun hashCode(): Int {
            return limits.contentHashCode()
        }
    }
}

data class BalanceResponse(
    val status: String,
    val data: BalanceData,
) {
    data class BalanceData(
        val phone: String,
        val balance: BalanceDetails,
        val history: Array<BalanceHistory>,
        val smsInfoStatus: String,
        val smsNotificationAvailable: Boolean,
        /** "food" or "virtual" */
        val cardType: String,
        /** Only present if cardType == "virtual" */
        val activationData: String?,
        /** Only present if cardType == "virtual" */
        val validUntil: String?,
    ) {
        data class BalanceDetails(val availableAmount: Float)
        data class BalanceHistory(
            val time: String,
            val amount: Float,
            val locationName: Array<String>,
            val trnType: Int,
            val mcc: String,
            val currency: String,
            val merchantId: String,
            val reversal: Boolean,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as BalanceHistory

                if (time != other.time) return false
                if (amount != other.amount) return false
                if (!locationName.contentEquals(other.locationName)) return false
                if (trnType != other.trnType) return false
                if (mcc != other.mcc) return false
                if (currency != other.currency) return false
                if (merchantId != other.merchantId) return false
                return reversal == other.reversal
            }

            override fun hashCode(): Int {
                var result = time.hashCode()
                result = 31 * result + amount.hashCode()
                result = 31 * result + locationName.contentHashCode()
                result = 31 * result + trnType
                result = 31 * result + mcc.hashCode()
                result = 31 * result + currency.hashCode()
                result = 31 * result + merchantId.hashCode()
                result = 31 * result + reversal.hashCode()
                return result
            }
        }
    }
}

const val apiBase = "https://meal.gift-cards.ru/api/1"
const val physicalCardApiBase = "$apiBase/cards"
const val virtualCardApiBase = "$apiBase/virtual-cards"

fun getApiUrl(cardData: CardData): String {
    return when (cardData) {
        is CardData.Virtual -> "$virtualCardApiBase/${cardData.card.phoneNumber}/${cardData.card.last4Digits}"
        is CardData.Physical -> "$physicalCardApiBase/${cardData.card.cardNumber}"
        else -> ""
    }
}

fun fetchLimits(cardData: CardData): LimitsResponse.LimitsData.Limit? {
    val url = getApiUrl(cardData)
    val json = URL("$url/limits").readText()
    val response = Gson().fromJson(json, LimitsResponse::class.java)
    if (response.status != "OK") {
        throw Exception("SUP API failed")
    }
    return response.data.limits.firstOrNull()
}

fun fetchBalance(cardData: CardData): BalanceResponse.BalanceData {
    val json = URL("${getApiUrl(cardData)}?limit=10").readText()
    val response = Gson().fromJson(json, BalanceResponse::class.java)
    if (response.status != "OK") {
        throw Exception("SUP API failed")
    }
    return response.data
}