package com.videodownloadertool.instadownloader.mutils;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Muicv on 8/17/2018.
 */

public class ClientConfig {

    @SerializedName("isAccept")
    public int isAccept;
    @SerializedName("percentRate")
    public int percentRate;

    @SerializedName("max_percent_ads")
    public int max_percent_ads;
    @SerializedName("fb_percent_ads")
    public int fb_percent_ads;


    @SerializedName("delay_show_ads")
    public int delay_show_ads;

    @SerializedName("isGoogleIp")
    public int isGoogleIp;


    @SerializedName("full_fb_id")
    public String FULL_FB_ID;
    @SerializedName("full_admob_id")
    public String FULL_ADMOB_ID;

    @SerializedName("banner_fb_id")
    public String BANNER_FB_ID;
    @SerializedName("banner_admob_id")
    public String BANNER_ADMOB_ID;
    @SerializedName("reward_admob_id")
    public String REWARD_ADMOB_ID;

}
