package org.avvento.apps.telefyna;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.File;
import java.util.Collections;

public class Metrics {

    private static String getFreeDiskSpace(String volume) {
        File path = new File(volume);
        if(path.exists()) {
            StatFs stats = new StatFs(path.getAbsolutePath());
            return String.format("<b>%s</b> FreeSpace: %d MB<br>", path.getAbsoluteFile(), (stats.getAvailableBlocksLong() * stats.getBlockSizeLong()) / (1024 * 1024));
        }
        return "";
    }

    private static String getFreeMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) Monitor.instance.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return String.format("FreeMemory: %d MB<br>", mi.availMem/(1024 * 1024));
    }

    private static String getUptime() {
        return String.format("UpTime: %s", Utils.formatDuration(SystemClock.uptimeMillis()));
    }

    public static String retrieve() {
        String metrics = "Network: " + String.join(",", Utils.logLocalIpAddresses()) + "<br>" + Utils.readUrl("https://ipinfo.io/json");
        metrics += getFreeDiskSpace(Monitor.instance.getAuditFilePath(""));
        metrics += getFreeDiskSpace(Monitor.instance.getProgramsFolderPath(false));
        metrics += getFreeDiskSpace(Monitor.instance.getProgramsFolderPath(true));
        metrics += getFreeMemory();
        metrics += getUptime();
        return metrics;
    }
}
