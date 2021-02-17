package org.avvento.apps.telefyna;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.listen.Maintenance;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;
import org.avvento.apps.telefyna.stream.Program;
import org.avvento.apps.telefyna.stream.Seek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import lombok.Getter;

public class Monitor extends AppCompatActivity implements PlayerNotificationManager.NotificationListener, Player.EventListener {
    public static final String PREFERENCES = "TelefynaPrefs";
    private static final String PLAYLIST_PLAY = "PLAYLIST_PLAY";
    private static final String PLAYLIST_LAST_MODIFIED = "PLAYLIST_LAST_MODIFIED";
    private static final String PLAYLIST_SEEK_TO = "PLAYLIST_SEEK_TO";
    private static final String PLAYLIST_PLAY_FORMAT = "%s-%d";
    public static Monitor instance;
    private SharedPreferences sharedpreferences;
    @Getter
    private Config configuration;
    @Getter
    private AlarmManager alarmManager;
    @Getter
    private Handler handler;
    private Maintenance maintenance;
    private Integer nowPlayingIndex;
    @Getter
    private SimpleExoPlayer player;
    @Getter
    private List<List<Program>> programsByIndex;
    private List<Playlist> playlistByIndex;
    private List<Program> currentBumpers;
    private File programsFolder;
    private boolean secondDefaultRepeatable = false;

    public String getProgramsFolderPath() {
        return programsFolder.getAbsolutePath();
    }

    public void addProgramsByIndex(List<Program> programs) {
        programsByIndex.add(programs);
    }

    public void addPlayListByIndex(Playlist playlist) {
        playlistByIndex.add(playlist);
    }

    // This is the first default playlist, it plays whenever nothing is scheduled or is available
    public Integer getFirstDefaultIndex() {
        return 0;
    }

