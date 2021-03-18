package org.avvento.apps.telefyna.listen.mail;

import android.os.AsyncTask;
import android.os.Build;

import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.audit.AuditAlert;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;

import androidx.annotation.RequiresApi;

public class SendEmail extends AsyncTask<AuditAlert, Integer, String> {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected String doInBackground(AuditAlert... auditAlerts) {
        if(Utils.internetConnected()) {
            new GMail(auditAlerts[0]).sendEmail();
        } else {
            Logger.log(AuditLog.Event.NO_INTERNET, "Sending emails failed, no internet connection");
        }
        return null;
    }
}