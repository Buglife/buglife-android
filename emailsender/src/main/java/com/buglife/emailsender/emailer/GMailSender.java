package com.buglife.emailsender.emailer;

import android.util.Base64;

import com.buglife.emailsender.serialization.ReportAttachment;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GMailSender extends Authenticator {

    private String mailhost = "smtp.gmail.com";
    private String user;
    private String password;
    private Session session;
    private Multipart multipart = new MimeMultipart();

    static {
        Security.addProvider(new JSSEProvider());
    }

    public GMailSender(String user, String password) {
        this.user = user;
        this.password = password;
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");
        session = Session.getDefaultInstance(props, this);
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    public synchronized void sendMail(
            String subject,
            String body,
            String sender,
            String recipients
    ) throws MessagingException {
            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler((DataSource) new ByteArrayDataSource(body.getBytes(), "text/plain"));
            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);
            message.setDataHandler(handler);
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            multipart.addBodyPart(messageBodyPart);

            // Put parts in message
            message.setContent(multipart);
            if (recipients.indexOf(',') > 0) {
                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(recipients)
                );
            } else {
                message.setRecipient(
                        Message.RecipientType.TO,
                        new InternetAddress(recipients)
                );
            }
            Transport.send(message);
    }

    public void addAttachment(ReportAttachment reportAttachment) throws MessagingException {
        BodyPart messageBodyPart = new MimeBodyPart();
        ByteArrayDataSource ds = new ByteArrayDataSource(
                Base64.decode(reportAttachment.getBase64AttachmentData(), 0),
                reportAttachment.getMimeType()
        );
        messageBodyPart.setDataHandler(new DataHandler(ds));
        messageBodyPart.setFileName(reportAttachment.getFilename());
        multipart.addBodyPart(messageBodyPart);
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null) {
                return "application/octet-stream";
            } else {
                return type;
            }
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }

}
