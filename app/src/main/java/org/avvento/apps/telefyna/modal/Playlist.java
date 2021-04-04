package org.avvento.apps.telefyna.modal;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Playlist {
    private static String DATE_FORMAT = "dd-MM-yyyy";

    private Boolean active = true;
    // lastModified date: Date#toLocaleString
    private String lastModified;
    private String name;
    private String description;
    // preview web color
    private String color;

    // graphics
    private Graphics graphics;

    /*
     * Each playlist can access 3 bumper folders and bumpers are only for local non resuming playlists;
     *  general (can be disabled by setting playingGeneralBumpers = false),
     *  specialBumpers
     *  one named after urlOrFolder
     */
    private boolean playingGeneralBumpers;
    // a name for folder in bumpers for special ones, this can be shared by other playlists by using the same name
    private String specialBumperFolder;
    private Type type = Type.ONLINE;
    /*
     * set url for non local folder or local folder where files should be added in the order with which they should play
     * use sub folders named in alphabetical order and symbolic links for fill ups
     * to maintain an order when playing, name programs or folders alphabetically
     */
    private String urlOrFolder;
    // days of the week [1-7=Sun-Sat]: if null, runs daily
    private boolean usingExternalStorage = false;
    private Integer[] days;
    // dates to schedule for, must be in DATE_FORMAT(dd-MM-yyyy)
    private String[] dates;
    // time to start stream in (HH:mm)
    private String start;
    // index to a playlist count from top this is scheduling, must be above it. use only with day, repeats and start fields
    private Integer schedule;

    public boolean isStarted() {
        Calendar current = Calendar.getInstance();
        Integer hour = Integer.parseInt(start.split(":")[0]);
        Integer min = Integer.parseInt(start.split(":")[1]);
        return hour < current.get(Calendar.HOUR_OF_DAY) || (hour == current.get(Calendar.HOUR_OF_DAY) && min <= current.get(Calendar.MINUTE));
    }

    public long getScheduledTime() {
        Calendar time = getStartTime();
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int min = time.get(Calendar.MINUTE);

        if(hour == 0 && min == 0) {// midnight
            time.set(Calendar.SECOND, 5);// at midnight, switch after 5 seconds since maintenance scheduler runs at 0 seconds
        }
        time.set(Calendar.MILLISECOND, 0);
        return time.getTimeInMillis();
    }

    public Calendar getStartTime() {
        String start = getStart();
        if(StringUtils.isNotBlank(start)) {
            Calendar startTime = Calendar.getInstance();
            Integer hour = Integer.parseInt(start.split(":")[0]);
            Integer min = Integer.parseInt(start.split(":")[1]);
            startTime.set(Calendar.HOUR_OF_DAY, hour);
            startTime.set(Calendar.MINUTE, min);
            startTime.set(Calendar.SECOND, 0);
            startTime.set(Calendar.MILLISECOND, 0);
            return startTime;
        }
        return null;
    }

    public boolean scheduledToday() {
        if (active == null || !active || StringUtils.isBlank(getStart())) {
            return false;
        } else if (ArrayUtils.isEmpty(days)) {
            return true;
        }
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        List<Integer> playoutDays = ArrayUtils.isEmpty(days) ? new ArrayList<>() : Arrays.asList(days);
        List<String> playoutDates = ArrayUtils.isEmpty(dates) ? new ArrayList<>() : Arrays.asList(dates);
        boolean dayScheduled = playoutDays.contains(now.get(Calendar.DAY_OF_WEEK));
        boolean dateScheduled = playoutDates.contains(dateFormat.format(now.getTime()));
        return dayScheduled || dateScheduled;
    }

    // only overrides days, dates and start but maintains the rest
    public Playlist schedule(Playlist parent) {
        this.type = parent.type;
        this.name = parent.name;
        this.description = parent.description;
        this.urlOrFolder = parent.urlOrFolder;
        this.usingExternalStorage = parent.usingExternalStorage;
        this.playingGeneralBumpers = parent.playingGeneralBumpers;
        this.specialBumperFolder = parent.specialBumperFolder;
        this.color = parent.color;
        if(parent.active != null && !parent.active) {
            this.active = false;
        } else if(this.active == null) {
            this.active = parent.active;
        }
        return this;
    }

    public enum Type {
        ONLINE, // An Online streaming playlist using a stream url
        LOCAL_SEQUENCED, // A local playlist starting from the first to the last alphabetical program by file naming
        LOCAL_RANDOMIZED, // A local playlist randlomy selecting programs
        LOCAL_RESUMING, // A local playlist resuming from the previous program at exact stopped time
        LOCAL_RESUMING_SAME, // A local playlist restarting the previous non completed program on the next playout
        LOCAL_RESUMING_NEXT, // A local playlist resuming from the next program
        LOCAL_RESUMING_ONE // A local one program selection playlist resuming from the next program
    }

}