    // This is the second default playlist, it plays whenever there is no internet connection
    private Integer getSecondDefaultIndex() {
        return 1;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<MediaItem> extractingMediaItemsFromPrograms(List<Program> programs) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Program program : programs) {
            mediaItems.add(program.getMediaItem());
        }
        return mediaItems;
    }

    private void resetTrackingNowPlaying(int index) {
        trackingNowPlaying(index, 0, 0);
    }

    private long playlistModified(int index) {
        return getLastModifiedFor(index) - getSharedPlaylistLastModified(index);
    }

    private void trackingNowPlaying(Integer index, int at, long seekTo) {
        if (isResuming(playlistByIndex.get(index))) {
            cachePlayingAt(index, at, seekTo);
        }
    }

    private void cachePlayingAt(Integer index, int at, long seekTo) {
        String programName = getMediaItemName(index, at);
        if (StringUtils.isNotBlank(programName)) {// exclude bumpers
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(getPlaylistPlayKey(index), at);
            editor.putLong(getPlaylistSeekTo(index), seekTo);
            editor.putLong(getPlaylistLastModified(index), getLastModifiedFor(index));
            editor.commit();
            Logger.log(AuditLog.Event.CACHE_NOW_PLAYING_RESUME, getPlayingAtIndexLabel(index), programName, seekTo);
        }
    }

    private String getMediaItemName(int index, int at) {
        boolean isBumpers = (!currentBumpers.isEmpty() && (at < currentBumpers.size()));
        List<Program> programs = programsByIndex.get(index);
        List<Program> items = isBumpers ? currentBumpers : programs;
        if (items.size() > 0) {
            return items.get(isBumpers ? at : at - (programs.size() - 1)).getName();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor);
        handleAnyException();
        instance = this;
        maintenance = new Maintenance();
        handler = new Handler();
        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        alarmManager = ((AlarmManager) instance.getSystemService(Context.ALARM_SERVICE));
        // allow network etc actions since telefyna depends on all of these
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
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
            if (storage != null) {
                String location = storage.getAbsolutePath().substring(0, StringUtils.ordinalIndexOf(storage.getAbsolutePath(), "/", storage.getAbsolutePath().contains("emulated") ? 4 : 3));
                return new File(location + postfix);
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
        return String.format("%s/telefynaAudit/%s.log", Environment.getExternalStorageDirectory().getAbsolutePath(), name);
    }

    public void initialiseConfiguration() {
        try {
            configuration = new Gson().fromJson(new BufferedReader(new FileReader(programsFolder.getAbsolutePath() + File.separator + "config.json")), Config.class);
            Logger.log(AuditLog.Event.CONFIGURATION);
        } catch (IOException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialization() {
        initialiseWithPermissions();
        programsFolder = getAppRootDirectory();
        programsByIndex = new ArrayList<>();
        playlistByIndex = new ArrayList<>();
        maintenance.run();
    }

    private void cacheNowPlaying() {
        if (nowPlayingIndex != null) {
            PlayerView playerView = findViewById(R.id.player);
            trackingNowPlaying(nowPlayingIndex, playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPeriodIndex(), playerView.getPlayer() == null ? 0 : playerView.getPlayer().getCurrentPosition());
        }
    }

    private SimpleExoPlayer buildPlayer() {
        DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
        builder.setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, 60000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        SimpleExoPlayer player = new SimpleExoPlayer.Builder(instance).setLoadControl(builder.build()).build();
        return player;
    }

    private void addBumpers(List<Program> bumpers, File bumperFolder, boolean addedFirstItem) {
        if (bumperFolder.exists() && bumperFolder.listFiles().length > 0) {
            Utils.setupLocalPrograms(bumpers, bumperFolder, addedFirstItem);
            Collections.reverse(bumpers);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void switchNow(int index, boolean isCurrentSlot) {
        currentBumpers = new ArrayList<>();
        player = buildPlayer();
        int firstDefaultIndex = getFirstDefaultIndex();
        int secondDefaultIndex = getSecondDefaultIndex();

        // setup objects, skip playlist with nothing to play
        List<Program> programs = programsByIndex.get(index);
        Playlist playlist = playlistByIndex.get(index);

        if (nowPlayingIndex == null || nowPlayingIndex != index || secondDefaultRepeatable) {// leave current program to proceed if it's the same being loaded
            if (!Utils.internetConnected() && secondDefaultIndex != index && Playlist.Type.ONLINE.equals(playlist.getType())) {
                switchNow(secondDefaultIndex, false);
                return;
            } else {
                if (programs.isEmpty()) {
                    Logger.log(AuditLog.Event.PLAYLIST_EMPTY_PLAY, getPlayingAtIndexLabel(index));
                    switchNow(firstDefaultIndex, false);
                    return;
                } else {
                    // log now playing
                    cacheNowPlaying();

                    // reset tracking now playing if the playlist programs were modified
                    long modifiedOffset = playlistModified(index);
                    List<MediaItem> bumperMediaItems = new ArrayList<>();

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
                    } else if (Playlist.Type.LOCAL_SEQUENCED.equals(playlist.getType())) {
                        // this is handled by default by Utils#setupLocalPrograms
                    }
                    int program = 0;
                    long position = 0;
                    // resume local resumable programs
                    if (isResuming(playlist)) {
                        int nextProgram = getSharedPlaylistMediaItem(index);
                        long nextSeekTo = getSharedPlaylistSeekTo(index);
                        if (playlist.getType().equals(Playlist.Type.LOCAL_RESUMING_NEXT)) {
                            if (nextProgram == programItems.size() - 1) {// last
                                nextProgram = 0;
                            } else {
                                nextProgram++; // next program excluding bumpers
                            }
                            nextSeekTo = 0;
                        } else if (playlist.getType().equals(Playlist.Type.LOCAL_RESUMING_SAME)) {
                            nextSeekTo = 0;
                        }
                        program = nextProgram;
                        position = nextSeekTo;
                        Logger.log(AuditLog.Event.RETRIEVE_NOW_PLAYING_RESUME, playlist.getName(), programs.get(nextProgram).getName(), nextSeekTo);
                    } else if (!playlist.getType().equals(Playlist.Type.ONLINE)) {// only add bumpers if not resuming and not online
                        String bumperFolder = getBumperDirectory();
                        List<Program> generalBumpers = new ArrayList<>(), specialBumpers = new ArrayList<>(), playListBumpers = new ArrayList<>();
                        // prepare general bumpers
                        if (playlist.isPlayingGeneralBumpers()) {
                            addBumpers(generalBumpers, new File(bumperFolder + File.separator + "General"), false);
                        }
                        // prepare special bumpers
                        String specialBumperFolder = playlist.getSpecialBumperFolder();
                        if (StringUtils.isNotBlank(specialBumperFolder)) {
                            addBumpers(specialBumpers, new File(bumperFolder + File.separator + specialBumperFolder), false);
                        }
                        // prepare playlist specific bumpers
                        addBumpers(playListBumpers, new File(bumperFolder + File.separator + playlist.getUrlOrFolder()), false);
                        currentBumpers.addAll(generalBumpers);
                        currentBumpers.addAll(specialBumpers);
                        currentBumpers.addAll(playListBumpers);

                        // add any bumpers if available only for non continuous local playlists
                        if (!isResuming(playlist)) {
                            for (Program bumper : currentBumpers) {
                                bumperMediaItems.add(bumper.getMediaItem());
                            }
                            programItems.addAll(0, bumperMediaItems);
                        }
                    }
                    // playing current local slot, TODO support more than one program
                    if (isCurrentSlot) {
                        Seek seek = seekCurrentSlot(playlist, programItems, program, position);
                        if(seek != null) {
                            Logger.log(AuditLog.Event.PLAYLIST_PLAY_CURRENT_SLOT, getPlayingAtIndexLabel(index), getMediaItemName(index, seek.getProgram()), formatDuration(seek.getPosition()));
                            program = seek.getProgram();
                            position = seek.getPosition();
                        } else { // slot is ended, switch to first default
                            switchNow(firstDefaultIndex, false);
                            return;
                        }

                    }
                    player.setMediaItems(programItems);
                    player.seekTo(program, position);
                    player.prepare();
                    Player current = ((PlayerView) findViewById(R.id.player)).getPlayer();
                    if (current != null) {
                        current.removeListener(instance);
                    }

                    player.addListener(instance);
                    player.setPlayWhenReady(true);
                    nowPlayingIndex = index;
                    secondDefaultRepeatable = false;
                }
            }
        }
    }

    private Seek seekCurrentSlot(Playlist playlist, List<MediaItem> programItems, int program, long position) {
        return getImmediateNonCompletedSlot(position, program, playlist, programItems);
    }

    // if playlist is resuming, no bumpers play; next plays next program, same plays the former un completed else exact time is resumed
    private boolean isResuming(Playlist playlist) {
        return playlist.getType().name().startsWith(Playlist.Type.LOCAL_RESUMING.name());
    }

    // retrieve video duration in milliseconds
    private long getDuration(String path) {
        MediaPlayer mMediaPlayer = null;
        long duration = 0;
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(instance, Uri.parse(path));
            mMediaPlayer.prepare();
            duration = mMediaPlayer.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
        return duration;
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hours));
        long secs = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.MINUTES.toMillis(mins));
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private Seek getImmediateNonCompletedSlot(Long position, Integer program, Playlist playlist, List<MediaItem> mediaItems) {
        long startTime = getStart(playlist).getTimeInMillis();
        if(isResuming(playlist) || playlist.getType().equals(Playlist.Type.LOCAL_RANDOMIZED) || playlist.getType().equals(Playlist.Type.ONLINE)) {
            return new Seek(program, position);
        } else {
            for (int i = 0; i < mediaItems.size(); i++) {// mediaItems are well ordered
                long now = Calendar.getInstance().getTimeInMillis();
                long duration = getDuration(mediaItems.get(i).mediaId);
                if (duration + startTime > now) {
                    // use the first item
                    return new Seek(i,  now - startTime);
                }
            }
            // unseekable, slot is ended
            return null;
        }
    }

    private Calendar getStart(Playlist playlist) {
        Integer hour = Integer.parseInt(playlist.getStart().split(":")[0]);
        Integer min = Integer.parseInt(playlist.getStart().split(":")[1]);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, hour);
        start.set(Calendar.MINUTE, min);
        start.set(Calendar.SECOND, 0);
        return start;
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        PlayerView playerView = findViewById(R.id.player);
        Player current = playerView.getPlayer();
        if (current == null || !player.equals(current)) {
            if (current != null) {
                current.stop();
                current.release();
                current = null;
            }
            playerView.setPlayer(player);
            Logger.log(AuditLog.Event.PLAYLIST_PLAY, getNowPlayingPlaylistLabel());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (nowPlayingIndex != null) {
            if (state == Player.STATE_ENDED) {
                Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getNowPlayingPlaylistLabel());
                secondDefaultRepeatable = true;
                switchNow(getFirstDefaultIndex(), false);
            } else if (state == Player.STATE_BUFFERING && Playlist.Type.ONLINE.equals(playlistByIndex.get(nowPlayingIndex).getType())) {
                player.seekTo(player.getContentDuration());// hack
            }
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (nowPlayingIndex != null) {
            int item = ((PlayerView) findViewById(R.id.player)).getPlayer().getCurrentPeriodIndex() - 1;// last item index
            trackingNowPlaying(nowPlayingIndex, item, 0);
            Logger.log(AuditLog.Event.PLAYLIST_ITEM_CHANGE, getNowPlayingPlaylistLabel(), getMediaItemName(nowPlayingIndex, item + 1));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Logger.log(AuditLog.Event.ERROR, String.format("%s: %s", error.getCause().toString(), error.getMessage()));
        cacheNowPlaying();
        // keep reloading existing program if internet is on and off
        if (error.getCause().getCause() instanceof UnknownHostException || error.getCause().getCause() instanceof IOException) {
            reload();
        }
    }

    // TODO reload?
    private void reload() {
        player.prepare();
        player.play();
    }

    private String getPlayingAtIndexLabel(Integer index) {
        String playlistName = playlistByIndex.get(index).getName();
        return String.format("%s #%d", playlistName, index);
    }

    private String getNowPlayingPlaylistLabel() {
        String playlistName = nowPlayingIndex == null ? "" : playlistByIndex.get(nowPlayingIndex).getName();
        return String.format("%s #%d", playlistName, nowPlayingIndex);
    }

    @Override
    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
        if (getConfiguration().isDisableNotifications()) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);
        }
    }

    private void shutDownHook() {
        cacheNowPlaying();
        Logger.log(AuditLog.Event.HEARTBEAT, "OFF");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutDownHook();
    }

    private long getLastModifiedFor(int index) {
        return getDirectoryToPlaylist(playlistByIndex.get(index).getUrlOrFolder()).lastModified();
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

    private void handleAnyException() {
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            //Catch your exception
            // Without System.exit() this will not work.
            System.exit(2);
        });
    }

    @Override
    public void onBackPressed() {
        //moveTaskToBack(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logger.log(AuditLog.Event.KEY_DOWN, keyCode);
        return super.onKeyDown(keyCode, event);
    }

}