package com.videodownloadertool.instadownloader;

import java.util.HashMap;
import java.util.List;

public class ExpandableListDataPump {
    private static List<String> audios;
    private static List<String> videos;

    public static void setData(List<String> audio, List<String> video) {
        audios = audio;
        videos = video;
    }


    public static HashMap<String, List<String>> getData() {
        HashMap<String, List<String>> expandableListDetail = new HashMap<>();

        expandableListDetail.put("Audio file", audios);
        expandableListDetail.put("Videos file", videos);
        return expandableListDetail;
    }
}
