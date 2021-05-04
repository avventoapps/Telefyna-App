package org.avvento.apps.telefyna.listen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.MimeTypes;

import org.avvento.apps.telefyna.Metrics;
import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Config;
import org.avvento.apps.telefyna.modal.Playlist;
import org.avvento.apps.telefyna.modal.Program;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.RequiresApi;

public class Maintenance {

    private static int CODE = 0;
    private Map<String, CurrentPlaylist> startedSlotsToday;
    private Map<String, PendingIntent> pendingIntents;

    /**
     * Called when Telefyna is launched and everyday at midnight
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void triggerMaintenance() {
        cancelPendingIntents();
        Monitor.instance.initialise();
        // switch to firstDefault when automation is turned off
        if (Monitor.instance.getConfiguration().isAutomationDisabled()) {
            int defaultIndex = Monitor.instance.getFirstDefaultIndex();
            Playlist playlist = Monitor.instance.getConfiguration().getPlaylists()[defaultIndex];
            Monitor.instance.addPlayListByIndex(playlist);
            Monitor.instance.switchNow(defaultIndex, false);
        } else {
            startedSlotsToday = new HashMap<>();
            pendingIntents = new HashMap<>();
            Logger.log(AuditLog.Event.METRICS, Metrics.retrieve());
            Logger.log(AuditLog.Event.MAINTENANCE);
            schedule();
        }
    }

    public void cancelPendingIntents() {
        if(pendingIntents != null && !pendingIntents.isEmpty()) {
            for(PendingIntent intent : pendingIntents.values()) {// TODO test
                Monitor.instance.getAlarmManager().cancel(intent);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void run() {
        triggerMaintenance();
        Logger.log(AuditLog.Event.HEARTBEAT, "ON");
        Monitor.instance.getMaintenanceHandler().postDelayed(new Runnable() {// maintainer
            public void run() {
                triggerMaintenance();
                Monitor.instance.getMaintenanceHandler().postDelayed(this, getMillsToMaintenanceTime());
            }
        }, getMillsToMaintenanceTime());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void schedule() {
        Config config = Monitor.instance.getConfiguration();

        if (config != null) {
            Playlist[] playlists = config.getPlaylists();
            List<String> starts = new ArrayList<>();
            for (int index = 0; index < playlists.length; index++) {
                Playlist playlist = playlists[index];
                Integer schedule = playlist.getSchedule();
                if (schedule != null) {
                    playlist = playlist.schedule(playlists[schedule]);
                }

                if (playlist.scheduledToday()) {
                    schedulePlaylistAtStart(playlist, index, starts);
                }
                Monitor.instance.addPlayListByIndex(playlist);
            }
            playCurrentSlot();
        }
    }

    public List<MediaItem> retrievePrograms(Playlist playlist) {
        List<MediaItem> programs = new ArrayList<>();
        if(playlist != null) {
            if (playlist.getType().equals(Playlist.Type.ONLINE)) {
                programs.add(MediaItem.fromUri(playlist.getUrlOrFolder()));
            } else {
                for (int i = 0; i < playlist.getUrlOrFolder().split("#").length; i++) {
                    List<MediaItem> pgms = new ArrayList<>();
                    File localPlaylistFolder = Monitor.instance.getDirectoryFromPlaylist(playlist, i);
                    if (localPlaylistFolder.exists() && localPlaylistFolder.listFiles().length > 0) {
                        boolean addedFirstItem = false;
                        Utils.setupLocalPrograms(pgms, localPlaylistFolder, addedFirstItem, playlist, false);
                        programs.addAll(pgms);
                    }
                }
            }
        }
        return programs;
    }

    private void schedulePlaylistAtStart(Playlist playlist, int index, List<String> starts) {
        // was scheduled, remove existing playlist to reschedule a new later one
        String start = playlist.getStart();
        if (starts.contains(start)) {
            PendingIntent operation = pendingIntents.get(start);
            if(operation != null) {
                Monitor.instance.getAlarmManager().cancel(operation);
            }
            startedSlotsToday.remove(start);
            starts.remove(start);
        }
        schedulePlayList(playlist, index);
        starts.add(start);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void playCurrentSlot() {
        if (!startedSlotsToday.isEmpty()) {
            List<String> slots = startedSlotsToday.keySet().stream().collect(Collectors.toList());
            Collections.sort(slots, Collections.reverseOrder());
            CurrentPlaylist currentPlaylist = startedSlotsToday.get(slots.get(0));
            // isCurrentSlot should only be true here
            Monitor.instance.switchNow(currentPlaylist.getIndex(), true);
        } else { // play first default
            Monitor.instance.switchNow(Monitor.instance.getFirstDefaultIndex(), false);
        }
    }

    private void schedulePlayList(Playlist playlist, int index) {
        String start = playlist.getStart();
        if (playlist.isStarted()) {
            startedSlotsToday.put(start, new CurrentPlaylist(index, playlist));
        } else {
            Intent intent = new Intent(Monitor.instance, PlaylistScheduler.class);
            intent.putExtra(PlaylistScheduler.PLAYLIST_INDEX, index);
            schedule(intent, playlist.getScheduledTime(), playlist.getStart());
        }
    }

    private void schedule(Intent intent, long mills, String start) {
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(Monitor.instance, CODE++, intent, 0);
        pendingIntents.put(start, alarmPendingIntent);
        Monitor.instance.getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, mills, alarmPendingIntent);
    }

    /*
     * Maintenance time is currently set to midnight
     */
    private long getMillsToMaintenanceTime() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - System.currentTimeMillis();
    }

    // TODO and use fix
    private boolean isSupportedImageAudioOrVideo(String url) {
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
        return MimeTypes.isAudio(type) || MimeTypes.isVideo(type);
    }
}
