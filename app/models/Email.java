package models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.persistence.*;

import javax.mail.*;
import javax.mail.internet.*;
import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

import play.Play;

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

    public static Finder<Integer, Email> find = new Finder<Integer, Email>(
        Email.class
    );

    public static Email findById(int id) {
        return find.where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

	public static Email create(String message) {
		Email e = new Email();
        e.organization = Organization.getByHost();
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
                Play.application().configuration().getString("smtp.host"));
			properties.setProperty("mail.smtp.port",
                Play.application().configuration().getString("smtp.port"));

			properties.setProperty("mail.smtp.auth", "true");
			Authenticator authenticator = new Authenticator();
			properties.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());

			// Get the default Session object.
			Session session = Session.getInstance(properties, new Authenticator());
            session.setDebug(Play.application().configuration().getBoolean("smtp.debug", false));
			parsedMessage = new MimeMessage(session, new ByteArrayInputStream(message.getBytes()));

			for (Enumeration e = parsedMessage.getAllHeaders(); e.hasMoreElements() ;) {
				Header h = (Header)e.nextElement();
				if (!h.getName().toLowerCase().equals("content-type") &&
					!h.getName().toLowerCase().equals("subject")) {
					parsedMessage.removeHeader(h.getName());
				}
			}
			parsedMessage.saveChanges();
        }
		catch (MessagingException e) {
			e.printStackTrace();
		}
    }
}

 class Authenticator extends javax.mail.Authenticator {
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(
            Play.application().configuration().getString("smtp.user"),
            Play.application().configuration().getString("smtp.password"));
	}
}