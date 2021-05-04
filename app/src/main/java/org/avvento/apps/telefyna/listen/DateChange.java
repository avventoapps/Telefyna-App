package org.avvento.apps.telefyna.listen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;

public class DateChange extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
            Logger.log(AuditLog.Event.TIME_CHANGED);
            //Monitor.instance.getMaintenance().run(); TODO do more here
        }
    }
}
