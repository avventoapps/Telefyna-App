package org.avvento.apps.telefyna.modal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Alerts {
    private Mailer mailer;
    private Receivers[] subscribers;
    private boolean enabled = true;
}
