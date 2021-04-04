package org.avvento.apps.telefyna.audit;

import android.os.Build;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.listen.mail.SendEmail;
import org.avvento.apps.telefyna.modal.Config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.RequiresApi;

public class Logger {
    private static SimpleDateFormat datetimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /*
     * TODO mail, save to file
     */
    public static void log(AuditLog.Event event, Object... params) {
        String message = String.format(event.getMessage(), params);
        if (event.equals(AuditLog.Event.ERROR)) {
            Log.e(event.name(), message);
        } else {
            Log.i(event.name(), message);
        }
        String path = Monitor.instance.getAuditLogsFilePath(getToday());
        String msg = String.format("%s %s: \n\t%s", getNow(), event.name(), message);
        try {
            FileUtils.writeStringToFile(new File(path), msg.replaceAll("<br>", ","), StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            Log.e("WRITING_AUDIT_ERROR", e.getMessage());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            emailAudit(event, msg);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void emailAudit(AuditLog.Event event, String msg) {
        // email notification
        Config config = Monitor.instance.getConfiguration();
        if(config != null && config.getAlerts() != null && (AuditLog.Event.Category.ADMIN.equals(event.getCategory()) || AuditLog.Event.Category.BROADCAST.equals(event.getCategory()))) {
            if(Utils.internetConnected()) {
                new SendEmail().execute(new AuditAlert(config.getAlerts(), event, msg));
            } else {
                Logger.log(AuditLog.Event.NO_INTERNET, "Sending emails failed, no internet connection");
            }
        }
    }

    private static String getNow() {
        return datetimeFormat.format(Calendar.getInstance().getTime());
    }

    public static String getToday() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    public static List<String> getAuditsForNDays(int days) {
        List<String> audits = new ArrayList<>();
        File auditDir = new File(Monitor.instance.getAuditFilePath(""));
        if(auditDir.exists()) {
            File[] auditContents = auditDir.listFiles();
            if(auditContents.length > 0) {
                for (int i = 0; i < days; i++) {
                    String audit;
                    if(i == 0) {
                        audit = Monitor.instance.getAuditLogsFilePath(getToday());
                    } else {
                        Calendar d = Calendar.getInstance();
                        d.add(Calendar.DAY_OF_YEAR, i * -1);// - one day
                        audit = Monitor.instance.getAuditLogsFilePath(dateFormat.format(d.getTime()));
                    }
                    audits.add(audit);
                }
            }
        }
        return audits;
    }
}
