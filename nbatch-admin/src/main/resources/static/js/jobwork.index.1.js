$(function () {
    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function statusLabel(status, text) {
        var labelClass = 'label-default';
        if (status === 1) {
            labelClass = 'label-success';
        } else if (status === 2) {
            labelClass = 'label-info';
        } else if (status === 3) {
            labelClass = 'label-danger';
        } else if (status === 4) {
            labelClass = 'label-warning';
        } else if (status === 5) {
            labelClass = 'label-primary';
        } else if (status === 6) {
            labelClass = 'label-default';
        } else if (status === 7) {
            labelClass = 'label-warning';
        }
        return '<small class="label ' + labelClass + '">' + escapeHtml(text || '-') + '</small>';
    }

    // Keep table cells readable after splitting composite info blocks into standalone columns.
    function textOrDash(value) {
        return escapeHtml(value || '-');
    }

    function dateTimeOrDash(value) {
        if (!value) {
            return '-';
        }
        if (typeof value === 'number' || /^\d{13}$/.test(String(value))) {
            return moment(Number(value)).format('YYYY-MM-DD HH:mm:ss');
        }
        var parsed = moment(value, [moment.ISO_8601, 'YYYY-MM-DD HH:mm:ss', 'YYYY-MM-DD'], true);
        if (parsed.isValid()) {
            return parsed.format('YYYY-MM-DD HH:mm:ss');
        }
        return escapeHtml(String(value).replace('T', ' ').substring(0, 19));
    }

    function dateOrDash(value) {
        if (!value) {
            return '-';
        }
        var parsed = moment(value, [moment.ISO_8601, 'YYYY-MM-DD', 'YYYY-MM-DD HH:mm:ss'], true);
        if (parsed.isValid()) {
            return parsed.format('YYYY-MM-DD');
        }
        return escapeHtml(String(value).substring(0, 10));
    }

    function numberOrNull(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        var parsed = Number(value);
        return isNaN(parsed) ? null : parsed;
    }

    function firstValue(row, names) {
        for (var i = 0; i < names.length; i++) {
            if (row[names[i]] !== undefined && row[names[i]] !== null) {
                return row[names[i]];
            }
        }
        return null;
    }

    function normalizeWorkRow(row) {
        row = row || {};
        var workId = firstValue(row, ['workId', 'work_id', 'id']);
        return $.extend({}, row, {
            workId: workId,
            workName: firstValue(row, ['workName', 'work_name', 'name']),
            workType: numberOrNull(firstValue(row, ['workType', 'work_type'])),
            workTypeName: firstValue(row, ['workTypeName', 'work_type_name', 'typeName']),
            jobGroup: firstValue(row, ['jobGroup', 'job_group']),
            jobGroupName: firstValue(row, ['jobGroupName', 'job_group_name', 'executorName']),
            turnDate: firstValue(row, ['turnDate', 'turn_date']),
            turnDateText: firstValue(row, ['turnDateText', 'turn_date_text', 'turnDate', 'turn_date']),
            workId: firstValue(row, ['workId', 'work_id', 'workId', 'work_id', 'id']) || workId,
            runWorkStatus: numberOrNull(firstValue(row, ['runWorkStatus', 'run_work_status'])),
            runWorkStatusName: firstValue(row, ['runWorkStatusName', 'run_work_status_name']),
            runWorkCreateTime: firstValue(row, ['runWorkCreateTime', 'run_work_create_time', 'runCreateTime', 'run_create_time', 'createTime', 'create_time']),
            createTime: firstValue(row, ['createTime', 'create_time', 'runCreateTime', 'run_create_time', 'runWorkCreateTime', 'run_work_create_time']),
            scheduleType: firstValue(row, ['scheduleType', 'schedule_type']),
            scheduleConf: firstValue(row, ['scheduleConf', 'schedule_conf', 'cronExpression', 'cron_expression']),
            cronExpression: firstValue(row, ['cronExpression', 'cron_expression', 'scheduleConf', 'schedule_conf']),
            scheduleStatus: numberOrNull(firstValue(row, ['scheduleStatus', 'schedule_status'])),
            triggerNextTime: firstValue(row, ['triggerNextTime', 'trigger_next_time'])
        });
    }

    function scheduleStatusLabel(row) {
        if (!row.scheduleConf && !row.cronExpression) {
            return '<small class="label label-default">未配置</small>';
        }
        return row.scheduleStatus === 1
            ? '<small class="label label-success">已启动</small>'
            : '<small class="label label-warning">已停止</small>';
    }

    // init date tables
    var jobWorkTable = $("#job_work_list").dataTable({
        "deferRender": true,
        "processing": true,
        "serverSide": true,
        "ajax": {
            url: base_url + "/work/pageList",
            type: "post",
            data: function (d) {
                var obj = {};
                obj.workName = $('#workName').val();
                obj.start = d.start;
                obj.length = d.length;
                return obj;
            },
            // Keep the table usable when an older server returns a wrapped or empty payload.
            dataSrc: function (json) {
                if (!json || typeof json !== 'object') {
                    layer.msg('作业数据格式错误', {icon: 2});
                    return [];
                }
                if (Array.isArray(json.data)) {
                    return json.data.map(normalizeWorkRow);
                }
                if (json.content && Array.isArray(json.content.data)) {
                    json.recordsTotal = json.recordsTotal == null ? json.content.recordsTotal : json.recordsTotal;
                    json.recordsFiltered = json.recordsFiltered == null ? json.content.recordsFiltered : json.recordsFiltered;
                    return json.content.data.map(normalizeWorkRow);
                }
                if (Array.isArray(json.rows)) {
                    json.recordsTotal = json.recordsTotal == null ? (json.total == null ? json.rows.length : json.total) : json.recordsTotal;
                    json.recordsFiltered = json.recordsFiltered == null ? json.recordsTotal : json.recordsFiltered;
                    return json.rows.map(normalizeWorkRow);
                }
                layer.msg(json.msg || '作业数据加载失败', {icon: 2});
                return [];
            },
            error: function () {
                layer.msg('作业数据加载失败', {icon: 2});
            }
        },
        "searching": false,
        "ordering": false,
        "autoWidth": false,
        "columns": [
            {
                "data": 'workName',
                "bSortable": false,
                "visible": true,
                "width": '190px',
                "render": function (data) { return textOrDash(data); }
            },
            {"data": 'workTypeName', "width": '110px', "render": function (data) { return textOrDash(data); }},
            {"data": 'runWorkStatusName', "width": '90px', "render": function (data, type, row) { return row.workId ? statusLabel(row.runWorkStatus, data) : '-'; }},
            {"data": 'turnDate', "width": '120px', "render": function (data) { return dateOrDash(data); }},
            {"data": 'runWorkCreateTime', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {"data": 'jobGroupName', "width": '130px', "render": function (data, type, row) { return textOrDash(data || row.jobGroup); }},
            {"data": 'cronExpression', "width": '150px', "render": function (data, type, row) { return '<code>' + textOrDash(data || row.scheduleConf) + '</code>'; }},
            {"data": 'scheduleStatus', "width": '90px', "render": function (data, type, row) { return scheduleStatusLabel(row); }},
            {"data": 'triggerNextTime', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {
                "data": I18n.system_opt,
                "width": '120px',
                "className": 'table-action-cell',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.workId] = row;
                        return '<div class="btn-group" data-work-id="' + escapeHtml(row.workId) + '">' +
                            '<button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>' +
                            '<button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">' +
                            '<span class="caret"></span><span class="sr-only">Toggle Dropdown</span>' +
                            '</button>' +
                            '<ul class="dropdown-menu" role="menu">' +
                            '<li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>' +
                            '<li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>' +
                            '<li><a href="javascript:void(0);" class="edit">作业管理</a></li>' +
                            '</ul></div>';
                    };
                }
            }
        ],
        "language": dataTableI18n
    });

    // table data
    var tableData = {};

    // search btn
    $('#searchBtn').on('click', function () {
        jobWorkTable.fnDraw();
    });

    // job operate
    $("#job_work_list").on('click', '.delete', function () {
        var url = base_url + "/work/delete";
        var id = $(this).closest('[data-work-id]').attr("data-work-id");
        var typeName = I18n.system_opt_del;

        layer.confirm(I18n.system_ok + typeName + '?', {
            icon: 3,
            title: I18n.system_tips,
            btn: [I18n.system_ok, I18n.system_cancel]
        }, function (index) {
            layer.close(index);

            $.ajax({
                type: 'POST',
                url: url,
                data: {
                    "id": id
                },
                dataType: "json",
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg(typeName + I18n.system_success);
                        jobWorkTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || typeName + I18n.system_fail);
                    }
                }
            });
        });
    });

    // add
    $(".add").click(function () {
        var url = base_url + "/work/addModel";
        layer.open({
            type: 2,
            area: getModalArea(980, 680),
            title: I18n.jobinfo_field_add,
            closeBtn: 1,
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: [I18n.system_ok, I18n.system_cancel],
            yes: function(index, layero) {
                var iframeWin = layero.find('iframe')[0].contentWindow;
                var form = iframeWin.document.getElementById('addModel');

                if (form) {
                    var formData = new FormData(form);

                    $.ajax({
                        url: base_url + '/work/insert',
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg(I18n.system_add_suc, {icon: 1});
                                layer.close(index);
                                jobWorkTable.fnDraw();
                            } else {
                                layer.msg(response.msg || I18n.system_add_fail, {icon: 2});
                            }
                        },
                        error: function() {
                            layer.msg(I18n.system_fail, {icon: 2});
                        }
                    });
                } else {
                    layer.msg(I18n.system_fail, {icon: 2});
                }
            },
            btn2: function(index) {
                layer.close(index);
            }
        });
    });

    // update
    $("#job_work_list").on('click', '.update',function() {
        var id = $(this).closest('[data-work-id]').attr("data-work-id");
        var url = base_url + "/work/updateModel?workId=" + id;
        layer.open({
            type: 2,
            area: getModalArea(980, 680),
            title: I18n.jobinfo_field_update,
            closeBtn: 1,
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: [I18n.system_ok, I18n.system_cancel],
            yes: function(index, layero) {
                var iframeWin = layero.find('iframe')[0].contentWindow;
                var form = iframeWin.document.getElementById('updateModel');

                if (form) {
                    var formData = new FormData(form);

                    $.ajax({
                        url: base_url + '/work/update',
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg(I18n.system_update_suc, {icon: 1});
                                layer.close(index);
                                jobWorkTable.fnDraw();
                            } else {
                                layer.msg(response.msg || I18n.system_update_fail, {icon: 2});
                            }
                        },
                        error: function() {
                            layer.msg(I18n.system_fail, {icon: 2});
                        }
                    });
                } else {
                    layer.msg(I18n.system_fail, {icon: 2});
                }
            },
            btn2: function(index) {
                layer.close(index);
            }
        });
    });

    // relation edit
    $("#job_work_list").on('click', '.edit',function() {
        var id = $(this).closest('[data-work-id]').attr("data-work-id");
        var url = base_url + "/work/editModel?workId=" + id;
        window.open(url, 'nbatch_work_flow_' + id,
            'width=1440,height=900,top=60,left=120,resizable=yes,scrollbars=yes');
    });


});
