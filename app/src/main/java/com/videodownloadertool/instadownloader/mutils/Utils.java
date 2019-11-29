package com.videodownloadertool.instadownloader.mutils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.List;

public class Utils {

    public static int isDevMode(Context context) {
        if (Integer.valueOf(Build.VERSION.SDK_INT) == 16) {
            return android.provider.Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0);
        } else if (Integer.valueOf(Build.VERSION.SDK_INT) >= 17) {
            return android.provider.Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        } else return 0;
    }


    public static String isPackageInstalled(Context c, String targetPackage) {
        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return "0";
        }
        return "1";
    }

    public static int getNumberAppInstall(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        return packages.size();
    }

    public static String getInstaller(Context context) {
        try {
            String result = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            if (result != null && result.length() > 0)
                return result;
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public static int getAppBuild(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
            localNameNotFoundException.printStackTrace();
        }
        return 0;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }
}
