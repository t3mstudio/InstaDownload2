package com.videodownloadertool.instadownloader;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.videodownloadertool.instadownloader.mutils.ClientConfig;
import com.videodownloadertool.instadownloader.mutils.InterstitialUtils;

import us.shandian.giga.ui.DownloadActivity;
import us.shandian.giga.util.Utility;

public class SettingsActivity extends AppCompatActivity {
//    public static final String PREFERENCES_NAME = "instagramPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        setupActionBar();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new MainPreference())
                .commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class MainPreference extends PreferenceFragmentCompat {

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if (key == null)
                return false;
            switch (key) {
                case "key_share":
                    try {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Ringtone Maker Pro");
                        String sAux = "\nLet me recommend you this application\n\n";
                        sAux = sAux + "https://play.google.com/store/apps/details?id=com.kingsapptool.ringtonemaker \n\n";
                        i.putExtra(Intent.EXTRA_TEXT, sAux);
                        startActivity(Intent.createChooser(i, "choose one"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;

                case "key_feedback":
                    if (getActivity() !=null){
                        sendFeedback(getActivity());return true;
                    }

                    else return false;

                case "download_folder":
                    ClientConfig clientConfig = InterstitialUtils.getSharedInstance().getClient();
                    if (clientConfig.isAccept != 0)
                        startActivity(new Intent(getActivity(), DownloadActivity.class));
                    return true;
            }


            return false;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_main, rootKey);

            Preference pref_version = findPreference("key_about");
            if (pref_version != null && getActivity() != null){
                try {
                    String body = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
                    if (body == null || body.equalsIgnoreCase("")) {
                        pref_version.setSummary("1.0");
                    } else
                        pref_version.setSummary(body);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }

            Preference insta_path = findPreference("insta_path");
            insta_path.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                return true;
            });

            Preference download_yt = findPreference("download_path");
            ClientConfig clientConfig = InterstitialUtils.getSharedInstance().getClient();

            if (download_yt != null) {
                if (clientConfig != null && clientConfig.isAccept != 0) {
                    download_yt.setVisible(true);

                    download_yt.setOnPreferenceClickListener(preference -> {
                        startActivity(new Intent(getContext(), DownloadActivity.class));
                        return true;
                    });
                } else {
                    download_yt.setVisible(false);
                }

                download_yt.setSummary(Utility.getVideoAudioStoragePath(getActivity()));
            }
        }

        private  void sendFeedback(Context context) {
            String body = null;
            try {
                body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
                body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS : Android \n " +
                        "Device OS Version : " + Build.VERSION.RELEASE + "\n App Version : 1.0.0" + "\n Device Brand : " +
                        Build.BRAND + "\n Device Model : " + Build.MODEL + "\n Device Manufacture : " + Build.MANUFACTURER
                        + "\n\n";
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"hawksaw23@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback from " + R.string.app_name);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            context.startActivity(Intent.createChooser(intent, "Send to"));
        }


    }
}
