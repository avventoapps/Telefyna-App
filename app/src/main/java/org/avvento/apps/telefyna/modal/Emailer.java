package org.avvento.apps.telefyna.modal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Emailer {
    private String email;
    private String pass;
    private String host = "smtp.gmail.com";
    private int port = 587;
}
