package com.videodownloadertool.instadownloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;
import com.kobakei.ratethisapp.RateThisApp;
import com.videodownloadertool.instadownloader.mutils.ClientConfig;
import com.videodownloadertool.instadownloader.mutils.FireBaseUtils;
import com.videodownloadertool.instadownloader.mutils.InterstitialUtils;
import com.videodownloadertool.instadownloader.mutils.PurchaseListener;
import com.videodownloadertool.instadownloader.mutils.PurchaseUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import us.shandian.giga.ui.DownloadActivity;
import us.shandian.giga.util.Utility;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
        , BaseSliderView.OnSliderClickListener, ViewPagerEx.OnPageChangeListener {

    public static final boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");
    public static final String PREFERENCES_NAME = "instagramPro";
    final private String TAG = "tuancon";
    private EditText edtURL;
    private TextView edtCaption, page_number;
    private Button btnSubmit, btnSave, btnCopy, btnRepost, btnPaste;
    private ImageView imgView;
    private ImageButton imgPlay;
    private ProgressBar progressBar;
    private RelativeLayout relative_layout;
    private RelativeLayout adContainer;
    private int posisi;
    private int photoposition;
    private int check_download = 0;
    static final int permission = 1;
    private int previousPosition = 0;
    private static int SLIDER_COUNT = 1;
    private int fixposition;
    private HashMap<String, String> url_maps = new HashMap<>();
    private SliderLayout mDemoSlider;
    private PagerIndicator pagerIndicator;
    private DefaultSliderView textSliderView;
    private int selectedColor;
    private int unSelectedColor;
    private ArrayList<String> videolink = new ArrayList<>();
    private ArrayList<String> photolink = new ArrayList<>();
    private ArrayList<String> videoposition = new ArrayList<>();
    private String link, filename, repost;
    private Model model = new Model();
    private SharedPreferences mPref;
    private SharedPreferences.Editor editor;

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private int navigation_item = -1;
    private ClientConfig clientConfig;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        InterstitialUtils.getSharedInstance().init(this);
        PurchaseUtils.getSharedInstance().init(this);
        Utility.initDownloadSetting(this);
        FireBaseUtils.getSharedInstance().init(this);
        clientConfig = InterstitialUtils.getSharedInstance().getClient();

        mPref = getApplicationContext().getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        editor = mPref.edit();

        if (mPref.getInt("install_first", 0) == 0) {
            mPref.edit().putInt("download_remain", 3).apply();
            mPref.edit().putInt("install_first", 1).apply();
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setTitle(getResources().getString(R.string.instagram));
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


//        int rate = mPref.getInt("rate", 0);
//
//        if (rate == 0) {
//            RatingDialog ratingDialog = new RatingDialog.Builder(this)
//                    .session(3)
//                    .threshold(4)
//                    .title("How was your experience with us?")
//                    .titleTextColor(R.color.black)
//                    .positiveButtonText("Not Now")
//                    .positiveButtonTextColor(R.color.black)
//                    .negativeButtonText("Never")
//                    .negativeButtonTextColor(R.color.black)
//                    .formTitle("Submit Feedback")
//                    .ratingBarBackgroundColor(R.color.grey_400)
//                    .formHint("Tell us where we can improve")
//                    .formSubmitText("Submit")
//                    .formCancelText("Cancel")
//                    .ratingBarColor(R.color.accent_orange)
//                    .feedbackTextColor(R.color.black)
//                    .onRatingChanged((float rating, boolean thresholdCleared) ->
//                            editor.putInt("rate", 1).apply()
//                    )
//                    .onRatingBarFormSumbit((String feedback) ->
//                            Toast.makeText(this, "Thanks for feedback !", Toast.LENGTH_SHORT).show()
//                    ).build();
//            Log.d("tuancon", "" + rate);
//
//
//            ratingDialog.show();
//        }

        RateThisApp.onCreate(this);
        RateThisApp.Config config = new RateThisApp.Config(1, 5);
        config.setMessage(R.string.rate_5_stars);
        RateThisApp.init(config);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        adContainer = findViewById(R.id.adView);
        pagerIndicator = findViewById(R.id.custom_indicator);
        relative_layout = findViewById(R.id.relative_layout);
        mDemoSlider = findViewById(R.id.slider);
        edtURL = findViewById(R.id.edtUrl);
        edtCaption = findViewById(R.id.edtCaption);
        btnSubmit = findViewById(R.id.btnSubmit);
        imgView = findViewById(R.id.imgView);
        page_number = findViewById(R.id.page_number);
        btnSave = findViewById(R.id.btnSave);
        btnCopy = findViewById(R.id.btnCopy);
        btnRepost = findViewById(R.id.btnRepost);
        btnPaste = findViewById(R.id.btnPaste);
        progressBar = findViewById(R.id.progress);
        imgPlay = findViewById(R.id.imgPlay);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        checkPermission();


        selectedColor = Color.parseColor("#3F51B5");
        unSelectedColor = Color.parseColor("#E0E0E0");
        mDemoSlider.setDuration(2000);
        pagerIndicator.setDefaultIndicatorColor(selectedColor, unSelectedColor);
        mDemoSlider.setCustomIndicator(pagerIndicator);
        btnSubmit.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        imgPlay.setOnClickListener(this);
        btnPaste.setOnClickListener(this);
        btnRepost.setOnClickListener(this);
        btnCopy.setOnClickListener(this);

        setupNavigationDrawer();


        edtURL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() == 0) {
                    btnSubmit.setText("HOW TO USE");
                } else {
                    btnSubmit.setText("SUBMIT URL");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        if (clientConfig.isAccept == 0) {
//            navigationView.getMenu().findItem(R.id.nav_download).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_yt).setVisible(false);
        } else {
//            navigationView.getMenu().findItem(R.id.nav_download).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_yt).setVisible(true);
        }

        if (mPref.contains("no_ads"))
            navigationView.getMenu().findItem(R.id.nav_inapp).setVisible(false);

        if (clientConfig.max_percent_ads == 100)
            requestBannerAds();

    }

    private void upgradePremium() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        if (clientConfig.isAccept != 0) {
            builder.setTitle(R.string.title_dialog_purchase)
                    .setMessage(R.string.message_dialog_purchase)
                    .setPositiveButton("PURCHASE", (dialogInterface, i) -> {
                        PurchaseUtils.getSharedInstance().purchaseItem(this, 0, new PurchaseListener() {
                            @Override
                            public void purchaseFailed(int item) {

                            }

                            @Override
                            public void purchaseSuccess(int item) {
                                editor.putInt("no_ads", 1).apply();
                                if (clientConfig.isAccept == 1)
                                    editor.putInt("youtube", 1).apply();

                                Toast.makeText(MainActivity.this, R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void purchaseCancel(int item) {

                            }
                        });

                    })
                    .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                    });
        } else {
            builder.setTitle(R.string.title_dialog_purchase)
                    .setMessage(R.string.message_dialog_purchase2)
                    .setPositiveButton("PURCHASE", (dialogInterface, i) -> {
                        PurchaseUtils.getSharedInstance().purchaseItem(this, 0, new PurchaseListener() {
                            @Override
                            public void purchaseFailed(int item) {

                            }

                            @Override
                            public void purchaseSuccess(int item) {
                                editor.putInt("no_ads", 1).apply();


                                Toast.makeText(MainActivity.this, R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void purchaseCancel(int item) {

                            }
                        });

                    })
                    .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                    });
        }
        builder.show();
    }


    private void requestBannerAds() {
        if (clientConfig != null && clientConfig.max_percent_ads != 0 &&  mPref.getInt("no_ads", 0) == 0 ) {
            AdView adView = new AdView(this);
            adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
            adView.setAdUnitId(clientConfig.BANNER_ADMOB_ID);
            adContainer.addView(adView);
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                switch (navigation_item) {
                    case R.id.nav_yt:
                        Intent intent = new Intent(MainActivity.this, YoutubeActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_settings:
                        Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                        editor.putInt("isAccept", clientConfig.isAccept).apply();
                        startActivity(intent1);
                        break;
                    case R.id.nav_inapp:
                        upgradePremium();
                        break;

//                    case R.id.nav_download:
//                        ClientConfig clientConfig = InterstitialUtils.getSharedInstance().getClient();
//                        if (clientConfig.isAccept != 0)
//                            startActivity(new Intent(MainActivity.this, DownloadActivity.class));
//                        break;
                }

                navigation_item = 1;
            }
        };


        toggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(toggle);

        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.nav_yt:
                    navigation_item = R.id.nav_yt;
                    break;

                case R.id.nav_settings:
                    navigation_item = R.id.nav_settings;
                    break;

                case R.id.nav_insta:
                    navigation_item = R.id.nav_insta;
                    break;

                case R.id.nav_inapp:
                    navigation_item = R.id.nav_inapp;
                    break;

//                case R.id.nav_download:
//                    navigation_item = R.id.nav_download;
//                    break;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

   /* private void upgradePremium() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_purchase)
                .setMessage(R.string.message_dialog_purchase)
                .setPositiveButton("PURCHASE", (dialogInterface, i) -> {
                    PurchaseUtils.getSharedInstance().purchaseItem(this, 0, new PurchaseListener() {
                        @Override
                        public void purchaseFailed(int item) {

                        }

                        @Override
                        public void purchaseSuccess(int item) {
                            editor.putInt("no_ads", 1).apply();
                            if (clientConfig.isAccept == 1)
                                editor.putInt("youtube", 1).apply();

                            Toast.makeText(MainActivity.this, R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void purchaseCancel(int item) {

                        }
                    });
                })
                .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                });

        builder.show();
    }
*/
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        // To prevent a memory leak on rotation, make sure to call stopAutoCycle() on the slider before activity or fragment is destroyed
        mDemoSlider.stopAutoCycle();
        super.onStop();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.insta_app:
                String app_package = "com.instagram.android";

                boolean is_installed = appInstalledOrNot(app_package);

                if (is_installed) {
                    dialog_insta();
                } else {
                    Toast.makeText(this, "You have not installed Instagram", Toast.LENGTH_LONG).show();
                }

                return true;
        }
        return true;
    }

    private void dialog_insta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.open_insta)
                .setMessage(R.string.msg_open_insta)
                .setPositiveButton("YES", (dialogInterface, i) -> {
                    try {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "You have not installed Instagram", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("NO", (dialogInterface, i) -> {
                });

        builder.show();
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onSliderClick(BaseSliderView slider) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        for (int i = 0; i < videoposition.size(); i++) {

            if (position == Integer.parseInt(videoposition.get(i))) {
                this.posisi = position;
                this.fixposition = i;
            }
        }

        if (position == posisi) {
            imgPlay.setVisibility(View.VISIBLE);

        } else {
            this.photoposition = position;
            imgPlay.setVisibility(View.GONE);
        }

        if ((previousPosition == SLIDER_COUNT) && (SLIDER_COUNT == 0)) {
            page_number.setVisibility(View.GONE);
            return;
        }

        //disable swipe at first or last slide
        //swipe left to right
        if (previousPosition == SLIDER_COUNT && position == 0) {
            if (SLIDER_COUNT != 1) {
                mDemoSlider.movePrevPosition();
                return;
            }
        }
        // swipe right to left
        else if (previousPosition == 0 && position == SLIDER_COUNT) {
            if (position != 1) {
                mDemoSlider.moveNextPosition();
                return;
            }
        }
        previousPosition = position;
        String text = String.format(getResources().getString(R.string.page_number), position + 1, SLIDER_COUNT + 1);
        page_number.setText(text);
        if (!(videolink.size() > 0) || SLIDER_COUNT == 1)
            page_number.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void getSave() {

        File direct = new File(Environment.getExternalStorageDirectory()
                + "/InstagramDownloader");

        boolean isExists = direct.exists();
        if (!isExists) {
            isExists = direct.mkdirs();
        }

        if (isExists) {


            DownloadManager mgr = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

            Uri downloadUri = null;
            if (imgPlay.getVisibility() == View.VISIBLE) {
                if (fixposition < videolink.size()) {
                    downloadUri = Uri.parse(videolink.get(fixposition));
                    filename = videolink.get(fixposition).substring(videolink.get(fixposition).lastIndexOf("/") + 1);
                }

            } else {
                if (photoposition < photolink.size()) {
                    downloadUri = Uri.parse(photolink.get(photoposition));
                    filename = photolink.get(photoposition).substring(photolink.get(photoposition).lastIndexOf("/") + 1);
                }

            }

            if (filename != null && filename.indexOf('?') > 0) {
                filename = filename.substring(0, filename.lastIndexOf('?'));

            }
            if (downloadUri != null) {
                DownloadManager.Request request = new DownloadManager.Request(
                        downloadUri);
                request.setAllowedNetworkTypes(
                        DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false).setTitle("Instagram Downloader Pro")
                        .setDescription("Downloading...")
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS
                                + "/InstagramDownloader/", filename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                Objects.requireNonNull(mgr).enqueue(request);
            }
        }
    }

    public void getMedia() {

        Uri uri = Uri.parse(videolink.get(fixposition));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/mp4");
        startActivity(intent);

    }

    public void getHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Help")
                .setMessage("1. Select post what you want\n2. Click menu on the right side\n3." +
                        " Select copy link\n4. Return to app\n5. Click Paste\n6. Submit URL")
                .setPositiveButton("OK", null)
                .show();
    }

    public void getRepost() {

        if (imgPlay.getVisibility() == View.VISIBLE) {

            filename = videolink.get(fixposition).substring(videolink.get(fixposition).lastIndexOf("/") + 1);

        } else {

            filename = photolink.get(photoposition).substring(photolink.get(photoposition).lastIndexOf("/") + 1);

        }

        if (filename.indexOf('?') > 0) {
            filename = filename.substring(0, filename.lastIndexOf('?'));

        }

        File direct = new File(Environment.getExternalStorageDirectory()
                + "/InstagramDownloader/" + filename);

        if (direct.exists() || check_download > 0) {
            check_download = 0;
            String type = URLConnection.guessContentTypeFromName(filename);
            String mediaPath = Environment.getExternalStorageDirectory() + "/InstagramDownloader/" + filename;
            Intent share = new Intent(Intent.ACTION_SEND);
            File file = new File(mediaPath);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkURI = FileProvider.getUriForFile(MainActivity.this,
                        MainActivity.this.getPackageName() + ".provider", file);
                share.setType(type);
                share.putExtra(Intent.EXTRA_STREAM, apkURI);

                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                share.setType(type);
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            }
            share.setPackage("com.instagram.android");
            startActivity(Intent.createChooser(share, "Share to"));

        } else {
            check_download++;
            getSave();
            getRepost();
        }
    }

    public boolean checkPermission() {
        if ((ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            return true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, permission);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == permission) {
            boolean permissionGranted = grantResults.length > 0;

            for (int result : grantResults) {
                permissionGranted &= result == PackageManager.PERMISSION_GRANTED;
            }

            if (!permissionGranted) {
                Toast.makeText(this, getResources().getString(R.string.grant_permission), Toast.LENGTH_SHORT).show();
                finish();
                Log.e("Permission Denied", "True");
            }
        }

    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        switch (v.getId()) {
            case R.id.btnSubmit:
                if (!btnSubmit.getText().toString().equalsIgnoreCase("How To Use")) {
                    relative_layout.setVisibility(View.VISIBLE);
                    adContainer.setVisibility(View.GONE);
                }
                checkUrl();

                break;
            case R.id.btnSave:
                if (checkPermission()) {
                    getSave();
//                    rate = mPref.getBoolean("rate", false);
//                    if (rate) {
//                        InterstitialUtils.getSharedInstance().showInterstitialAds(null);
//                    } else {
//                        ratingDialog.show();
//                    }

                    Toast.makeText(this, "Download successfully file: " + filename, Toast.LENGTH_SHORT).show();
                    FireBaseUtils.getSharedInstance().logEventDownload("INSTA");
                }

                break;
            case R.id.imgPlay:
                getMedia();
                break;
            case R.id.btnPaste:
                page_number.setVisibility(View.GONE);
                relative_layout.setVisibility(View.GONE);
                if (adContainer.getVisibility() != View.VISIBLE)
                    adContainer.setVisibility(View.VISIBLE);
                ClipData clipData1 = Objects.requireNonNull(clipboardManager).getPrimaryClip();
                if (clipData1 != null && clipboardManager.getPrimaryClip() != null && clipData1.getItemCount() > 0) {
                    ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                    if (edtURL == null || edtURL.getText() == null) {
                        Toast.makeText(this, "Please insert link to download", Toast.LENGTH_SHORT).show();
                    } else if (item != null && item.getText() != null)
                        edtURL.setText(item.getText().toString());
                }
                break;
            case R.id.btnRepost:
                if (checkPermission())
                    getRepost();
                break;
            case R.id.btnCopy:
                ClipData clipData = ClipData.newPlainText("", repost + edtCaption.getText());
                Objects.requireNonNull(clipboardManager).setPrimaryClip(clipData);
                Toast.makeText(this, "Caption has been copied", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void checkUrl() {
        if (edtURL == null || edtURL.getText() == null) {
            Toast.makeText(this, "Please insert link to download", Toast.LENGTH_SHORT).show();
        } else {
            link = edtURL.getText().toString().trim();

            if (TextUtils.isEmpty(link)) {
                getHelp();
            } else {
                try {

                    String json = Jsoup.connect("https://api.instagram.com/oembed/?url=" + link).ignoreContentType(true).execute().body();
                    JSONObject reader = new JSONObject(json);
                    String title = reader.getString("title");
                    String author = reader.getString("author_name");
                    model.setTitle('\n' + title);
                    model.setAuthor('@' + author);

                    new Async().execute();

                } catch (IOException | JSONException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Error")
                            .setMessage("Error, be sure url correct")
                            .setCancelable(false)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class Async extends AsyncTask<String, String, Void> {

        @Override
        protected void onPostExecute(Void result) {

            SLIDER_COUNT = url_maps.size() - 1;
            for (String name : url_maps.keySet()) {
                try {
                    String[] data = url_maps.get(name).split("batas");
                    if (data.length > 1) {
                        videoposition.add(name);
                        videolink.add(data[1]);


                        if (videoposition.size() > 0)
                            if (0 == Integer.parseInt(videoposition.get(0))) {
                                posisi = 0;
                                fixposition = 0;
                            }


                        if (0 == posisi) {
                            imgPlay.setVisibility(View.VISIBLE);

                        } else {
                            photoposition = 0;
                            imgPlay.setVisibility(View.GONE);
                        }
                        Log.d(TAG, "total_video" + videolink.size());
                    }


                    textSliderView = new DefaultSliderView(MainActivity.this);
                    textSliderView
                            .image(data[0])
                            .setOnSliderClickListener(MainActivity.this);
                    photolink.add(data[0]);
                    mDemoSlider.addSlider(textSliderView);
                    if (photolink.size() > 0)
                        Log.d(TAG, "total_photo" + photolink.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mDemoSlider.stopAutoCycle();
            mDemoSlider.addOnPageChangeListener(MainActivity.this);
            edtCaption.setText(model.getTitle());
            edtCaption.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);
            btnRepost.setVisibility(View.VISIBLE);
            btnCopy.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);

//            showInterstitial();
        }


        @Override
        protected Void doInBackground(String... params) {
            String url2 = "";

            try {

                Connection con = Jsoup.connect(link);
                Document doc = con.get();
                repost = "Repost from " + model.getAuthor() + model.getTitle();

                Elements meta = doc.getElementsByTag("script");
                String data = meta.toString().substring(meta.toString().indexOf("window._sharedData") + 21);
                JSONObject reader = new JSONObject(data);
                JSONObject jsonObject = new JSONObject(reader.getString("entry_data"));
                JSONArray jsonArray = jsonObject.getJSONArray("PostPage");
                JSONObject object = jsonArray.getJSONObject(0);
                JSONObject object1 = object.getJSONObject("graphql");
                JSONObject object2 = object1.getJSONObject("shortcode_media");

                if (object2.isNull("edge_sidecar_to_children")) {
                    if (object2.getBoolean("is_video")) {

                        url2 = object2.getString("video_url");
                    }

                    url_maps.put("0", object2.getString("display_url") + "batas" + url2);

                } else {

                    JSONObject object3 = object2.getJSONObject("edge_sidecar_to_children");
                    JSONArray jsonArray1 = object3.getJSONArray("edges");
                    for (int i = 0; i < jsonArray1.length(); i++) {
                        JSONObject zero = jsonArray1.getJSONObject(i);
                        JSONObject node = zero.getJSONObject("node");
                        Boolean cek = (Boolean) node.getBoolean("is_video");
                        if (cek) {

                            url2 = node.getString("video_url");
                        }
                        String display = node.getString("display_url");
                        url_maps.put(String.valueOf(i), display + "batas" + url2);
                    }

                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPreExecute() {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(btnSubmit.getWindowToken(), 0);
            btnSubmit.setEnabled(false);

            btnSave.setVisibility(View.GONE);
            btnCopy.setVisibility(View.GONE);
            btnRepost.setVisibility(View.GONE);
            edtCaption.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            imgView.setVisibility(View.GONE);
            imgPlay.setVisibility(View.GONE);

            posisi = -1;
            mDemoSlider.removeAllSliders();

            url_maps.clear();
            videolink.clear();
            photolink.clear();
            videoposition.clear();

            textSliderView = null;
            if (edtURL != null && edtURL.getText() != null) {
                link = edtURL.getText().toString().trim();
            }

        }

    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }


}
