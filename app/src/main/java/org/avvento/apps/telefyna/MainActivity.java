package org.avvento.apps.telefyna;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements PlayerNotificationManager.NotificationListener {

    public DisplayManager displayManager = null;
    public Display[] presentationDisplays = null;
    private Monitor monitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        displayManager =  (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager!= null) {
            presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
            monitor = new Monitor(MainActivity.this, presentationDisplays.length > 0 ? presentationDisplays[0] : ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
            monitor.show();
        }
    }

    @Override
    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
        if(monitor.getConfiguration().isDisableNotifications()) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);
        }
    }
}