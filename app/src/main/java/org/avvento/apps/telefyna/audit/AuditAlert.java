package org.avvento.apps.telefyna.audit;

import org.avvento.apps.telefyna.modal.Alerts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuditAlert {
    private Alerts alerts;
    private AuditLog.Event event;
    private String message;
}
