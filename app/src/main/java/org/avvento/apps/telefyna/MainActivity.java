package org.avvento.apps.telefyna;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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
}