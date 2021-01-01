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

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.scheduler.Maintenance;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;
import org.avvento.apps.telefyna.stream.Program;

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
    private Integer nowPlayingIndex;
    private Integer nextPlayingIndex;
    @Getter
    private SimpleExoPlayer player;
    private PlayerView playerView;
    @Getter
    private Map<Integer, List<Program>> programsByIndex;
    private File programsFolder;

    public String getProgramsFolderPath() {
        return programsFolder.getAbsolutePath();
    }

    public void putProgramsByIndex(Integer index, List<Program> mediaItems) {
        programsByIndex.put(index, mediaItems);
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
    private List<MediaItem> extractingMediaItemsFromPrograms(List<Program> programs) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for(Program program : programs) {
            mediaItems.add(program.getMediaItem());
        };
        return mediaItems;
    }

    private boolean resuming(Playlist playlist) {
        return playlist.isResuming() && !Playlist.Type.ONLINE.equals(playlist.getType());
    }

    private void resetTrackingNowPlaying(int index) {
        trackingNowPlaying(index, 0, 0);
    }

    private long playlistModified(int index) {
        return getLastModifiedFor(index) -  getSharedPlaylistLastModified(index);
    }

    private void trackingNowPlaying(Integer index, int at, long seekTo) {
        if(Playlist.Type.LOCAL.equals(getConfiguration().getPlaylists()[index].getType())) {
            cachePlayingAt(index, at, seekTo);
        }
    }

    private void cachePlayingAt(Integer index, int at, long seekTo) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt(getPlaylistPlayKey(index), at);
        editor.putLong(getPlaylistSeekTo(index), seekTo);
        editor.putLong(getPlaylistLastModified(index), getLastModifiedFor(index));
        editor.commit();
        Logger.log(AuditLog.Event.CACHE_NOW_PLAYING_RESUME, getPlayingAtIndexLabel(index), programsByIndex.get(index).get(at).getName(), seekTo);
    }

    private Integer getSharedPlaylistMediaItem(Integer index) {
        return sharedpreferences.getInt(getPlaylistPlayKey(index), 0);
    }

    private long getSharedPlaylistLastModified(Integer index) {
        return sharedpreferences.getLong(getPlaylistLastModified(index), getLastModifiedFor(index));
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
            if (programsFolderLookup != null && programsFolderLookup.exists() && programsFolderLookup.isDirectory() && programsFolderLookup.listFiles() != null && programsFolderLookup.listFiles().length > 0) {
                return programsFolderLookup;
            }
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + postfix);
    }

    public String getPlaylistDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "playlist";
    }

    public String getAuditLogsFilePath(String name) {
        return String.format("%saudit%s.log", programsFolder.getAbsolutePath() + File.separator, File.separator + name);
    }

    public Config initialiseConfiguration() {
        Config config = null;
        try {
            config = new Gson().fromJson(new BufferedReader(new FileReader(programsFolder.getAbsolutePath() + File.separator + "config.json")), Config.class);
            Logger.log(AuditLog.Event.CONFIGURATION);
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
        programsByIndex = new HashMap<>();
        playerView = findViewById(R.id.player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setUseController(false);
        maintenance.run();
        shutDownHook();
    }

    private void cacheNowPlaying() {
        if(nowPlayingIndex != null) {
            trackingNowPlaying(nowPlayingIndex, playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPeriodIndex(), playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPosition());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void switchNow(int index) {
        if(nowPlayingIndex == null || nowPlayingIndex != index) {// leave current program to proceed if it's the same being loaded
            Playlist playlist = getConfiguration().getPlaylists()[index];
            List<Program> programs = programsByIndex.get(index);
            if (programs.isEmpty()) {
                switchToDefault();
            }
            long modifiedOffset = playlistModified(index);
            if (modifiedOffset > 0) {
                Logger.log(AuditLog.Event.PLAYLIST_MODIFIED, getPlayingAtIndexLabel(index), modifiedOffset / 1000);
                resetTrackingNowPlaying(index);
            }
            player = new SimpleExoPlayer.Builder(instance).build();
            if (playlist.isClone()) {// only play the clone
                index = playlist.getClone();
                programs = programsByIndex.get(index);
            }
            nextPlayingIndex = index;
            playlist = getConfiguration().getPlaylists()[nextPlayingIndex];
            Logger.log(AuditLog.Event.PLAYLIST, new GsonBuilder().setPrettyPrinting().create().toJson(playlist));

            player.setMediaItems(extractingMediaItemsFromPrograms(programs));
            if (resuming(playlist)) {
                long lastPlayedProgramSeekto = getSharedPlaylistSeekTo(index);
                int lastPlayedProgram = getSharedPlaylistMediaItem(index);
                player.seekTo(lastPlayedProgram, lastPlayedProgramSeekto);
                Logger.log(AuditLog.Event.RETRIEVE_NOW_PLAYING_RESUME, playlist.getName(), programs.get(lastPlayedProgram).getName(), lastPlayedProgramSeekto);
            }
            player.prepare();
            player.addListener(instance);
            player.play();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            resetTrackingNowPlaying(nowPlayingIndex);
            Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getNowPlayingPlaylistLabel());
            switchToDefault();
        } else if (state == Player.STATE_READY) {
            Player current = playerView.getPlayer();
            if(nowPlayingIndex == null || nowPlayingIndex != nextPlayingIndex) {
                cacheNowPlaying();
                if(current != null) {
                    current.removeListener(instance);
                    current.stop();
                    current.release();
                }
                playerView.setPlayer(player);
                nowPlayingIndex = nextPlayingIndex;
            }
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        int item = playerView.getPlayer().getCurrentPeriodIndex() - 1;// last item index
        trackingNowPlaying(nowPlayingIndex, item, 0);
        Logger.log(AuditLog.Event.PLAYLIST_ITEM_CHANGE, getNowPlayingPlaylistLabel(),  programsByIndex.get(nowPlayingIndex).get(item + 1).getName());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        cacheNowPlaying();
        if(!isNetworkConnected()) {
            Logger.log(AuditLog.Event.NO_INTERNET, getNowPlayingPlaylistLabel());
            switchToSecondDefault();
        } else {
            switchToDefault();
        }
    }

    private String getPlayingAtIndexLabel(Integer index) {
        String playlistName = getConfiguration().getPlaylists()[index].getName();
        return String.format("%s #%d", playlistName, index);
    }

    private String getNowPlayingPlaylistLabel() {
        String playlistName = nowPlayingIndex == null ? "" : getConfiguration().getPlaylists()[nowPlayingIndex].getName();
        return String.format("%s #%d", playlistName, nowPlayingIndex);
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
                cacheNowPlaying();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutDownHook();
    }

    private long getLastModifiedFor(int index) {
        return getDirectoryToPlaylist(getConfiguration().getPlaylists()[index].getUrlOrFolder()).lastModified();
    }

    public File getDirectoryToPlaylist(String urlOrFolder) {
        return new File(getPlaylistDirectory() + File.separator + urlOrFolder);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
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

    @RequiresApi(api = Build.VERSION_CODES.N)
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();
        if(getConfiguration() == null) {
            maintenance.run();
        }
    }
}