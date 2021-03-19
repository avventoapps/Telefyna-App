package org.avvento.apps.telefyna.modal;

import org.avvento.apps.telefyna.audit.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Receivers {
    // emails separated by #
    private String emails;
    private boolean attachConfig = false;
    // days whose audit logs should be added backwards starting today
    private int attachAuditLog = 0;
    private AuditLog.Event.Category eventCategory;
}
