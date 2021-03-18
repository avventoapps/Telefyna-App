package org.avvento.apps.telefyna.listen.mail;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Draft {
    private String from;
    private String pass;
    private List<InternetAddress> bcc = new ArrayList<>();
    private String subject;
    private String body;
    private boolean allowsAttachments = false;
}
