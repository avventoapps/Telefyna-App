package org.avvento.apps.telefyna.listen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.avvento.apps.telefyna.Monitor;

public class TelefynaUnCaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static String CRASH = "crash";
    public static String EXCEPTION = "exception";

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Intent intent = new Intent(Monitor.instance, Start.class);
        intent.putExtra(CRASH, true);
        intent.putExtra(EXCEPTION, ex.getMessage());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(Monitor.instance, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) Monitor.instance.getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        Monitor.instance.finish();
        System.exit(2);
    }
}