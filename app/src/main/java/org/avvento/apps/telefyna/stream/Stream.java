package org.avvento.apps.telefyna.stream;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TODO use green dao in the future or a better orm
 *
 * Each stream can only run one playlist, sub playlists are called playouts.
 */
@Getter
@Setter
@NoArgsConstructor
public class Stream {

    // time for local folder playlist scheduling
    private Playlist[] playlist;
}
