package org.avvento.apps.telefyna;

import android.net.Uri;
import android.os.Build;

import com.google.android.exoplayer2.MediaItem;

import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.stream.Program;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.RequiresApi;

public class Utils {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean internetConnected() {
        try {
            // credit to google for creating exoplayer here
            URLConnection conn = (new URL("http://www.google.com")).openConnection();
            conn.setConnectTimeout(250);
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (IOException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
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

}
