package org.avvento.apps.telefyna;

import android.app.AlarmManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
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
    private Map<Integer, Player> players;
    private Player currentPlayer;
    private PlayerView playerView;

    public Map<Integer, Player> getPlayers() {
        return players;
    }

    public void putPlayer(Integer index, Player player) {
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
                Player player = players.get(index);
                if (i == index) {
                    player.setPlayWhenReady(true);
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
        handler = new Handler();
        maintenance.run();
     }
}