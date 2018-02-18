package models;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

public class OrgConfig {
    public String name;
    public String people_url;
    public String str_manual_title;
    public String str_manual_title_short;
    public String str_res_plan_short;
    public String str_res_plan;
    public String str_res_plan_cap;
    public String str_res_plans;
    public String str_res_plans_cap;
    public String str_jc_name = "Judicial Committee";
    public String str_jc_name_short = "JC";
    public String str_findings = "Findings";
    public String str_corporation = "Corporation";
    public String str_committee = "Committee";
    public String str_clerk = "Clerk";

    public boolean show_no_contest_plea = false;
    public boolean show_na_plea = false;
    public boolean show_severity = false;
    public boolean use_minor_referrals = true;
    public boolean show_checkbox_for_res_plan = true;
    public boolean track_writer = true;
    public boolean filter_no_charge_cases = false;
    public boolean show_findings_in_rp_list = true;
    public boolean use_year_in_case_number = false;
    public boolean hide_location_for_print = false;

    public boolean enable_file_sharing = false;

    public Organization org;

    public TimeZone time_zone = TimeZone.getTimeZone("US/Eastern");

    public static OrgConfig get() {
        Organization org = Organization.getByHost();
        OrgConfig result = configs.get(org.name);
        result.org = org;
        return result;
    }

    public String getReferralDestination(Charge c) {
        return "School Meeting";
    }

    static HashMap<String, OrgConfig> configs = new HashMap<String, OrgConfig>();
    static void register(String name, OrgConfig config) {
        configs.put(name, config);
    }

    public String getCaseNumberPrefix(Meeting m) {
        if (use_year_in_case_number) {
            return new SimpleDateFormat("YYYY-MM-dd-").format(m.date);
        } else {
            return new SimpleDateFormat("MM-dd-").format(m.date);
        }
    }
}

class ThreeRiversVillageSchool extends OrgConfig {
    private static final ThreeRiversVillageSchool INSTANCE = new ThreeRiversVillageSchool();

    public ThreeRiversVillageSchool() {
        name = "Three Rivers Village School";
        people_url = "https://trvs.demschooltools.com";

        str_manual_title = "Management Manual";
        str_manual_title_short = "Manual";
        str_res_plan_short = "RP";
        str_res_plan = "resolution plan";
        str_res_plan_cap = "Resolution plan";
        str_res_plans = "resolution plans";
        str_res_plans_cap = "Resolution plans";
        str_jc_name = "Justice Committee";

        show_checkbox_for_res_plan = false;

        enable_file_sharing = true;

        OrgConfig.register(name, this);
    }

    public static ThreeRiversVillageSchool getInstance() {
        return INSTANCE;
    }
}

class PhillyFreeSchool extends OrgConfig {
    private static final PhillyFreeSchool INSTANCE = new PhillyFreeSchool();

    public PhillyFreeSchool() {
        name = "Philly Free School";
        people_url = "https://pfs.demschooltools.com";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";

        show_no_contest_plea = true;
        show_na_plea = true;
        show_severity = true;

        OrgConfig.register(name, this);
    }

    public static PhillyFreeSchool getInstance() {
        return INSTANCE;
    }
}

class Fairhaven extends OrgConfig {
    private static final Fairhaven INSTANCE = new Fairhaven();

    public Fairhaven() {
        name = "Fairhaven School";
        people_url = "https://fairhaven.demschooltools.com";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
		str_findings = "JC Report";

        show_findings_in_rp_list = false;
        use_year_in_case_number = true;

        OrgConfig.register(name, this);
    }

    public static Fairhaven getInstance() {
        return INSTANCE;
    }
}

class TheCircleSchool extends OrgConfig {
    private static final TheCircleSchool INSTANCE = new TheCircleSchool();

    public TheCircleSchool() {
        name = "The Circle School";
        people_url = "https://tcs.demschooltools.com";

        str_manual_title = "Management Manual";
        str_manual_title_short = "Mgmt. Man.";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
		str_findings = "Findings";

        track_writer = false;
        filter_no_charge_cases = true;
        hide_location_for_print = true;

        OrgConfig.register(name, this);
    }

    @Override
    public String getReferralDestination(Charge c) {
        if (c.plea.equals("Not Guilty")) {
            return "trial";
        } else {
            return "School Meeting";
        }
    }

    public static TheCircleSchool getInstance() {
        return INSTANCE;
    }

    @Override
    public String getCaseNumberPrefix(Meeting m) {
        return new SimpleDateFormat("yyyyMMdd").format(m.date);
    }
}

class MakariosLearningCommunity extends OrgConfig {
    private static final MakariosLearningCommunity INSTANCE =
        new MakariosLearningCommunity();

    public MakariosLearningCommunity() {
        name = "Makarios Learning Community";
        people_url = "https://mlc.demschooltools.com";
        time_zone = TimeZone.getTimeZone("US/Central");

        str_manual_title = "Management Manual";
        str_manual_title_short = "Mgmt. Man.";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
        str_findings = "Findings";

        OrgConfig.register(name, this);
    }

    public static MakariosLearningCommunity getInstance() {
        return INSTANCE;
    }
}

class TheOpenSchool extends OrgConfig {
    private static final TheOpenSchool INSTANCE = new TheOpenSchool();

    public TheOpenSchool() {
        name = "The Open School";
        people_url = "https://tos.demschooltools.com";
        time_zone = TimeZone.getTimeZone("US/Pacific");

        str_manual_title = "Law Book";
        str_manual_title_short = "Law Book";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
        str_findings = "Findings";
        str_jc_name = "Civics Board";
        str_jc_name_short = "CB";

        OrgConfig.register(name, this);
    }

    public static TheOpenSchool getInstance() {
        return INSTANCE;
    }
}

class Houston extends OrgConfig {
    private static final Houston INSTANCE = new Houston();
    public TimeZone time_zone = TimeZone.getTimeZone("US/Central");

    public Houston() {
        name = "Houston Sudbury School";
        people_url = "https://hss.demschooltools.com";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
        str_findings = "Findings";

        track_writer = false;
        use_year_in_case_number = true;

        OrgConfig.register(name, this);
    }

    public static Houston getInstance() {
        return INSTANCE;
    }
}


class Sandbox extends OrgConfig {
    private static final Sandbox INSTANCE = new Sandbox();
    public TimeZone time_zone = TimeZone.getTimeZone("US/Eastern");

    public Sandbox() {
        name = "DemSchoolTools sandbox area";
        people_url = "https://sandbox.demschooltools.com";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
        str_findings = "Findings";

        track_writer = true;
        enable_file_sharing = true;

        OrgConfig.register(name, this);
    }

    public static Sandbox getInstance() {
        return INSTANCE;
    }
}

class Clearview extends OrgConfig {
    private static final Clearview INSTANCE = new Clearview();
    public TimeZone time_zone = TimeZone.getTimeZone("US/Central");

    public Clearview() {
        name = "Clearview Sudbury School";
        people_url = "https://css.demschooltools.com";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "Sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";
        str_findings = "Findings";

        track_writer = false;

        OrgConfig.register(name, this);
    }

    public static Clearview getInstance() {
        return INSTANCE;
    }
}
