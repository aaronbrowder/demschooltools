var Handlebars = require('handlebars');

var utils = require('./utils');

function zeroPad(minutes) {
    if (minutes < 10) {
        return "0" + minutes;
    } else {
        return "" + minutes;
    }
}

function dbTimeToUserTime(str) {
    if (str === null || str.length == 0) {
        return;
    }

    var splits = str.split(":");
    var hours = parseInt(splits[0]);
    var minutes = parseInt(splits[1]);

    var ampm = "AM";

    if (hours >= 12) {
        ampm = "PM";
    }

    if (hours > 12) {
        hours -= 12;
    }

    return "" + hours + ":" + zeroPad(minutes) + " " + ampm;
}

function typedToUserTime(s) {
    if (!s.match(/^[0-9]+$/)) {
        return s;
    }

    if (s.length < 3) {
        s = s + "00";
    }

    var num = parseInt(s);
    var hours = Math.floor(num / 100);
    var minutes = num % 100;

    if (hours < 0 || hours > 12 || minutes < 0 || minutes > 59) {
        return "";
    }

    var ampm = "AM";
    if (hours == 12 || hours <= 6) {
        ampm = "PM";
    }

    return "" + hours + ":" + zeroPad(minutes) + " " + ampm;
}

function Day(data, start_input, end_input) {
    var self = this;

    self.activateCode = function() {
        var the_code = self.start_input.val();

        self.end_input.val("");
        self.end_input.hide();
        if (app.codes[the_code]) {
            self.color_bar.css("background-color", app.codes[the_code].color).show();
        } else {
            self.color_bar.hide();
        }

        self.code_mode = true;
    };

    self.deactivateCode = function() {
        self.code_mode = false;
        self.end_input.show();
        self.color_bar.hide();
    };

    self.checkForCode = function() {
        var the_code = self.start_input.val();

        if (the_code.length > 0 && !the_code.match(/[0-9]/)) {
            self.activateCode();
        } else {
            self.deactivateCode();
        }
    };

    self.onChange = function() {
        self.dirty = true;
        self.checkForCode();
    };

    self.onBlur = function() {
        self.start_input.val(typedToUserTime(self.start_input.val()));
        self.end_input.val(typedToUserTime(self.end_input.val()));
    };

    this.save = function() {
        self.dirty = false;
        var url = "/attendance/saveDay?day_id=" + self.id;
        if (self.code_mode) {
            url += "&code=" + self.start_input.val();
        } else {
            url += "&start_time=" + self.start_input.val() +
                "&end_time=" + self.end_input.val();
        }
        $.post(url);
    };

    self.start_input = $(start_input);
    self.end_input = $(end_input);
    self.color_bar = self.end_input.parent().find('.color-bar');
    self.id = data.id;
    self.dirty = false;
    self.code_mode = false;

    if (data.code) {
        self.start_input.val(data.code);
        self.checkForCode();
    } else {
        self.start_input.val(dbTimeToUserTime(data.start_time));
        self.end_input.val(dbTimeToUserTime(data.end_time));
    }

    self.start_input.blur(self.onBlur);
    self.end_input.blur(self.onBlur);
    self.start_input.on(utils.TEXT_AREA_EVENTS, self.onChange);
    self.end_input.on(utils.TEXT_AREA_EVENTS, self.onChange);

    return self;
}

function PersonRow(person, days, week, el) {
    var self = this;

    self.setDirty = function() { self.dirty = true; }

    this.removePerson = function() {
        $.post("/attendance/deletePersonWeek?person_id=" + person.person_id +
               "&monday=" + app.monday).done(function(data) {
            self.el.remove();
            app.person_rows.splice(app.person_rows.indexOf(self), 1);
            addAdditionalPerson(self.person);
        });
    };

    this.save = function() {
        self.dirty = false;
        $.post("/attendance/saveWeek?week_id=" + self.week.id +
               "&extra_hours=" + self.week_el.val());
    };

    self.person = person;
    self.days = [];
    self.week = week;
    self.el = el;
    self.dirty = false;

    var inputs = el.find("input");
    self.el.find("img").click(self.removePerson);

    for (var i = 0; i < 5; i++) {
        self.days.push(new Day(days[i], inputs[i*2], inputs[i*2+1]));
    }

    self.week_el = $(inputs[10]);
    self.week_el.val(week.extra_hours);

    self.week_el.on(utils.TEXT_AREA_EVENTS, self.setDirty);

    return self;
}

