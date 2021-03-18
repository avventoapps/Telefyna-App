package org.avvento.apps.telefyna.listen.mail;

import android.os.Build;

import org.avvento.apps.telefyna.Monitor;
import org.avvento.apps.telefyna.Telefyna;
import org.avvento.apps.telefyna.Utils;
import org.avvento.apps.telefyna.audit.AuditAlert;
import org.avvento.apps.telefyna.audit.AuditLog;
import org.avvento.apps.telefyna.audit.Logger;
import org.avvento.apps.telefyna.modal.Alerts;
import org.avvento.apps.telefyna.modal.Email;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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

public class GMail {
    private final Properties emailProperties;
    private final AuditAlert auditAlert;
    private Session mailSession;
    private MimeMessage emailMessage;

    public GMail(AuditAlert auditAlert) {
        this.auditAlert = auditAlert;
        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port",  "587");
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.starttls.enable", "true");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String decodePass(String pass) {
        // 3rd "ThanksForUsingTelefyna, We lauched Telefyna in 2021 by God's grace"
        String hash = new String(Base64.getDecoder().decode("Vmtkb2FHSnRkSHBTYlRsNVZsaE9jR0p0WkZWYVYzaHNXbTVzZFZsVGQyZFdNbFZuWWtkR01Wa3lhR3hhUTBKVldsZDRiRnB1YkhWWlUwSndZbWxCZVUxRVNYaEpSMG8xU1VWa2RscERaSHBKUjJSNVdWZE9iQT09"), StandardCharsets.UTF_8);
        return new String(Base64.getDecoder().decode(pass.replace(hash, "")), StandardCharsets.UTF_8);
    }

    private void createEmailMessage(Email email, Draft draft) throws MessagingException, UnsupportedEncodingException {
        if(Utils.isValidEmail(draft.getFrom())) {
            mailSession = Session.getDefaultInstance(emailProperties, null);
            emailMessage = new MimeMessage(mailSession);
            emailMessage.setFrom(new InternetAddress(draft.getFrom(), draft.getFrom()));
            Arrays.stream(email.getEmails().split("#")).forEach(emailAdd -> {
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
            setEmailBody(email, draft);
        }
    }

    private BodyPart attach(String attachment, Draft draft) throws MessagingException {
        BodyPart bodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachment);
        bodyPart.setDataHandler(new DataHandler(source));
        bodyPart.setFileName(attachment);
        return bodyPart;
    }

    public void setEmailBody(Email email, Draft draft) throws MessagingException {
        if(!email.isAttachConfig() && email.getAttachAuditLog() == 0) {
            emailMessage.setContent(draft.getBody(), "text/html");// for a html email
        } else {
            Multipart multipart = new MimeMultipart();
            String config = Monitor.instance.getConfigFile();
            if (email.isAttachConfig() && new File(config).exists()) {
                multipart.addBodyPart(attach(config, draft));
            }
            for(String audit: Logger.getAuditsForNDays(email.getAttachAuditLog())) {
                if(new File(audit).exists()) {
                    multipart.addBodyPart(attach(audit, draft));
                }
            }
            emailMessage.setContent(multipart);
        }
    }

    private void mailNow(Email email, Draft draft) {
        try {
            createEmailMessage(email, draft);
            Transport transport = mailSession.getTransport("smtp");
            String emailHost = "smtp.gmail.com";
            transport.connect(emailHost, draft.getFrom(), draft.getPass());
            transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
            transport.close();
            Logger.log(AuditLog.Event.EMAIL, draft.getSubject(), email.getEmails(), "SUCCEEDED");
        } catch (Exception e) {
            Logger.log(AuditLog.Event.EMAIL, draft.getSubject(), email.getEmails().replaceAll("#", ", "), "FAILED with: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendEmail() {
        if(auditAlert != null && auditAlert.getEvent() != null && auditAlert.getAlerts() != null && auditAlert.getAlerts().getEmailer() != null) {
            Draft draft = new Draft();
            draft.setFrom(auditAlert.getAlerts().getEmailer().getEmail());
            draft.setPass(decodePass(auditAlert.getAlerts().getEmailer().getPass()));
            draft.setSubject(Logger.getToday() + " Telefyna Alert: " + auditAlert.getEvent().name());

            for(Email email : auditAlert.getAlerts().getSubscribers()) {
                if (AuditLog.Event.Category.ADMIN.equals((auditAlert.getEvent().getCategory()))) {
                    draft.setBody("Dear admin,<br><br> " + auditAlert.getMessage() + " <br><br><br>This is a Telefyna system notification, please don't respond to it.<br><br>TelefynaBot");
                    draft.setAllowsAttachments(true);
                    mailNow(email, draft);
                } else if (AuditLog.Event.Category.BROADCAST.equals(email.getEventCategory()) && AuditLog.Event.Category.BROADCAST.equals((auditAlert.getEvent().getCategory()))) {
                    draft.setBody("Dear broadcaster,<br><br> " + auditAlert.getMessage() + " <br><br><br>This is a Telefyna system notification, please don't respond to it.<br><br>TelefynaBot");
                    mailNow(email, draft);
                }
            }
        }
    }
}
