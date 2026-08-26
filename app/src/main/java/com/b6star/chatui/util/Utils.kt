package com.b6star.chatui.util

import android.os.Build

object Utils {

    fun calculateCostUsd(
        modelName: String,
        promptTokens: Int,
        candidatesTokens: Int,
        thoughtsTokens: Int
    ): Double {
        val pricePerMillionUsd = when {
            modelName.contains("pro") -> 10.0 to 30.0
            modelName.contains("lite") -> 0.075 to 0.3
            else -> 0.15 to 0.6
        }
        val promptCost = promptTokens / 1_000_000.0 * pricePerMillionUsd.first
        val outputTokens = candidatesTokens + thoughtsTokens
        val outputCost = outputTokens / 1_000_000.0 * pricePerMillionUsd.second
        return promptCost + outputCost
    }

    fun calculateCostKrw(costUsd: Double): Double = costUsd * EXCHANGE_RATE_KRW

    fun formatCost(krw: Double): String = when {
        krw <= 0.0 -> "\$0"
        krw < 1.0 -> "\$%.2f".format(krw)
        else -> "\$%,.0f".format(krw)
    }

    fun formatDurationMs(ms: Long): String = when {
        ms >= 1000 -> "%.2fs".format(ms / 1000.0)
        else -> "${ms}ms"
    }

    fun formatCurrency(amount: Double): String = "\$%,.0f".format(amount)

    fun getDeviceModel(): String = Build.MODEL ?: "unknown"

    fun getOsVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    private const val EXCHANGE_RATE_KRW = 1400.0
}
