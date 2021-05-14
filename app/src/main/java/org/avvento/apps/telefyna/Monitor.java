package org.avvento.apps.telefyna;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vinay.ticker.lib.TickerView;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.listen.Maintenance;
import org.avvento.apps.telefyna.listen.TelefynaUnCaughtExceptionHandler;
import org.avvento.apps.telefyna.modal.Config;
import org.avvento.apps.telefyna.modal.Graphics;
import org.avvento.apps.telefyna.modal.LowerThird;
import org.avvento.apps.telefyna.modal.News;
import org.avvento.apps.telefyna.modal.Playlist;
import org.avvento.apps.telefyna.modal.Seek;
import org.avvento.apps.telefyna.player.TelefynaRenderersFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String PLAYLIST_LAST_PLAYED = "PLAYLIST_LAST_PLAYED";
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
    @Getter
    private Handler maintenanceHandler;
    @Getter
    private Maintenance maintenance;
    private Integer nowPlayingIndex;
    private Integer failedBecauseOfInternetIndex;
    @Getter
    private SimpleExoPlayer player;
    private Playlist currentPlaylist;
    private List<Playlist> playlistByIndex;
    private List<MediaItem> currentBumpers;
    private List<MediaItem> programItems;
    private TickerView tickerView;
    private VideoView lowerThirdView;
    private int lowerThirdLoop = 1;
    private Runnable keepOnAir;
    private boolean offAir = false;
    private boolean fillingForLackOfInternet = false;
    private Integer nowProgramItem = 0;
    private Integer startOnePlayProgramItem;
    @Getter
    private SimpleDateFormat dateFormat;

    public void addPlayListByIndex(Playlist playlist) {
        playlistByIndex.add(playlist);
    }

    // This is the first default playlist, it plays whenever automation is disabled ot nothing is scheduled/available
    public Integer getFirstDefaultIndex() {
        return 0;
    }

    // This is the second default playlist, it plays whenever there is no internet connection
    private Integer getSecondDefaultIndex() {
        return 1;
    }

    private void resetTrackingNowPlaying(int index) {
        trackingNowPlaying(index, -1, false);
    }

    private long playlistModified(int index) {
        return getLastModifiedFor(index) - getSharedPlaylistLastModified(index);
    }

    private void trackingNowPlaying(Integer index, long seekTo, boolean noProgramTransition) {
        if (playlistByIndex.get(index).isResuming()) {
            cachePlayingAt(index, seekTo, noProgramTransition);
        }
    }

    private void cachePlayingAt(Integer index, long seekTo, boolean noProgramTransition) {
        int at = Playlist.Type.LOCAL_RESUMING_ONE.equals(currentPlaylist.getType()) && startOnePlayProgramItem != null ? 0 : nowProgramItem;
        int atValue = Playlist.Type.LOCAL_RESUMING_ONE.equals(currentPlaylist.getType()) && startOnePlayProgramItem != null ? startOnePlayProgramItem : nowProgramItem;
        atValue = noProgramTransition ? atValue - 1 : atValue;
        String programName = getMediaItemName(programItems.get(at));
        if (StringUtils.isNotBlank(programName)) {// exclude bumpers
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(getPlaylistPlayKey(index), atValue);
            editor.putLong(getPlaylistSeekTo(index), seekTo);
            editor.putLong(getPlaylistLastModified(index), getLastModifiedFor(index));
            editor.putString(getPlaylistLastPlayed(index), dateFormat.format(Calendar.getInstance().getTime()));
            editor.commit();
            Logger.log(AuditLog.Event.CACHE_NOW_PLAYING_RESUME, getPlayingAtIndexLabel(index), programName, at + "-" + seekTo);
        }
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

    private boolean resumableDay(Integer index) {
        String now = dateFormat.format(Calendar.getInstance().getTime());
        try {
            return dateFormat.parse(now).after(dateFormat.parse((sharedpreferences.getString(getPlaylistLastPlayed(getPlaylistIndex(index)), now))));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
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

    private String getPlaylistLastPlayed(int index) {
        return String.format(PLAYLIST_PLAY_FORMAT, PLAYLIST_LAST_PLAYED, index);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor);

        // handle any uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(new TelefynaUnCaughtExceptionHandler());
        if (getIntent().getBooleanExtra(TelefynaUnCaughtExceptionHandler.CRASH, false)) {
            Logger.log(AuditLog.Event.CRASH, getIntent().getStringExtra(TelefynaUnCaughtExceptionHandler.EXCEPTION));
        }

        instance = this;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        maintenance = new Maintenance();
        maintenanceHandler = new Handler();
        handler = new Handler();
        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        alarmManager = ((AlarmManager) instance.getSystemService(Context.ALARM_SERVICE));
        // allow network etc actions since telefyna depends on all of these
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        initialiseWithPermissions();
        maintenance.run();
    }

    /**
     * Returns the first location of app root directory on the system in precedence; external drive via usb, external sdcard, internal sdcard
     *
     * @return
     */
    private File getAppRootDirectory(boolean useExternalStorage) {
        String postfix = "/telefyna";
        if(useExternalStorage) {
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
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + postfix);
    }

    private File getRestartFile() {
        return new File(getAuditFilePath("restart.txt"));
    }

    private File getRebootFile() {
        return new File(getAuditFilePath("reboot.txt"));
    }

    private File getAuditConfigFile() {
        return new File(getAuditFilePath("config.json"));
    }

    private File getBackupConfigFile() {
        return new File(getAuditFilePath("backupConfig.txt"));
    }

    private File getBackupConfigResetFile() {
        return new File(getAuditFilePath("backupConfigReset.txt"));
    }

    private File getReInitializerFile() {
        return new File(getAuditFilePath("init.txt"));
    }

    public String getBumperDirectory(boolean useExternalStorage) {
        return getProgramsFolderPath(useExternalStorage) + File.separator + "bumper";
    }

    public String getProgramsFolderPath(boolean useExternalStorage) {
        return getAppRootDirectory(useExternalStorage).getAbsolutePath();
    }

    public String getLowerThirdDirectory(boolean useExternalStorage) {
        return getProgramsFolderPath(useExternalStorage) + File.separator + "lowerThird";
    }

    public String getPlaylistDirectory(boolean useExternalStorage) {
        return getProgramsFolderPath(useExternalStorage) + File.separator + "playlist";
    }

    public String getConfigFile() {
        return getProgramsFolderPath(false) + File.separator + "config.json";
    }

    public String getAuditFilePath(String name) {
        return String.format("%s/telefynaAudit/%s", Environment.getExternalStorageDirectory().getAbsolutePath(), name);
    }

    public String getAuditLogsFilePath(String name) {
        return getAuditFilePath(String.format("%s" + AuditLog.ENDPOINT, name));
    }

    public void initialise() {
        playlistByIndex = new ArrayList<>();
        try {
            FileReader reader = new FileReader(getConfigFile());
            configuration = new Gson().fromJson(new BufferedReader(reader), Config.class);
            reader.close();
            Logger.log(AuditLog.Event.CONFIGURATION);
        } catch (IOException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
    }
    
    private void cacheNowPlaying(boolean noProgramTransition) {
        PlayerView playerView = getPlayerView(false);
        Integer now = getPlaylistIndex(nowPlayingIndex);
        if (now != null && playerView != null && player != null && now != null) {
            trackingNowPlaying(now, player.getCurrentPosition(), noProgramTransition);
        }
    }

    private SimpleExoPlayer buildPlayer() {
        /*int delay = getConfiguration().getWait() * 1000;
        DefaultLoadcacheNowPlayingControl.Builder builder = new DefaultLoadControl.Builder();
        builder.setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS + delay, (DefaultLoadControl.DEFAULT_MAX_BUFFER_MS + (delay * 2)) * 2, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS + delay, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS + delay);
        SimpleExoPlayer player = new SimpleExoPlayer.Builder(instance).setLoadControl(builder.build()).build();
        */
        TelefynaRenderersFactory renderersFactory = new TelefynaRenderersFactory(instance);
        player = new SimpleExoPlayer.Builder(instance, renderersFactory).build();


        return player;
    }

    private void addBumpers(List<MediaItem> bumpers, File bumperFolder, boolean addedFirstItem) {
        if (bumperFolder.exists() && bumperFolder.listFiles().length > 0) {
            Utils.setupLocalPrograms(bumpers, bumperFolder, addedFirstItem, currentPlaylist);
            Collections.reverse(bumpers);
        }
    }

    private int getPlaylistIndex(Integer index) {
        if(index == null) {
            return index;
        }
        return playlistByIndex.get(index).getSchedule() != null ? playlistByIndex.get(index).getSchedule() : index;
    }

    private boolean samePlaylistPlaying(int index) {
        if(nowPlayingIndex != null) {
            int now = getPlaylistIndex(nowPlayingIndex);
            int next = getPlaylistIndex(index);
            return now == next;
        }
        return false;
    }

    private boolean playTheSame(int index) {
        return player != null && !player.isPlaying() && samePlaylistPlaying(index);
    }

    // TODO don't use until tested & fix
    private Integer getCurrentIndex() {
        Map<String, Integer> startedSlotsToday = new HashMap<>();
        List<String> starts = new ArrayList<>();
        Playlist[] playlists = getConfiguration().getPlaylists();
        for(int i = 0; i < playlists.length; i++) {
            Playlist playlist = playlists[i];
            String start = playlist.getStart();
            if(starts.contains(start)) { // consider the last
                starts.remove(start);
                startedSlotsToday.remove(start);
            }
            if(playlist.scheduledToday() && playlist.isStarted()) {
                starts.add(start);
                startedSlotsToday.put(start, i);
            }
        }
        List<String> slots = startedSlotsToday.keySet().stream().collect(Collectors.toList());
        Collections.sort(slots, Collections.reverseOrder());
        return startedSlotsToday.get(slots.get(0));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public synchronized void switchNow(int index, boolean isCurrentSlot) {
        Playlist playlist = playlistByIndex.get(index);
        Logger.log(AuditLog.Event.PLAYLIST, getPlayingAtIndexLabel(index), new GsonBuilder().setPrettyPrinting().create().toJson(playlist));

        // re-maintain if init file exists drop it and reload schedule
        if (getReInitializerFile().exists()) {
            getReInitializerFile().delete();
            maintenance.run();
            return;
        }

        if (!samePlaylistPlaying(index) || playTheSame(index)) {// leave current program to proceed if it's the same being loaded
            // setup objects, skip playlist with nothing to play
            nowPlayingIndex = index;
            currentPlaylist = playlist;
            programItems = maintenance.retrievePrograms(currentPlaylist);
            currentBumpers = new ArrayList<>();
            Monitor.instance.getHandler().removeCallbacksAndMessages(null);
            int firstDefaultIndex = getFirstDefaultIndex();
            int secondDefaultIndex = getSecondDefaultIndex();

            if (Playlist.Type.ONLINE.equals(currentPlaylist.getType()) && !Utils.internetConnected() && secondDefaultIndex != nowPlayingIndex) {
                Monitor.instance.getHandler().postDelayed(() -> {
                    if (Utils.internetConnected()) {
                        switchNow(nowPlayingIndex, isCurrentSlot);
                        return;
                    } else {
                        fillingForLackOfInternet = true;
                        switchNow(secondDefaultIndex, isCurrentSlot);
                        return;
                    }
                }, getConfiguration().getWait() * 1000);
            } else {
                keepBroadcasting();
                if (secondDefaultIndex == nowPlayingIndex && ((Playlist.Type.ONLINE.equals(currentPlaylist.getType()) && !Utils.internetConnected()) || (!Playlist.Type.ONLINE.equals(currentPlaylist.getType()) && programItems.isEmpty()))) {
                    Logger.log(AuditLog.Event.EMPTY_FILLERS);
                    switchNow(firstDefaultIndex, isCurrentSlot);
                    return;
                } else {
                    if (programItems.isEmpty()) {
                        Logger.log(AuditLog.Event.PLAYLIST_EMPTY_PLAY, getPlayingAtIndexLabel(nowPlayingIndex));
                        switchNow(currentPlaylist.getEmptyReplacer() != null ? currentPlaylist.getEmptyReplacer() : firstDefaultIndex, isCurrentSlot);
                        return;
                    } else {
                        player = buildPlayer();// TODO test
                        // reset tracking now playing if the playlist programs were modified
                        long modifiedOffset = playlistModified(nowPlayingIndex);

                        if (modifiedOffset > 0) {
                            Logger.log(AuditLog.Event.PLAYLIST_MODIFIED, getPlayingAtIndexLabel(nowPlayingIndex), modifiedOffset / 1000);
                            resetTrackingNowPlaying(nowPlayingIndex);
                        }

                        nowProgramItem = currentPlaylist.getSeekTo().getProgram();
                        startOnePlayProgramItem = null;
                        long nowPosition = currentPlaylist.getSeekTo().getPosition();
                        if (!Playlist.Type.ONLINE.equals(currentPlaylist.getType())) {
                            // resume local resumable programs
                            if (currentPlaylist.isResuming()) {
                                Integer previousProgram = getSharedPlaylistMediaItem(getPlaylistIndex(nowPlayingIndex));
                                long previousSeekTo = getSharedPlaylistSeekTo(getPlaylistIndex(nowPlayingIndex));
                                if (nowProgramItem == 0 && (currentPlaylist.getType().equals(Playlist.Type.LOCAL_RESUMING_NEXT) || currentPlaylist.getType().equals(Playlist.Type.LOCAL_RESUMING_ONE))) {
                                    // previousProgram == -1 when it was reset
                                    if (previousProgram == -1 || previousProgram == programItems.size() - 1) {// first or last
                                        nowProgramItem = 0;
                                    } else if(resumableDay(nowPlayingIndex)) {
                                        nowProgramItem = previousProgram + 1; // next program excluding bumpers
                                    } else {
                                        nowProgramItem = previousProgram;
                                    }
                                    previousSeekTo = 0;
                                } else if (currentPlaylist.getType().equals(Playlist.Type.LOCAL_RESUMING_SAME)) {
                                    nowProgramItem = previousProgram;
                                    previousSeekTo = 0;
                                }

                                Logger.log(AuditLog.Event.RETRIEVE_NOW_PLAYING_RESUME, currentPlaylist.getName(), getMediaItemName(programItems.get(nowProgramItem)), previousSeekTo);
                                if(currentPlaylist.getType().equals(Playlist.Type.LOCAL_RESUMING_ONE)) {
                                    MediaItem item = programItems.get(nowProgramItem);
                                    programItems.clear();
                                    programItems.add(item);
                                    startOnePlayProgramItem = nowProgramItem;
                                    nowProgramItem = 0;
                                } else if(currentPlaylist.getType().equals(Playlist.Type.LOCAL_RESUMING)) {
                                    nowPosition = nowPosition > 0 ? nowPosition : previousSeekTo;
                                }
                            } else {
                                String bumperFolder = getBumperDirectory(currentPlaylist.isUsingExternalStorage());
                                List<MediaItem> generalBumpers = new ArrayList<>(), specialBumpers = new ArrayList<>(), playListBumpers = new ArrayList<>();
                                // prepare general bumpers
                                if (currentPlaylist.isPlayingGeneralBumpers()) {
                                    addBumpers(generalBumpers, new File(bumperFolder + File.separator + "General"), false);
                                }
                                // prepare special bumpers
                                String specialBumperFolder = currentPlaylist.getSpecialBumperFolder();
                                if (StringUtils.isNotBlank(specialBumperFolder)) {
                                    addBumpers(specialBumpers, new File(bumperFolder + File.separator + specialBumperFolder), false);
                                }
                                // prepare playlist specific bumpers
                                addBumpers(playListBumpers, new File(bumperFolder + File.separator + currentPlaylist.getUrlOrFolder().split("#")[0]), false);
                                currentBumpers.addAll(playListBumpers);
                                currentBumpers.addAll(specialBumpers);
                                currentBumpers.addAll(generalBumpers);
                                programItems.addAll(0, currentBumpers);
                            }

                            if (isCurrentSlot) {
                                Seek seek = seekImmediateNonCompletedSlot(currentPlaylist, programItems);
                                if (seek != null) {
                                    nowProgramItem = seek.getProgram() == programItems.size() - 1 ? seek.getProgram() : nowProgramItem + seek.getProgram();
                                    nowPosition = seek.getProgram() == programItems.size() - 1 ? seek.getPosition() : nowProgramItem + seek.getPosition();
                                } else { // slot is ended, switch to fillers
                                    Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getPlayingAtIndexLabel(nowPlayingIndex));
                                    switchNow(secondDefaultIndex, false);
                                    return;
                                }
                            }
                        }
                        Player current = getPlayerView(true).getPlayer();
                        if (current != null) {
                            current.removeListener(instance);
                        }
                        player.setMediaItems(programItems);
                        player.seekTo(nowProgramItem, nowPosition);
                        player.prepare();
                        player.addListener(instance);
                        player.setPlayWhenReady(true);
                        Logger.log(isCurrentSlot ? AuditLog.Event.PLAYLIST_PLAY : AuditLog.Event.PLAYLIST_SWITCH, getNowPlayingPlaylistLabel(), Utils.formatDuration(nowPosition), getMediaItemName(programItems.get(nowProgramItem)));
                        // log now playing
                        cacheNowPlaying(false);
                        triggerGraphics(nowPosition);
                    }
                }
            }
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying){
        PlayerView playerView = getPlayerView(false);
        Player current = playerView.getPlayer();
        if (current == null || !player.equals(current)) {// change of player is proof of a switch
            while (player.isPlaying()) {
                if (current != null) {
                    current.release();
                    current = null;
                }
                Logger.log(AuditLog.Event.PLAYING_NOW);
                playerView.setPlayer(player);
                break;
            }
        }
    }

    // retrieve video duration in milliseconds
    private Long getDuration(String path) {
        MediaPlayer mMediaPlayer = null;
        long duration = 0;
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(instance, Uri.parse(path));
            mMediaPlayer.prepare();
            duration = mMediaPlayer.getDuration();
        } catch (Exception e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        } finally {
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
        return duration;
    }

    private Seek seekImmediateNonCompletedSlot(Playlist playlist, List<MediaItem> mediaItems) {
        Calendar start = playlist.getStartTime();
        if(start != null) {
            Long startTime = start.getTimeInMillis();
            long now = Calendar.getInstance().getTimeInMillis();
            for (int i = 0; i < mediaItems.size(); i++) {// mediaItems are well ordered
                Long duration = getDuration(mediaItems.get(i).mediaId);
                if ((duration + startTime) > now) {
                    // use the first item
                    return new Seek(i, now - startTime);
                }
            }
        }
        // unseekable, slot is ended
        return null;
    }

    private PlayerView getPlayerView(boolean reset) {
        PlayerView playerView = findViewById(R.id.player);
        if(reset) {
            playerView.showController();
            playerView.invalidate();
        }
        return playerView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (nowPlayingIndex != null) {
            if (state == Player.STATE_ENDED) {
                Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getNowPlayingPlaylistLabel());
                switchNow(getSecondDefaultIndex(), false);
            } else if (state == Player.STATE_BUFFERING && Playlist.Type.ONLINE.equals(currentPlaylist.getType())) {
                player.seekTo(player.getContentDuration());// hack
            }
        }
    }

    private String getMediaItemName(MediaItem mediaItem) {
        String name = "";
        try {
            name = URLDecoder.decode(mediaItem.mediaId.replace("file://", ""), "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
        return name;
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (nowPlayingIndex != null) {
            nowProgramItem++;
            cacheNowPlaying(false);
            Logger.log(AuditLog.Event.PLAYLIST_ITEM_CHANGE, getNowPlayingPlaylistLabel(), getMediaItemName(mediaItem));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Logger.log(AuditLog.Event.ERROR, String.format("%s: %s", error.getCause().toString(), error.getMessage()));
        cacheNowPlaying(currentPlaylist.getType().name().startsWith("LOCAL_RESUMING"));
        // keep reloading existing program if internet is on and off
        if (error.getCause().getCause() instanceof UnknownHostException || error.getCause().getCause() instanceof IOException) {
            Logger.log(AuditLog.Event.NO_INTERNET, "Failing to play program because of no internet connection");
            failedBecauseOfInternetIndex = nowPlayingIndex;
            // this will wait for set time on config before reloading
        } else if(error.getCause().getCause() instanceof UnrecognizedInputFormatException && player.isCurrentWindowSeekable()) {
            player.seekTo(nowProgramItem + 1, 0);
        } else if(!player.isPlaying()) {
           switchNow(nowPlayingIndex, false);
        }
    }

    private String getPlayingAtIndexLabel(Integer index) {
        String playlistName = playlistByIndex.get(getPlaylistIndex(index)).getName();
        return String.format("%s #%d", playlistName, index);
    }

    private String getNowPlayingPlaylistLabel() {
        String playlistName = nowPlayingIndex == null ? "" : currentPlaylist.getName();
        return String.format("%s #%d", playlistName, nowPlayingIndex);
    }

    @Override
    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
        if (getConfiguration().isNotificationsDisabled()) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);
        }
    }

    private void shutDownHook() {
        Logger.log(AuditLog.Event.HEARTBEAT, "OFF");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutDownHook();
    }

    private long getLastModifiedFor(int index) {
        return getDirectoryFromPlaylist(playlistByIndex.get(index)).lastModified();
    }

    public File getDirectoryFromPlaylist(Playlist playlist, int i) {
        return new File(getPlaylistDirectory(playlist.isUsingExternalStorage()) + File.separator + playlist.getUrlOrFolder().split("#")[i].trim());
    }

    private File getDirectoryFromPlaylist(Playlist playlist) {
        return getDirectoryFromPlaylist(playlist, 0);
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

    @Override
    public void onBackPressed() {
        //moveTaskToBack(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0) {
            Logger.log(AuditLog.Event.KEY_PRESS, KeyEvent.keyCodeToString(event.getKeyCode()) + "#" + event.getKeyCode());
        }
        return super.onKeyDown(event.getKeyCode(), event);
    }

    private void triggerGraphics(long nowPosition) {
        hideLogo();
        hideTicker();
        hideLowerThird();
        Graphics graphics = currentPlaylist.getGraphics();
        if(graphics != null) {
            // handle logo
            if(graphics.isDisplayLogo()) {
                showLogo(graphics.getLogoPosition());
            }

            // handle lowerThird
            LowerThird[] lowerThirds = graphics.getLowerThirds();
            if(lowerThirds != null) {
                Arrays.stream(lowerThirds).forEach(ltd -> {
                    if(StringUtils.isNotBlank(ltd.getStarts()) && ltd.getFile() != null) {
                        Arrays.stream(ltd.getStartsArray()).forEach(s -> {
                            long start = Math.round(s * 60 * 1000);//s is in minutes, send in mills
                            Monitor.instance.getHandler().postDelayed(() -> {
                                showLowerThird(ltd);
                            }, start - nowPosition);
                        });
                    }
                });
            }

            // handle ticker
            News news = graphics.getNews();
            if(news != null) {
                String[] messages = news.getMessagesArray();

                if(messages.length > 0) {
                    initTickers(news);
                    Arrays.stream(news.getStartsArray()).forEach(s -> {
                        long start = Math.round(s * 60 * 1000);//s is in minutes, send in mills
                        if(start >= nowPosition) {
                            Monitor.instance.getHandler().postDelayed(() -> {
                                showTicker(news);
                            }, start - nowPosition);
                        }
                    });
                }
            }
        }
    }

    private void hideLowerThird() {
        if(lowerThirdView != null && View.GONE != lowerThirdView.getVisibility()) {
            //TODO lowerThirdView.animate().translationX(lowerThirdView.getWidth()); etc should be in the clip
            lowerThirdView.setVisibility(View.GONE);
            Logger.log(AuditLog.Event.LOWER_THIRD_OFF);
        }
    }

    private void hideTicker() {
        if(tickerView != null && View.GONE != tickerView.getVisibility()) {
            tickerView.setVisibility(View.GONE);
            tickerView.removeChildViews();
            tickerView.destroyAllScheduledTasks();
            Logger.log(AuditLog.Event.DISPLAY_NEWS_OFF);
        }
    }

    private void hideLogo() {
        View topLogo = findViewById(R.id.topLogo);
        View bottomLogo = findViewById(R.id.bottomLogo);

        if(View.GONE != topLogo.getVisibility() || View.GONE != bottomLogo.getVisibility()) {
            topLogo.setVisibility(View.GONE);
            bottomLogo.setVisibility(View.GONE);
            Logger.log(AuditLog.Event.DISPLAY_LOGO_OFF);
        }
    }

    private void showLowerThird(LowerThird lowerThird) {
        String path = getLowerThirdDirectory(currentPlaylist.isUsingExternalStorage()) + File.separator + lowerThird.getFile();
        File lowerThirdClip =  new File(path);
        if(Utils.validPlayableItem(lowerThirdClip)) {
            Logger.log(AuditLog.Event.LOWER_THIRD_ON, path);
            lowerThirdView = (VideoView) findViewById(R.id.lowerThird); // initiate a video view
            lowerThirdView.setVideoURI(Uri.fromFile(lowerThirdClip));
            lowerThirdView.start();
            lowerThirdView.setVisibility(View.VISIBLE);
            lowerThirdView.setOnCompletionListener(mediaPlayer -> {
                if(lowerThird.getReplays() >= lowerThirdLoop) {
                    // replay
                    lowerThirdLoop++;
                    lowerThirdView.start();
                } else {
                    hideLowerThird();
                    lowerThirdLoop = 1;
                }
            });

            lowerThirdView.setOnErrorListener((mp, what, extra) -> {
                Logger.log(AuditLog.Event.ERROR, "Failed to play " + lowerThird.getFile());
                return true;
            });
        }
    }

    private void initTickers(News news) {
        tickerView = findViewById(R.id.tickerView);
        tickerView.setReplays(news.getReplays());
        tickerView.setDisplacement(news.getSpeed().getDisplacement());
        tickerView.setBackgroundColor(getColor(android.R.color.transparent));
        for (String message : news.getMessagesArray()) {
            tickerView.addChildView(tickerView(message));
        }
    }

    private void showTicker(News news) {
        Logger.log(AuditLog.Event.DISPLAY_NEWS_ON, news.getMessages());
        tickerView.showTickers();//TODO add time run in context
        tickerView.setVisibility(View.VISIBLE);
    }

    private void showLogo(Graphics.LogoPosition logoPosition) {
        File logo =  new File(getProgramsFolderPath(false) + File.separator + "logo.png");
        if(logo.exists() && logoPosition != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(logo.getAbsolutePath());
            ImageView logoView;
            if(Graphics.LogoPosition.TOP.equals(logoPosition)) {
                logoView = findViewById(R.id.topLogo);
                Logger.log(AuditLog.Event.DISPLAY_LOGO_ON, Graphics.LogoPosition.TOP.name());
            } else {
                logoView = findViewById(R.id.bottomLogo);
                Logger.log(AuditLog.Event.DISPLAY_LOGO_ON, Graphics.LogoPosition.BOTTOM.name());
            }
            logoView.setImageBitmap(myBitmap);
            logoView.setVisibility(View.VISIBLE);
        }
    }

    private TextView tickerView(String message) {
        TextView tickerView = new TextView(instance);
        tickerView.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        tickerView.setText(message);
        tickerView.setTextSize(getResources().getDimension(R.dimen.tickerFontSize));
        tickerView.setBackgroundColor(getColor(R.color.trans));
        tickerView.setTextColor(ContextCompat.getColor(instance, android.R.color.white));
        tickerView.setPadding(100, 2, 100, 2);
        return tickerView;
    }

    private Config regenerateConfiguration(boolean resetSeekTo) {
        Config config = getConfiguration();
        Playlist[] playlists = config.getPlaylists();
        for(int i = 0; i < playlists.length; i++) {
            Playlist playlist = playlists[i];
            if(playlist.isResuming()) {
                playlist.setSeekTo(resetSeekTo ? new Seek(0, 0) : new Seek(getSharedPlaylistMediaItem(i), getSharedPlaylistSeekTo(i)));
                playlists[i] = playlist;
            }
        }
        config.setPlaylists(playlists);
        return config;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void keepBroadcasting() {
        if(nowPlayingIndex != null) {
            if (keepOnAir != null) {
                handler.removeCallbacks(keepOnAir);
            }
            long delay = getConfiguration().getWait() * 1000;
            handler.postDelayed(keepOnAir = () -> {
                if(getRebootFile().exists()) {
                    getRebootFile().delete();
                    rebootDevice();
                } else if (getRestartFile().exists()) {// restart event
                    getRestartFile().delete();
                    restartApp();
                } else {
                    if(getBackupConfigFile().exists()) {
                        backupConfig(false);
                    }
                    if(getBackupConfigResetFile().exists()) {
                        backupConfig(true);
                    }
                    if (nowPlayingIndex == getSecondDefaultIndex() && fillingForLackOfInternet && Utils.internetConnected() && failedBecauseOfInternetIndex != null) {// switch back from fillers once internet is back
                        fillingForLackOfInternet = false;
                        Logger.log(AuditLog.Event.INTERNET_RESTORED);
                        switchNow(failedBecauseOfInternetIndex, false);
                        failedBecauseOfInternetIndex = null;
                    } else {
                        handler.postDelayed(keepOnAir, delay);
                        if (offAir) {//been off air for the past delay
                            offAir = false;
                            Logger.log(AuditLog.Event.STUCK, delay / 1000);
                            switchNow(nowPlayingIndex, false);
                        } else {
                            if (player == null || !player.isPlaying()) {
                                offAir = true;
                            } else {
                                offAir = false;
                            }
                        }
                    }
                }
            }, delay);
        }
    }

    private void rebootDevice() {
        try{
            Runtime.getRuntime().exec("su -c reboot");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void restartApp() {
        maintenance.cancelPendingIntents();
        Intent intent = new Intent(Monitor.instance, Monitor.class);
        PendingIntent mPendingIntent = PendingIntent.getActivity(Monitor.instance, 700000001, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        Logger.log(AuditLog.Event.RESTARTING);
        Monitor.instance.finish();
        System.exit(2);
    }

    private void backupConfig(boolean resetSeekTo) {
        if(resetSeekTo) {
            getBackupConfigResetFile().delete();
        } else {
            getBackupConfigFile().delete();
        }
        Config config = regenerateConfiguration(resetSeekTo);
        try {
            Logger.log(AuditLog.Event.BACK_UP);
            FileWriter writer = new FileWriter(getAuditConfigFile(), false);
            new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
    }

}