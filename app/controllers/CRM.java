package controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.csvreader.CsvWriter;
import com.ecwid.mailchimp.*;
import com.ecwid.mailchimp.method.v2_0.lists.ListMethodResult;
import com.feth.play.module.pa.PlayAuthenticate;
import com.google.inject.Inject;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;


@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class CRM extends Controller {

    Application mApplication;

    @Inject
    public CRM(final Application app) {
        mApplication = app;
    }

    public static final String CACHE_RECENT_COMMENTS = "CRM-recentComments-";

    public Result recentComments() {
        return ok(views.html.cached_page.render(
            new CachedPage(CACHE_RECENT_COMMENTS,
                "All people",
                "crm",
                "recent_comments") {
                @Override
                String render() {
                    List<Comment> recent_comments = Comment.find
                        .fetch("person")
                        .fetch("completed_tasks", new FetchConfig().query())
                        .fetch("completed_tasks.task", new FetchConfig().query())
                        .where().eq("person.organization", Organization.getByHost())
                        .orderBy("created DESC").setMaxRows(20).findList();

                    return views.html.people_index.render(recent_comments).toString();
                }
            }));
    }

    public Result person(Integer id) {
        Person the_person = Person.findById(id);

        List<Person> family_members =
            Person.find.where().isNotNull("family").eq("family", the_person.family).
                ne("person_id", the_person.person_id).findList();

        Set<Integer> family_ids = new HashSet<Integer>();
        family_ids.add(the_person.person_id);
        for (Person family : family_members) {
            family_ids.add(family.person_id);
        }

        List<Comment> all_comments = Comment.find.where().in("person_id", family_ids).
            order("created DESC").findList();

        String comment_destination = "<No email set>";
        boolean first = true;
        for (NotificationRule rule :
                NotificationRule.findByType(NotificationRule.TYPE_COMMENT)) {
            if (first) {
                comment_destination = rule.email;
                first = false;
            } else {
                comment_destination += ", " + rule.email;
            }
        }


        return ok(views.html.family.render(
            the_person,
            family_members,
            all_comments,
            comment_destination));
    }

    public Result jsonPeople(String query) {
		Expression search_expr = null;

		HashSet<Person> selected_people = new HashSet<Person>();
		boolean first_time = true;

		for (String term : query.split(" ")) {
			List<Person> people_matched_this_round;

			Expression this_expr =
				Expr.or(Expr.ilike("last_name", "%" + term + "%"),
				Expr.ilike("first_name", "%" + term + "%"));
			this_expr = Expr.or(this_expr,
				Expr.ilike("address", "%" + term + "%"));
			this_expr = Expr.or(this_expr,
				Expr.ilike("email", "%" + term + "%"));

			people_matched_this_round =
				Person.find.where().add(this_expr)
					.eq("organization", Organization.getByHost())
					.eq("is_family", false)
                    .findList();

			List<PhoneNumber> phone_numbers =
				PhoneNumber.find.where().ilike("number", "%" + term + "%")
                    .eq("owner.organization", Organization.getByHost())
                    .findList();
			for (PhoneNumber pn : phone_numbers) {
				people_matched_this_round.add(pn.owner);
			}

			if (first_time) {
				selected_people.addAll(people_matched_this_round);
			} else {
				selected_people.retainAll(people_matched_this_round);
			}

			first_time = false;
		}

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Person p : selected_people) {
            HashMap<String, String> values = new HashMap<String, String>();
            String label = p.first_name;
            if (p.last_name != null) {
                label = label + " " + p.last_name;
            }
            values.put("label", label);
            values.put("id", "" + p.person_id);
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    // personId should be -1 if the client doesn't care about a particular
    // person, but just wants a list of available tags. In that case,
    // the "create new tag" functionality is also disabled.
    public Result jsonTags(String term, Integer personId) {
        String like_arg = "%" + term + "%";
        List<Tag> selected_tags =
            Tag.find.where()
                .eq("organization", Organization.getByHost())
                .ilike("title", "%" + term + "%").findList();

		List<Tag> existing_tags = null;
        if (personId >= 0) {
            Person p = Person.findById(personId);
            if (p != null) {
                existing_tags = p.tags;
            }
        }

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Tag t : selected_tags) {
            if (existing_tags == null || !existing_tags.contains(t)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", t.title);
                values.put("id", "" + t.id);
                result.add(values);
            }
        }

        boolean tag_already_exists = false;
        for (Tag t : selected_tags) {
            if (t.title.toLowerCase().equals(term.toLowerCase())) {
                tag_already_exists = true;
            }
        }

        if (personId >= 0 && !tag_already_exists) {
            HashMap<String, String> values = new HashMap<String, String>();
            values.put("label", "Create new tag: " + term);
            values.put("id", "-1");
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

	public Collection<Person> getTagMembers(Integer tagId, String familyMode) {
        List<Person> people = Tag.findById(tagId).people;

        Set<Person> selected_people = new HashSet<Person>();

		selected_people.addAll(people);

		if (!familyMode.equals("just_tags")) {
			for (Person p : people) {
				if (p.family != null) {
					selected_people.addAll(p.family.family_members);
				}
			}
		}

		if (familyMode.equals("family_no_kids")) {
			Set<Person> no_kids = new HashSet<Person>();
			for (Person p2 : selected_people) {
				if (p2.dob == null ||
					CRM.calcAge(p2) > 18) {
					no_kids.add(p2);
				}
			}
			selected_people = no_kids;
		}

		return selected_people;
	}

	public Result renderTagMembers(Integer tagId, String familyMode) {
        Tag the_tag = Tag.findById(tagId);
		return ok(views.html.to_address_fragment.render(the_tag.title,
			getTagMembers(tagId, familyMode)));
	}

    public Result addTag(Integer tagId, String title, Integer personId) {
        CachedPage.remove(Application.CACHE_INDEX);
        CachedPage.remove(Attendance.CACHE_INDEX);

        Person p = Person.findById(personId);
        if (p == null) {
            return badRequest();
        }

        Tag the_tag;
        if (tagId == null) {
            the_tag = Tag.create(title);
        } else {
            the_tag = Tag.find.ref(tagId);
        }

        the_tag.people.add(p);
        the_tag.save();

        PersonTagChange ptc = PersonTagChange.create(
            the_tag,
            p,
            mApplication.getCurrentUser(),
            true);


        p.tags.add(the_tag);

        notifyAboutTag(the_tag, p, true);

        return ok(views.html.tag_fragment.render(the_tag, p));
    }

    public Result removeTag(Integer person_id, Integer tag_id) {
        Tag t = Tag.findById(tag_id);
        Person p = Person.findById(person_id);

        if (Ebean.createSqlUpdate("DELETE from person_tag where person_id=" + person_id +
            " AND tag_id=" + tag_id).execute() == 1) {
            PersonTagChange ptc = PersonTagChange.create(
                t,
                p,
                mApplication.getCurrentUser(),
                false);
        }

        notifyAboutTag(t, p, false);

        return ok();
    }

    public static void notifyAboutTag(Tag t, Person p, boolean was_add) {
        for (NotificationRule rule : t.notification_rules) {
            play.libs.mailer.Email mail = new play.libs.mailer.Email();
            if (was_add) {
                mail.setSubject(getInitials(p) + " added to tag " + t.title);
            } else {
                mail.setSubject(getInitials(p) + " removed from tag " + t.title);
            }
            mail.addTo(rule.email);
            mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
            mail.setBodyHtml(views.html.tag_email.render(t, p, was_add).toString());
            play.libs.mailer.MailerPlugin.send(mail);
        }
    }

    public Result allPeople() {
        return ok(views.html.all_people.render(Person.all()));
    }

	public Result viewTag(Integer id) {
        Tag the_tag = Tag.find
            .fetch("people")
            .fetch("people.phone_numbers", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        List<Person> people = the_tag.people;

        Set<Person> people_with_family = new HashSet<Person>();
        for (Person p : people) {
            if (p.family != null) {
                people_with_family.addAll(p.family.family_members);
            }
        }

        return ok(views.html.tag.render(
            the_tag, people, people_with_family, the_tag.use_student_display));
    }

    public Result downloadTag(Integer id) throws IOException {
        Tag the_tag = Tag.find
            .fetch("people")
            .fetch("people.phone_numbers", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        response().setHeader("Content-Type", "text/csv; charset=utf-8");
        response().setHeader("Content-Disposition", "attachment; filename=" +
            the_tag.title + ".csv");

        List<Person> people = the_tag.people;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Charset charset = Charset.forName("UTF-8");
        CsvWriter writer = new CsvWriter(baos, ',', charset);

        writer.write("First name");
        writer.write("Last name");
        writer.write("Display (JC) name");
        writer.write("Gender");
        writer.write("DOB");
        writer.write("Email");
        writer.write("Phone 1");
        writer.write("Phone 1 comment");
        writer.write("Phone 2");
        writer.write("Phone 2 comment");
        writer.write("Phone 3");
        writer.write("Phone 3 comment");
        writer.write("Neighborhood");
        writer.write("Street");
        writer.write("City");
        writer.write("State");
        writer.write("ZIP");
        writer.write("Notes");
        writer.write("Previous school");
        writer.write("School district");
        writer.write("Grade");
        writer.endRecord();

        for (Person p : people) {
            writer.write(p.first_name);
            writer.write(p.last_name);
            writer.write(p.getDisplayName());
            writer.write(p.gender);
            if (p.dob != null) {
                writer.write(Application.yymmddDate(p.dob));
            } else {
                writer.write("");
            }
            writer.write(p.email);
            for (int i = 0; i < 3; i++) {
                if (i < p.phone_numbers.size()) {
                    writer.write(p.phone_numbers.get(i).number);
                    writer.write(p.phone_numbers.get(i).comment);
                } else {
                    writer.write("");
                    writer.write("");
                }
            }

            writer.write(p.neighborhood);
            writer.write(p.address);
            writer.write(p.city);
            writer.write(p.state);
            writer.write(p.zip);
            writer.write(p.notes);
            writer.write(p.previous_school);
            writer.write(p.school_district);
            writer.write(p.grade);
            writer.endRecord();
        }

        writer.close();
        // Adding the BOM here causes Excel 2010 on Windows to realize
        // that the file is Unicode-encoded.
        return ok("\ufeff" + new String(baos.toByteArray(), charset));
    }

    public Result newPerson() {
        Form<Person> personForm = Form.form(Person.class);
        return ok(views.html.new_person.render(personForm));
    }

    public Result makeNewPerson() {
        Form<Person> personForm = Form.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.new_person.render(filledForm)
            );
        } else {
            Person new_person = Person.create(filledForm);
            return redirect(routes.CRM.person(new_person.person_id));
        }
    }

	static Email getPendingEmail() {
		return Email.find.where()
            .eq("organization", Organization.getByHost())
            .eq("deleted", false).eq("sent", false).orderBy("id ASC").setMaxRows(1).findUnique();
	}

	public static boolean hasPendingEmail() {
		return getPendingEmail() != null;
	}

	public Result viewPendingEmail() {
		Email e = getPendingEmail();
		if (e == null) {
			return redirect(routes.CRM.recentComments());
		}

        Tag staff_tag = Tag.find.where()
            .eq("organization", Organization.getByHost())
            .eq("title", "Staff").findUnique();
        List<Person> people = staff_tag.people;

        ArrayList<String> test_addresses = new ArrayList<String>();
        for (Person p : people) {
            test_addresses.add(p.email);
        }
		test_addresses.add("staff@threeriversvillageschool.org");

        ArrayList<String> from_addresses = new ArrayList<String>();
        from_addresses.add("office@threeriversvillageschool.org");
        from_addresses.add("evan@threeriversvillageschool.org");
        from_addresses.add("jmp@threeriversvillageschool.org");
        from_addresses.add("jancey@threeriversvillageschool.org");
        from_addresses.add("info@threeriversvillageschool.org");
        from_addresses.add("staff@threeriversvillageschool.org");

        e.parseMessage();
		return ok(views.html.view_pending_email.render(e, test_addresses, from_addresses));
	}

    public Result sendTestEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
        e.parseMessage();

		try {
			MimeMessage to_send = new MimeMessage(e.parsedMessage);
			to_send.addRecipient(Message.RecipientType.TO,
						new InternetAddress(values.get("dest_email")[0]));
			to_send.setFrom(new InternetAddress("Papal DB <noreply@threeriversvillageschool.org>"));
			Transport.send(to_send);
		} catch (MessagingException ex) {
			ex.printStackTrace();
		}

        return ok();
    }

    public Result sendEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
        e.parseMessage();

		int tagId = Integer.parseInt(values.get("tagId")[0]);
        Tag theTag = Tag.findById(tagId);
		String familyMode = values.get("familyMode")[0];
		Collection<Person> recipients = getTagMembers(tagId, familyMode);

		boolean hadErrors = false;

		for (Person p : recipients) {
			if (p.email != null && !p.email.equals("")) {
				try {
					MimeMessage to_send = new MimeMessage(e.parsedMessage);
					to_send.addRecipient(Message.RecipientType.TO,
								new InternetAddress(p.email, p.first_name + " " + p.last_name));
					to_send.setFrom(new InternetAddress(values.get("from")[0]));
					Transport.send(to_send);
				} catch (MessagingException ex) {
					ex.printStackTrace();
					hadErrors = true;
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    hadErrors = true;
				}
			}
		}

		// Send confirmation email
		try {
			MimeMessage to_send = new MimeMessage(e.parsedMessage);
			to_send.addRecipient(Message.RecipientType.TO,
						new InternetAddress("Staff <staff@threeriversvillageschool.org>"));
			String subject = "(Sent to " + theTag.title + " " + familyMode + ") " + to_send.getSubject();
			if (hadErrors) {
				subject = "***ERRORS*** " + subject;
			}
			to_send.setSubject(subject);
			to_send.setFrom(new InternetAddress(values.get("from")[0]));
            Transport.send(to_send);
		} catch (MessagingException ex) {
			ex.printStackTrace();
		}

		e.delete();
        return ok();
    }

    public Result deleteEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
        e.delete();

        return ok();
    }

    public Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.CRM.recentComments());
    }

    public Result editPerson(Integer id) {
        return ok(views.html.edit_person.render(Person.findById(id).fillForm()));
    }

    public Result savePersonEdits() {
        CachedPage.remove(Application.CACHE_INDEX);
        CachedPage.remove(Attendance.CACHE_INDEX);

        Form<Person> personForm = Form.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.edit_person.render(filledForm)
            );
        }

        return redirect(routes.CRM.person(Person.updateFromForm(filledForm).person_id));
    }

    public static String getInitials(Person p) {
        String result = "";
        if (p.first_name != null && p.first_name.length() > 0) {
            result += p.first_name.charAt(0);
        }
        if (p.last_name != null && p.last_name.length() > 0) {
            result += p.last_name.charAt(0);
        }
        return result;
    }

    public Result addComment() {
        CachedPage.remove(CACHE_RECENT_COMMENTS);
        Form<Comment> filledForm = Form.form(Comment.class).bindFromRequest();
        Comment new_comment = new Comment();

        new_comment.person = Person.findById(Integer.parseInt(filledForm.field("person").value()));
        new_comment.user = mApplication.getCurrentUser();
        new_comment.message = filledForm.field("message").value();

        String task_id_string = filledForm.field("comment_task_ids").value();

        if (task_id_string.length() > 0 || new_comment.message.length() > 0) {
            new_comment.save();

            String[] task_ids = task_id_string.split(",");
            for (String id_string : task_ids) {
                if (!id_string.isEmpty()) {
                    int id = Integer.parseInt(id_string);
                    if (id >= 1) {
                        CompletedTask.create(Task.findById(id), new_comment);
                    }
                }
            }

            if (filledForm.field("send_email").value() != null) {
                for (NotificationRule rule :
                        NotificationRule.findByType(NotificationRule.TYPE_COMMENT)) {
                    play.libs.mailer.Email mail = new play.libs.mailer.Email();
                    mail.setSubject("DemSchoolTools comment: " + new_comment.user.name + " & " + getInitials(new_comment.person));
                    mail.addTo(rule.email);
                    mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                    mail.setBodyHtml(views.html.comment_email.render(Comment.find.byId(new_comment.id)).toString());
                    play.libs.mailer.MailerPlugin.send(mail);
                }
            }

            return ok(views.html.comment_fragment.render(Comment.find.byId(new_comment.id), false));
        } else {
            return ok();
        }
    }

    public static int calcAge(Person p) {
        return (int)((new Date().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static int calcAgeAtBeginningOfSchool(Person p) {
		if (p.dob == null) {
			return -1;
		}
        return (int)((Application.getStartOfYear().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static String formatDob(Date d) {
		if (d == null) {
			return "---";
		}
        return new SimpleDateFormat("MM/dd/yy").format(d);
    }

    public static String formatDate(Date d) {
        d = new Date(d.getTime() + OrgConfig.get().time_zone.getOffset(d.getTime()));
        Date now = new Date();

		long diffHours = (now.getTime() - d.getTime()) / 1000 / 60 / 60;

        // String format = "EEE MMMM d, h:mm a";
		String format;

		if (diffHours < 24) {
			format = "h:mm a";
		} else if (diffHours < 24 * 7) {
			format = "EEEE, MMMM d";
		} else {
            format = "MM/d/yy";
        }
        return new SimpleDateFormat(format).format(d);
    }

    public Result viewTaskList(Integer id) {
        TaskList list = TaskList.findById(id);
        List<Person> people = list.tag.people;

        return ok(views.html.task_list.render(list, people));
    }

    public Result viewMailchimpSettings() {
        MailChimpClient mailChimpClient = new MailChimpClient();
        Organization org = OrgConfig.get().org;

        Map<String, ListMethodResult.Data> mc_list_map =
            Public.getMailChimpLists(mailChimpClient, org.mailchimp_api_key);

        return ok(views.html.view_mailchimp_settings.render(
            Form.form(Organization.class), org,
            MailchimpSync.find.where().eq("tag.organization", OrgConfig.get().org).findList(),
            mc_list_map));
    }

    public Result saveMailchimpSettings() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("mailchimp_api_key")) {
            OrgConfig.get().org.setMailChimpApiKey(values.get("mailchimp_api_key")[0]);
        }

        if (values.containsKey("mailchimp_updates_email")) {
            OrgConfig.get().org.setMailChimpUpdatesEmail(values.get("mailchimp_updates_email")[0]);
        }

        if (values.containsKey("sync_type")) {
            for (String tag_id : values.get("tag_id")) {
                Tag t = Tag.findById(Integer.parseInt(tag_id));
                MailchimpSync sync = MailchimpSync.create(t,
                    values.get("mailchimp_list_id")[0],
                    values.get("sync_type")[0].equals("local_add"),
                    values.get("sync_type")[0].equals("local_remove"));
            }
        }

        if (values.containsKey("remove_sync_id")) {
            MailchimpSync sync = MailchimpSync.find.byId(Integer.parseInt(
                values.get("remove_sync_id")[0]));
            sync.delete();
        }

        return redirect(routes.CRM.viewMailchimpSettings());
    }
}
