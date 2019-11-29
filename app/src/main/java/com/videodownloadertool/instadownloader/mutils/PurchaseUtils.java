package com.videodownloadertool.instadownloader.mutils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.videodownloadertool.instadownloader.MainActivity;

import java.util.Arrays;
import java.util.List;

public class PurchaseUtils implements PurchasesUpdatedListener {
    private static PurchaseUtils sharedInstance;

    private BillingClient billingClient;
    private boolean canPurchase = false;
    private List<String> sku_list = Arrays.asList("unlimited");

    private int currentItem;
    private List<SkuDetails> skuDetailsList;
    private PurchaseListener purchaseListener;


    public static PurchaseUtils getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new PurchaseUtils();
        }
        return sharedInstance;
    }

    public void init(Context context) {

        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    canPurchase = true;
                    Log.d(AppConstants.log_tag, "Billing connected");
                    queryPurchase();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d(AppConstants.log_tag, "Billing Disconnected");
            }
        });
    }

    private void queryPurchase() {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(sku_list).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult1, skuDetailsList) -> {
                    if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && skuDetailsList != null) {

                        this.skuDetailsList = skuDetailsList;
                    } else {
                        Log.d(AppConstants.log_tag, "purchase failed to query");
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(AppConstants.log_tag, "purchase cancel");
            if (purchaseListener != null)
                purchaseListener.purchaseCancel(currentItem);
        } else {
            Log.d(AppConstants.log_tag, "purchase others error");
            if (purchaseListener != null)
                purchaseListener.purchaseFailed(currentItem);
        }
    }

    public void purchaseItem(Activity context, int item, PurchaseListener listener) {
        if (MainActivity.DEBUG) {
            if (listener != null)
                listener.purchaseSuccess(item);
        } else {
            if (canPurchase && skuDetailsList != null && skuDetailsList.size() > item) {
                currentItem = item;
                this.purchaseListener = listener;

                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetailsList.get(item))
                        .build();
                billingClient.launchBillingFlow(context, flowParams);
                Log.d(AppConstants.log_tag, "purchase show dialog");
            } else {
                if (listener != null)
                    listener.purchaseFailed(item);
            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (purchaseListener != null)
                purchaseListener.purchaseSuccess(currentItem);
            Log.d(AppConstants.log_tag, "purchase successful");
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .setDeveloperPayload(purchase.getDeveloperPayload())
                            .build();
            billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(AppConstants.log_tag, "consume OK");
                }
            });
        } else {
            if (purchaseListener != null)
                purchaseListener.purchaseFailed(currentItem);
        }
    }
}
