package org.avvento.apps.telefyna.ftp;

import org.apache.ftpserver.usermanager.impl.BaseUser;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FtpDetails {
    private boolean start = true;
    private Integer port = 2221;
    private BaseUser[] users = new BaseUser[]{};
}
