var utils = require('./utils');
var Handlebars = require('handlebars');

SAVE_TIMEOUT = 2000;

function showSomethingInSidebar(url) {
    $("#sidebar").html("<h2>Loading...</h2>");
    $.get(url,
          null,
           function(data, status, jqXHR) {
            // Remove a href links from data to avoid inadvertent clicks
            var patt = /<a nosidebar href="[^"]+"/gi;

            data = data.replace(patt, "<a ");

            $("#sidebar").html(data);
            $("#sidebar").find("table.sortable").each(function (i) {
                sorttable.makeSortable(this);
                sorttable.innerSortFunction.apply($(this).find("th")[1], []);
            });
            utils.limitHeight('.should-limit');
    });
}

function showPersonHistoryInSidebar(person) {
    showSomethingInSidebar("/personHistory/" + person.id);
}

function showRuleHistoryInSidebar(rule_id) {
    showSomethingInSidebar("/ruleHistory/" + rule_id);
}

function selectNextInput(cur_input) {
    var next = $(":input:eq(" + ($(":input").index(cur_input) + 1) + ")");
    next.focus();
}

function Person(id, name) {
    this.name = name;
    this.id = id;
    var self = this;

    this.render = function() {
        return app.person_template({"name": name});
    };

    // called by PeopleChooser after self.el has been
    // initialized.
    this.activateClick = function() {
        self.el.click(
            function() {
                showPersonHistoryInSidebar(self);
            }
        );
    };
}

