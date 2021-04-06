package org.avvento.apps.telefyna;

import android.net.Uri;
import android.os.Build;

import com.google.android.exoplayer2.MediaItem;

import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Playlist;
import org.avvento.apps.telefyna.modal.Program;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

public class Utils {

    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * seconds
     */
    public static boolean internetConnected() {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8");
            if (process.waitFor() == 0) {
                return true;
            } else {
                return false;
            }
        } catch (IOException | InterruptedException e) {
            Logger.log(AuditLog.Event.NO_INTERNET, e.getMessage());
            return false;
        }
    }

    public static void setupLocalPrograms(List<Program> programs, File fileOrFolder, boolean addedFirstItem, Playlist playlist, boolean orderPrograms) {
        if (fileOrFolder.exists()) {
            File[] fileOrFolderList = fileOrFolder.listFiles();
            if(orderPrograms) {
                Arrays.sort(fileOrFolderList);// ordering programs alphabetically
            }
            for (int j = 0; j < fileOrFolderList.length; j++) {
                File file = fileOrFolderList[j];
                if (file.isDirectory()) {
                    setupLocalPrograms(programs, file, addedFirstItem, playlist, orderPrograms);
                } else {
                    if (j == 0 && !addedFirstItem) {// first in the folder if not yet addedFirstItem
                        programs.add(0, extractProgramFromFile(file, playlist.isUsingExternalStorage()));
                        addedFirstItem = true;
                    } else {
                        programs.add(extractProgramFromFile(file, playlist.isUsingExternalStorage()));
                    }
                }
            }
            if (Playlist.Type.LOCAL_RANDOMIZED.equals(playlist.getType())) {
                Collections.shuffle(programs);
                Collections.shuffle(programs);
            }
        }
    }

    private static Program extractProgramFromFile(File file, boolean usingExternalStorage) {
        return new Program(file.getAbsolutePath().split(Monitor.instance.getProgramsFolderPath(usingExternalStorage))[1], MediaItem.fromUri(Uri.fromFile(file)));
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
