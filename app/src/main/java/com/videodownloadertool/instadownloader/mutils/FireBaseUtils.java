package com.videodownloadertool.instadownloader.mutils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class FireBaseUtils {
    private static FireBaseUtils sharedInstance;
    private FirebaseAnalytics mFirebaseAnalytics;

    public static FireBaseUtils getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new FireBaseUtils();
        }
        return sharedInstance;
    }

    public void init(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void logEventDownload(String name) {
        Bundle params = new Bundle();
        params.putString("dname", name);
        mFirebaseAnalytics.logEvent("DOWNLOAD", params);
    }
}
