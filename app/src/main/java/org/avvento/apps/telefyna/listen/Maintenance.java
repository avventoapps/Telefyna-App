package org.avvento.apps.telefyna.listen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.MimeTypes;

import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;
import org.avvento.apps.telefyna.stream.Program;

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
     * Called when Telefyns is lauched and everyday at midnight
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void triggerMaintenance() {
        Monitor.instance.initialiseConfiguration();
        // switch to firstDefault when automation is turned off
        if(Monitor.instance.getConfiguration().isAutomationDisabled()) {
            Monitor.instance.switchNow(Monitor.instance.getFirstDefaultIndex(), false);
        } else {
            startedSlotsToday = new HashMap<>();
            pendingIntents = new HashMap<>();
            Logger.log(AuditLog.Event.MAINTENANCE);
            schedule();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void run() {
        Logger.log(AuditLog.Event.HEARTBEAT, "ON");
        triggerMaintenance();
        Monitor.instance.getHandler().postDelayed(new Runnable() {// maintainer
            public void run() {
                triggerMaintenance();
                Monitor.instance.getHandler().postDelayed(this, getMillsToMaintenanceTime());
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
                Integer clone = playlist.getClone();
                List<Program> programs = new ArrayList<>();
                if (clone == null) {
                    if (playlist.getType().equals(Playlist.Type.ONLINE)) {
                        programs.add(new Program(playlist.getName(), MediaItem.fromUri(playlist.getUrlOrFolder())));
                    } else {
                        File localPlaylistFolder = Monitor.instance.getDirectoryToPlaylist(playlist.getUrlOrFolder());
                        if (localPlaylistFolder.exists() && localPlaylistFolder.listFiles().length > 0) {
                            boolean addedFirstItem = false;
                            Utils.setupLocalPrograms(programs, localPlaylistFolder, addedFirstItem);
                        }
                    }
                } else {
                    programs = Monitor.instance.getProgramsByIndex().get(clone);
                    playlist = playlist.copy(playlists[clone]);
                }

                if (playlist.scheduledToday()) {
                    schedulePlaylistAtStart(playlist, index, starts);
                }
                Monitor.instance.addProgramsByIndex(programs);
                Monitor.instance.addPlayListByIndex(playlist);
            }
            playCurrentSlot();
        }
    }

    private void schedulePlaylistAtStart(Playlist playlist, int index, List<String> starts) {
        // was scheduled, remove existing playlist to reschedule a new later one
        String start = playlist.getStart();
        if (starts.contains(start)) {
            Monitor.instance.getAlarmManager().cancel(pendingIntents.get(start));
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
        Integer hour = Integer.parseInt(start.split(":")[0]);
        Integer min = Integer.parseInt(start.split(":")[1]);

        Calendar current = Calendar.getInstance();
        if (hour < current.get(Calendar.HOUR_OF_DAY) || (hour == current.get(Calendar.HOUR_OF_DAY) && min <= current.get(Calendar.MINUTE))) {
            startedSlotsToday.put(start, new CurrentPlaylist(index, playlist));
        } else {
            Intent intent = new Intent(Monitor.instance, PlaylistScheduler.class);
            intent.putExtra(PlaylistScheduler.PLAYLIST_INDEX, index);
            schedule(intent, nextTime(hour, min), playlist.getStart());
        }
    }

    private void schedule(Intent intent, long mills, String start) {
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(Monitor.instance, CODE++, intent, 0);
        pendingIntents.put(start, alarmPendingIntent);
        Monitor.instance.getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, mills, alarmPendingIntent);
    }

    private long nextTime(int hour, int min) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, min);
        next.set(Calendar.SECOND, hour == 0 && min == 0 ? 5 : 0);// switch after 5 seconds since maintenance scheduler runs at 0 seconds
        next.set(Calendar.MILLISECOND, 0);
        return next.getTimeInMillis();
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
