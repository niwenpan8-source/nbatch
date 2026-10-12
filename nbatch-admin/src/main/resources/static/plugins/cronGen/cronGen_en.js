(function (window) {
    window.NBatchCronGen.install({
        placeholder: "cron expression...",
        runTime: "Recent Run Time: ",
        rangePrefix: "Cycle from",
        fromPrefix: "From",
        specific: "Specify",
        none: "Not specified",
        tabs: {
            second: "Second",
            min: "Minute",
            hour: "Hour",
            day: "Day",
            month: "Month",
            week: "Week",
            year: "Year"
        },
        second: {
            every: "Per second, allowed wildcard[, - * /]",
            unit: "second",
            intervalMiddle: "second start, per",
            intervalSuffix: "second execute once"
        },
        min: {
            every: "Per minute, allowed wildcard[, - * /]",
            unit: "minute",
            intervalMiddle: "minute start, per",
            intervalSuffix: "minute execute once"
        },
        hour: {
            every: "Per hour, allowed wildcard[, - * /]",
            unit: "hour",
            intervalMiddle: "hour start, per",
            intervalSuffix: "hour execute once"
        },
        day: {
            every: "Per day, allowed wildcard[, - * / L W]",
            unit: "day",
            intervalMiddle: "day start, per",
            intervalSuffix: "day execute once",
            workdayPrefix: "Nearest weekday of day",
            workdaySuffix: "in month",
            last: "Last day of month"
        },
        month: {
            every: "Per month, allowed wildcard[, - * /]",
            unit: "month",
            intervalMiddle: "month start, per",
            intervalSuffix: "month execute once"
        },
        week: {
            every: "Per week, allowed wildcard[, - * / L #]",
            rangePrefix: "Cycle from weekday",
            intervalPrefix: "The",
            intervalMiddle: "week's weekday",
            lastPrefix: "Last weekday of month"
        },
        year: {
            none: "Not specified, optional wildcard[, - * /]",
            every: "Per year",
            rangePrefix: "Cycle from",
            unit: "year"
        }
    });
})(window);
