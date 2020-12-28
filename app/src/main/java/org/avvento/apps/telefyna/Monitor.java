package org.avvento.apps.telefyna;

import android.app.AlarmManager;
import android.app.Presentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.scheduler.Maintenance;
import org.avvento.apps.telefyna.stream.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class Monitor extends Presentation implements Player.EventListener {
    public static Monitor instance;
    private Config configuration;
    private AlarmManager alarmManager;
    private Handler handler;
    private Maintenance maintenance;
    private SimpleExoPlayer currentPlayer;
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private Map<Integer, List<MediaItem>> playout;

    public Monitor(Context outerContext, Display display) {
        super(outerContext, display);
    }

    public void putPlayout(Integer index, List<MediaItem> mediaItems) {
        playout.put(index, mediaItems);
    }

    public Map<Integer, List<MediaItem>> getPlayout() {
        return playout;
    }

    public Config getConfiguration() {
        return configuration;
    }

    public AlarmManager getAlarmManager() {
        return alarmManager;
    }

    public Handler getHandler() {
        return handler;
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
        player = new SimpleExoPlayer.Builder(getContext()).build();
        for(int i = 0; i < mediaItems.size(); i++) {
            if(i == 0) {
                player.setMediaItem(mediaItems.get(i));
            } else {
                player.addMediaItem(mediaItems.get(i));
            }
        }
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(this);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        maintenance = new Maintenance();
        handler = new Handler();

        instance.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        configuration = readConfiguration();
        alarmManager = ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE));

        initPlayer();
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
            storages = ContextCompat.getExternalFilesDirs(getContext(), null);
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
    private void initPlayer() {
        playout = new HashMap<>();
        playerView = findViewById(R.id.player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setUseController(false);
        maintenance.run();
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if(isPlaying && currentPlayer != player) {
            if (currentPlayer != null) {
                currentPlayer.release();
            }
            playerView.setPlayer(player);
            currentPlayer = player;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            switchToDefault();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        switchToDefault();
    }

}
