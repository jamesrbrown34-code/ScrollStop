package com.scrollstop.app.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BillingStatus { READY, UNAVAILABLE, PENDING, ERROR }
data class BillingState(
    val isPremium: Boolean = false,
    val status: BillingStatus = BillingStatus.UNAVAILABLE,
    val message: String? = null,
    val monthlyPrice: String? = null,
    val yearlyPrice: String? = null,
    val monthlyYearlyEquivalent: String? = null,
    val annualSavingsPercent: Int? = null
)

class BillingRepository(context: Context) : PurchasesUpdatedListener {
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()
    private val details = mutableMapOf<String, ProductDetails>()
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    fun connect() = client.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(status = BillingStatus.READY, message = null)
                queryProducts(); refreshPurchases()
            } else _state.value = BillingState(status = BillingStatus.UNAVAILABLE, message = result.debugMessage)
        }
        override fun onBillingServiceDisconnected() { _state.value = _state.value.copy(status = BillingStatus.UNAVAILABLE) }
    })

    fun refreshPurchases() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) applyPurchases(purchases)
            else _state.value = BillingState(status = BillingStatus.UNAVAILABLE, message = result.debugMessage)
        }
    }

    fun launchPurchase(activity: Activity, productId: String, preferTrial: Boolean = false) {
        val product = details[productId] ?: run { _state.value = _state.value.copy(message = "Plans are unavailable. Try again shortly."); return }
        val offers = product.subscriptionOfferDetails ?: run { _state.value = _state.value.copy(message = "This plan is unavailable."); return }
        val offer = if (preferTrial) {
            offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros == 0L }
                ?: offers.firstOrNull()
        } else {
            offers.firstOrNull()
        } ?: run { _state.value = _state.value.copy(message = "This plan is unavailable."); return }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> applyPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(message = "Purchase cancelled.")
            else -> _state.value = _state.value.copy(status = BillingStatus.ERROR, message = result.debugMessage)
        }
    }

    private fun queryProducts() {
        val products = listOf("scrollstop_monthly", "scrollstop_yearly").map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, response ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                response.productDetailsList.forEach { details[it.productId] = it }
                val monthlyPhase = basePlanPhase("scrollstop_monthly")
                val yearlyPhase = basePlanPhase("scrollstop_yearly")
                val monthlyMicros = monthlyPhase?.priceAmountMicros ?: 0L
                val yearlyMicros = yearlyPhase?.priceAmountMicros ?: 0L
                val yearlyEquivalentMicros = monthlyMicros * 12
                val savingsPercent = if (yearlyMicros > 0 && yearlyEquivalentMicros > yearlyMicros) {
                    ((yearlyEquivalentMicros - yearlyMicros) * 100 / yearlyEquivalentMicros).toInt()
                } else null
                _state.value = _state.value.copy(
                    monthlyPrice = monthlyPhase?.let { formatPrice(it.priceAmountMicros, it.priceCurrencyCode) },
                    yearlyPrice = yearlyPhase?.let { formatPrice(it.priceAmountMicros, it.priceCurrencyCode) },
                    monthlyYearlyEquivalent = monthlyPhase?.let {
                        formatPrice(yearlyEquivalentMicros, it.priceCurrencyCode)
                    },
                    annualSavingsPercent = savingsPercent
                )
            }
        }
    }

    private fun basePlanPhase(productId: String): com.android.billingclient.api.ProductDetails.PricingPhase? {
        val product = details[productId] ?: return null
        return product.subscriptionOfferDetails
            ?.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros != 0L }
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
    }

    private fun formatPrice(amountMicros: Long, currencyCode: String): String = runCatching {
        val numberFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault())
        numberFormat.currency = java.util.Currency.getInstance(currencyCode)
        numberFormat.format(amountMicros / 1_000_000.0)
    }.getOrElse { "$amountMicros" }

    private fun applyPurchases(purchases: List<Purchase>) {
        val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) { }
        }
        _state.value = BillingState(
            isPremium = active,
            status = BillingStatus.READY,
            monthlyPrice = _state.value.monthlyPrice,
            yearlyPrice = _state.value.yearlyPrice,
            monthlyYearlyEquivalent = _state.value.monthlyYearlyEquivalent,
            annualSavingsPercent = _state.value.annualSavingsPercent
        )
    }
}
