package org.avvento.apps.telefyna.stream;

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
    private String name;
    private String description;
    // by default a playlist is enabled
    private boolean active = true;
    // day of the week [1-7=Sun-Sat]: if null, runs daily
    private Integer day;
    // time to start stream in (hhmm)
    private String start;
    private Type type = Type.ONLINE;
    /*
     * days of the week [1-7=Sun-Sat] for the repeat of the same time of the playlist:
     * if null, repeats is daily, if [], never repeats
     */
    private Integer[] repeats;
    /*
     * set url for non local folder or local folder where files should be added in the order with which they should play
     * use sub folders named in alphabetical order and symbolic links for fill ups
     * to maintain an order when playing, name programs or folders alphabetically
     */
    private String urlOrFolder;
    // index to a playlist count from top this is cloning, must be above it. use only with day, repeats and start fields
    private Integer clone;
    // use to order playlist to proceed proceed from program that was playing next next time
    private boolean resuming = false;

    public boolean scheduledToday() {
        List<Integer> days = new ArrayList<>();
        if(!isActive()) {
            return false;
        }
        if(day == null) {
            return true;
        } else {
            days.add(day);
        }
        if(repeats != null) {
            days.addAll(Arrays.asList(repeats));
        }
        return days.isEmpty() ? false : days.contains(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
    }

    public enum Type {
        LOCAL, ONLINE
    }

    public Playlist copy(Integer day, Integer[] repeats, String start) {
        this.day = day;
        this.repeats = repeats;
        this.start = start;
        return this;
    }
}
