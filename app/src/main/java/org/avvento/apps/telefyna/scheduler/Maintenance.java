package org.avvento.apps.telefyna.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.google.android.exoplayer2.MediaItem;

import org.avvento.apps.telefyna.MainActivity;
import org.avvento.apps.telefyna.stream.Playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.RequiresApi;

public class Maintenance {

    private Map<String, CurrentPlaylist> startedSlotsToday = new HashMap<>();
    private static int CODE = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void run() {
        schedule();
        MainActivity.instance.getHandler().postDelayed(new Runnable() {
            public void run() {
                schedule();
                MainActivity.instance.getHandler().postDelayed(this, getMillsToMidNight());
            }
        }, getMillsToMidNight());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void schedule() {
        Playlist[] playlists = MainActivity.instance.getConfiguration().getPlaylists();
        for (int index = 0; index< playlists.length; index++) {
            Playlist playlist = playlists[index];
            Integer clone = playlist.getClone();
            List<MediaItem> mediaItems = new ArrayList<>();
            if(clone == null) {
                if(Playlist.Type.LOCAL.equals(playlist.getType())) {
                    File localPlaylistFolder = new File(MainActivity.instance.getPlaylistDirectory() + File.separator + playlist.getUrlOrFolder());
                    boolean addedFirstItem = false;
                    setupLocalPlaylist(mediaItems, localPlaylistFolder, addedFirstItem);
                } else {
                    mediaItems.add(MediaItem.fromUri(playlist.getUrlOrFolder()));
                }
            } else {
                clone--;
                mediaItems = MainActivity.instance.getPlayout().get(clone);
                playlist = playlists[clone].copy(playlist.isActive(), playlist.getDay(), playlist.getRepeats(), playlist.getStart());
            }
            if (!mediaItems.isEmpty()) {
                if(playlist.isActive()) {
                    schedulePlayList(playlist, index);
                }
                MainActivity.instance.putPlayout(index, mediaItems);
            }
        }
        playCurrentSlot();
    }

    private void setupLocalPlaylist(List<MediaItem> mediaItems, File fileOrFolder, boolean addedFirstItem) {
        if(fileOrFolder.exists()) {
            File[] fileOrFolderList = fileOrFolder.listFiles();
            Arrays.sort(fileOrFolderList);// ordering programs alphabetically
            for (int j = 0; j < fileOrFolderList.length; j++) {
                File file = fileOrFolderList[j];
                if (file.isDirectory()) {
                    setupLocalPlaylist(mediaItems, file, addedFirstItem);
                } else {
                    if (j == 0 && !addedFirstItem) {
                        mediaItems.add(0, MediaItem.fromUri(Uri.fromFile(file)));
                        addedFirstItem = true;
                    } else {
                        mediaItems.add(MediaItem.fromUri(Uri.fromFile(file)));
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void playCurrentSlot() {
        if(!startedSlotsToday.isEmpty()) {
            List<String> slots = startedSlotsToday.keySet().stream().collect(Collectors.toList());
            Collections.sort(slots, Collections.reverseOrder());
            CurrentPlaylist currentPlaylist = startedSlotsToday.get(slots.get(0));
            MainActivity.instance.switchNow(currentPlaylist.getIndex());
        }
    }

    private void schedulePlayList(Playlist playlist, int index) {
        if(playlist.scheduledToday()) {
            String start = playlist.getStart();
            Integer hour = Integer.parseInt(start.substring(0, 2));
            Integer min = Integer.parseInt(start.substring(2, 4));

            Calendar current = Calendar.getInstance();
            if (hour < current.get(Calendar.HOUR_OF_DAY) || (hour == current.get(Calendar.HOUR_OF_DAY) && min <= current.get(Calendar.MINUTE))) {
                startedSlotsToday.put(playlist.getStart(), new CurrentPlaylist(index, playlist));
            } else {
                Intent intent = new Intent(MainActivity.instance, PlaylistScheduler.class);
                intent.putExtra(PlaylistScheduler.PLAYLIST_INDEX, index);
                schedule(intent, nextTime(hour, min));
            }
        }
    }

    private void schedule(Intent intent, long mills) {
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(MainActivity.instance, CODE++, intent, 0);
        MainActivity.instance.getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, mills, alarmPendingIntent);
    }

    private long nextTime(int hour, int min) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, min);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        return next.getTimeInMillis();
    }

    /*
     * Runs at midnight
     */
    private long getMillsToMidNight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return (c.getTimeInMillis() - System.currentTimeMillis());
    }
}
