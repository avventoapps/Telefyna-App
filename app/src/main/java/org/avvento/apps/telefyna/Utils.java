package org.avvento.apps.telefyna;

import android.net.Uri;
import android.os.Build;

import com.google.android.exoplayer2.MediaItem;

import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Program;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

public class Utils {

    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * seconds
     */
    public static boolean internetConnected(int delay) {
        try {
            // credit to google for creating exoplayer here
            // TODO unfortunately google (http://www.google.com) is on in uganda for free without data. changed to http://example.com
            URLConnection conn = (new URL("http://example.com")).openConnection();
            conn.setConnectTimeout(delay * 1000);
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (IOException e) {
            Logger.log(AuditLog.Event.NO_INTERNET, e.getMessage());
            return false;
        }
    }

    public static void setupLocalPrograms(List<Program> programs, File fileOrFolder, boolean addedFirstItem) {
        if (fileOrFolder.exists()) {
            File[] fileOrFolderList = fileOrFolder.listFiles();
            Arrays.sort(fileOrFolderList);// ordering programs alphabetically
            for (int j = 0; j < fileOrFolderList.length; j++) {
                File file = fileOrFolderList[j];
                if (file.isDirectory()) {
                    setupLocalPrograms(programs, file, addedFirstItem);
                } else {
                    if (j == 0 && !addedFirstItem) {// first in the folder if not yet addedFirstItem
                        programs.add(0, extractProgramFromFile(file));
                        addedFirstItem = true;
                    } else {
                        programs.add(extractProgramFromFile(file));
                    }
                }
            }
        }
    }

    private static Program extractProgramFromFile(File file) {
        return new Program(file.getAbsolutePath().split(Monitor.instance.getProgramsFolderPath())[1], MediaItem.fromUri(Uri.fromFile(file)));
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.matches(regex);
    }

    public static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hours));
        long secs = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.MINUTES.toMillis(mins));
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

}
