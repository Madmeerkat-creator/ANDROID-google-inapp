package com.munice.miraclenighttest

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
import com.munice.miraclenighttest.Sku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingModule(
    private val activity: Activity,
    private val lifeCycleScope: LifecycleCoroutineScope,
    private val callback: Callback
) {
    private val consumableSkus = setOf(Sku.BUY_4COINS)

    // 구매관련 업데이트 수신
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, skuDetailsList ->
            when {
                billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null -> {
                    Log.d("mainTag", "SKULIST IS : $skuDetailsList")
                    // 제대로 구매 완료, 구매 확인 처리를 해야합니다. 3일 이내 구매확인하지 않으면 자동으로 환불됩니다.
                    for (sku in skuDetailsList) {
                        Log.d("mainTag", "SKU IS : $sku")
                        confirmPurchase(sku)
                    }
                }
                else -> {
                    // 구매 실패
                    callback.onFailure(billingResult.responseCode)
                }
            }
        }

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        Log.d("mainTag", "INIT START")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d("mainTag", "onBillingSetupFinished")
                Log.d("mainTag", "billingResult.responseCode = $billingResult.responseCode")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 여기서부터 billingClient 활성화 됨
                    callback.onBillingModulesIsReady()
                } else {
                    callback.onFailure(billingResult.responseCode)
                }
            }

            override fun onBillingServiceDisconnected() {
                // GooglePlay와 연결이 끊어졌을때 재시도하는 로직이 들어갈 수 있음.
                Log.e("BillingModule", "Disconnected.")
            }
        })
        Log.d("mainTag", "INIT END")
    }


    private fun confirmPurchase(purchase: Purchase) {
        Log.d("mainTag", "confirmPurchase start")
        when {
            consumableSkus.contains(Sku.BUY_4COINS) -> {
                // 소비성 구매는 consume을 해주어야합니다.
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                lifeCycleScope.launch(Dispatchers.IO) {
                    val result = billingClient.consumePurchase(consumeParams)
                    withContext(Dispatchers.Main) {
                        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            callback.onSuccess(purchase)
                        }
                    }
                }
            }
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged -> {
                // 구매는 완료되었으나 확인이 되어있지 않다면 구매 확인 처리를 합니다.
                val ackPurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                lifeCycleScope.launch(Dispatchers.IO) {
                    val result = billingClient.acknowledgePurchase(ackPurchaseParams.build())
                    withContext(Dispatchers.Main) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            callback.onSuccess(purchase)
                        } else {
                            callback.onFailure(result.responseCode)
                        }
                    }
                }
            }
        }
    }


    /**
     * 원하는 sku id를 가지고있는 상품 정보를 가져옵니다.
     * @param sku sku 목록
     * @param resultBlock sku 상품정보 콜백
     */
    fun querySkuDetail(
        type: String = BillingClient.SkuType.INAPP,
        vararg sku: String,
        resultBlock: (List<SkuDetails>) -> Unit = {}
    ) {
        SkuDetailsParams.newBuilder().apply {
            // 인앱, 정기결제 유형중에서 고름. (SkuType.INAPP, SkuType.SUBS)
            setSkusList(sku.asList()).setType(type)
            // 비동기적으로 상품정보를 가져옵니다.
            lifeCycleScope.launch(Dispatchers.IO) {
                val skuDetailResult = billingClient.querySkuDetails(build())
                withContext(Dispatchers.Main) {
                    resultBlock(skuDetailResult.skuDetailsList ?: emptyList())
                }
            }
        }
    }


    /**
     * 구매 시작하기
     * @param skuDetail 구매하고자하는 항목. querySkuDetail()을 통해 획득한 SkuDetail
     */
    fun purchase(
        skuDetail: SkuDetails
    ) {
        Log.d("mainTag", "skuDetail = $skuDetail")
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetail)
            .build()

        // 구매 절차를 시작, OK라면 제대로 된것입니다.
        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        Log.d("mainTag", "responseCode = $responseCode")
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            callback.onFailure(responseCode)
        }
        // 이후 부터는 purchasesUpdatedListener를 거치게 됩니다.
    }


//    /**
//     * 구매 여부 체크, 소비성 구매가 아닌 항목에 한정.
//     * @param sku
//     */
//    fun checkPurchased(
//        sku: String,
//        resultBlock: (purchased: Boolean) -> Unit
//    ) {
//        billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList?.let { purchaseList ->
//            for (purchase in purchaseList) {
//                if (purchase.skus.get(0) == sku && purchase.isPurchaseConfirmed()) {
//                    return resultBlock(true)
//                }
//            }
//            return resultBlock(false)
//        }
//    }

//    // 구매 확인 검사 Extension
//    private fun Purchase.isPurchaseConfirmed(): Boolean {
//        return this.isAcknowledged && this.purchaseState == Purchase.PurchaseState.PURCHASED
//    }

    suspend fun onResume(type: String) {
        if (billingClient.isReady) {
            billingClient.queryPurchasesAsync(type).purchasesList?.let { purchaseList ->
                for (purchase in purchaseList) {
                    if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        confirmPurchase(purchase)
                    }
                }
            }
        }
    }


    interface Callback {
        fun onBillingModulesIsReady()
        fun onSuccess(purchase: Purchase)
        fun onFailure(errorCode: Int)
    }
}