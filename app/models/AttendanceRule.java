package models;

import controllers.Application;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import java.math.*;
import javax.persistence.*;
import java.sql.Time;
import com.fasterxml.jackson.annotation.*;
import play.data.*;
import com.avaje.ebean.*;

@Entity
public class AttendanceRule extends Model {

    @Id
    public Integer id;

    @ManyToOne()
    public Organization organization;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public Date start_date;
    public Date end_date;

    public String notification_email;

    public boolean expired;

    public int days_of_week;

    public String absence_code;

    public Double min_hours;

    public Time latest_start_time;
    public Time latest_departure_time;

    @Transient
    final int MONDAY = 1;
    @Transient
    final int TUESDAY = 2;
    @Transient
    final int WEDNESDAY = 4;
    @Transient
    final int THURSDAY = 8;
    @Transient
    final int FRIDAY = 16;

    public static Finder<Integer, AttendanceRule> find = new Finder<Integer, AttendanceRule>(
        AttendanceRule.class
    );

    public static AttendanceRule findById(Integer id) {
        return find.where().eq("organization", Organization.getByHost()).eq("id", id).findUnique();
    }

    public static List<AttendanceRule> all() {
        return find.where().eq("organization", Organization.getByHost()).findList();
    }

    public static AttendanceRule create(Form<AttendanceRule> form) throws Exception {
        AttendanceRule rule = form.get();

        rule.save();
        return rule;
    }

    public static void delete(Integer id) {
        AttendanceRule rule = find.ref(id);
        rule.delete();
    }

    public void updateFromForm(Form<AttendanceRule> form) throws Exception {
        save();
    }

    public String getFormattedPerson() {
        if (person != null) {
            return person.getDisplayName();
        }
        return "Everyone";
    }

    public String getFormattedDate(Date d) {
        if (d == null) {
            return "-";
        }
        return Application.formatDateMdy(d);
    }

    public String getFormattedDaysOfWeek() {
        List<String> result = new ArrayList<String>();
        if ((days_of_week & MONDAY) == MONDAY) {
            result.add("M");
        }
        if ((days_of_week & TUESDAY) == TUESDAY) {
            result.add("T");
        }
        if ((days_of_week & WEDNESDAY) == WEDNESDAY) {
            result.add("W");
        }
        if ((days_of_week & THURSDAY) == THURSDAY) {
            result.add("Th");
        }
        if ((days_of_week & FRIDAY) == FRIDAY) {
            result.add("F");
        }
        return String.join(",", result);
    }

    public String getFormattedTime(Time time) {
        if (time == null) {
            return "";
        }
        DateFormat format = new SimpleDateFormat("h:mm a");
        return format.format(time.getTime());
    }

    public String getCodeColor(Map<String, AttendanceCode> codes_map) {
        AttendanceCode code = codes_map.get(absence_code);
        if (code != null) {
            return code.color;
        }
        return "transparent";
    }
}
