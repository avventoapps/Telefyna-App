package org.avvento.apps.telefyna.listen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.MimeTypes;

import org.apache.commons.lang3.StringUtils;
import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.stream.Config;
import org.avvento.apps.telefyna.stream.Playlist;
import org.avvento.apps.telefyna.stream.Program;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.RequiresApi;

public class Maintenance {

    private Map<String, CurrentPlaylist> startedSlotsToday;
    private static int CODE = 0;

    private void logMaintenance() {
        Logger.log(AuditLog.Event.MAINTENANCE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void prepareSchedule(boolean fromMaintainer) {
        startedSlotsToday = new HashMap<>();
        if(fromMaintainer) {
            Monitor.instance.initialiseConfiguration();
        }
        schedule();
        logMaintenance();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void run() {
        prepareSchedule(false);
        Monitor.instance.getHandler().postDelayed(new Runnable() {// maintainer
            public void run() {
                prepareSchedule(true);
                Monitor.instance.getHandler().postDelayed(this, getMillsToMaintenanceTime());
                logMaintenance();
            }
        }, getMillsToMaintenanceTime());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void schedule() {
        Config config = Monitor.instance.getConfiguration();
        if(config != null) {
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
                            setupLocalPrograms(programs, localPlaylistFolder, addedFirstItem);
                        }
                    }
                } else {
                    programs = Monitor.instance.getProgramsByIndex().get(clone);
                    playlist = playlists[clone].copy(playlist);
                    playlist.setClone(clone);// only use playlist scheduling details for scheduling
                }
                // take the first start in a day to avoid scheduling for more than once
                if (playlist.scheduledToday() && StringUtils.isNotBlank(playlist.getStart()) && !starts.contains(playlist.getStart())) {
                    schedulePlayList(playlist, index);
                    starts.add(playlist.getStart());
                }
                Monitor.instance.putProgramsByIndex(index, programs);
            }
            playCurrentSlot();
        }
    }

    private Program extractProgramFromFile(File file) {
        return new Program(file.getAbsolutePath().split(Monitor.instance.getProgramsFolderPath())[1], MediaItem.fromUri(Uri.fromFile(file)));
    }

    public void setupLocalPrograms(List<Program> programs, File fileOrFolder, boolean addedFirstItem) {
        if(fileOrFolder.exists()) {
            File[] fileOrFolderList = fileOrFolder.listFiles();
            Arrays.sort(fileOrFolderList);// ordering programs alphabetically
            for (int j = 0; j < fileOrFolderList.length; j++) {
                File file = fileOrFolderList[j];
                if (file.isDirectory()) {
                    setupLocalPrograms(programs, file, addedFirstItem);
                } else {
                    if (j == 0 && !addedFirstItem) {
                        programs.add(0, extractProgramFromFile(file));
                        addedFirstItem = true;
                    } else {
                        programs.add(extractProgramFromFile(file));
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
            Monitor.instance.switchNow(currentPlaylist.getIndex());
        } else { // play first default
            Monitor.instance.switchNow(Monitor.instance.getFirstDefaultIndex());
        }
    }

    private void schedulePlayList(Playlist playlist, int index) {
        String start = playlist.getStart();
        Integer hour = Integer.parseInt(start.split(":")[0]);
        Integer min = Integer.parseInt(start.split(":")[1]);

        Calendar current = Calendar.getInstance();
        if (hour < current.get(Calendar.HOUR_OF_DAY) || (hour == current.get(Calendar.HOUR_OF_DAY) && min <= current.get(Calendar.MINUTE))) {
            startedSlotsToday.put(start, new CurrentPlaylist(playlist.isClone() ? playlist.getClone() : index, playlist));
        } else {
            Intent intent = new Intent(Monitor.instance, PlaylistScheduler.class);
            intent.putExtra(PlaylistScheduler.PLAYLIST_INDEX, index);
            schedule(intent, nextTime(hour, min));
        }
    }

    private void schedule(Intent intent, long mills) {
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(Monitor.instance, CODE++, intent, 0);
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
     * Runs at midnight
     */
    private long getMillsToMaintenanceTime() {// close to midnight
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
