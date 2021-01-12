package org.avvento.apps.telefyna;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

public class Utils {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean internetConnected() {
        try {
            // credit to google for creating exoplayer here
            (new URL("http://www.google.com")).openConnection().connect();
            return true;
        } catch (IOException e) {
            Logger.log(AuditLog.Event.ERROR, e.getMessage());
            return false;
        }
    }
}
