package org.avvento.apps.telefyna.stream;

import com.google.android.exoplayer2.MediaItem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Program {
    private String name;
    private MediaItem mediaItem;
}
