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
            val value: Int,
            val usedValue: Int,
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
        val cardType: String,
    ) {
        data class BalanceDetails(val availableAmount: Int)
        data class BalanceHistory(
            val time: String,
            val amount: Int,
            val locationName: Array<String>,
            val trnType: Int,
            val mcc: Int,
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
                result = 31 * result + amount
                result = 31 * result + locationName.contentHashCode()
                result = 31 * result + trnType
                result = 31 * result + mcc
                result = 31 * result + currency.hashCode()
                result = 31 * result + merchantId.hashCode()
                result = 31 * result + reversal.hashCode()
                return result
            }
        }
    }
}

const val apiBase = "https://meal.gift-cards.ru/api/1/cards"

fun getLimits(cardNumber: String): LimitsResponse.LimitsData.Limit {
    val json = URL("$apiBase/$cardNumber/limits").readText()
    val response = Gson().fromJson(json, LimitsResponse::class.java)
    if (response.status != "OK") {
        throw Exception("SUP API failed")
    }
    if (response.data.limits.isEmpty()) {
        throw Exception("SUP API returned an empty list")
    }
    return response.data.limits[0]
}

fun getBalance(cardNumber: String): BalanceResponse.BalanceData {
    val json = URL("$apiBase/$cardNumber?limit=10").readText()
    val response = Gson().fromJson(json, BalanceResponse::class.java)
    if (response.status != "OK") {
        throw Exception("SUP API failed")
    }
    return response.data
}