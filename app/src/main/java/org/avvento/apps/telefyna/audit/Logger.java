package org.avvento.apps.telefyna.audit;

import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.avvento.apps.telefyna.Monitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
    private static SimpleDateFormat datetimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static SimpleDateFormat dateFormat  = new SimpleDateFormat("yyyy-MM-dd");

    /*
     * TODO mail, save to file
     */
    public static void log(AuditLog.Event event, Object... params) {
        String message = String.format(event.getMessage(), params);
        Log.i(event.name(), message);
        try {
            FileUtils.writeStringToFile(new File(Monitor.instance.getAuditLogsFilePath(getToday())), String.format("%s %s: \n\t%s", getNow(), event.name(), message), StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            Log.e("WRITING_AUDIT_ERROR", e.getMessage());
        }
    }

    public static String getNow() {
        return datetimeFormat.format(Calendar.getInstance().getTime());
    }

    public static String getToday() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }
}
