package org.avvento.apps.telefyna;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
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

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import org.avvento.apps.telefyna.modal.Config;
import org.avvento.apps.telefyna.modal.Graphics;
import org.avvento.apps.telefyna.modal.LowerThird;
import org.avvento.apps.telefyna.modal.News;
import org.avvento.apps.telefyna.modal.Playlist;
import org.avvento.apps.telefyna.modal.Program;
import org.avvento.apps.telefyna.modal.Seek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private List<MediaItem> programItems;
    private TickerView tickerView;
    private VideoView lowerThirdView;
    private int lowerThirdLoop = 1;
    private Runnable keepOnair;
    private boolean offAir = false;

    public String getProgramsFolderPath() {
        return programsFolder.getAbsolutePath();
    }

    public void addProgramsByIndex(List<Program> programs) {
        programsByIndex.add(programs);
    }

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
        String programName = getMediaItemName(programItems.get(at));
        if (StringUtils.isNotBlank(programName)) {// exclude bumpers
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(getPlaylistPlayKey(index), at);
            editor.putLong(getPlaylistSeekTo(index), seekTo);
            editor.putLong(getPlaylistLastModified(index), getLastModifiedFor(index));
            editor.commit();
            Logger.log(AuditLog.Event.CACHE_NOW_PLAYING_RESUME, getPlayingAtIndexLabel(index), programName, seekTo);
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
    public File getAppRootDirectory() {
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

    public File getReInitializerFile() {
        return new File(getAuditFilePath("init.txt"));
    }

    public String getBumperDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "bumper";
    }

    public String getLowerThirdDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "lowerThird";
    }

    public String getPlaylistDirectory() {
        return programsFolder.getAbsolutePath() + File.separator + "playlist";
    }

    public String getConfigFile() {
        return programsFolder.getAbsolutePath() + File.separator + "config.json";
    }

    public String getAuditFilePath(String name) {
        return String.format("%s/telefynaAudit/%s", Environment.getExternalStorageDirectory().getAbsolutePath(), name);
    }

    public String getAuditLogsFilePath(String name) {
        return getAuditFilePath(String.format("%s" + AuditLog.ENDPOINT, name));
    }

    public void initialiseConfiguration() {
        try {
            configuration = new Gson().fromJson(new BufferedReader(new FileReader(getConfigFile())), Config.class);
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
            PlayerView playerView = getPlayerView();
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
        // re-maintain if init file exists drop it and reload schedule
        if(getReInitializerFile().exists()) {
            getReInitializerFile().delete();
            maintenance.run();
            return;
        }
        currentBumpers = new ArrayList<>();
        int firstDefaultIndex = getFirstDefaultIndex();
        int secondDefaultIndex = getSecondDefaultIndex();

        // setup objects, skip playlist with nothing to play
        List<Program> programs = programsByIndex.get(index);
        Playlist playlist = playlistByIndex.get(index);

        if (nowPlayingIndex == null || nowPlayingIndex != index || playTheSame(index)) {// leave current program to proceed if it's the same being loaded
            if (!Utils.internetConnected() && secondDefaultIndex != index && Playlist.Type.ONLINE.equals(playlist.getType())) {
                switchNow(secondDefaultIndex, false);
                return;
            } else {
                player = buildPlayer();
                keepBroadcasting();
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

                    Logger.log(AuditLog.Event.PLAYLIST, getPlayingAtIndexLabel(index), new GsonBuilder().setPrettyPrinting().create().toJson(playlist));

                    // extract and add programs
                    programItems = extractingMediaItemsFromPrograms(programs);
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
                        if (seek != null) {
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
                    Player current = getPlayerView().getPlayer();
                    if (current != null) {
                        current.removeListener(instance);
                    }

                    player.addListener(instance);
                    player.setPlayWhenReady(true);
                    Logger.log(AuditLog.Event.PLAYLIST_PLAY, getPlayingAtIndexLabel(index), formatDuration(position), getMediaItemName(programItems.get(program)));
                    nowPlayingIndex = index;
                    triggerGraphics(position);
                }
            }
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        PlayerView playerView = getPlayerView();
        Player current = playerView.getPlayer();
        if (current == null || !player.equals(current)) {// change of player is proof of a switch
            if (current != null) {
                current.stop();
                current.release();
                current = null;
            }
            playerView.setPlayer(player);
        }
    }

    private boolean playTheSame(int index) {
        return player != null && (!player.isPlaying() && nowPlayingIndex == index);
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
        if (isResuming(playlist) || playlist.getType().equals(Playlist.Type.LOCAL_RANDOMIZED) || playlist.getType().equals(Playlist.Type.ONLINE)) {
            return new Seek(program, position);
        } else {
            for (int i = 0; i < mediaItems.size(); i++) {// mediaItems are well ordered
                long now = Calendar.getInstance().getTimeInMillis();
                long duration = getDuration(mediaItems.get(i).mediaId);
                if (duration + startTime > now) {
                    // use the first item
                    return new Seek(i, now - startTime);
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

    private PlayerView getPlayerView() {
        PlayerView playerView = findViewById(R.id.player);
        playerView.setControllerShowTimeoutMs(0);
        playerView.setControllerHideOnTouch(false);
        playerView.showController();
        return playerView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (nowPlayingIndex != null) {
            if (state == Player.STATE_ENDED) {
                Logger.log(AuditLog.Event.PLAYLIST_COMPLETED, getNowPlayingPlaylistLabel());
                switchNow(getSecondDefaultIndex(), false);
            } else if (state == Player.STATE_BUFFERING && Playlist.Type.ONLINE.equals(playlistByIndex.get(nowPlayingIndex).getType())) {
                player.seekTo(player.getContentDuration());// hack
            }
        }
    }

    private String getMediaItemName(MediaItem mediaItem) {
        String name = "";
        try {
            name = URLDecoder.decode(mediaItem.mediaId.replace("file://", "").replace(programsFolder.getAbsolutePath(), ""), "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
        return name;
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (nowPlayingIndex != null) {
            int item = getPlayerView().getPlayer().getCurrentPeriodIndex() - 1;// last item index
            trackingNowPlaying(nowPlayingIndex, item, 0);
            Logger.log(AuditLog.Event.PLAYLIST_ITEM_CHANGE, getNowPlayingPlaylistLabel(), getMediaItemName(mediaItem));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Logger.log(AuditLog.Event.ERROR, String.format("%s: %s", error.getCause().toString(), error.getMessage()));
        cacheNowPlaying();
        // keep reloading existing program if internet is on and off
        if (error.getCause().getCause() instanceof UnknownHostException || error.getCause().getCause() instanceof IOException) {
            Logger.log(AuditLog.Event.NO_INTERNET, "Failing to play program because of no internet connection");
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
        if (getConfiguration().isNotificationsDisabled()) {
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
        Logger.log(AuditLog.Event.KEY_PRESS, KeyEvent.keyCodeToString(keyCode) + "#" + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    private void triggerGraphics(long position) {
        hideLogo();
        hideTicker();
        hideLowerThird();
        Playlist currentPlayList = playlistByIndex.get(nowPlayingIndex);
        Graphics graphics = currentPlayList.getGraphics();
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
                            }, start - position);
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
                        if(start >= position) {
                            Monitor.instance.getHandler().postDelayed(() -> {
                                showTicker();
                            }, start - position);
                        }
                    });
                }
            }
        }
    }

    private void hideLowerThird() {
        if(lowerThirdView != null) {
            //TODO lowerThirdView.animate().translationX(lowerThirdView.getWidth()); etc should be in the clip
            lowerThirdView.setVisibility(View.GONE);
            Logger.log(AuditLog.Event.LOWER_THIRD_OFF);
        }
    }

    private void hideTicker() {
        if(tickerView != null) {
            tickerView.setVisibility(View.GONE);
            tickerView.removeChildViews();
            tickerView.destroyAllScheduledTasks();
            Logger.log(AuditLog.Event.DISPLAY_NEWS_OFF);
        }
    }

    private void hideLogo() {
        findViewById(R.id.topLogo).setVisibility(View.GONE);
        findViewById(R.id.bottomLogo).setVisibility(View.GONE);
        Logger.log(AuditLog.Event.DISPLAY_LOGO_OFF);
    }

    private void playCurrentLowerThird() {
        if(lowerThirdView != null) {
            lowerThirdView.setVisibility(View.VISIBLE);
            lowerThirdView.start();
        }
    }

    private void showLowerThird(LowerThird lowerThird) {
        String path = getLowerThirdDirectory() + File.separator + lowerThird.getFile();
        File lowerThirdClip =  new File(path);
        if(lowerThirdClip.exists()) {
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
        tickerView.setBackgroundColor(getColor(android.R.color.transparent));
        Logger.log(AuditLog.Event.DISPLAY_NEWS_ON, news.getMessages());
        for (String message : news.getMessagesArray()) {
            tickerView.addChildView(tickerView(message));
        }
    }

    private void showTicker() {
        tickerView.showTickers();//TODO add time run in context
        tickerView.setVisibility(View.VISIBLE);
    }

    private void showLogo(Graphics.LogoPosition logoPosition) {
        File logo =  new File(programsFolder.getAbsolutePath() + File.separator + "logo.png");
        if(logo.exists() && logoPosition != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(logo.getAbsolutePath());
            ImageView myImage;
            if(Graphics.LogoPosition.TOP.equals(logoPosition)) {
                myImage = findViewById(R.id.topLogo);
                Logger.log(AuditLog.Event.DISPLAY_LOGO_ON, Graphics.LogoPosition.TOP.name());
            } else {
                myImage = findViewById(R.id.bottomLogo);
                Logger.log(AuditLog.Event.DISPLAY_LOGO_ON, Graphics.LogoPosition.BOTTOM.name());
            }
            myImage.setImageBitmap(myBitmap);
            myImage.setVisibility(View.VISIBLE);
        }
    }

    private TextView tickerView(String message) {
        TextView tickerView = new TextView(instance);
        tickerView.setLayoutParams(new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        tickerView.setText(message);
        tickerView.setTextSize(getResources().getDimension(R.dimen.tickerFontSize));
        tickerView.setBackgroundColor(getColor(R.color.trans));
        tickerView.setTextColor(ContextCompat.getColor(instance, android.R.color.white));
        tickerView.setPadding(10, 2, 10, 2);
        return tickerView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void keepBroadcasting() {
        if(keepOnair != null) {
            handler.removeCallbacks(keepOnair);
        }
        long delay = 60000;// one minute of being off air will trigger the same program
        handler.postDelayed(keepOnair = () -> {
            handler.postDelayed(keepOnair, delay);
            if(offAir) {//been off air for the past delay
                offAir = false;
                Logger.log(AuditLog.Event.STUCK, delay/1000);
                switchNow(nowPlayingIndex, false);// replay the same program
            } else {
                if (!player.isPlaying()) {
                    offAir = true;
                } else {
                    offAir = false;
                }
            }
        }, delay);
    }

}