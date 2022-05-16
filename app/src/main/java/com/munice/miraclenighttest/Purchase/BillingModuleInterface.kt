package com.munice.miraclenighttest.Purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

interface BillingModuleInterface {

    suspend fun onResume(type: String)

    fun querySkuDetail(type: String = BillingClient.SkuType.INAPP, vararg sku: String, resultBlock: (List<SkuDetails>) -> Unit = {})

    fun purchase(skuDetail: SkuDetails)

//    fun checkPurchased(sku: String, resultBlock: (purchased: Boolean) -> Unit)

    fun confirmPurchase(purchase: Purchase)

//    fun Purchase.isPurchaseConfirmed(): Boolean

}