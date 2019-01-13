var Handlebars = require('handlebars');

var utils = require('./utils');

var result_template_str = 
    '<div class="result" data-id="{{id}}">' +
        '<span class="label label-success">{{name}}</span>' +
        '<img src="/assets/images/x.png">' +
    '</div>';

var result_template = Handlebars.compile(result_template_str);

var Chooser = function(el, allowMultiple, source, getLabel, onClick, onChange, onAdd, onRemove) {

    this.el = el;
    var self = this;

    this.results = [];

    this.search_box = el.find(".search");
    this.search_box.autocomplete({
        autoFocus: true,
        delay: 0,
        minLength: 2,
        source: source,
    });

    this.search_box.bind( "autocompleteselect", function(event, ui) {
        self.addResult(ui.item.id, ui.item.label);

        if (onAdd) {
            onAdd(ui.item.id);
        }
        if (onChange) { 
            onChange();
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addResult = function(id, title) {
        // Don't add results that have already been added.
        for (var i in self.results) {
            if (id == self.results[i].id) {
                return;
            }
        }

        if (!allowMultiple) {
            self.search_box.hide();
            utils.selectNextInput(self.search_box);
        }

        self.results.push(id);

        var result_el = $(result_template({name: title, id: id}));
        self.el.find(".results").append(result_el);

        if (onClick) {
            result_el.find(".label").click(function() {
                onClick(id);
            });
        }

        result_el.find("img").click(function() { self.removeResult(result_el); });
    };

    this.removeResult = function(result_el) {
        $(result_el).remove();

        for (var i in self.results) {
            if (self.results[i] == result_el.data('id')) {
                self.results.splice(i, 1);
            }
        }

        if (!allowMultiple) {
            self.search_box.show();
        }

        if (onRemove) {
            onRemove(result_el.data('id'));
        }
        if (onChange) { 
            onChange();
        }
    };

    this.clear = function() {
        self.results = [];
    }

    this.loadData = function(json) {
        if (allowMultiple) {
            for (var i in json) {
                self.addResult(json[i].id, getLabel(json[i]));
            }
        } else if (json) {
            self.addResult(json.id, getLabel(json));
        }
    };
}

module.exports = {
    Chooser: Chooser
}
