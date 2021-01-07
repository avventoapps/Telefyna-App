package org.avvento.apps.telefyna;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
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
import org.avvento.apps.telefyna.listen.Maintenance;
import org.avvento.apps.telefyna.listen.NetworkState;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;
import org.avvento.apps.telefyna.stream.Program;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import lombok.Getter;

public class Monitor extends AppCompatActivity implements PlayerNotificationManager.NotificationListener, Player.EventListener, NetworkState.NetworkStateListener {
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
    private List<Program> currentBumpers;
    private File programsFolder;
    private NetworkState networkState;
    private Integer currentPlayingNetworkIndex;

    public String getProgramsFolderPath() {
        return programsFolder.getAbsolutePath();
    }

    public void putProgramsByIndex(Integer index, List<Program> mediaItems) {
        programsByIndex.put(index, mediaItems);
    }

    public Integer getFirstDefaultIndex() {return 0;}
    private Integer getSecondDefaultIndex() {return 1;}

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<MediaItem> extractingMediaItemsFromPrograms(List<Program> programs) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for(Program program : programs) {
            mediaItems.add(program.getMediaItem());
        };
        return mediaItems;
    }

    private void resetTrackingNowPlaying(int index) {
        trackingNowPlaying(index, 0, 0);
    }

    private long playlistModified(int index) {
        return getLastModifiedFor(index) -  getSharedPlaylistLastModified(index);
    }

    private void trackingNowPlaying(Integer index, int at, long seekTo) {
        if(getConfiguration().getPlaylists()[index].getType().name().startsWith(Playlist.Type.LOCAL_RESUMING.name())) {
            cachePlayingAt(index, at, seekTo);
        }
    }

    private void cachePlayingAt(Integer index, int at, long seekTo) {
        String programName = getMediaItemName(index, at);
        if(StringUtils.isNotBlank(programName)) {// exclude bumpers
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(getPlaylistPlayKey(index), at);
            editor.putLong(getPlaylistSeekTo(index), seekTo);
            editor.putLong(getPlaylistLastModified(index), getLastModifiedFor(index));
            editor.commit();
            Logger.log(AuditLog.Event.CACHE_NOW_PLAYING_RESUME, getPlayingAtIndexLabel(index), programName, seekTo);
        }
    }

    private String getMediaItemName(int index, int at) {
        if(currentBumpers.isEmpty() || (at > currentBumpers.size())) {
            List<Program> programs = programsByIndex.get(index);
            if(programs.size() > at) {
                return programs.get(at).getName();
            }
        }
        return null;
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
        startNetworkStateListener(instance);
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

    public String getBumperDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "bumper";
    }

    public String getPlaylistDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "playlist";
    }

    public String getAuditLogsFilePath(String name) {
        return String.format("%saudit%s.log", programsFolder.getAbsolutePath() + File.separator, File.separator + name);
    }

    public void initialiseConfiguration() {
        try {
            configuration = new Gson().fromJson(new BufferedReader(new FileReader(programsFolder.getAbsolutePath() + File.separator + "config.json")), Config.class);
            Logger.log(AuditLog.Event.CONFIGURATION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialization() {
        initialiseWithPermissions();
        programsFolder = getAppRootDirectory();
        initialiseConfiguration();
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

    private SimpleExoPlayer buildPlayer() {
        DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
        builder.setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, 60000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        return new SimpleExoPlayer.Builder(instance).setLoadControl(builder.build()).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void switchNow(int index) {
        currentBumpers = new ArrayList<>();
        if(nowPlayingIndex == null || nowPlayingIndex != index) {// leave current program to proceed if it's the same being loaded
            // setup objects, skip playlist with nothing to play
            Playlist playlist = getConfiguration().getPlaylists()[index];
            List<Program> programs = programsByIndex.get(index);
            nextPlayingIndex = index;
            player = buildPlayer();
            playlist = getConfiguration().getPlaylists()[nextPlayingIndex];
            if (programs.isEmpty()) {
                switchNow(getFirstDefaultIndex());
                return;
            }

            // reset tracking now playing if the playlist programs were modified
            long modifiedOffset = playlistModified(index);
            if (modifiedOffset > 0) {
                Logger.log(AuditLog.Event.PLAYLIST_MODIFIED, getPlayingAtIndexLabel(index), modifiedOffset / 1000);
                resetTrackingNowPlaying(index);
            }

            Logger.log(AuditLog.Event.PLAYLIST, new GsonBuilder().setPrettyPrinting().create().toJson(playlist));

            // extract and add programs
            List<MediaItem> programItems = extractingMediaItemsFromPrograms(programs);
            // handle resuming local playlists
            if (Playlist.Type.LOCAL_RANDOMIZED.equals(playlist.getType())) {
                Collections.shuffle(programItems);
            }
            // resume local resumable programs
            if (playlist.getType().name().startsWith(Playlist.Type.LOCAL_RESUMING.name())) {
                int nextProgram = getSharedPlaylistMediaItem(index);
                long nextSeekTo = getSharedPlaylistSeekTo(index);
                if (nextProgram > 0 && nextSeekTo > 0) {
                    if (playlist.getType().equals(Playlist.Type.LOCAL_RESUMING_NEXT) && nextProgram == programItems.size() - 1) {
                        nextProgram++; // next program excluding bumpers
                        nextSeekTo = C.POSITION_UNSET;
                    }
                    player.seekTo(nextProgram, nextSeekTo);
                    Logger.log(AuditLog.Event.RETRIEVE_NOW_PLAYING_RESUME, playlist.getName(), programs.get(nextProgram).getName(), nextSeekTo);
                }
            } else if(!playlist.getType().equals(Playlist.Type.ONLINE)) {// only add bumpers if not resuming and not online
                // prepare bumpers
                File bumperFolder = new File(getBumperDirectory() + File.separator + playlist.getUrlOrFolder());
                File generalBumperFolder = new File(getBumperDirectory() + File.separator + "General");
                addBumpers(bumperFolder, false);
                addBumpers(generalBumperFolder, true);
                List<MediaItem> bumperMediaItems = new ArrayList<>();
                // add any bumpers if available only for non continuous local playlists
                for (Program bumber : currentBumpers) {
                    bumperMediaItems.add(bumber.getMediaItem());
                }
                programItems.addAll(0, bumperMediaItems);
            }
            player.setMediaItems(programItems);

            player.prepare();
            Player current = playerView.getPlayer();
            if (current != null) {
                current.removeListener(instance);
            }

            player.addListener(instance);
            player.play();
        }
    }

    private void addBumpers(File bumperFolder, boolean addedFirstItem) {
        if (bumperFolder.exists() && bumperFolder.listFiles().length > 0) {
            maintenance.setupLocalPrograms(currentBumpers, bumperFolder, addedFirstItem);
            Collections.reverse(currentBumpers);
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if(isPlaying) {
            Player current = playerView.getPlayer();
            if(nowPlayingIndex == null || nowPlayingIndex != nextPlayingIndex) {
                if(current != null) {
                    current.stop();
                    current.release();
                }
                playerView.setPlayer(player);
                nowPlayingIndex = nextPlayingIndex;
                cacheNowPlaying();
                if(nowPlayingIndex != getSecondDefaultIndex()) {
                    currentPlayingNetworkIndex = null;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getNowPlayingPlaylistLabel());
            resetTrackingNowPlaying(nowPlayingIndex);
            switchNow(getFirstDefaultIndex());
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        int item = playerView.getPlayer().getCurrentPeriodIndex() - 1;// last item index
        trackingNowPlaying(nowPlayingIndex, item, 0);
        Logger.log(AuditLog.Event.PLAYLIST_ITEM_CHANGE, getNowPlayingPlaylistLabel(), getMediaItemName(nowPlayingIndex, item + 1));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Logger.log(AuditLog.Event.ERROR, String.format("%s: %s", error.getCause().toString(), error.getMessage()));
        cacheNowPlaying();
        if(isNetworkConnected()) {// else is handled by network listener
            switchNow(getFirstDefaultIndex());
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
        while (!missingPermissions().isEmpty()) {
            askForPermissions(missingPermissions());
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
        if(player != null && !player.isPlaying()) {
            player.play();
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if(player != null && !player.isPlaying()) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        unregisterNetworkState(instance);
        super.onPause();
    }

    private void startNetworkStateListener(Context currentContext) {
        networkState = new NetworkState();
        networkState.addListener((NetworkState.NetworkStateListener) currentContext);
        registerNetworkState(currentContext);
    }

    private void registerNetworkState(Context currentContext) {
        currentContext.registerReceiver(networkState, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void unregisterNetworkState(Context currentContext) {
        currentContext.unregisterReceiver(networkState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void networkAvailable() {
        Logger.log(AuditLog.Event.NETWORK_STATE, "");
        if(currentPlayingNetworkIndex != null && nowPlayingIndex != null && getSecondDefaultIndex() == nowPlayingIndex && Playlist.Type.ONLINE.equals(getConfiguration().getPlaylists()[currentPlayingNetworkIndex].getType())) {
            switchNow(currentPlayingNetworkIndex);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void networkUnavailable() {
        Logger.log(AuditLog.Event.NETWORK_STATE, "Un-");
        if(Playlist.Type.ONLINE.equals(getConfiguration().getPlaylists()[nowPlayingIndex == null ? nextPlayingIndex : nowPlayingIndex].getType())) {
            currentPlayingNetworkIndex = nowPlayingIndex == null ? nextPlayingIndex : nowPlayingIndex;
            switchNow(getSecondDefaultIndex());
        }
    }
}