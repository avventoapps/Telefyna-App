package org.avvento.apps.telefyna.audit;

public class AuditLog {
    private static String SEPARATOR = "\n\n";
    private static String SPLITTER = "--------------------------------------------------------------";

    public enum Event {
        HEARTBEAT("TELEFYNA has been turned: %s"),
        KEY_DOWN("Key: %d has been pressed down"),
        CONFIGURATION("Initialized configurations"),
        MAINTENANCE("Ran maintenance"),
        PLAYLIST(SPLITTER + "[ Preparing to play playlist: %s"),
        PLAYLIST_PLAY(SPLITTER + "] Now playing playlist: %s"),
        PLAYLIST_EMPTY_PLAY(SPLITTER + "Attempted to play an empty playlist: %s"),
        PLAYLIST_MODIFIED("Playlist: %s is resetting resuming since it was modified %s seconds ago"),
        PLAYLIST_ITEM_CHANGE("Playing playlist: %s now playing program: %s"),
        PLAYLIST_COMPLETED("Completed playing playlist: %s"),
        CACHE_NOW_PLAYING_RESUME("Playlist: %s will next be resuming program: %s at: %d"),
        RETRIEVE_NOW_PLAYING_RESUME("Resuming Playlist: %s program: %s at: %d"),
        PLAYLIST_PLAY_CURRENT_SLOT("Playing current playlist: %s program: %s at: %s"),
        ERROR("%s");

        private String message;

        Event(String message) {
            this.message = message;
        }

        public String getMessage() {
            return String.format("%s %s ", message, SEPARATOR);
        }

    }
}
