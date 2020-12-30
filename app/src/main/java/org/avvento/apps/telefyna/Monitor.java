package org.avvento.apps.telefyna;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.scheduler.Maintenance;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import lombok.Getter;

public class Monitor extends AppCompatActivity implements PlayerNotificationManager.NotificationListener, Player.EventListener {
    public static final String PREFERENCES = "TelefynaPrefs" ;
    private static final String PLAYLIST_PLAY = "PLAYLIST_PLAY";
    private static final String PLAYLIST_LAST_MODIFIED = "PLAYLIST_LAST_MODIFIED";
    private static final String PLAYLIST_SEEK_TO = "PLAYLIST_SEEK_TO";
    private static final String PLAYLIST_PLAY_FORMAT = "%s-%d";
    private SharedPreferences sharedpreferences;
    public static Monitor instance;
    @Getter
    private Config configuration;
    @Getter
    private AlarmManager alarmManager;
    @Getter
    private Handler handler;
    private Maintenance maintenance;
    private int nowPlayingIndex;
    @Getter
    private SimpleExoPlayer player;
    private PlayerView playerView;
    @Getter
    private Map<Integer, List<MediaItem>> playout;
    private File programsFolder;

    public void putPlayout(Integer index, List<MediaItem> mediaItems) {
        playout.put(index, mediaItems);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void switchToDefault() {
        switchNow(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void switchToSecondDefault() {
        switchNow(1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void switchNow(int index) {
        List<MediaItem> mediaItems = playout.get(index);
        if(mediaItems.isEmpty()) {
            switchToDefault();
        }
        Playlist playlist = getConfiguration().getPlaylists()[index];
        if(playlist.isClone()) {
            index = playlist.getClone() - 1;
            playlist = getConfiguration().getPlaylists()[index];
        }
        if(Playlist.Type.LOCAL.equals(playlist.getType())) {
            if (playlist.isResuming()) {
                int lastPlayedMediaIndex = getSharedPlaylistKey(index);
                if (lastPlayedMediaIndex > 0 && lastPlayedMediaIndex != mediaItems.size() - 1) {// resuming if not first or last
                    mediaItems = mediaItems.subList(lastPlayedMediaIndex, mediaItems.size());
                }
            }
            if(playlistModified(index)){
                resetTrackingNowPlaying(index);
            }
        }
        player = new SimpleExoPlayer.Builder(instance).build();

        for(int i = 0; i < mediaItems.size(); i++) {
            if(i == 0) {
                player.setMediaItem(mediaItems.get(i));
                if(playlist.isResuming()) {
                    player.seekTo(getSharedPlaylistSeekTo(index));
                }
            } else {
                player.addMediaItem(mediaItems.get(i));
            }
        }
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(instance);
        nowPlayingIndex = index;
    }

    private void resetTrackingNowPlaying(int index) {
        trackingNowPlaying(index, 0, getLastModifiedFor(index).lastModified(), 0);
    }

    private boolean playlistModified(int index) {
        return getLastModifiedFor(index).lastModified() >  getSharedPlaylistLastModified(index);
    }

    private void trackingNowPlaying(int index, int at, long lastModified, long seekTo) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt(getPlaylistPlayKey(index), at);
        editor.putLong(getPlaylistLastModified(index), lastModified);
        editor.putLong(getPlaylistSeekTo(index), seekTo);
        editor.commit();
    }

    private Integer getSharedPlaylistKey(Integer index) {
        return sharedpreferences.getInt(getPlaylistPlayKey(index), 0);
    }

    private long getSharedPlaylistLastModified(Integer index) {
        return sharedpreferences.getLong(getPlaylistLastModified(index), 0);
    }

    private long getSharedPlaylistSeekTo(Integer index) {
        return sharedpreferences.getLong(getPlaylistSeekTo(index), 0);
    }

    private String getPlaylistPlayKey(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_PLAY, index);
    }

    private String getPlaylistLastModified(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_LAST_MODIFIED, index);
    }

    private String getPlaylistSeekTo(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_SEEK_TO, index);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        maintenance = new Maintenance();
        handler = new Handler();
        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.monitor);
        super.onCreate(savedInstanceState);
        alarmManager = ((AlarmManager) instance.getSystemService(Context.ALARM_SERVICE));

        initialization();
    }

