package org.avvento.apps.telefyna;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.android.exoplayer2.MediaItem;

import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Playlist;
import org.avvento.apps.telefyna.modal.Program;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.4.4");
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

    public static void setupLocalPrograms(List<MediaItem> programs, File fileOrFolder, boolean addedFirstItem, Playlist playlist) {
        if (fileOrFolder.exists()) {
            File[] fileOrFolderList = fileOrFolder.listFiles();
            if(Playlist.Type.LOCAL_SEQUENCED.equals(playlist.getType()) || playlist.isResuming()) {
                Arrays.sort(fileOrFolderList);// ordering programs alphabetically
            }
            for (int j = 0; j < fileOrFolderList.length; j++) {
                File file = fileOrFolderList[j];
                if (file.isDirectory()) {
                    setupLocalPrograms(programs, file, addedFirstItem, playlist);
                } else if(Utils.validPlayableItem(file)) {
                    if (j == 0 && !addedFirstItem) {// first in the folder if not yet addedFirstItem
                        programs.add(0, new MediaItem.Builder().setUri(Uri.fromFile(file)).setMediaId(Uri.fromFile(file).toString()).build());
                        addedFirstItem = true;
                    } else {
                        programs.add(new MediaItem.Builder().setUri(Uri.fromFile(file)).setMediaId(Uri.fromFile(file).toString()).build());
                    }
                }
            }
            if (Playlist.Type.LOCAL_RANDOMIZED.equals(playlist.getType())) {
                Collections.shuffle(programs);
                Collections.shuffle(programs);
            }
        }
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

    public static String readUrl(String urlString) {
        BufferedReader reader;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            if (reader != null) {
                reader.close();
            }
            return buffer.toString();
        } catch (Exception e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
        return null;
    }

    public static List<String> logLocalIpAddresses() {
        Enumeration<NetworkInterface> nwis;
        List<String> ips = new ArrayList<>();
        try {
            nwis = NetworkInterface.getNetworkInterfaces();
            while (nwis.hasMoreElements()) {
                NetworkInterface ni = nwis.nextElement();
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    ips.add(String.format("%s: %s/%d", ni.getDisplayName(), ia.getAddress(), ia.getNetworkPrefixLength()));
                }
            }
        } catch (Exception e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
        }
        return ips;
    }

    // TODO add more better algorithm
    public static boolean validPlayableItem(File file) {
        return file.exists() && !file.getName().startsWith(".");
    }

}
