package org.avvento.apps.telefyna.stream;

import org.avvento.apps.telefyna.ftp.FtpDetails;

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
    private boolean disableNotifications = true;
    private FtpDetails ftpDetails;
    private Playlist[] playlists;
}
