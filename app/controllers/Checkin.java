package controllers;

import java.io.*;
import java.sql.Time;
import java.util.*;
import java.util.stream.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;


@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_CHECKIN_APP)
public class Checkin extends Controller {

    public Result checkinData(String time) throws ParseException {
        Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time);
        List<CheckinPerson> people = Application.attendancePeople().stream()
            .sorted(Comparator.comparing(Person::getDisplayName))
            .filter(p -> p.pin != null && !p.pin.isEmpty())
            .map(p -> new CheckinPerson(p, AttendanceDay.findCurrentDay(date, p.person_id)))
            .collect(Collectors.toList());

        // add admin
        Person admin = new Person();
        admin.person_id = -1;
        admin.first_name = "Admin";
        admin.pin = OrgConfig.get().org.attendance_admin_pin;
        people.add(0, new CheckinPerson(admin, null));

        List<String> absence_codes = AttendanceCode.all(OrgConfig.get().org).stream()
            .map(c -> c.code)
            .collect(Collectors.toList());

        return ok(Json.stringify(Json.toJson(new CheckinData(people, absence_codes))));
    }

    public Result checkinMessage(String time_string, int person_id, boolean is_arriving) throws ParseException {
        Date date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time_string);
        Time time = new Time(date.getTime());
        AttendanceDay attendance_day = AttendanceDay.findCurrentDay(date, person_id);
        // if this is an invalid day, ignore the message
        if (attendance_day == null) {
            return ok();
        }
        // Messages from the app should never overwrite absence codes.
        if (attendance_day.code == null || attendance_day.code.isEmpty()) {
            // Don't overwrite the start time. This way the earliest start time is the one we use.
            if (is_arriving && attendance_day.start_time == null) {
                attendance_day.start_time = time;
                attendance_day.update();
            }
            // DO overwrite the end time. This way the latest end time is the one we use.
            else if (!is_arriving) {
                attendance_day.end_time = time;
                attendance_day.update();
            }
        }
        return ok();
    }

    public Result adminMessage(int person_id, String in_time, String out_time, String absence_code, String time_string) throws Exception {
        Date date = new Date();
        // We use time_string to determine which day it is according to the client. This could be different
        // from the current day according to the server, depending on the client's time zone.
        if (!time_string.isEmpty()) {
            date = new SimpleDateFormat("M/d/yyyy, h:mm:ss a").parse(time_string);
        }
        AttendanceDay attendance_day = AttendanceDay.findCurrentDay(date, person_id);
        if (attendance_day != null) {
            attendance_day.edit(absence_code, in_time, out_time);
        }
        return ok();
    }
}
