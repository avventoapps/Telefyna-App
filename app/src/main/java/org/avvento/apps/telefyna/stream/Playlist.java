package org.avvento.apps.telefyna.stream;

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
    private String name;
    private String description;
    // by default a playlist is enabled
    private boolean active = true;
    // days of the week [1-7=Sun-Sat]: if null, runs daily
    private Integer[] days;
    // dates to schedule for
    private String[] dates;
    // time to start stream in (hhmm)
    private String start;
    private Type type = Type.ONLINE;
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

    public boolean isClone() {
        return clone != null;
    }

    public boolean scheduledToday() {
        if(!isActive()) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        List<Integer> playoutDays = (days == null || days.length == 0) ? new ArrayList<>() : Arrays.asList(days);
        List<String> playoutDates = (dates == null || dates.length == 0) ? new ArrayList<>() : Arrays.asList(dates);
        boolean dayScheduled = playoutDays.isEmpty() ? true : playoutDays.contains(now.get(Calendar.DAY_OF_WEEK));
        boolean dateScheduled = playoutDates.contains(dateFormat.format(now.getTime()));
        return dayScheduled || dateScheduled;
    }

    public enum Type {
        LOCAL, ONLINE
    }

    // only overrides days, dates and start but maintains the rest
    public Playlist copy(Playlist clone) {
        clone.setDays(this.days == null ? clone.getDays(): this.days);
        clone.setDates(this.dates == null ? clone.getDates() : this.dates);
        clone.setStart(this.getStart() == null ? clone.getStart() : this.getStart());
        return clone;
    }
}
