package models;

import java.util.*;

import javax.persistence.*;

import play.data.*;
import play.data.validation.Constraints.*;
import com.avaje.ebean.Model;


@Entity
public class PersonChange extends Model {
    @OneToOne()
    @JoinColumn(name="person_id")
    public Person person;

    public String old_email="";

    public String new_email="";

    @Column(insertable = false, updatable = false)
    public Date time;

    public static Finder<Integer, PersonChange> find = new Finder<Integer, PersonChange>(
        PersonChange.class
    );

    public static PersonChange create(Person p, String new_email) {
        PersonChange result = new PersonChange();
        result.person = p;
        result.old_email = p.email;
        result.new_email = new_email;

        result.save();
        return result;
    }
}
