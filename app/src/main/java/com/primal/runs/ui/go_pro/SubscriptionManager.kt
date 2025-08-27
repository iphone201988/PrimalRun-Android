package com.primal.runs.ui.go_pro

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.gson.Gson

class SubscriptionManager(
    val context: Context,
    val listener: PurchaseListener
) : PurchasesUpdatedListener
{
    private val MONTHLY_SUBS = "montly_subs"
    private val QUARTERLY_SUBS = "quarterly_subs"
    private val ANNUAL_SUBS = "annual_subscription"

    private lateinit var plansList: MutableList<ProductDetails>
    val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()


    fun startConnection(onConnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                } else {

                    Log.d("startConnection", "onConnected: ")
                }
            }

            override fun onBillingServiceDisconnected() {

                Log.d("startConnection", "onBillingServiceDisconnected: ")
            }
        })
    }

    // Fetch available subscriptions
    fun queryAvailableSubscriptions(callback: (List<ProductDetails>) -> Unit
                                    /*callbackError: (String) -> Unit*/) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(MONTHLY_SUBS)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(QUARTERLY_SUBS)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ANNUAL_SUBS)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()



                )
            ).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                plansList = productDetailsList
                Log.d("plansList", "queryAvailableSubscriptions: ${Gson().toJson(plansList)}")
                callback(productDetailsList)
            } else {
                Log.d("billingResult", "queryAvailableSubscriptions: ${billingResult.responseCode}")
                //callbackError("No subscriptions available")
            }
        }
    }

    fun purchaseSubscription(activity: Activity, planId: String?) {

        val productDetails = plansList.find { it.productId == planId }
        val billingFlowParams = BillingFlowParams.newBuilder()

            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails!!)
                        .setOfferToken(productDetails.subscriptionOfferDetails?.get(0)?.offerToken!!)
                        .build()
                )
            ).build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {

                listener.handlePurchase(purchase)
                //handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {


        }
        else {

        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // Acknowledge the purchase if it hasn’t been acknowledged
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                }
            }
        }
    }

    interface PurchaseListener{
        fun handlePurchase(purchase: Purchase)

        /*fun onPurchaseSuccess()
        fun onPurchaseFailed()*/

    }
}