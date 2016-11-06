package controllers;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.persistence.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.postgresql.util.PSQLException;

import play.Play;

import models.Person;

public class Utils
{
    static MustacheFactory sMustache = new DefaultMustacheFactory();

    public static Calendar parseDateOrNow(String date_string) {
        Calendar result = new GregorianCalendar();

        try {
            Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
            if (parsed_date != null) {
                result.setTime(parsed_date);
            }
        } catch (ParseException  e) {
            System.out.println("Failed to parse given date (" + date_string + "), using current");
        }

        return result;
    }

    public static void eatIfUniqueViolation(PersistenceException pe) throws PersistenceException {
        PSQLException e = (PSQLException)pe.getCause();
        if (e == null) {
            throw pe;
        }

        if (!e.getSQLState().equals("23505")) {
            throw pe;
        } else {
            System.out.println("Ate a 23505 error");
        }
    }

    // day_of_week is, e.g., Calendar.TUESDAY
    public static void adjustToPreviousDay(Calendar date, int day_of_week) {
        int dow = date.get(Calendar.DAY_OF_WEEK);
        if (dow >= day_of_week) {
            date.add(GregorianCalendar.DATE, -(dow - day_of_week));
        } else {
            date.add(GregorianCalendar.DATE, day_of_week - dow - 7);
        }
    }

    public static String toJson(Object o) {
        ObjectMapper m = new ObjectMapper();
        m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        // Something like this should work, but I can't get it to work now.
        // If you fix this, head to edit_attendance_week.scala.html and
        // simplify it a bit.
        //SimpleModule module = new SimpleModule("MyMapKeySerializerModule",
        //    new Version(1, 0, 0, null));
        //
        //module.addKeySerializer(Person.class, new PersonKeySerializer());
        //m = m.registerModule(module);
        //
        try
        {
            return m.writeValueAsString(o);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String renderMustache(String templateName, Object scopes) {
        StringWriter writer = new StringWriter();
        sMustache.compile(
            new InputStreamReader(
                Play.application().resourceAsStream("public/mustache/" + templateName)),
            templateName)
            .execute(writer, scopes);
        return writer.toString();
    }
}

//class PersonKeySerializer extends JsonSerializer<Person>
//{
//    @Override
//    public void serialize(Person p, JsonGenerator jgen, SerializerProvider provider)
//        throws IOException, JsonProcessingException
//    {
//        jgen.writeFieldName(String.valueOf(p.person_id));
//    }
//}
