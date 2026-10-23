(function (window, $) {
    if (!$) {
        return;
    }

    var FIELD_DEFAULTS = {
        second: "*",
        min: "*",
        hour: "*",
        day: "*",
        month: "*",
        week: "?",
        year: ""
    };

    var TABS = [
        { id: "Secondly", key: "second" },
        { id: "Minutes", key: "min" },
        { id: "Hourly", key: "hour" },
        { id: "Daily", key: "day" },
        { id: "Monthly", key: "month" },
        { id: "Weekly", key: "week" },
        { id: "Yearly", key: "year" }
    ];

    function pad2(value) {
        value = String(value);
        return value.length === 1 ? "0" + value : value;
    }

    function values(start, end) {
        var list = [];
        for (var i = start; i <= end; i++) {
            list.push(String(i));
        }
        return list;
    }

    function cloneDefaults() {
        return $.extend({}, FIELD_DEFAULTS);
    }

    function trimValue(value) {
        return $.trim(value || "");
    }

    function splitCron(expression) {
        var parts = trimValue(expression).split(/\s+/);
        if (parts.length < 6) {
            return cloneDefaults();
        }
        return {
            second: parts[0],
            min: parts[1],
            hour: parts[2],
            day: parts[3],
            month: parts[4],
            week: parts[5],
            year: parts.length > 6 ? parts[6] : ""
        };
    }

    function buildCron(fields) {
        var result = [
            fields.second || FIELD_DEFAULTS.second,
            fields.min || FIELD_DEFAULTS.min,
            fields.hour || FIELD_DEFAULTS.hour,
            fields.day || FIELD_DEFAULTS.day,
            fields.month || FIELD_DEFAULTS.month,
            fields.week || FIELD_DEFAULTS.week
        ];
        if (fields.year) {
            result.push(fields.year);
        }
        return result.join(" ");
    }

    function activeTabKey($panel) {
        var href = $panel.find("#CronGenTabs li.active a").attr("href") || "#Secondly";
        var id = href.replace("#", "");
        for (var i = 0; i < TABS.length; i++) {
            if (TABS[i].id === id) {
                return TABS[i].key;
            }
        }
        return "second";
    }

    function applyLowerDefaults(fields, activeKey) {
        if (activeKey !== "second" && (fields.second === "" || fields.second === "*")) {
            fields.second = "0";
        }
        if (activeKey !== "second" && activeKey !== "min" && (fields.min === "" || fields.min === "*")) {
            fields.min = "0";
        }
        if (activeKey !== "second" && activeKey !== "min" && activeKey !== "hour" && (fields.hour === "" || fields.hour === "*")) {
            fields.hour = "0";
        }
        if (activeKey === "week" && fields.week !== "?") {
            fields.day = "?";
        }
        if (activeKey === "day" && fields.day !== "?") {
            fields.week = "?";
        }
    }

    function textInput(id, value, width) {
        return $("<input/>", {
            type: "text",
            id: id,
            value: value,
            inputmode: "numeric",
            autocomplete: "off"
        }).addClass("cron-number").css({
            width: width || "35px",
            height: "20px",
            textAlign: "center",
            margin: "0 3px"
        });
    }

    function radio(name, mode, id) {
        return $("<input/>", {
            type: "radio",
            name: name,
            value: mode,
            id: id || null
        });
    }

    function line() {
        return $("<div/>").addClass("line");
    }

    function optionLine(field, mode, labelParts, id) {
        var $line = line().attr({ "data-field": field, "data-mode": mode });
        $line.append(radio(field, mode, id));
        for (var i = 0; i < labelParts.length; i++) {
            var part = labelParts[i];
            if (part && part.jquery) {
                $line.append(part);
            } else {
                $line.append(part);
            }
        }
        return $line;
    }

    function checkboxRows(field, from, to, perRow) {
        var rows = [];
        var $row;
        for (var i = from; i <= to; i++) {
            if ((i - from) % perRow === 0) {
                $row = $("<div/>").addClass("imp " + field + "List");
                rows.push($row);
            }
            $row.append($("<input/>", {
                type: "checkbox",
                value: String(i),
                disabled: "disabled"
            }).css({ marginLeft: "5px" }));
            $row.append(field === "week" ? String(i) : pad2(i));
        }
        return rows;
    }

    function appendTimeTab($content, cfg, labels) {
        var $tab = $("<div/>", { id: cfg.id }).addClass("tab-pane");
        if (cfg.active) {
            $tab.addClass("active");
        }
        $tab.append(optionLine(cfg.field, "every", [cfg.everyText]));
        $tab.append(optionLine(cfg.field, "range", [
            labels.rangePrefix,
            textInput(cfg.field + "Start_0", cfg.rangeStart),
            "-",
            textInput(cfg.field + "End_0", cfg.rangeEnd),
            cfg.unit
        ]));
        $tab.append(optionLine(cfg.field, "interval", [
            labels.fromPrefix,
            textInput(cfg.field + "Start_1", cfg.intervalStart),
            cfg.intervalMiddle,
            textInput(cfg.field + "End_1", cfg.intervalStep),
            cfg.intervalSuffix
        ]));
        $tab.append(optionLine(cfg.field, "specific", [labels.specific], cfg.appointId));
        $.each(checkboxRows(cfg.field, cfg.from, cfg.to, cfg.perRow), function (_, $row) {
            $tab.append($row);
        });
        $content.append($tab);
    }

    function appendDayTab($content, labels) {
        var $tab = $("<div/>", { id: "Daily" }).addClass("tab-pane");
        $tab.append(optionLine("day", "every", [labels.day.every]));
        $tab.append(optionLine("day", "none", [labels.none]));
        $tab.append(optionLine("day", "range", [
            labels.rangePrefix,
            textInput("dayStart_0", "1"),
            "-",
            textInput("dayEnd_0", "2"),
            labels.day.unit
        ]));
        $tab.append(optionLine("day", "interval", [
            labels.fromPrefix,
            textInput("dayStart_1", "1"),
            labels.day.intervalMiddle,
            textInput("dayEnd_1", "1"),
            labels.day.intervalSuffix
        ]));
        $tab.append(optionLine("day", "workday", [
            labels.day.workdayPrefix,
            textInput("dayStart_2", "1"),
            labels.day.workdaySuffix
        ]));
        $tab.append(optionLine("day", "last", [labels.day.last]));
        $tab.append(optionLine("day", "specific", [labels.specific], "day_appoint"));
        $.each(checkboxRows("day", 1, 31, 10), function (_, $row) {
            $tab.append($row);
        });
        $content.append($tab);
    }

    function appendMonthTab($content, labels) {
        var $tab = $("<div/>", { id: "Monthly" }).addClass("tab-pane");
        $tab.append(optionLine("month", "every", [labels.month.every]));
        $tab.append(optionLine("month", "none", [labels.none]));
        $tab.append(optionLine("month", "range", [
            labels.rangePrefix,
            textInput("monthStart_0", "1"),
            "-",
            textInput("monthEnd_0", "2"),
            labels.month.unit
        ]));
        $tab.append(optionLine("month", "interval", [
            labels.fromPrefix,
            textInput("monthStart_1", "1"),
            labels.month.intervalMiddle,
            textInput("monthEnd_1", "1"),
            labels.month.intervalSuffix
        ]));
        $tab.append(optionLine("month", "specific", [labels.specific], "month_appoint"));
        $.each(checkboxRows("month", 1, 12, 6), function (_, $row) {
            $tab.append($row);
        });
        $content.append($tab);
    }

    function appendWeekTab($content, labels) {
        var $tab = $("<div/>", { id: "Weekly" }).addClass("tab-pane");
        $tab.append(optionLine("week", "every", [labels.week.every]));
        $tab.append(optionLine("week", "none", [labels.none]));
        $tab.append(optionLine("week", "range", [
            labels.week.rangePrefix,
            textInput("weekStart_0", "1"),
            "-",
            textInput("weekEnd_0", "2")
        ]));
        $tab.append(optionLine("week", "interval", [
            labels.week.intervalPrefix,
            textInput("weekStart_1", "1"),
            labels.week.intervalMiddle,
            textInput("weekEnd_1", "1")
        ]));
        $tab.append(optionLine("week", "last", [
            labels.week.lastPrefix,
            textInput("weekStart_2", "1")
        ]));
        $tab.append(optionLine("week", "specific", [labels.specific], "week_appoint"));
        $.each(checkboxRows("week", 1, 7, 7), function (_, $row) {
            $tab.append($row);
        });
        $content.append($tab);
    }

    function appendYearTab($content, labels) {
        var $tab = $("<div/>", { id: "Yearly" }).addClass("tab-pane");
        $tab.append(optionLine("year", "none", [labels.year.none]));
        $tab.append(optionLine("year", "every", [labels.year.every]));
        $tab.append(optionLine("year", "range", [
            labels.year.rangePrefix,
            textInput("yearStart_0", "2016", "45px"),
            "-",
            textInput("yearEnd_0", "2017", "45px"),
            labels.year.unit
        ]));
        $content.append($tab);
    }

    function buildPanel(labels) {
        var $main = $("<div/>", { id: "CronGenMainDiv" });
        var $tabs = $("<ul/>", { "class": "nav nav-tabs", id: "CronGenTabs" });
        $.each(TABS, function (index, tab) {
            $("<li/>", { "class": index === 0 ? "active" : "" })
                .append($("<a/>", { href: "#" + tab.id, id: tab.id + "Tab" }).text(labels.tabs[tab.key]))
                .appendTo($tabs);
        });
        var $content = $("<div/>", { "class": "tab-content" }).css({ border: 0, marginTop: "-20px" });

        appendTimeTab($content, {
            id: "Secondly", field: "second", active: true, from: 0, to: 59, perRow: 10,
            unit: labels.second.unit, everyText: labels.second.every, appointId: "sencond_appoint",
            rangeStart: "1", rangeEnd: "2", intervalStart: "0", intervalStep: "1",
            intervalMiddle: labels.second.intervalMiddle, intervalSuffix: labels.second.intervalSuffix
        }, labels);
        appendTimeTab($content, {
            id: "Minutes", field: "min", from: 0, to: 59, perRow: 10,
            unit: labels.min.unit, everyText: labels.min.every, appointId: "min_appoint",
            rangeStart: "1", rangeEnd: "2", intervalStart: "0", intervalStep: "1",
            intervalMiddle: labels.min.intervalMiddle, intervalSuffix: labels.min.intervalSuffix
        }, labels);
        appendTimeTab($content, {
            id: "Hourly", field: "hour", from: 0, to: 23, perRow: 6,
            unit: labels.hour.unit, everyText: labels.hour.every, appointId: "hour_appoint",
            rangeStart: "1", rangeEnd: "2", intervalStart: "0", intervalStep: "1",
            intervalMiddle: labels.hour.intervalMiddle, intervalSuffix: labels.hour.intervalSuffix
        }, labels);
        appendDayTab($content, labels);
        appendMonthTab($content, labels);
        appendWeekTab($content, labels);
        appendYearTab($content, labels);

        var $container = $("<div/>", { "class": "container-fluid" }).css({ marginTop: "30px", marginLeft: "-14px" });
        $("<div/>", { "class": "row-fluid" })
            .append($("<div/>", { "class": "span12" }).append($content))
            .appendTo($container);
        $main.append($tabs).append($container);
        $main.append($("<br/>").css({ paddingTop: "10px" }));
        $main.append($("<label/>").text(labels.runTime));
        $main.append($("<textarea/>", { id: "runTime", rows: 6, readonly: "readonly" }).css({
            width: "90%",
            resize: "none",
            background: "none",
            border: "none",
            outline: "none"
        }));
        return $main;
    }

    function setTabByField($panel, field) {
        var tab = "Secondly";
        $.each(TABS, function (_, item) {
            if (item.key === field) {
                tab = item.id;
            }
        });
        $panel.find("#CronGenTabs li").removeClass("active");
        $panel.find("#CronGenTabs a[href='#" + tab + "']").parent().addClass("active");
        $panel.find(".tab-pane").removeClass("active");
        $panel.find("#" + tab).addClass("active");
    }

    function setRadio($panel, field, mode) {
        $panel.find("input[name='" + field + "'][value='" + mode + "']").prop("checked", true);
    }

    function selectValues($panel, field, expression) {
        var values = expression === "*" ? allValuesFor(field) : expression.split(",");
        $.each(values, function (_, value) {
            $panel.find("." + field + "List input[value='" + value + "']").prop("checked", true);
        });
    }

    function allValuesFor(field) {
        if (field === "hour") {
            return values(0, 23);
        }
        if (field === "day") {
            return values(1, 31);
        }
        if (field === "month") {
            return values(1, 12);
        }
        if (field === "week") {
            return values(1, 7);
        }
        return values(0, 59);
    }

    function applyGenericField($panel, field, value) {
        value = value || FIELD_DEFAULTS[field];
        if (value === "*") {
            setRadio($panel, field, "every");
        } else if (value.indexOf("-") > -1) {
            var range = value.split("-");
            setRadio($panel, field, "range");
            $panel.find("#" + field + "Start_0").val(range[0]);
            $panel.find("#" + field + "End_0").val(range[1]);
        } else if (value.indexOf("/") > -1) {
            var interval = value.split("/");
            setRadio($panel, field, "interval");
            $panel.find("#" + field + "Start_1").val(interval[0]);
            $panel.find("#" + field + "End_1").val(interval[1]);
        } else {
            setRadio($panel, field, "specific");
            selectValues($panel, field, value);
        }
    }

    function applyDayField($panel, value) {
        if (value === "*") {
            setRadio($panel, "day", "every");
        } else if (value === "?") {
            setRadio($panel, "day", "none");
        } else if (value.indexOf("-") > -1) {
            var range = value.split("-");
            setRadio($panel, "day", "range");
            $panel.find("#dayStart_0").val(range[0]);
            $panel.find("#dayEnd_0").val(range[1]);
        } else if (value.indexOf("/") > -1) {
            var interval = value.split("/");
            setRadio($panel, "day", "interval");
            $panel.find("#dayStart_1").val(interval[0]);
            $panel.find("#dayEnd_1").val(interval[1]);
        } else if (value.indexOf("W") > -1) {
            setRadio($panel, "day", "workday");
            $panel.find("#dayStart_2").val(value.replace("W", ""));
        } else if (value === "L") {
            setRadio($panel, "day", "last");
        } else {
            setRadio($panel, "day", "specific");
            selectValues($panel, "day", value);
        }
    }

    function applyWeekField($panel, value) {
        if (value === "*") {
            setRadio($panel, "week", "every");
        } else if (value === "?") {
            setRadio($panel, "week", "none");
        } else if (value.indexOf("-") > -1) {
            var range = value.split("-");
            setRadio($panel, "week", "range");
            $panel.find("#weekStart_0").val(range[0]);
            $panel.find("#weekEnd_0").val(range[1]);
        } else if (value.indexOf("/") > -1) {
            var interval = value.split("/");
            setRadio($panel, "week", "interval");
            $panel.find("#weekStart_1").val(interval[0]);
            $panel.find("#weekEnd_1").val(interval[1]);
        } else if (value.indexOf("L") > -1) {
            setRadio($panel, "week", "last");
            $panel.find("#weekStart_2").val(value.replace("L", ""));
        } else {
            setRadio($panel, "week", "specific");
            selectValues($panel, "week", value);
        }
    }

    function applyYearField($panel, value) {
        if (!value) {
            setRadio($panel, "year", "none");
        } else if (value === "*") {
            setRadio($panel, "year", "every");
        } else if (value.indexOf("-") > -1) {
            var range = value.split("-");
            setRadio($panel, "year", "range");
            $panel.find("#yearStart_0").val(range[0]);
            $panel.find("#yearEnd_0").val(range[1]);
        }
    }

    function applyCronToPanel($panel, expression) {
        var fields = splitCron(expression);
        applyGenericField($panel, "second", fields.second);
        applyGenericField($panel, "min", fields.min);
        applyGenericField($panel, "hour", fields.hour);
        applyDayField($panel, fields.day);
        applyGenericField($panel, "month", fields.month);
        applyWeekField($panel, fields.week);
        applyYearField($panel, fields.year);
        updateCheckboxState($panel);
        if (expression) {
            if (fields.week && fields.week !== "?") {
                setTabByField($panel, "week");
            } else if (fields.day && fields.day !== "*" && fields.day !== "?") {
                setTabByField($panel, "day");
            } else if (fields.hour && fields.hour !== "*" && fields.hour !== "0") {
                setTabByField($panel, "hour");
            } else if (fields.min && fields.min !== "*" && fields.min !== "0") {
                setTabByField($panel, "min");
            }
        }
    }

    function selectedMode($panel, field) {
        return $panel.find("input[name='" + field + "']:checked").val();
    }

    function text($panel, id) {
        return trimValue($panel.find("#" + id).val());
    }

    function numberText($panel, id, fallback) {
        var value = text($panel, id);
        return /^\d+$/.test(value) ? value : fallback;
    }

    function positiveNumberText($panel, id, fallback) {
        var value = numberText($panel, id, fallback);
        return parseInt(value, 10) > 0 ? value : fallback;
    }

    function defaultIntervalStart(field) {
        return field === "second" || field === "min" || field === "hour" ? "0" : "1";
    }

    function specificValue($panel, field) {
        var all = allValuesFor(field);
        var selected = [];
        $panel.find("." + field + "List input:checked").each(function () {
            selected.push($(this).val());
        });
        if (!selected.length) {
            selected.push(all[0]);
            $panel.find("." + field + "List input[value='" + all[0] + "']").prop("checked", true);
        }
        return selected.length === all.length ? "*" : selected.join(",");
    }

    function genericValue($panel, field) {
        var mode = selectedMode($panel, field);
        if (mode === "range") {
            return numberText($panel, field + "Start_0", "1") + "-" + numberText($panel, field + "End_0", "2");
        }
        if (mode === "interval") {
            return numberText($panel, field + "Start_1", defaultIntervalStart(field)) + "/" + positiveNumberText($panel, field + "End_1", "1");
        }
        if (mode === "specific") {
            return specificValue($panel, field);
        }
        return "*";
    }

    function dayValue($panel) {
        var mode = selectedMode($panel, "day");
        if (mode === "none") {
            return "?";
        }
        if (mode === "range") {
            return numberText($panel, "dayStart_0", "1") + "-" + numberText($panel, "dayEnd_0", "2");
        }
        if (mode === "interval") {
            return numberText($panel, "dayStart_1", "1") + "/" + positiveNumberText($panel, "dayEnd_1", "1");
        }
        if (mode === "workday") {
            return numberText($panel, "dayStart_2", "1") + "W";
        }
        if (mode === "last") {
            return "L";
        }
        if (mode === "specific") {
            return specificValue($panel, "day");
        }
        return "*";
    }

    function weekValue($panel) {
        var mode = selectedMode($panel, "week");
        if (mode === "none") {
            return "?";
        }
        if (mode === "range") {
            return numberText($panel, "weekStart_0", "1") + "-" + numberText($panel, "weekEnd_0", "2");
        }
        if (mode === "interval") {
            return numberText($panel, "weekStart_1", "1") + "/" + positiveNumberText($panel, "weekEnd_1", "1");
        }
        if (mode === "last") {
            return numberText($panel, "weekStart_2", "1") + "L";
        }
        if (mode === "specific") {
            return specificValue($panel, "week");
        }
        return "*";
    }

    function yearValue($panel) {
        var mode = selectedMode($panel, "year");
        if (mode === "every") {
            return "*";
        }
        if (mode === "range") {
            return numberText($panel, "yearStart_0", "2016") + "-" + numberText($panel, "yearEnd_0", "2017");
        }
        return "";
    }

    function readPanelCron($panel) {
        var fields = {
            second: genericValue($panel, "second"),
            min: genericValue($panel, "min"),
            hour: genericValue($panel, "hour"),
            day: dayValue($panel),
            month: genericValue($panel, "month"),
            week: weekValue($panel),
            year: yearValue($panel)
        };
        applyLowerDefaults(fields, activeTabKey($panel));
        return buildCron(fields);
    }

    function updateCheckboxState($panel) {
        $.each(["second", "min", "hour", "day", "month", "week"], function (_, field) {
            var specific = selectedMode($panel, field) === "specific";
            $panel.find("." + field + "List input").prop("disabled", !specific);
            if (specific && !$panel.find("." + field + "List input:checked").length) {
                $panel.find("." + field + "List input").first().prop("checked", true);
            }
            if (!specific) {
                $panel.find("." + field + "List input").prop("checked", false);
            }
        });
    }

    function refreshRunTime($panel, expression) {
        var $runTime = $panel.find("#runTime");
        if (!trimValue(expression) || typeof base_url === "undefined") {
            $runTime.val("");
            return;
        }
        $.ajax({
            type: "GET",
            url: base_url + "/jobinfo/nextTriggerTime",
            data: {
                scheduleType: "CRON",
                scheduleConf: expression
            },
            dataType: "json",
            success: function (data) {
                if (data.code === 200 && data.content) {
                    $runTime.val(data.content.join("\n"));
                } else {
                    $runTime.val(data.msg || "");
                }
            }
        });
    }

    function updateValue($panel, $source, $display) {
        updateCheckboxState($panel);
        var expression = readPanelCron($panel);
        $source.val(expression);
        $display.val(expression);
        refreshRunTime($panel, expression);
    }

    function bindPanel($panel, $source, $display) {
        $panel.off(".cronGen");
        applyCronToPanel($panel, $source.val());
        refreshRunTime($panel, $source.val());
        $panel.on("click.cronGen", "#CronGenTabs a", function (e) {
            e.preventDefault();
            var target = $(this).attr("href");
            $panel.find("#CronGenTabs li").removeClass("active");
            $(this).parent().addClass("active");
            $panel.find(".tab-pane").removeClass("active");
            $panel.find(target).addClass("active");
        });
        $panel.on("focus.cronGen", ".line input[type='text']", function () {
            $(this).closest(".line").find("input[type='radio']").prop("checked", true);
            updateCheckboxState($panel);
        });
        $panel.on("input.cronGen change.cronGen", "input[type='text'], input[type='checkbox'], input[type='radio']", function () {
            updateValue($panel, $source, $display);
        });
    }

    function positionCronPopover($button) {
        var popover = $button.data("bs.popover");
        var $popover = popover && popover.tip ? popover.tip() : $();
        var $modalBody = $button.closest(".modal-body");
        var $offsetParent = $popover.offsetParent();
        var parentOffset = $offsetParent.length && !$offsetParent.is("body") && !$offsetParent.is("html")
            ? $offsetParent.offset()
            : { left: 0, top: 0 };
        var boundaryLeft = 12;
        var boundaryRight = $(window).width() - 12;
        var boundaryTop = 12;
        if ($modalBody.length) {
            var modalOffset = $modalBody.offset();
            boundaryLeft = modalOffset.left + 12;
            boundaryRight = modalOffset.left + $modalBody.outerWidth() - 24;
            boundaryTop = modalOffset.top + 12;
        }
        var popoverWidth = Math.min(420, Math.max(320, boundaryRight - boundaryLeft));
        var buttonOffset = $button.offset();
        var left = Math.min(buttonOffset.left + $button.outerWidth() - popoverWidth, boundaryRight - popoverWidth);
        left = Math.max(boundaryLeft, left);
        var top = buttonOffset.top + $button.outerHeight() + 10;
        if ($modalBody.length) {
            top = Math.max(boundaryTop, Math.min(top, boundaryTop + $modalBody.outerHeight() - 80));
        }
        $popover.css({
            width: popoverWidth,
            maxWidth: popoverWidth,
            left: left - parentOffset.left,
            top: top - parentOffset.top
        });
        $popover.find(".arrow").css({
            left: Math.max(24, Math.min(popoverWidth - 24, buttonOffset.left + ($button.outerWidth() / 2) - left))
        });
    }

    function getPopoverContainer($source) {
        var $modal = $source.closest(".modal");
        return $modal.length ? $modal : "body";
    }

    function install(labels) {
        $.fn.cronGen = function (options) {
            options = $.extend({}, $.fn.cronGen.defaultOptions, options || {});
            $(".cron-gen-trigger").popover("hide");
            return this.each(function () {
                var $source = $(this);
                $source.prev(".cron-gen-input-group").remove();
                $source.show().hide();

                var $group = $("<div/>").addClass("input-group cron-gen-input-group");
                var $display = $("<input/>", {
                    type: "text",
                    placeholder: labels.placeholder,
                    name: "cronGen_display"
                }).addClass("form-control").val($source.val());
                var $button = $("<button/>", {
                    type: "button",
                    "class": "btn btn-default cron-gen-trigger"
                }).append($("<i/>").addClass("fa fa-edit"));

                $display.on("input change", function () {
                    $source.val(trimValue($display.val()));
                });
                $group.append($display);
                $group.append($("<span/>").addClass("input-group-btn").append($button));
                $source.before($group);

                $button.popover({
                    html: true,
                    trigger: "manual",
                    container: getPopoverContainer($source),
                    content: function () {
                        return buildPanel(labels);
                    },
                    template: "<div class=\"popover cron-gen-popover\"><div class=\"arrow\"></div><div class=\"popover-inner\"><h3 class=\"popover-title\"></h3><div class=\"popover-content\"><p></p></div></div></div>",
                    sanitize: false,
                    placement: options.direction
                }).on("shown.bs.popover", function () {
                    var popover = $button.data("bs.popover");
                    var $tip = popover && popover.tip ? popover.tip() : $();
                    var $panel = $tip.find("#CronGenMainDiv");
                    $tip.off("click.cronGen").on("click.cronGen", function (e) {
                        e.stopPropagation();
                    });
                    bindPanel($panel, $source, $display);
                    if (typeof fitCronPopovers === "function") {
                        fitCronPopovers();
                    } else {
                        positionCronPopover($button);
                    }
                }).on("click", function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    var popover = $button.data("bs.popover");
                    var $tip = popover && popover.tip ? popover.tip() : $();
                    var visible = $tip.hasClass("in");
                    $(".cron-gen-trigger").not($button).popover("hide");
                    if (visible) {
                        $button.popover("hide");
                    } else {
                        $button.popover("show");
                    }
                });
            });
        };
        $.fn.cronGen.defaultOptions = {
            direction: "bottom"
        };

        $(document).off("click.cronGen").on("click.cronGen", function (e) {
            if (!$(e.target).closest(".cron-gen-popover, .cron-gen-trigger").length) {
                $(".cron-gen-trigger").popover("hide");
            }
        });
    }

    window.NBatchCronGen = {
        install: install
    };
})(window, jQuery);
