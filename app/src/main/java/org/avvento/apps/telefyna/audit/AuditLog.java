package org.avvento.apps.telefyna.audit;

import java.util.Arrays;

public class AuditLog {
    private static String SEPARATOR = "\n\n";
    private static String SPLITTER = "--------------------------------------------------------------";
    public static String ENDPOINT = ".log";

    public enum Event {
        //admin
        HEARTBEAT("TELEFYNA has been turned: %s"),
        KEY_PRESS("%s has been pressed"),
        CONFIGURATION("Initialized configurations"),
        MAINTENANCE("Ran maintenance"),
        METRICS("%s"),
        ERROR("%s"),
        CACHE_NOW_PLAYING_RESUME("Playlist: %s will next be resuming program: %s at: %s"),
        RETRIEVE_NOW_PLAYING_RESUME("Resuming Playlist: %s program: %s at: %d"),
        STUCK("Relaunching having been stuck for %d seconds"),
        EMPTY_FILLERS("There are no fillers installed"),
        CRASH("Relaunched Telefyna after crash because of: %s"),

        //scheduler
        PLAYLIST(SPLITTER + "[ Preparing to play playlist: %s: %s"),
        PLAYLIST_PLAY("Playing Playlist: %s from: %s %s"),
        PLAYLIST_SWITCH("Switching to Playlist: %s from: %s %s"),
        PLAYLIST_EMPTY_PLAY(SPLITTER + "Attempted to play an empty playlist: %s"),
        PLAYLIST_MODIFIED("Playlist: %s is resetting resuming since it was modified %s seconds ago"),
        PLAYLIST_ITEM_CHANGE("Playing playlist: %s now playing program: %s"),
        PLAYLIST_COMPLETED(SPLITTER + "] Completed playing playlist: %s"),
        DISPLAY_LOGO_OFF("Turning OFF Logo"),
        DISPLAY_LOGO_ON("Turning ON Logo at the %s"),
        DISPLAY_NEWS_ON("Displaying news/info ticker with messages: %s"),
        DISPLAY_NEWS_OFF("Turning OFF news/info ticker"),
        LOWER_THIRD_ON("Displaying %s lower third"),
        LOWER_THIRD_OFF("Turning OFF lower third"),

        // system
        EMAIL("Sending email: '%s' to: %s %s"),
        NO_INTERNET("%s");

        private String message;

        Event(String message) {
            this.message = message;
        }

        public String getMessage() {
            return String.format("%s %s ", message, SEPARATOR);
        }

        public Category getCategory() {
            Event[] admins = new Event[]{HEARTBEAT, KEY_PRESS, CONFIGURATION, MAINTENANCE, METRICS, ERROR, CACHE_NOW_PLAYING_RESUME, RETRIEVE_NOW_PLAYING_RESUME, STUCK, EMPTY_FILLERS, CRASH};
            Event[] schedulers = new Event[]{PLAYLIST, PLAYLIST_PLAY, PLAYLIST_SWITCH, PLAYLIST_EMPTY_PLAY, PLAYLIST_MODIFIED, PLAYLIST_ITEM_CHANGE, PLAYLIST_COMPLETED, DISPLAY_LOGO_OFF, DISPLAY_LOGO_ON, DISPLAY_NEWS_ON, DISPLAY_NEWS_OFF, LOWER_THIRD_ON, LOWER_THIRD_OFF};

            if(Arrays.asList(admins).contains(this)) {
                return Category.ADMIN;
            } else if(Arrays.asList(schedulers).contains(this)) {
                return Category.BROADCAST;
            } else {
                return Category.SYSTEM;
            }
        }

        public enum Category {
            ADMIN, BROADCAST, SYSTEM
        }

    }
}
