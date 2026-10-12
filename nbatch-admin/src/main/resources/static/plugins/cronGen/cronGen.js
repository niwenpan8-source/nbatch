(function (window) {
    window.NBatchCronGen.install({
        placeholder: "cron表达式...",
        runTime: "最近运行时间: ",
        rangePrefix: "周期 从",
        fromPrefix: "从",
        specific: "指定",
        none: "不指定",
        tabs: {
            second: "秒",
            min: "分钟",
            hour: "小时",
            day: "日",
            month: "月",
            week: "周",
            year: "年"
        },
        second: {
            every: "每秒 允许的通配符[, - * /]",
            unit: "秒",
            intervalMiddle: "秒开始,每",
            intervalSuffix: "秒执行一次"
        },
        min: {
            every: "每分钟 允许的通配符[, - * /]",
            unit: "分钟",
            intervalMiddle: "分钟开始,每",
            intervalSuffix: "分钟执行一次"
        },
        hour: {
            every: "每小时 允许的通配符[, - * /]",
            unit: "小时",
            intervalMiddle: "小时开始,每",
            intervalSuffix: "小时执行一次"
        },
        day: {
            every: "每天 允许的通配符[, - * / L W]",
            unit: "日",
            intervalMiddle: "日开始,每",
            intervalSuffix: "天执行一次",
            workdayPrefix: "每月",
            workdaySuffix: "号最近的那个工作日",
            last: "本月最后一天"
        },
        month: {
            every: "每月 允许的通配符[, - * /]",
            unit: "月",
            intervalMiddle: "月开始,每",
            intervalSuffix: "月执行一次"
        },
        week: {
            every: "每周 允许的通配符[, - * / L #]",
            rangePrefix: "周期 从星期",
            intervalPrefix: "第",
            intervalMiddle: "周的星期",
            lastPrefix: "本月最后一个星期"
        },
        year: {
            none: "不指定 允许的通配符[, - * /] 非必填",
            every: "每年",
            rangePrefix: "周期从",
            unit: "年"
        }
    });
})(window);
