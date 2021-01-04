package org.avvento.apps.telefyna.audit;

public class AuditLog {
    private static String SEPARATOR = "\n\n";

    public enum Event {
        CONFIGURATION("Initialized configurations"),
        MAINTENANCE("Ran maintenance"),
        PLAYLIST("Preparing to play playlist: %s"),
        PLAYLIST_MODIFIED("Playlist: %s is resetting resuming since it was modified %s seconds ago"),
        PLAYLIST_ITEM_CHANGE("Playing playlist: %s now playing program: %s"),
        PLAYLIST_COMPLETED("Completed playing playlist: %s"),
        CACHE_NOW_PLAYING_RESUME("Playlist: %s will next be resuming program: %s at: %d"),
        RETRIEVE_NOW_PLAYING_RESUME("Resuming Playlist: %s program: %s at: %d"),
        NETWORK_STATE("Network Is %sAvailable"),
        ERROR("%s");

        private String message;

        public String getMessage() {
            return String.format( "%s %s ", message, SEPARATOR);
        }

        Event(String message) {
            this.message = message;
        }

    }
}
