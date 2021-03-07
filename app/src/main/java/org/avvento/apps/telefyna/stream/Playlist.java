package org.avvento.apps.telefyna.stream;

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

    private boolean active = true;
    // lastModified date: Date#toLocaleString
    private String lastModified;
    private String name;
    private String description;
    
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
    private Integer[] days;
    // dates to schedule for, must be in DATE_FORMAT(dd-MM-yyyy)
    private String[] dates;
    // time to start stream in (HH:mm)
    private String start;
    // index to a playlist count from top this is cloning, must be above it. use only with day, repeats and start fields
    private Integer clone;

    public boolean scheduledToday() {
        if (!isActive() || StringUtils.isBlank(start)) {
            return false;
        } else if (ArrayUtils.isEmpty(days) && ArrayUtils.isEmpty(dates)) {
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
    public Playlist copy(Playlist clone) {
        this.type = clone.type;
        this.name = clone.name;
        this.description = clone.description;
        this.active = clone.active;
        this.urlOrFolder = clone.urlOrFolder;
        this.playingGeneralBumpers = clone.playingGeneralBumpers;
        this.specialBumperFolder = clone.specialBumperFolder;
        return this;
    }

    public enum Type {
        ONLINE, // online streaming, Tested supported formats; HLS 
        LOCAL_SEQUENCED, // organizes program alphabetically considering their tree
        LOCAL_RESUMING, // resuming the same program from the last playing duration
        LOCAL_RESUMING_SAME, // resuming the same program from the start
        LOCAL_RESUMING_NEXT, // resuming the next program from the start
        LOCAL_RANDOMIZED // randlomy selects programs
    }

}
