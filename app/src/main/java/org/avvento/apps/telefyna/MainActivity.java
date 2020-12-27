package org.avvento.apps.telefyna;

import android.app.AlarmManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.scheduler.Maintenance;
import org.avvento.apps.telefyna.stream.Config;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements ExoPlayer.EventListener {
    public static MainActivity instance;
    private Config configuration;
    private AlarmManager alarmManager;
    private Handler handler;
    private Maintenance maintenance;
    private Map<Integer, SimpleExoPlayer> players;
    private SimpleExoPlayer currentPlayer;
    private PlayerView playerView;

    private VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    private void initVLC() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mVideoLayout = findViewById(R.id.video_layout);
        mMediaPlayer.attachViews(mVideoLayout, null, true, false);
    }



    public Map<Integer, SimpleExoPlayer> getPlayers() {
        return players;
    }

    public void putPlayer(Integer index, SimpleExoPlayer player) {
        players.put(index, player);
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

    public void switchNow(int index) {
        Iterator<Integer> it = players.keySet().iterator();
        if(it.hasNext()) {
            if (currentPlayer != null) {
                currentPlayer.pause();
            }
            while (it.hasNext()) {
                int i = it.next();
                SimpleExoPlayer player = players.get(index);
                if (i == index) {
                    player.prepare();
                    player.setPlayWhenReady(true);
                    player.addListener(this);
                    playerView.setPlayer(player);
                    currentPlayer = player;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        instance = this;
        maintenance = new Maintenance();
        handler = new Handler();

        instance.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        configuration = readConfiguration();
        alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));

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
            storages = ContextCompat.getExternalFilesDirs(this, null);
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

    public File getPlaylistDirectory() {
        return new File(getAppRootDirectory().getAbsolutePath() + File.separator + "playlist");
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
        players = new HashMap<>();
        playerView = findViewById(R.id.player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setUseController(false);
        maintenance.run();
     }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if(isBehindLiveWindow(error)) {

        }
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException error) {
        if (error.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = error.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}