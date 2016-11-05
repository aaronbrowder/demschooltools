package models;

import java.io.*;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;
import com.fasterxml.jackson.annotation.*;

import controllers.Application;

import play.libs.Json;

@Entity
public class AttendanceDay extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_day_id_seq")
    public Integer id;

    public Date day;

    @ManyToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public String code;

    public Time start_time;
    public Time end_time;

    public static Finder<Integer, AttendanceDay> find = new Finder<Integer, AttendanceDay>(
        AttendanceDay.class
    );

    public static AttendanceDay create(Date day, Person p)
    {
        AttendanceDay result = new AttendanceDay();
        result.person = p;
        result.day = day;
        result.save();

        return result;
    }

    public static Time parseTime(String time_string) throws Exception {
        if (time_string == null || time_string.equals("")) {
            return null;
        }

		String[] formats = {"h:mm a", "h:mma"};

		for (String format : formats) {
			try {
				Date d = new SimpleDateFormat(format).parse(time_string);
				return new Time(d.getTime());
			} catch (ParseException e) {
			}
		}

		return null;
    }

    public void edit(String code, String start_time, String end_time) throws Exception {
        if (code.equals("")) {
            this.code = null;
        } else {
            this.code = code;
        }

        this.start_time = parseTime(start_time);
        this.end_time = parseTime(end_time);

        this.update();
    }

    @JsonIgnore
    public double getHours() {
        return (end_time.getTime() - start_time.getTime()) / (1000.0 * 60 * 60);
    }
}