    /**
     * Returns the first location of app root directory on the system in precedence; external drive via usb, external sdcard, internal sdcard
     *
     * @return
     */
    private File getAppRootDirectory() {
        String postfix = "/telefyna";
        String mntUsb = "/mnt/usb";
        File[] storages = new File(mntUsb).listFiles();
        if (storages == null) {
            storages = ContextCompat.getExternalFilesDirs(instance, null);
        }
        ArrayUtils.reverse(storages);
        for (File storage : storages) {
            String location = storage.getAbsolutePath();
            if (!storage.getAbsolutePath().startsWith(mntUsb)) {
                location = storage.getAbsolutePath().substring(0, StringUtils.ordinalIndexOf(storage.getAbsolutePath(), "/", storage.getAbsolutePath().contains("emulated") ? 4 : 3));
            }
            File programsFolderLookup = new File(location + postfix);
            if (programsFolderLookup.exists() && programsFolderLookup.isDirectory() && programsFolderLookup.listFiles().length > 0) {
                return programsFolderLookup;
            }
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + postfix);
    }

    public String getPlaylistDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "playlist";
    }

    public Config initialiseConfiguration() {
        Config config = null;
        try {
            config = new Gson().fromJson(new BufferedReader(new FileReader(programsFolder.getAbsolutePath() + File.separator + "config.json")), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialization() {
        initialiseWithPermissions();
        programsFolder = getAppRootDirectory();
        configuration = initialiseConfiguration();
        playout = new HashMap<>();
        playerView = findViewById(R.id.player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setUseController(false);
        maintenance.run();
        shutDownHook();
    }

    private void setTrackNowtoCurrent() {
        trackingNowPlaying(nowPlayingIndex, playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPeriodIndex(), getLastModifiedFor(nowPlayingIndex).lastModified(), playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPosition());
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Player current = playerView.getPlayer();
        if(!player.equals(current)) {
            setTrackNowtoCurrent();
            if(current != null) {
                current.stop();
                current.release();
            }
            playerView.setPlayer(player);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            resetTrackingNowPlaying(nowPlayingIndex);
            switchToDefault();
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        trackingNowPlaying(nowPlayingIndex, playerView.getPlayer().getCurrentPeriodIndex(), getLastModifiedFor(nowPlayingIndex).lastModified(), 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        setTrackNowtoCurrent();
        if(!isNetworkConnected()) {
            switchToSecondDefault();
        } else {
            switchToDefault();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
        if(getConfiguration().isDisableNotifications()) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);
        }
    }

    private void shutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // TODO anything to do here
                setTrackNowtoCurrent();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutDownHook();
    }

    private File getLastModifiedFor(Integer index) {
        return getDirectoryToPlaylist(getConfiguration().getPlaylists()[index].getUrlOrFolder());
    }

    public File getDirectoryToPlaylist(String urlOrFolder) {
        return new File(getPlaylistDirectory() + File.separator + urlOrFolder);
    }

    private void initialiseWithPermissions() {
        List<String> missingPermissions = missingPermissions();
        if (!missingPermissions.isEmpty()) {
            askForPermissions(missingPermissions);
        }
    }

    private void askForPermissions(List<String> permissions) {
        ActivityCompat.requestPermissions(instance, permissions.toArray(new String[permissions.size()]), PackageManager.PERMISSION_GRANTED);
    }

    private List<String> missingPermissions() {
        List<String> allPermissions = Arrays.asList(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECEIVE_BOOT_COMPLETED, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE);
        List<String> missingPermissions = new ArrayList<>();
        for (int i = 0; i < allPermissions.size(); i++) {
            String permission = allPermissions.get(i);
            if (ContextCompat.checkSelfPermission(instance, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions.length > 0) {
            for (int i = 0; i <= grantResults.length - 1; i++) {
                if (grantResults[i] != requestCode) {
                    initialiseWithPermissions();
                    break;
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}