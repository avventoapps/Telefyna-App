package org.avvento.apps.telefyna.stream;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * By default this is set to 3ABN TV (Telefyna)
 */
@Getter
@Setter
@NoArgsConstructor
public class Config {
    /**
     * Update whenever a change to your configuration is made so you can track your configurations well
     */
    private String version;
    private String name;
    private boolean automationDisabled;
    private boolean notificationsDisabled;
    private Playlist[] playlists;
}
