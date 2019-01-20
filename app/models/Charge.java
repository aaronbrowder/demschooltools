package models;

import controllers.*;

import java.util.*;
import java.util.Date;
import java.util.Map;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.*;

import com.avaje.ebean.Model;

@Entity
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Charge extends Model implements Comparable<Charge> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_id_seq")
    public Integer id;

    @ManyToOne
    @JoinColumn(name="person_id")
    public Person person;

    @ManyToOne
    @JoinColumn(name="rule_id")
    public Entry rule;

    @ManyToOne
    @JoinColumn(name="case_id")
    public Case the_case;

    @ManyToOne()
    @JoinColumn(name="referenced_charge_id")
    @JsonIgnore
    public Charge referenced_charge;

    @OneToMany(mappedBy="referenced_charge")
    @JsonIgnore
    public List<Charge> referencing_charges;

    static final String EMPTY_PLEA = "<no plea>";
    public String plea = EMPTY_PLEA;
    public String resolution_plan = "";

    public boolean referred_to_sm;
    public String sm_decision;
    public Date sm_decision_date;

    public boolean rp_complete = false;
    public Date rp_complete_date = null;

    public String severity = "";

    public String minor_referral_destination = "";

    public ResolutionPlanType rp_type = ResolutionPlanType.None;

    public Integer rp_progress = 0;
    public Integer rp_max_days;
    public boolean rp_start_immediately = false;
    public String rp_escape_clause = "";
    public String rp_text = "";

    public static Finder<Integer, Charge> find = new Finder<Integer, Charge>(Charge.class);

    public static Charge findById(int id) {
        return find.where().eq("the_case.meeting.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }


    public static Charge create(Case c)
    {
        Charge result = new Charge();
        result.the_case = c;
        result.save();

        return result;
    }

    public static Charge generateFromReference(Case c, Charge referenced_charge)
    {
        Charge result = new Charge();
        result.the_case = c;
        result.person = referenced_charge.person;
        result.rule = Entry.findBreakingResPlanEntry();
        result.referenced_charge = referenced_charge;
        result.rp_max_days = 1;
        result.save();

        if (referenced_charge.rp_type == ResolutionPlanType.Restriction) {
            referenced_charge.setRPComplete(true);
        }

        return result;
    }

    public void edit(Map<String, String[]> query_string) {
        resolution_plan = query_string.get("resolution_plan")[0];

        if (query_string.containsKey("rp_type")) {
            rp_type = ResolutionPlanType.valueOf(query_string.get("rp_type")[0]);
        } else {
            rp_type = ResolutionPlanType.None;
        }

        if (query_string.containsKey("rp_max_days")) {
            rp_max_days = Utils.tryParseInt(query_string.get("rp_max_days")[0], 1);
        }

        if (query_string.containsKey("rp_start_immediately")) {
            rp_start_immediately = Boolean.parseBoolean(query_string.get("rp_start_immediately")[0]);
        }

        if (query_string.containsKey("rp_escape_clause")) {
            rp_escape_clause = query_string.get("rp_escape_clause")[0];
        }

        if (query_string.containsKey("rp_text")) {
            rp_text = query_string.get("rp_text")[0];
        }

        if (query_string.containsKey("plea")) {
            plea = query_string.get("plea")[0];
        } else {
            plea = "";
        }

        if (plea.equals("")) {
            plea = EMPTY_PLEA;
        }

		if (query_string.containsKey("severity")) {
            severity = query_string.get("severity")[0];
        }

        if (query_string.containsKey("person_id")) {
            person = Person.find.ref(Integer.parseInt(query_string.get("person_id")[0]));
        } else {
            person = null;
        }

        if (query_string.containsKey("rule_id")) {
            rule = Entry.find.ref(Integer.parseInt(query_string.get("rule_id")[0]));
        } else {
            rule = null;
        }

        referred_to_sm = Boolean.parseBoolean(query_string.get("referred_to_sm")[0]);

        if (query_string.containsKey("minor_referral_destination")) {
            minor_referral_destination = query_string.get("minor_referral_destination")[0];
        }
    }

    public boolean displayInResolutionPlanList() {
        return this.person != null && this.rule != null
			&& !this.referred_to_sm && !this.plea.equals("Not Guilty");
    }

    public void updateSchoolMeetingDecision(String decision, Date date) {
        decision = decision.trim();
        if (!decision.equals(this.sm_decision)) {
            this.rp_complete = false;
            this.rp_complete_date = null;
        }

        this.sm_decision_date = date;

        if (!decision.equals("")) {
            this.sm_decision = decision;
            // If a decision is recorded, assume it was done today
            // if not otherwise specified.
            if (this.sm_decision_date == null) {
                this.sm_decision_date = new Date();
            }
        } else {
            this.sm_decision = null;
        }
    }

    public void setRPComplete(boolean complete) {
        this.rp_complete = complete;
        if (complete) {
            this.rp_complete_date = new Date();
        } else {
            this.rp_complete_date = null;
        }
        this.save();
    }

    public String getRuleTitle() {
        if (this.rule == null) {
            return "<< no rule >>";
        } else {
            return this.rule.getNumber() + " " + this.rule.title;
        }
    }

    @Override
    public int compareTo(Charge c2) {
		if (the_case.meeting.date != null) {
			return the_case.meeting.date.compareTo(c2.the_case.meeting.date);
		}
		return 0;
    }

    public String getDayOfWeek() {
        if (sm_decision_date != null) {
            return Application.formatDayOfWeek(sm_decision_date) + "&mdash;SM";
        } else {
            return Application.formatDayOfWeek(the_case.meeting.date);
        }
    }

    public String getRpText() {
        String result = rp_text.isEmpty() ? resolution_plan : rp_text;
        if (!result.isEmpty() && !result.endsWith(".")) {
            result += ".";
        }
        return result;
    }

    public void buildChargeReferenceChain(ArrayList<Charge> chain) {
        if (referenced_charge != null) {
            referenced_charge.buildChargeReferenceChain(chain);
        }
        chain.add(this);
    }
}
