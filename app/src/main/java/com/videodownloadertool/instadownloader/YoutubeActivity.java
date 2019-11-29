package com.videodownloadertool.instadownloader;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.videodownloadertool.instadownloader.mutils.AdCloseListener;
import com.videodownloadertool.instadownloader.mutils.AdRewardListener;
import com.videodownloadertool.instadownloader.mutils.AppConstants;
import com.videodownloadertool.instadownloader.mutils.InterstitialUtils;
import com.videodownloadertool.instadownloader.mutils.PurchaseListener;
import com.videodownloadertool.instadownloader.mutils.PurchaseUtils;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import us.shandian.giga.ui.DownloadActivity;
import us.shandian.giga.ui.DownloadDialog;
import us.shandian.giga.util.ACCEPT_DOWNLOAD;
import us.shandian.giga.util.ExtractorHelper;
import us.shandian.giga.util.ListHelper;

@SuppressLint("SetJavaScriptEnabled")
public class YoutubeActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.webview)
    WebView webView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.fab_download)
    FloatingActionButton fab_download;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private String url_download;
    private SearchView searchView;
    private SharedPreferences.Editor editor;
    private SharedPreferences mPrefs;

    @State
    protected int isAccept = 1;
    private Dialog dialog;
    private ProgressDialog progressDialog;

    protected Boolean isPendingShowDownload = false;
    protected StreamInfo result;
    protected ACCEPT_DOWNLOAD accept_download = ACCEPT_DOWNLOAD.FULL;

    private WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            super.onProgressChanged(view, newProgress);
        }
    };

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            view.loadUrl(url);
            Log.d("tuanvn", "shouldOverrideUrlLoading");
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            Log.d("tuanvn", "onLoadResource");

            url_download = view.getUrl();
            String temp = url_download.toLowerCase();

            if ((temp.contains("youtu.be") && temp.contains("?v="))
                    || (temp.contains("youtube.com") && temp.contains("?v="))) {
                fab_download.show();
            } else
                fab_download.hide();
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            super.onPageFinished(view, url);
            Log.d("tuanvn", "onPageFinished");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        Icepick.restoreInstanceState(this, savedInstanceState);

        ButterKnife.bind(this);
        mPrefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();
        if (InterstitialUtils.getSharedInstance().getClient() != null)
            isAccept = InterstitialUtils.getSharedInstance().getClient().isAccept;

        initWebView();

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setTitle(getResources().getString(R.string.youtube));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        if (toolbar.getOverflowIcon() != null)
            toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fab_download.setOnClickListener(this);

        addBannerAds();
    }

    private void addBannerAds() {
        if (mPrefs.getInt("no_ads", 0) == 0) {
            RelativeLayout bannerView = (RelativeLayout) findViewById(R.id.adView);

            AdView adView = new AdView(this);
            adView.setAdSize(getAdSize());
            if (MainActivity.DEBUG)
                adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
            else {
                adView.setAdUnitId(new String(Base64.decode(AppConstants.ID_4, Base64.DEFAULT)));
            }
            bannerView.addView(adView);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    private AdSize getAdSize() {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    private void upgradePremium() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        if (isAccept != 0) {
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
                                if (isAccept == 1)
                                    editor.putInt("youtube", 1).apply();

                                Toast.makeText(YoutubeActivity.this, R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
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


                                Toast.makeText(YoutubeActivity.this, R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_yt, menu);

        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            }
        }

        MenuItem item_search = menu.findItem(R.id.searchBar);
        MenuItem item_dl = menu.findItem(R.id.action_dl);
        MenuItem item_purchase = menu.findItem(R.id.action_purchase);

        searchView = (SearchView) item_search.getActionView();

        //set icon search color
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(YoutubeActivity.this, R.drawable.ic_action_action_search));

        //set hint color
//        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
//        searchEditText.setTextColor(getResources().getColor(R.color.white));
//        searchEditText.setHintTextColor(getResources().getColor(R.color.white));

        searchView.setQueryHint(getResources().getString(R.string.search_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadWebView(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        //check to show purchase tab or not
        if (mPrefs.contains("no_ads")) {
            item_purchase.setVisible(false);
        }


//        if (clientConfig.isAccept != 0) {
//            item_dl.setVisible(true);
//        } else {
//            item_dl.setVisible(false);
//        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_refresh:
                if (url_download != null && !url_download.equalsIgnoreCase("")) {
                    loadWebView(url_download);
                }
                break;
            case R.id.action_purchase:
                upgradePremium();
                break;
            case android.R.id.home:
                if (checkFocusRec(searchView)) {
                    searchView.clearFocus();
                    searchView.onActionViewCollapsed();
                } else
                    super.onBackPressed();

                break;

            case R.id.action_dl:
                startActivity(new Intent(YoutubeActivity.this, DownloadActivity.class));
                break;
        }

        return true;
    }

    private boolean checkFocusRec(View view) {
        if (view.isFocused())
            return true;

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (checkFocusRec(viewGroup.getChildAt(i)))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else
            super.onBackPressed();
    }

    private void loadWebView(String content) {
        if (content.length() > 0) {
            if (content.toLowerCase().contains("youtu.be") || content.toLowerCase().contains("youtube.com")) {
                url_download = content;
            } else {
                url_download = "https://www.youtube.com/results?search_query=" + Uri.encode(content);
            }

            webView.loadUrl(url_download);

        } else {
            Toast.makeText(this, "invalid url.", Toast.LENGTH_SHORT).show();

        }

    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(webChromeClient);
        webView.loadUrl("https://m.youtube.com/");

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_download:
                progressDialog = new ProgressDialog(view.getContext(), R.style.AppCompatProgressDialogStyle);
                progressDialog.setMessage("Loading...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);

                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();


                checkLinkDownload(url_download);

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (isPendingShowDownload) {
            isPendingShowDownload = false;
            showDirectDialogDownload();
        }
    }

    private void showDirectDialogDownload() {
        if (result == null) {
            Toast.makeText(this, R.string.link_error, Toast.LENGTH_SHORT).show();
            return;
        }
        List<VideoStream> sortedVideoStreams = ListHelper.getSortedStreamVideosList(this,
                result.getVideoStreams(),
                result.getVideoOnlyStreams(),
                false);
        int selectedVideoStreamIndex = ListHelper.getDefaultResolutionIndex(this,
                sortedVideoStreams);

        FragmentManager fm = getSupportFragmentManager();
        DownloadDialog downloadDialog = DownloadDialog.newInstance(result);
        downloadDialog.setVideoStreams(sortedVideoStreams);
        downloadDialog.setAudioStreams(result.getAudioStreams());
        downloadDialog.setSelectedVideoStream(selectedVideoStreamIndex);

        downloadDialog.setAcceptDownload(accept_download);
        downloadDialog.show(fm, "downloadDialog");
        fm.beginTransaction().commitAllowingStateLoss();
    }

    private void showDownloadPlanDialog() {


        if (mPrefs.getInt("youtube", 0) == 1) {
            showDirectDialogDownload();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert);

            View root = getLayoutInflater().inflate(R.layout.layout_fab_download_dialog, null);
            builder.setTitle(R.string.title_dl_plan_dialog)
                    .setView(root)
                    .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                    })
                    .setCancelable(false);

            Button btn_dl_audio_free = root.findViewById(R.id.btn_dl_audio_free);
            Button btn_dl_low_quality = root.findViewById(R.id.btn_dl_low_quality);
            Button btn_purchase_dl = root.findViewById(R.id.btn_purchase_dl);

            int download_trial = mPrefs.getInt("download_trial", 0);
            if (download_trial == 1) {
                btn_purchase_dl.setText(getResources().getString(R.string.purchase));
            } else {
                btn_purchase_dl.setText(getResources().getString(R.string.trial));
            }

            btn_dl_audio_free.setOnClickListener(view -> {
                accept_download = ACCEPT_DOWNLOAD.ONLY_AUDIO;
                InterstitialUtils.getSharedInstance().showInterstitialAds(new AdCloseListener() {
                    @Override
                    public void onAdClose() {
                        if (dialog != null)
                            dialog.dismiss();
                        isPendingShowDownload = true;
                    }

                    @Override
                    public void onNoAd() {
                        if (dialog != null)
                            dialog.dismiss();
                        showDirectDialogDownload();
                    }
                });


            });

            btn_dl_low_quality.setOnClickListener(view -> {
                accept_download = ACCEPT_DOWNLOAD.LOW_VIDEO;
                btn_dl_low_quality.setEnabled(false);
                InterstitialUtils.getSharedInstance().showRewardVideoAds(new AdRewardListener() {
                    @Override
                    public void onRewarded() {
                        if (dialog != null)
                            dialog.dismiss();
                        isPendingShowDownload = true;
                    }

                    @Override
                    public void onAdNotAvailable() {
                        if (dialog != null)
                            dialog.dismiss();
                        showDirectDialogDownload();
                        Toast.makeText(YoutubeActivity.this, R.string.reward_video_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });

            });

            btn_purchase_dl.setOnClickListener(view -> {
                accept_download = ACCEPT_DOWNLOAD.FULL;
                if (download_trial == 1) {
                    PurchaseUtils.getSharedInstance().purchaseItem(YoutubeActivity.this, 0, new PurchaseListener() {
                        @Override
                        public void purchaseFailed(int item) {

                        }

                        @Override
                        public void purchaseSuccess(int item) {
                            if (dialog != null)
                                dialog.dismiss();


                            editor.putInt("no_ads", 1).apply();

                            if (isAccept == 1)
                                editor.putInt("youtube", 1).apply();

                            Toast.makeText(view.getContext(), R.string.restart_take_effect, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void purchaseCancel(int item) {

                        }
                    });
                } else {
                    if (dialog != null)
                        dialog.dismiss();

                    showDirectDialogDownload();
                }
            });
            dialog = builder.create();
            dialog.show();
        }
    }


    private void checkLinkDownload(String url) {

        String urlExtra = url;
        if (url.contains("?list")) {
            if (url.contains("&v=")) {
                String idextra = url.split("&v=")[1];
                if (idextra.contains("&")) {
                    urlExtra = "https://m.youtube.com/watch?v=" + idextra.split("&")[0];
                } else {
                    urlExtra = "https://m.youtube.com/watch?v=" + idextra;
                }
            }

        } else if (url.contains("?v=")) {
            urlExtra = url.split("&")[0];
        }
        //https://youtu.be/7kRueqzOb6g
        Log.d("adsdk", "----- " + urlExtra);

        Disposable disposable = ExtractorHelper.getStreamInfo(0, urlExtra, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((@NonNull StreamInfo result) -> {
                    this.result = result;
                    if (!YoutubeActivity.this.isDestroyed() && progressDialog.isShowing())
                        progressDialog.dismiss();

                    showDownloadPlanDialog();
                }, (@NonNull Throwable throwable) -> {
                    Log.d("adsdk", "errror");
                    if (!YoutubeActivity.this.isDestroyed() && progressDialog.isShowing())
                        progressDialog.dismiss();

                    Toast.makeText(this, R.string.link_error, Toast.LENGTH_SHORT).show();
                });

    }


}