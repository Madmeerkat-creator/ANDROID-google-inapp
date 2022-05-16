package com.munice.miraclenighttest.Purchase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.munice.miraclenighttest.BillingModuleInterfaceModule
import com.munice.miraclenighttest.Sku
import com.munice.miraclenighttest.databinding.ActivityPurchaseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PurchaseActivity : AppCompatActivity() {
    private val TAG = "mainTag"
    private lateinit var binding: ActivityPurchaseBinding
    private lateinit var bm: BillingModuleInterfaceModule
    @DelicateCoroutinesApi
    private val defaultScope: CoroutineScope = GlobalScope

    //    private lateinit var bm: BillingModule
    private var mSkuDetails = listOf<SkuDetails>()
        set(value) {
            field = value
            setSkuDetailsView()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bm = BillingModuleInterfaceModule(
            this,
            lifecycleScope,
            object : BillingModuleInterfaceModule.Callback {

                override fun onBillingModulesIsReady() {
                    bm.querySkuDetail(BillingClient.SkuType.INAPP, Sku.BUY_4COINS) { skuDetails ->
                        mSkuDetails = skuDetails
                    }
                    Log.d("mainTag", "mSkuDetails = $mSkuDetails")
                }

                override fun onSuccess(purchase: Purchase) {
                    Log.d("mainTag", "SUCCESS!!")
                    when (purchase.skus.get(0)) {
                        Sku.BUY_4COINS -> {
                            Log.d("mainTag", "DO something Buy coins!!")
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.d("mainTag", "FAIL!!")
                    when (errorCode) {
                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                            Toast.makeText(
                                this@PurchaseActivity,
                                "이미 구입한 상품입니다.",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            Toast.makeText(this@PurchaseActivity, "구매를 취소하셨습니다.", Toast.LENGTH_LONG)
                                .show()
                        }
                        else -> {
                            Toast.makeText(
                                this@PurchaseActivity,
                                "error: $errorCode",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                }
            })

        setClickListeners()
    }

    private fun setSkuDetailsView() {
        val builder = StringBuilder()
        for (skuDetail in mSkuDetails) {
            Log.d("mainTag", "hi : $skuDetail")
            builder.append("<${skuDetail.title}>\n")
            builder.append(skuDetail.price)
            builder.append("\n======================\n\n")
        }
        binding.tvSku.text = builder
    }

    private fun setClickListeners() {
        with(binding) {
            btnPurchaseCrystal.setOnClickListener {
                Log.d("mainTag", "clicked button btnPurchaseCrystal!!")
                mSkuDetails.find { it.sku == Sku.BUY_4COINS }?.let { skuDetail ->
                    bm.purchase(skuDetail)
                } ?: also {
                    Toast.makeText(applicationContext, "상품을 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        defaultScope.launch {
            bm.onResume(BillingClient.SkuType.INAPP)
        }
    }

}