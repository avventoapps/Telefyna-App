package org.avvento.apps.telefyna;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
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
import org.avvento.apps.telefyna.stream.NowPlaying;
import org.avvento.apps.telefyna.stream.Playlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import lombok.Getter;

public class Monitor extends AppCompatActivity implements PlayerNotificationManager.NotificationListener, Player.EventListener {
    public static final String PREFERENCES = "TelefynaPrefs" ;
    private static final String PLAYLIST_PLAY = "PLAYLIST_PLAY";
    private static final String PLAYLIST_LAST_MODIFIED = "PLAYLIST_LAST_MODIFIED";
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
    private NowPlaying nowPlaying = new NowPlaying();
    @Getter
    private SimpleExoPlayer player;
    private PlayerView playerView;
    @Getter
    private Map<Integer, List<MediaItem>> playout;

    public void putPlayout(Integer index, List<MediaItem> mediaItems) {
        playout.put(index, mediaItems);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void switchToDefault() {
        switchNow(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void switchNow(int index) {
        List<MediaItem> mediaItems = playout.get(index);
        if(mediaItems.isEmpty()) {
            switchToDefault();
        }
        Playlist playlist = getConfiguration().getPlaylists()[index];
        if(playlist.isResuming() && !playlistModifed(index)) {
            int lastPlayedMediaIndex = sharedpreferences.getInt(getPlaylistPlayKey(index), 0);
            if (lastPlayedMediaIndex > 0) {// resuming
                mediaItems = mediaItems.subList(lastPlayedMediaIndex + 1, mediaItems.size());
            }
        } else {
            resetNowPlaying(index);
        }
        player = new SimpleExoPlayer.Builder(instance).build();

        for(int i = 0; i < mediaItems.size(); i++) {
            if(i == 0) {
                player.setMediaItem(mediaItems.get(i));
            } else {
                player.addMediaItem(mediaItems.get(i));
            }
        }
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(instance);
        nowPlaying.setPlaylistIndex(index);
    }

    private void resetNowPlaying(int index) {
        trackNowPlaying(index, 0, getDirectoryToPlaylist(getConfiguration().getPlaylists()[index].getUrlOrFolder()).lastModified());
    }

    private boolean playlistModifed(int index) {
        return getDirectoryToPlaylist(getConfiguration().getPlaylists()[index].getUrlOrFolder()).lastModified() >  sharedpreferences.getLong(getPlaylistLastModified(index), 0);
    }

    private void trackNowPlaying(int index, int at, long lastModified) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt(getPlaylistPlayKey(index), at);
        editor.putLong(getPlaylistLastModified(index), lastModified);
        editor.commit();
    }

    private String getPlaylistPlayKey(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_PLAY, index);
    }

    private String getPlaylistLastModified(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_LAST_MODIFIED, index);
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
        configuration = readConfiguration();
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
        return getAppRootDirectory().getAbsolutePath() + File.separator + "playlist";
    }

    private Config readConfiguration() {
        Config config = null;
        try {
            config = new Gson().fromJson(new BufferedReader(new FileReader(getAppRootDirectory().getAbsolutePath() + File.separator + "config.json")), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialization() {
        playout = new HashMap<>();
        playerView = findViewById(R.id.player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setUseController(false);
        maintenance.run();
        shutDownHook();
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if(isPlaying && !player.equals(nowPlaying.getPlayer())) {
            if (nowPlaying.getPlayer() != null) {
                nowPlaying.getPlayer().release();
            }
            playerView.setPlayer(player);
            nowPlaying.setPlayer(player);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlaybackStateChanged(int state) {
        int index = nowPlaying.getPlaylistIndex();
        if (state == Player.STATE_ENDED) {
            resetNowPlaying(index);
            switchToDefault();
        } else if(state == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            nowPlaying.setPlayingMediaIndex(nowPlaying.getPlaylistIndex() + 1);
            trackNowPlaying(index, nowPlaying.getPlayingMediaIndex(), getDirectoryToPlaylist(getConfiguration().getPlaylists()[index].getUrlOrFolder()).lastModified());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        switchToDefault();
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
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutDownHook();
    }

    public File getDirectoryToPlaylist(String urlOrFolder) {
        return new File(getPlaylistDirectory() + File.separator + urlOrFolder);
    }
}