package org.avvento.apps.telefyna.stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Seek {
    private int program;
    private long position;
}
