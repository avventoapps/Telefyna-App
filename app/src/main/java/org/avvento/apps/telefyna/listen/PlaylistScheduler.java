package org.avvento.apps.telefyna.listen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;

import androidx.annotation.RequiresApi;

public class PlaylistScheduler extends BroadcastReceiver {
    public static String PLAYLIST_INDEX = "playlist_index";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Monitor.instance.switchNow(intent.getIntExtra(PLAYLIST_INDEX, Monitor.instance.getFirstDefaultIndex()), false);
        } catch (Exception e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
    }
}
