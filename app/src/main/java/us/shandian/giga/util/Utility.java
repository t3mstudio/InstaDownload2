package us.shandian.giga.util;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.videodownloadertool.instadownloader.R;
import com.videodownloadertool.instadownloader.mutils.AppConstants;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import us.shandian.giga.get.DownloaderImpl;
import us.shandian.giga.io.StoredFileHelper;
import us.shandian.giga.streams.io.SharpStream;

public class Utility {

    public static final String VIDEO_AUDIO_PATH = "YouTubeFiles";

    public static void initDownloadSetting(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            locale = context.getResources().getConfiguration().locale;
        }
        NewPipe.init(getDownloader(), new Localization(locale.getCountry(), locale.getLanguage()));

        initStorageVideo(context);
        initStorageAudio(context);
        initNotificationChannel(context);
        configureRxJavaErrorHandler();
    }

    protected static boolean isDisposedRxExceptionsReported() {
        return false;
    }

    private static void configureRxJavaErrorHandler() {
        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(@io.reactivex.annotations.NonNull Throwable throwable) {
                Log.e(AppConstants.log_tag, "RxJavaPlugins.ErrorHandler called with -> : " +
                        "throwable = [" + throwable.getClass().getName() + "]");

                if (throwable instanceof UndeliverableException) {
                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                    throwable = throwable.getCause();
                }

                final List<Throwable> errors;
                if (throwable instanceof CompositeException) {
                    errors = ((CompositeException) throwable).getExceptions();
                } else {
                    errors = Collections.singletonList(throwable);
                }

                for (final Throwable error : errors) {
                    if (isThrowableIgnored(error)) return;
                    if (isThrowableCritical(error)) {
                        reportException(error);
                        return;
                    }
                }

                // Out-of-lifecycle exceptions should only be reported if a debug user wishes so,
                // When exception is not reported, log it
                if (isDisposedRxExceptionsReported()) {
                    reportException(throwable);
                } else {
                    Log.e(AppConstants.log_tag, "RxJavaPlugin: Undeliverable Exception received: ", throwable);
                }
            }

            private boolean isThrowableIgnored(@io.reactivex.annotations.NonNull final Throwable throwable) {
                // Don't crash the application over a simple network problem
                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
                        IOException.class, SocketException.class, // network api cancellation
                        InterruptedException.class, InterruptedIOException.class); // blocking code disposed
            }

            private boolean isThrowableCritical(@io.reactivex.annotations.NonNull final Throwable throwable) {
                // Though these exceptions cannot be ignored
                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
                        NullPointerException.class, IllegalArgumentException.class, // bug in app
                        OnErrorNotImplementedException.class, MissingBackpressureException.class,
                        IllegalStateException.class); // bug in operator
            }

            private void reportException(@io.reactivex.annotations.NonNull final Throwable throwable) {
                // Throw uncaught exception that will trigger the report system
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), throwable);
            }
        });
    }

    public static String getVideoAudioStoragePath(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = context.getString(R.string.download_path_video_key);
        return prefs.getString(key, "null");
    }

    private static void initStorageVideo(Context context) {
        initStorageVideoAudio(context, R.string.download_path_video_key, Environment.DIRECTORY_DOWNLOADS);
    }

    private static void initStorageAudio(Context context) {
        initStorageVideoAudio(context, R.string.download_path_audio_key, Environment.DIRECTORY_DOWNLOADS);
    }

    private static void initStorageVideoAudio(Context context, int keyID, String defaultDirectoryName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = context.getString(keyID);
        String downloadPath = prefs.getString(key, null);
        if ((downloadPath != null) && (!downloadPath.isEmpty())) return;

        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString(key, new File(getDir(defaultDirectoryName), VIDEO_AUDIO_PATH).toURI().toString());
        spEditor.apply();
    }

    private static File getDir(String defaultDirectoryName) {
        return new File(Environment.getExternalStorageDirectory(), defaultDirectoryName);
    }

    private static DownloaderImpl getDownloader() {
        return DownloaderImpl.init(null);
    }

    private static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        final String id = context.getString(R.string.notification_channel_id);
        final CharSequence name = context.getString(R.string.notification_channel_name);
        final String description = context.getString(R.string.notification_channel_description);

        // Keep this below DEFAULT to avoid making noise on every notification update
        final int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(mChannel);

        setUpUpdateNotificationChannel(importance, context);
    }

    /**
     * Set up notification channel for app update.
     *
     * @param importance
     */
    @TargetApi(Build.VERSION_CODES.O)
    private static void setUpUpdateNotificationChannel(int importance, Context context) {

        final String appUpdateId
                = context.getString(R.string.app_update_notification_channel_id);
        final CharSequence appUpdateName
                = context.getString(R.string.app_update_notification_channel_name);
        final String appUpdateDescription
                = context.getString(R.string.app_update_notification_channel_description);

        NotificationChannel appUpdateChannel
                = new NotificationChannel(appUpdateId, appUpdateName, importance);
        appUpdateChannel.setDescription(appUpdateDescription);

        NotificationManager appUpdateNotificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        appUpdateNotificationManager.createNotificationChannel(appUpdateChannel);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return String.format("%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f kB", bytes / 1024d);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / 1024d / 1024d);
        } else {
            return String.format("%.2f GB", bytes / 1024d / 1024d / 1024d);
        }
    }

    public static String formatSpeed(float speed) {
        if (speed < 1024) {
            return String.format("%.2f B/s", speed);
        } else if (speed < 1024 * 1024) {
            return String.format("%.2f kB/s", speed / 1024);
        } else if (speed < 1024 * 1024 * 1024) {
            return String.format("%.2f MB/s", speed / 1024 / 1024);
        } else {
            return String.format("%.2f GB/s", speed / 1024 / 1024 / 1024);
        }
    }

    public static void writeToFile(@NonNull File file, @NonNull Serializable serializable) {

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            objectOutputStream.writeObject(serializable);
        } catch (Exception e) {
            //nothing to do
        }
        //nothing to do
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T readFromFile(File file) {
        T object;
        ObjectInputStream objectInputStream = null;

        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            object = (T) objectInputStream.readObject();
        } catch (Exception e) {
            Log.e("Utility", "Failed to deserialize the object", e);
            object = null;
        }

        if (objectInputStream != null) {
            try {
                objectInputStream.close();
            } catch (Exception e) {
                //nothing to do
            }
        }

        return object;
    }

    @Nullable
    public static String getFileExt(String url) {
        int index;
        if ((index = url.indexOf("?")) > -1) {
            url = url.substring(0, index);
        }

        index = url.lastIndexOf(".");
        if (index == -1) {
            return null;
        } else {
            String ext = url.substring(index);
            if ((index = ext.indexOf("%")) > -1) {
                ext = ext.substring(0, index);
            }
            if ((index = ext.indexOf("/")) > -1) {
                ext = ext.substring(0, index);
            }
            return ext.toLowerCase();
        }
    }

    public static FileType getFileType(char kind, String file) {
        switch (kind) {
            case 'v':
                return FileType.VIDEO;
            case 'a':
                return FileType.MUSIC;
            case 's':
                return FileType.SUBTITLE;
            //default '?':
        }

        if (file.endsWith(".srt") || file.endsWith(".vtt") || file.endsWith(".ssa")) {
            return FileType.SUBTITLE;
        } else if (file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".flac") || file.endsWith(".m4a") || file.endsWith(".opus")) {
            return FileType.MUSIC;
        } else if (file.endsWith(".mp4") || file.endsWith(".mpeg") || file.endsWith(".rm") || file.endsWith(".rmvb")
                || file.endsWith(".flv") || file.endsWith(".webp") || file.endsWith(".webm")) {
            return FileType.VIDEO;
        }

        return FileType.UNKNOWN;
    }

    @ColorInt
    public static int getBackgroundForFileType(Context ctx, FileType type) {
        int colorRes = R.color.background_download_item;
//        switch (type) {
//            case MUSIC:
//                colorRes = R.color.audio_left_to_load_color;
//                break;
//            case VIDEO:
//                colorRes = R.color.video_left_to_load_color;
//                break;
//            case SUBTITLE:
//                colorRes = R.color.subtitle_left_to_load_color;
//                break;
//            default:
//                colorRes = R.color.gray;
//        }

        return ContextCompat.getColor(ctx, colorRes);
    }

    @ColorInt
    public static int getForegroundForFileType(Context ctx, FileType type) {
        int colorRes = R.color.forground_download_item;
//        switch (type) {
//            case MUSIC:
//                colorRes = R.color.audio_already_load_color;
//                break;
//            case VIDEO:
//                colorRes = R.color.video_already_load_color;
//                break;
//            case SUBTITLE:
//                colorRes = R.color.subtitle_already_load_color;
//                break;
//            default:
//                colorRes = R.color.gray;
//                break;
//        }

        return ContextCompat.getColor(ctx, colorRes);
    }

    @DrawableRes
    public static int getIconForFileType(FileType type) {
        switch (type) {
            case MUSIC:
                return R.drawable.music;
            case VIDEO:
                return R.drawable.video;
//            case SUBTITLE:
//                return R.drawable.subtitle;
            default:
                return R.drawable.video;
        }
    }

    public static String checksum(StoredFileHelper source, String algorithm) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        SharpStream i;

        try {
            i = source.getStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] buf = new byte[1024];
        int len;

        try {
            while ((len = i.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
        } catch (IOException e) {
            // nothing to do
        }

        byte[] digest = md.digest();

        // HEX
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }
//
//    public static void copyToClipboard(Context context, String str) {
//        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
//
//        if (cm == null) {
//            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        cm.setPrimaryClip(ClipData.newPlainText("text", str));
//        Toast.makeText(context, R.string.msg_copied, Toast.LENGTH_SHORT).show();
//    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean mkdir(File p, boolean allDirs) {
        if (p != null && p.exists()) return true;

        if (allDirs)
            p.mkdirs();
        else
            p.mkdir();

        return p.exists();
    }

    public static long getContentLength(HttpURLConnection connection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return connection.getContentLengthLong();
        }

        try {
            return Long.parseLong(connection.getHeaderField("Content-Length"));
        } catch (Exception err) {
            // nothing to do
        }

        return -1;
    }

    public enum FileType {
        VIDEO,
        MUSIC,
        SUBTITLE,
        UNKNOWN
    }
}
