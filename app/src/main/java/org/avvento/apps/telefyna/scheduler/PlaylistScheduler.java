package org.avvento.apps.telefyna.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.avvento.apps.telefyna.MainActivity;

public class PlaylistScheduler extends BroadcastReceiver {
    public static String PLAYLIST_INDEX = "playlist_index";

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.instance.switchNow(intent.getIntExtra(PLAYLIST_INDEX, 0));
    }
}
