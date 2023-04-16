package models;

import io.ebean.*;
import controllers.Public;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Properties;

@javax.persistence.Entity
public class Email extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_id_seq")
    public Integer id;

    @ManyToOne()
    public Organization organization;

    public String message;
	public boolean sent;
	public boolean deleted;

    public static Finder<Integer, Email> find = new Finder<>(
			Email.class
	);

    public static Email findById(int id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id).findOne();
    }

	public static Email create(String message, Organization org) {
		Email e = new Email();
        e.organization = org;
		e.message = message;
		e.save();
		return e;
	}

    @Transient
    public MimeMessage parsedMessage;

    public void markSent() {
        this.sent = true;
		this.save();
    }

	public void markDeleted() {
		this.deleted = true;
		this.save();
	}

    public void parseMessage() {
        try {
			// Get system properties
			Properties properties = new Properties();

			// Setup mail server
			properties.setProperty("mail.smtp.host",
					Public.sConfig.getString("smtp.host"));
			properties.setProperty("mail.smtp.port",
					Public.sConfig.getString("smtp.port"));

			properties.setProperty("mail.smtp.auth", "true");
			Authenticator authenticator = new Authenticator();
			properties.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());

			// Get the default Session object.
			Session session = Session.getInstance(properties, new Authenticator());
            session.setDebug(Public.sConfig.getBoolean("smtp.debug"));
			parsedMessage = new MimeMessage(session, new ByteArrayInputStream(message.getBytes()));

			for (Enumeration e = parsedMessage.getAllHeaders(); e.hasMoreElements() ;) {
				Header h = (Header)e.nextElement();
				if (!h.getName().equalsIgnoreCase("content-type") &&
						!h.getName().equalsIgnoreCase("subject")) {
					parsedMessage.removeHeader(h.getName());
				}
			}
			parsedMessage.saveChanges();
        } catch (MessagingException e) {
			e.printStackTrace();
		}
    }
}

 class Authenticator extends javax.mail.Authenticator {
	 protected PasswordAuthentication getPasswordAuthentication() {
		 return new PasswordAuthentication(
            Public.sConfig.getString("smtp.user"),
            Public.sConfig.getString("smtp.password"));
	 }
}