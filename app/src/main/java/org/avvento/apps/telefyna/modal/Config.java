package org.avvento.apps.telefyna.modal;

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
    private boolean automationDisabled = false;
    private boolean notificationsDisabled = true;
    // seconds to wait for internet
    private int internetWait = 30;
    private Alerts alerts;
    private Playlist[] playlists;
}
