package com.videodownloadertool.instadownloader.mutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.videodownloadertool.instadownloader.MainActivity;

import java.util.Date;
import java.util.Random;

public class InterstitialUtils {

    private static InterstitialUtils sharedInstance;

    private InterstitialAd interstitialAd;
    private AdCloseListener adCloseListener;
    private AdRewardListener adRewardListener;

    private Date lastTimeShowInterstitialAds;
    private ClientConfig clientConfig;

    private RewardedVideoAd mRewardedVideoAd;

    public static InterstitialUtils getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new InterstitialUtils();
        }
        return sharedInstance;
    }

    public ClientConfig getClient() {
        return clientConfig;
    }

    public void init(Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
        clientConfig = new ClientConfig();

        if (mPrefs.contains("youtube")) //đã nâng cấp lên premium version có youtube
        {
            clientConfig.max_percent_ads = 0;
            clientConfig.isAccept = 2;
            clientConfig.percentRate = 0;
            clientConfig.isGoogleIp = 0;
        } else {
            if(mPrefs.contains("isAccept"))
            {
                boolean isAccept = false;
                try
                {
                    isAccept = mPrefs.getBoolean("isAccept", false);
                }
                catch (ClassCastException e)
                {
                    isAccept = ( 1 == mPrefs.getInt("isAccept", 0)  );
                }

                if (isAccept) {
                    clientConfig.max_percent_ads = 100;
                    clientConfig.isAccept = 1;
                    clientConfig.percentRate = 0;
                    clientConfig.isGoogleIp = 0;
                    clientConfig.delay_show_ads = 60;

                    clientConfig.BANNER_ADMOB_ID = new String(Base64.decode(AppConstants.ID_1, Base64.DEFAULT));
                    clientConfig.FULL_ADMOB_ID = new String(Base64.decode(AppConstants.ID_2, Base64.DEFAULT));
                    clientConfig.REWARD_ADMOB_ID = new String(Base64.decode(AppConstants.ID_3, Base64.DEFAULT));
                } else {
                    clientConfig.max_percent_ads = 0;
                    clientConfig.isAccept = 0;
                    clientConfig.percentRate = 0;
                    clientConfig.isGoogleIp = 1;
                }

            }
            else
            {
                if (mPrefs.contains("no_ads") || Utils.isDevMode(context) == 1 ||
                        !"com.android.vending".equals(Utils.getInstaller(context))) {
                    mPrefs.edit().putBoolean("isAccept", false).apply();
                    clientConfig.max_percent_ads = 0;
                    clientConfig.isAccept = 0;
                    clientConfig.percentRate = 0;
                    clientConfig.isGoogleIp = 1;
                } else {
                    mPrefs.edit().putBoolean("isAccept",true).apply();
                    clientConfig.max_percent_ads = 100;
                    clientConfig.isAccept = 1;
                    clientConfig.percentRate = 0;
                    clientConfig.isGoogleIp = 0;
                    clientConfig.delay_show_ads = 60;

                    clientConfig.BANNER_ADMOB_ID = new String(Base64.decode(AppConstants.ID_1, Base64.DEFAULT));
                    clientConfig.FULL_ADMOB_ID = new String(Base64.decode(AppConstants.ID_2, Base64.DEFAULT));
                    clientConfig.REWARD_ADMOB_ID = new String(Base64.decode(AppConstants.ID_3, Base64.DEFAULT));
                }
            }
        }

        if (MainActivity.DEBUG) {
            clientConfig.isAccept = 1;
            clientConfig.max_percent_ads = 100;
            clientConfig.isGoogleIp = 0;
            clientConfig.fb_percent_ads = 0;
            clientConfig.BANNER_ADMOB_ID = "ca-app-pub-3940256099942544/6300978111";
            clientConfig.FULL_ADMOB_ID = "ca-app-pub-3940256099942544/1033173712";
            clientConfig.REWARD_ADMOB_ID = "ca-app-pub-3940256099942544/5224354917";
        }


        if (clientConfig == null || clientConfig.isGoogleIp == 1 || clientConfig.max_percent_ads == 0) {
            return;
        }

        interstitialAd = new InterstitialAd(context);
        interstitialAd.setAdUnitId(clientConfig.FULL_ADMOB_ID);
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                if (adCloseListener != null)
                    adCloseListener.onAdClose();

                loadInterstitialAds();
            }
        });

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(context);
        mRewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
            }

            @Override
            public void onRewardedVideoAdOpened() {
            }

            @Override
            public void onRewardedVideoStarted() {
            }

            @Override
            public void onRewardedVideoAdClosed() {
                loadRewardVideoAds();
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {
                if (adRewardListener != null)
                    adRewardListener.onRewarded();
            }

            @Override
            public void onRewardedVideoAdLeftApplication() {
            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {
            }

            @Override
            public void onRewardedVideoCompleted() {
            }
        });

        loadInterstitialAds();
        loadRewardVideoAds();
    }

    private void loadInterstitialAds() {
        if (interstitialAd != null && !interstitialAd.isLoaded() && !interstitialAd.isLoading()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            interstitialAd.loadAd(adRequest);
        }
    }

    private void loadRewardVideoAds() {
        mRewardedVideoAd.loadAd(clientConfig.REWARD_ADMOB_ID,
                new AdRequest.Builder().build());
    }

    public void showRewardVideoAds(AdRewardListener adRewardListener) {
        if (clientConfig == null || clientConfig.isGoogleIp == 1 || clientConfig.max_percent_ads == 0) {
            if (adRewardListener != null)
                adRewardListener.onAdNotAvailable();
            return;
        }

        if (mRewardedVideoAd != null && mRewardedVideoAd.isLoaded()) {
            this.adRewardListener = adRewardListener;
            mRewardedVideoAd.show();
        } else {
            loadRewardVideoAds();
            if (adRewardListener != null)
                adRewardListener.onAdNotAvailable();
        }
    }

    public void showInterstitialAds(AdCloseListener adCloseListener) {
        if (clientConfig == null || clientConfig.isGoogleIp == 1 || clientConfig.max_percent_ads == 0) {
            if (adCloseListener != null)
                adCloseListener.onNoAd();
            return;
        }

        long timeBetween = Long.MAX_VALUE;
        if (lastTimeShowInterstitialAds != null)
            timeBetween = new Date().getTime() - lastTimeShowInterstitialAds.getTime();

        if (timeBetween > clientConfig.delay_show_ads * 1000) {
            if (interstitialAd != null && interstitialAd.isLoaded()) {
                this.adCloseListener = adCloseListener;
                lastTimeShowInterstitialAds = new Date();
                interstitialAd.show();
            } else {
                loadInterstitialAds();
                if (adCloseListener != null)
                    adCloseListener.onNoAd();
            }
        } else {
            if (adCloseListener != null)
                adCloseListener.onNoAd();
        }
    }
}

