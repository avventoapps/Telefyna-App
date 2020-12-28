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
    private Playlist[] playlists;
}
