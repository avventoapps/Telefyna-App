package org.avvento.apps.telefyna.stream;

import com.google.android.exoplayer2.SimpleExoPlayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NowPlaying {
    private int playlistIndex = 0;
    private SimpleExoPlayer player;
}
