package org.avvento.apps.telefyna.listen.mail;

import android.os.Build;

import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.audit.AuditAlert;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Receivers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import androidx.annotation.RequiresApi;

public class Mail {
    private final Properties emailProperties;
    private final AuditAlert auditAlert;
    private Session mailSession;
    private MimeMessage emailMessage;

    public Mail(AuditAlert auditAlert) {
        this.auditAlert = auditAlert;
        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port",  auditAlert.getAlerts().getEmailer().getPort());
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.starttls.enable", "true");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String decodePass(String pass) {
        // 3rd "ThanksForUsingTelefyna, We lauched Telefyna in 2021 by God's grace"
        String hash = new String(Base64.getDecoder().decode("Vmtkb2FHSnRkSHBTYlRsNVZsaE9jR0p0WkZWYVYzaHNXbTVzZFZsVGQyZFdNbFZuWWtkR01Wa3lhR3hhUTBKVldsZDRiRnB1YkhWWlUwSndZbWxCZVUxRVNYaEpSMG8xU1VWa2RscERaSHBKUjJSNVdWZE9iQT09"), StandardCharsets.UTF_8);
        return new String(Base64.getDecoder().decode(pass.replace(hash, "")), StandardCharsets.UTF_8);
    }

    private void createEmailMessage(Receivers receivers, Draft draft) throws MessagingException, UnsupportedEncodingException {
        if(Utils.isValidEmail(draft.getFrom())) {
            mailSession = Session.getDefaultInstance(emailProperties, null);
            emailMessage = new MimeMessage(mailSession);
            emailMessage.setFrom(new InternetAddress(draft.getFrom(), draft.getFrom()));
            Arrays.stream(receivers.getEmails().split("#")).forEach(emailAdd -> {
                if (Utils.isValidEmail(emailAdd.trim())) {
                    try {
                        draft.getBcc().add(new InternetAddress(emailAdd.trim()));
                    } catch (AddressException e) {
                        Logger.log(AuditLog.Event.ERROR, e.getMessage());
                    }
                }
            });
            emailMessage.addRecipients(Message.RecipientType.BCC, draft.getBcc().toArray(new InternetAddress[]{}));
            emailMessage.setSubject(draft.getSubject());
            setEmailBody(receivers, draft);
        }
    }

    private BodyPart attach(String attachment, Draft draft) throws MessagingException {
        BodyPart bodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachment);
        bodyPart.setDataHandler(new DataHandler(source));
        bodyPart.setFileName(attachment);
        return bodyPart;
    }

    public void setEmailBody(Receivers receivers, Draft draft) throws MessagingException {
        if(!receivers.isAttachConfig() && receivers.getAttachAuditLog() == 0) {
            emailMessage.setContent(draft.getBody(), "text/html");// for a html email
        } else if(AuditLog.Event.MAINTENANCE.equals(auditAlert.getEvent())) {
            Multipart multipart = new MimeMultipart();
            String config = Monitor.instance.getConfigFile();
            if (receivers.isAttachConfig() && new File(config).exists()) {
                multipart.addBodyPart(attach(config, draft));
            }
            for(String audit: Logger.getAuditsForNDays(receivers.getAttachAuditLog())) {
                if(new File(audit).exists()) {
                    multipart.addBodyPart(attach(audit, draft));
                }
            }
            emailMessage.setContent(multipart);
        }
    }

    private void mailNow(Receivers receivers, Draft draft) {
        try {
            createEmailMessage(receivers, draft);
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(auditAlert.getAlerts().getEmailer().getHost(), draft.getFrom(), draft.getPass());
            transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
            transport.close();
            Logger.log(AuditLog.Event.EMAIL, draft.getSubject(), receivers.getEmails(), "SUCCEEDED");
        } catch (Exception e) {
            Logger.log(AuditLog.Event.EMAIL, draft.getSubject(), receivers.getEmails().replaceAll("#", ", "), "FAILED with: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendEmail() {
        if(auditAlert != null && auditAlert.getEvent() != null && auditAlert.getAlerts() != null && auditAlert.getAlerts().getEmailer() != null) {
            Draft draft = new Draft();
            draft.setFrom(auditAlert.getAlerts().getEmailer().getEmail());
            draft.setPass(decodePass(auditAlert.getAlerts().getEmailer().getPass()));
            draft.setSubject(String.format("%s %s %s Alert: %s", Logger.getToday(), Monitor.instance.getConfiguration().getName(), auditAlert.getEvent().getCategory(), auditAlert.getEvent().name()));

            for(Receivers receivers : auditAlert.getAlerts().getSubscribers()) {
                if (AuditLog.Event.Category.ADMIN.equals((auditAlert.getEvent().getCategory()))) {
                    draft.setBody(String.format("Dear admin,<br><br> %s <br><br><br>This is a %s system notification, please don't respond to it.<br><br>TelefynaBot", auditAlert.getMessage(), Monitor.instance.getConfiguration().getName()));
                    draft.setAllowsAttachments(true);
                    mailNow(receivers, draft);
                } else if (AuditLog.Event.Category.BROADCAST.equals(receivers.getEventCategory()) && AuditLog.Event.Category.BROADCAST.equals((auditAlert.getEvent().getCategory()))) {
                    draft.setBody(String.format("Dear broadcaster,<br><br> %s <br><br><br>This is a %s system notification, please don't respond to it.<br><br>TelefynaBot", auditAlert.getMessage(), Monitor.instance.getConfiguration().getName()));
                    mailNow(receivers, draft);
                }
            }
        }
    }
}
