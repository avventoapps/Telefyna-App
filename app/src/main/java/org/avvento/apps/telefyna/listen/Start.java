package org.avvento.apps.telefyna.listen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.avvento.apps.telefyna.Monitor;

public class Start extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, Monitor.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtras(intent);
        context.startActivity(i);
    }
}