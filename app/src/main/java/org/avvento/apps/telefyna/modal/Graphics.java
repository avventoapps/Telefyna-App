package org.avvento.apps.telefyna.modal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Graphics {
    private boolean displayLogo = false;
    private LogoPosition logoPosition = LogoPosition.TOP;
    private News news;

    public enum LogoPosition {
        TOP,
        BOTTOM
    }
}