function addNewPersonRow(people) {
    var ids = [];
    for (var i = 0; i < people.length; i++) {
        ids.push(people[i].person_id);
    }
    $.post("/attendance/createPersonWeek", {
            'person_id[]': ids,
            monday: app.monday
        }).done(function(data) {
            var results = $.parseJSON(data);
            for (var i = 0; i < results.length; i++) {
                var result = results[i];
                loadRow(result.week.person, result.days, result.week, $(".table"));
            }
        });
}

function addAdditionalPerson(person) {
    var new_el = $("#additional-people").append(
        app.additional_person_template({
            "name": person.first_name + " " + person.last_name
        })).children(":last-child");

    new_el.find("a").click(function () {
        addNewPersonRow([person]);
        new_el.remove();
    });
}

function addAllAdditionalPeople() {
    $("#additional-people").empty();
    addNewPersonRow(app.initial_data.additional_people);
}

function loadRow(person, days, week, dest_el) {
    var insert_before_i;
    for (var i = 0; i < app.person_rows.length; i++) {
        var p2 = app.person_rows[i].person;
        if ((p2.first_name + ' ' + p2.last_name) >
            (person.first_name + ' ' + person.last_name)) {
            insert_before_i = i;
            break;
        }
    }
    var new_row_el = $($.parseHTML(
        app.person_row_template({
            "first_name": person.first_name,
            "last_name": person.last_name,
        })));
    if (insert_before_i !== undefined) {
        app.person_rows[insert_before_i].el.before(new_row_el);
    } else {
        dest_el.append(new_row_el);
    }

    var new_row = new PersonRow(person, days, week, new_row_el);
    if (insert_before_i !== undefined) {
        app.person_rows.splice(insert_before_i, 0, new_row);
    } else {
        app.person_rows.push(new_row);
    }
}

function setNoSchool(day_num) {
    for (var i in app.person_rows) {
        app.person_rows[i].days[day_num].start_input.val("_NS_");
        app.person_rows[i].days[day_num].onChange();
    }
}

function handleNoSchoolButton(day_num) {
    return function() {
        $( "#dialog-confirm" ).dialog({
              resizable: false,
              height: 240,
              modal: true,
              buttons: {
                "Erase existing data": function() {
                    setNoSchool(day_num);
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                     $( this ).dialog( "close" );
                }
              }
            });
    };
}

function saveIfNeeded() {
    for (var i in app.person_rows) {
        if (app.person_rows[i].dirty) {
            app.person_rows[i].save();
        }
        for (var j in app.person_rows[i].days) {
            if (app.person_rows[i].days[j].dirty) {
                app.person_rows[i].days[j].save();
            }
        }
    }

    window.setTimeout(saveIfNeeded, 2000);
}

window.initAttendanceWeek = function() {
    app.person_row_template = Handlebars.compile($("#person-row-template").html().trim());
    app.additional_person_template =
        Handlebars.compile($("#additional-person-template").html());

    var no_school_buttons = $("button.no-school");
    for (var i = 0; i < 5; i++) {
        var button = no_school_buttons[i];
        $(button).click(handleNoSchoolButton(i));
    }

    for (i in app.initial_data.active_people) {
        var person = app.initial_data.active_people[i];
        loadRow(person,
                app.initial_data.days[person.person_id],
                app.initial_data.weeks[person.person_id],
                $('.attendance-view tbody'));
    }

    for (i in app.initial_data.additional_people) {
        person = app.initial_data.additional_people[i];
        addAdditionalPerson(person);
    }

    saveIfNeeded();

    $("button.add-all").click(addAllAdditionalPeople);
};
