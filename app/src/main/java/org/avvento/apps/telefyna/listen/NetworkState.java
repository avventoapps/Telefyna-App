package org.avvento.apps.telefyna.listen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

public class NetworkState extends BroadcastReceiver {
    protected List<NetworkStateListener> listeners;
    protected Boolean connected;

    public NetworkState() {
        listeners = new ArrayList<>();
        connected = null;
    }

    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {    //Boolean that indicates whether there is a complete lack of connectivity
            connected = false;
        }
        notifyStateToAll();
    }

    private void notifyStateToAll() {
        for(NetworkStateListener eachNetworkStateListener : listeners) {
            notifyState(eachNetworkStateListener);
        }
    }

    private void notifyState(NetworkStateListener networkStateListener) {
        if(connected == null || networkStateListener == null) {
            return;
        }
        if(connected == true) {
            networkStateListener.networkAvailable();
        } else {
            networkStateListener.networkUnavailable();
        }
    }

    public void addListener(NetworkStateListener networkStateListener) {
        listeners.add(networkStateListener);
    }

    public void removeListener(NetworkStateListener networkStateListener) {
        listeners.remove(networkStateListener);
    }

    public interface NetworkStateListener {
        void networkAvailable();
        void networkUnavailable();
    }
}