function PeopleChooser(el, on_add, on_remove) {
    this.el = el;
    this.people = [];
    var self = this;

    this.search_box = el.find(".person_search");
    this.search_box.autocomplete({
        source: app.people,
        delay: 0,
        autoFocus: true,
    });

    self.one_person_mode = false;

//  el.mouseleave(function() {
//      if (self.people.length > 0) {
//          self.search_box.fadeOut();
//      } } );
//  el.mouseenter(function() { self.search_box.fadeIn(); } );
//
    this.search_box.bind( "autocompleteselect", function(event, ui) {
        new_person = self.addPerson(ui.item.id, ui.item.label);

        if (on_add && new_person) {
            on_add(new_person);
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addPerson = function(id, name) {

        // Don't add people who have already been added.
        for (var i in self.people) {
            if (id == self.people[i].id) {
                return;
            }
        }

        if (self.one_person_mode) {
            self.search_box.hide();
            self.el.find(".glyphicon").hide();
            selectNextInput(self.search_box);
        }

        var p = new Person(id, name);
        self.people.push(p);
        p.el = self.el.find(".people").append(p.render()).children(":last-child");
        p.activateClick();

        p.el.find("img").click(function() { self.removePerson(p); });

        return p;
    };

    this.removePerson = function(person) {
        $(person.el).remove();

        for (var i in self.people) {
            if (self.people[i] == person) {
                self.people.splice(i, 1);
            }
        }

        if (self.one_person_mode) {
            self.search_box.show();
            self.el.find(".glyphicon").show();
        }

        if (on_remove) {
            on_remove(person);
        }
    };

    this.loadPeople = function(people) {
        for (var i in people) {
            self.addPerson(people[i].id, people[i].name);
        }
    };

    this.setOnePersonMode = function(one_person_mode) {
        self.one_person_mode = one_person_mode;
        return self;
    };
}

function RuleChooser(el, on_change) {
    this.el = el;
    var self = this;

    this.search_box = el.find(".rule_search");
    this.search_box.autocomplete({
        source: app.rules,
        delay: 0,
        autoFocus: true,
    });

    this.search_box.bind( "autocompleteselect", function(event, ui) {
        self.setRule(ui.item.id, ui.item.label);

        if (on_change) { on_change(); }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.setRule = function(id, title) {
        self.search_box.hide();

        self.rule = id;
        self.rule_el =
            self.el.prepend(app.rule_template({name: title})).
                children(":first-child");

        self.rule_el.find(".label").click(function() {
            showRuleHistoryInSidebar(self.rule);
        });

        self.rule_el.find("img").click(function() { self.unsetRule(); });

        selectNextInput(self.search_box);
    };

    this.unsetRule = function() {
        $(self.rule_el).remove();

        self.rule = null;
        self.rule_el = null;

        self.search_box.show();

        if (on_change) { on_change(); }
    };

    this.loadData = function(json) {
        self.setRule(json.id, json.title);
    };
}

function Charge(charge_id, el) {
    var self = this;

    var SEVERITIES = {
        "mild": "Mild",
        "moderate" : "Moderate",
        "serious": "Serious",
        "severe": "Severe"
    };

    this.checkReferralLabelHighlight = function() {
        if (el.find(".minor-referral-destination").val()) {
            el.find(".minor-referral-label").addClass("highlight");
        } else {
            el.find(".minor-referral-label").removeClass("highlight");
        }
    };

    this.loadData = function(json) {
        el.find(".resolution_plan").val(json.resolution_plan);
        if (json.plea == "Guilty") {
            el.find(".plea-guilty").prop("checked", true);
        } else if (json.plea == "No Contest") {
            el.find(".plea-no-contest").prop("checked", true);
        } else if (json.plea == "Not Guilty") {
            el.find(".plea-not-guilty").prop("checked", true);
        } else if (json.plea == "N/A") {
            el.find(".plea-na").prop("checked", true);
        }

        // rule, person
        if (json.person) {
            self.people_chooser.addPerson(
                json.person.person_id,
                utils.displayName(json.person));
        }

        if (json.rule) {
            self.rule_chooser.loadData(json.rule);
        }

        if (json.referred_to_sm) {
            el.find(".refer-to-sm").prop("checked", true);
        }

        for (var key in SEVERITIES) {
            if (json.severity == SEVERITIES[key]) {
                el.find(".severity-" + key).prop("checked", true);
            }
        }

        el.find(".minor-referral-destination").val(json.minor_referral_destination);
        self.checkReferralLabelHighlight();
    };

    this.saveIfNeeded = function() {
        window.setTimeout(self.saveIfNeeded, 5000);
        if (!self.is_modified) {
            return;
        }

        url = "/saveCharge?id=" + charge_id;

        if (self.people_chooser.people.length > 0) {
            url += "&person_id=" + self.people_chooser.people[0].id;
        }

        url += "&resolution_plan=" + encodeURIComponent(el.find(".resolution_plan").val());

        for (var key in SEVERITIES) {
            if (el.find(".severity-" + key).prop("checked")) {
                url += "&severity=" + SEVERITIES[key];
            }
        }

        plea = el.find(".plea-guilty");
        if (plea.prop("checked")) {
            url += "&plea=Guilty";
        }

        plea = el.find(".plea-no-contest");
        if (plea.prop("checked")) {
            url += "&plea=No Contest";
        }

        plea = el.find(".plea-not-guilty");
        if (plea.prop("checked")) {
            url += "&plea=Not Guilty";
        }

        plea = el.find(".plea-na");
        if (plea.prop("checked")) {
            url += "&plea=N/A";
        }

        refer = el.find(".refer-to-sm");
        url += "&referred_to_sm=" + refer.prop("checked");

        minor_referral_destination = el.find(".minor-referral-destination");
        if (minor_referral_destination.length > 0) {
            url += "&minor_referral_destination=" +
                encodeURIComponent(minor_referral_destination.val());
        }

        if (self.rule_chooser.rule) {
            url += "&rule_id=" + self.rule_chooser.rule;
        }

        $.post(url, function(data) {
            self.is_modified = false;
        });
    };

    this.removeCharge = function() {
        self.el.remove();
        $.post("/removeCharge?id=" + charge_id);
        // Don't try to save any remaining modifications if
        // this charge has been removed.
        self.is_modified = false;
        // TODO: remove from Case.charges list
    };

    this.markAsModified = function() {
        self.is_modified = true;
        if ((self.people_chooser.people.length === 0) ||
            !self.rule_chooser.rule) {
            el.find(".last-rp").html("");
        }

        if (self.people_chooser.people.length == 1 &&
            self.rule_chooser.rule) {
            url = "/getLastRp";
            url += "/" + self.people_chooser.people[0].id;
            url += "/" + self.rule_chooser.rule;
            $.get(url, function (data) {
                el.find(".last-rp").html(data);
                el.find(".last-rp .more-info").click(function() {
                    showSomethingInSidebar(
                        '/personRuleHistory' +
                        '/' + self.people_chooser.people[0].id +
                        '/' + self.rule_chooser.rule);
                });
            });
        }

        self.checkReferralLabelHighlight();
    };

    this.old_rp = null;
    this.checkText = function() {
        if (el.find(".resolution_plan").val() !== self.old_rp) {
            self.markAsModified();
            self.old_rp = el.find(".resolution_plan").val();
        }
    };

    self.el = el;
    self.is_modified = false;
    window.setTimeout(self.saveIfNeeded, 5000);

    self.remove_button = el.find("button");
    self.remove_button.click(self.removeCharge);

    el.find(".resolution_plan").change(self.markAsModified);
    el.find(".resolution_plan").on(utils.TEXT_AREA_EVENTS, self.checkText);
    el.find(".plea-guilty").change(self.markAsModified);
    el.find(".plea-no-contest").change(self.markAsModified);
    el.find(".plea-na").change(self.markAsModified);
    el.find(".plea-not-guilty").change(self.markAsModified);
    el.find(".plea-not-guilty").change(function() {
            self.el.find(".refer-to-sm").prop("checked", true);
    });
    el.find(".refer-to-sm").change(self.markAsModified);
    el.find(".minor-referral-destination").change(self.markAsModified);
    el.find(".minor-referral-destination").on(utils.TEXT_AREA_EVENTS, self.markAsModified);

    self.people_chooser = new PeopleChooser(el.find(".people_chooser"),
                                            self.markAsModified,
                                            self.markAsModified);
    self.people_chooser.setOnePersonMode(true);

    self.rule_chooser = new RuleChooser(el.find(".rule_chooser"),
                                        self.markAsModified);

    el.find("input[type=radio]").prop("name", "plea-" + charge_id);

    // el.mouseleave(function() { self.remove_button.hide(); } );
    // el.mouseenter(function() { self.remove_button.show(); } );
    for (var key in SEVERITIES) {
        el.find(".severity-" + key).change(self.markAsModified);
    }
    el.find(".severity[type=radio]").prop("name", "severity-" + charge_id);
}

function Case (id, el) {
    var self = this;

    this.saveIfNeeded = function() {
        window.setTimeout(self.saveIfNeeded, SAVE_TIMEOUT);
        if (!self.is_modified) {
            return;
        }

        $.post("/saveCase?id=" + id, {
            closed: !self.el.find("input.continued").prop("checked"),
            date: self.el.find(".date").val(),
            findings: self.el.find(".findings").val(),
            location: self.el.find(".location").val(),
            time: self.el.find(".time").val()
        }, function(data) {
            self.is_modified = false;
        });
    };

    this.markAsModified = function() {
        self.is_modified = true;
    };

    this.loadData = function(data) {
        el.find(".location").val(data.location);
        el.find(".date").val(data.date);
        el.find(".time").val(data.time);
        el.find(".findings").val(data.findings);
        el.find("input.continued").prop("checked", data.date_closed === null);

        for (var i in data.people_at_case) {
            pac = data.people_at_case[i];
            if (pac.role == app.ROLE_TESTIFIER) {
                self.testifier_chooser.addPerson(
                    pac.person.person_id,
                    utils.displayName(pac.person));
            } else if (pac.role == app.ROLE_WRITER) {
                self.writer_chooser.addPerson(
                    pac.person.person_id,
                    utils.displayName(pac.person));
            }
        }

        for (i in data.charges) {
            ch = data.charges[i];
            new_charge = self.addChargeNoServer(ch.id);
            new_charge.loadData(ch);
        }
    };

    this.addCharge = function() {
        $('body').animate({'scrollTop': $('body').scrollTop() + 100}, 'slow');
        $.post("/addCharge?case_id=" + id,
               function(data, textStatus, jqXHR) {
            self.addChargeNoServer(parseInt(data));
        } );
    };

    this.addChargeNoServer = function(charge_id) {
        var new_charge_el = self.el.find(".charges").append(
            app.charge_template()).children(":last-child");
        var new_charge = new Charge(charge_id, new_charge_el);
        self.charges.push(new_charge);
        return new_charge;
    };

    self.old_findings = null;

    this.checkText = function() {
        if (el.find(".findings").val() !== self.old_findings) {
            self.markAsModified();
            self.old_findings = el.find(".findings").val();
        }
    };

    this.id = id;
    this.el = el;
    if (config.track_writer) {
        this.writer_chooser = new PeopleChooser(el.find(".writer"),
            function(person) {
                $.post("/addPersonAtCase?case_id=" + id +
                       "&person_id=" + person.id +
                       "&role=" + app.ROLE_WRITER);
            },
            function(person) {
                $.post("/removePersonAtCase?case_id=" + id +
                       "&person_id=" + person.id +
                       "&role=" + app.ROLE_WRITER);
            });
    }

    this.testifier_chooser = new PeopleChooser(
        el.find(".testifier"),
        function(person) {
            $.post("/addPersonAtCase?case_id=" + id +
                   "&person_id=" + person.id +
                   "&role=" + app.ROLE_TESTIFIER);
        },
        function(person) {
            $.post("/removePersonAtCase?case_id=" + id +
                   "&person_id=" + person.id +
                   "&role=" + app.ROLE_TESTIFIER);
        });
    this.is_modified = false;
    this.charges = [];

    el.find(".location").change(self.markAsModified);
    el.find(".findings").change(self.markAsModified);
    el.find(".findings").on(utils.TEXT_AREA_EVENTS, self.checkText);
    el.find(".date").change(self.markAsModified);
    el.find(".time").change(self.markAsModified);
    el.find("input.continued").change(self.markAsModified);

    el.find(".add-charges").click(self.addCharge);

    window.setTimeout(self.saveIfNeeded, SAVE_TIMEOUT);
}

function addPersonAtMeeting(person, role) {
    $.post("/addPersonAtMeeting?meeting_id=" + app.meeting_id +
           "&person_id=" + person.id +
           "&role=" + role );
}

function removePersonAtMeeting(person, role) {
    $.post("/removePersonAtMeeting?meeting_id=" + app.meeting_id +
           "&person_id=" + person.id +
           "&role=" + role );
}

function makePeopleChooser(selector, role) {
    return new PeopleChooser(
        $(selector),
        function(person) { addPersonAtMeeting(person, role); },
        function(person) { removePersonAtMeeting(person, role); });
}

function loadInitialData() {
    app.committee_chooser.loadPeople(app.initial_data.committee);
    app.chair_chooser.loadPeople(app.initial_data.chair);
    app.notetaker_chooser.loadPeople(app.initial_data.notetaker);
    app.sub_chooser.loadPeople(app.initial_data.sub);

    for (var i in app.initial_data.cases) {
        data = app.initial_data.cases[i];
        new_case = addCaseNoServer(data.id, data.case_number);
        new_case.loadData(data);
    }
}

function checkDirties() {
    var num_dirty = 0;

    for (var i in app.cases) {
        var c = app.cases[i];
        if (c.is_modified) {
            num_dirty++;
        }
        for (var j in c.charges) {
            var ch = c.charges[j];
            if (ch.is_modified) {
                num_dirty++;
            }
        }
    }

    if (num_dirty < 3) {
        $("#notice").hide();
    } else {
        $("#notice").show();
        $("#notice").html("" + num_dirty + " unsaved changes. Connection lost?");
    }

    window.setTimeout(checkDirties, 1000);
}

function continueCase(event) {
    var list_item = $(event.target).parent();
    var id = list_item.prop("id");
    var splits = id.split("-");
    var case_id = splits[splits.length-1];

    $.post("/continueCase?meeting_id=" + app.meeting_id +
           "&case_id=" + case_id, "",
           function(data, textStatus, jqXHR) {
        var case_data = $.parseJSON(data);
        var new_case = addCaseNoServer(case_data.id, case_data.case_number);
        new_case.loadData(case_data);
        $('body').animate({'scrollTop': new_case.el.offset().top + 500}, 'slow');
        list_item.remove();
    });
}

window.initMinutesPage = function() {
    Handlebars.registerHelper('render', function(partialId, options) {
      var selector = 'script[type="text/x-handlebars-template"]#' + partialId,
          source = $(selector).html(),
          html = Handlebars.compile(source)(options.hash);

      return new Handlebars.SafeString(html);
    });

    $("#meeting").append(Handlebars.compile($("#meeting-template").html())());

    Handlebars.registerPartial("people-chooser", $("#people-chooser").html());
    Handlebars.registerPartial("rule-chooser", $("#rule-chooser").html());

    app.case_template = Handlebars.compile($("#case-template").html());
    app.person_template = Handlebars.compile($("#person-template").html());
    app.charge_template = Handlebars.compile($("#charge-template").html());
    app.rule_template = Handlebars.compile($("#rule-template").html());

    app.committee_chooser =
        makePeopleChooser(".committee", app.ROLE_JC_MEMBER);

    app.chair_chooser =
        makePeopleChooser(".chair", app.ROLE_JC_CHAIR);
    app.chair_chooser.setOnePersonMode(true);

    app.notetaker_chooser =
        makePeopleChooser(".notetaker", app.ROLE_NOTE_TAKER);
    app.sub_chooser =
        makePeopleChooser(".sub", app.ROLE_JC_SUB);

    loadInitialData();

    $(".continue-cases li").click(continueCase);
    $("button.add-case").click(addCase);

    window.onbeforeunload = function(e) {
        var dirty = false;
        for (var i in app.cases) {
            var c = app.cases[i];
            if (c.is_modified) {
                dirty = true;
            }
            for (var j in c.charges) {
                var ch = c.charges[j];
                if (ch.is_modified) {
                    dirty = true;
                }
            }
        }

        if (dirty) {
            return "UNsaved changes! Please give me a few more seconds...";
        }
        return null;
    };

    checkDirties();
};

function addCaseNoServer(id, number)
{
    new_case = $("#meeting-cases").append(
        app.case_template({"num": number})).
        children(":last-child");

    var case_obj = new Case(id, new_case);
    app.cases.push(case_obj);

    $("#meeting-cases").append(case_obj.el);
    case_obj.el.find(".date").datepicker({
        showOtherMonths: true,
        selectOtherMonths: true});
    case_obj.el.find(".date").datepicker("option", "dateFormat", "yy-mm-dd");

    return case_obj;
}

function addCase()
{
    $.post("/newCase?meeting_id=" + app.meeting_id, "",
           function(data, textStatus, jqXHR) {
        id_num_pair = $.parseJSON(data);
        var new_case = addCaseNoServer(id_num_pair[0], id_num_pair[1]);
        $('body').animate({'scrollTop': new_case.el.offset().top + 500}, 'slow');
    });
}
