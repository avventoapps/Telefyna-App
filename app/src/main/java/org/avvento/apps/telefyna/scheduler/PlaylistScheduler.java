package org.avvento.apps.telefyna.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.avvento.apps.telefyna.Monitor;

import androidx.annotation.RequiresApi;

public class PlaylistScheduler extends BroadcastReceiver {
    public static String PLAYLIST_INDEX = "playlist_index";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onReceive(Context context, Intent intent) {
        Monitor.instance.switchNow(intent.getIntExtra(PLAYLIST_INDEX, 0));
    }
}
