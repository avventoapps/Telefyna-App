package org.avvento.apps.telefyna.listen;

import org.avvento.apps.telefyna.stream.Playlist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
// TODO rename to a better meaningful name
public class CurrentPlaylist {
    private int index;
    private Playlist playlist;
}
