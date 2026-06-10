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
        var parsed = moment(value, [moment.ISO_8601, 'YYYY-MM-DD HH:mm:ss', 'YYYY-MM-DD'], true);
        if (parsed.isValid()) {
            return parsed.format('YYYY-MM-DD HH:mm:ss');
        }
        return escapeHtml(String(value).replace('T', ' ').substring(0, 19));
    }

    function runInfoDetail(row) {
        return '<div class="work-node-run-detail">' +
            '<div class="work-node-run-item"><span class="work-node-run-label">运行节点ID</span><span class="node-info-id">' + textOrDash(row.runNodeId) + '</span></div>' +
            '<div class="work-node-run-item"><span class="work-node-run-label">运行作业ID</span><span class="node-info-id">' + textOrDash(row.runWorkId) + '</span></div>' +
            '</div>';
    }

    function openNodeForm(url, title) {
        layer.open({
            type: 2,
            area: getModalArea(980, 720),
            title: title,
            closeBtn: 1,
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: [I18n.system_ok, I18n.system_cancel],
            yes: function(index, layero) {
                var iframeWin = layero.find('iframe')[0].contentWindow;
                var form = iframeWin.document.querySelector('form');
                if (iframeWin.beforeSubmit && !iframeWin.beforeSubmit()) {
                    return;
                }
                if (form) {
                    var formData = new FormData(form);
                    $.ajax({
                        url: form.getAttribute('action'),
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg(I18n.system_success, {icon: 1});
                                layer.close(index);
                                workNodeTable.fnDraw();
                            } else {
                                layer.msg(response.msg || I18n.system_fail, {icon: 2});
                            }
                        },
                        error: function() {
                            layer.msg(I18n.system_fail, {icon: 2});
                        }
                    });
                }
            },
            btn2: function(index) {
                layer.close(index);
            }
        });
    }

    // init date tables
    var workNodeTable = $("#work_node_list").dataTable({
        "deferRender": true,
        "processing": true,
        "serverSide": true,
        "ajax": {
            url: base_url + "/node/pageList",
            type: "post",
            data: function (d) {
                var obj = {};
                obj.nodeType = $('#nodeType').val();
                obj.workId = $('#workId').val();
                obj.start = d.start;
                obj.length = d.length;
                return obj;
            }
        },
        "searching": false,
        "ordering": false,
        "autoWidth": false,
        "columns": [
            {
                "data": null,
                "width": '44px',
                "className": 'details-control',
                "render": function () {
                    return '<button type="button" class="btn btn-default btn-xs work-node-detail-toggle" title="查看运行ID" aria-expanded="false"><i class="fa fa-plus"></i></button>';
                }
            },
            {
                "data": 'nodeName',
                "bSortable": false,
                "visible": true,
                "width": '180px',
                "render": function (data) { return '<span class="node-main-text">' + textOrDash(data) + '</span>'; }
            },
            {"data": 'nodeId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'workName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeTypeName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'dbType', "width": '100px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeStatus', "width": '80px', "render": function (data, type, row) { return statusLabel(row.nodeStatus, row.nodeStatus === 1 ? I18n.jobinfo_opt_start : I18n.jobinfo_opt_stop); }},
            {"data": 'nodeRunStatusName', "width": '90px', "render": function (data, type, row) { return row.runNodeId ? statusLabel(row.nodeRunStatus, data) : '-'; }},
            {"data": 'turnDateText', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {"data": 'retryTimes', "width": '80px', "render": function (data) { return escapeHtml(data == null ? '-' : data); }},
            {"data": 'startTimeText', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {"data": 'endTimeText', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {
                "data": I18n.system_opt,
                "width": '120px',
                "className": 'table-action-cell',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.nodeId] = row;
                        // A single log entry opens a tabbed page that contains execution and detail logs.
                        var logButtons = row.runNodeId
                            ? '<li><a href="javascript:void(0);" class="view-log">日志</a></li>\n'
                            : '';
                        var rerunButton = row.runNodeId
                            ? '<li><a href="javascript:void(0);" class="rerun-node">重跑</a></li>\n'
                            : '';
                        return '<div class="btn-group">\n' +
                            '     <button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>\n' +
                            '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                            '       <span class="caret"></span>\n' +
                            '       <span class="sr-only">Toggle Dropdown</span>\n' +
                            '     </button>\n' +
                            '     <ul class="dropdown-menu" role="menu" data-node-id="' + escapeHtml(row.nodeId) + '">\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            rerunButton +
                            logButtons +
                            '     </ul>\n' +
                            '   </div>';
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
        workNodeTable.fnDraw();
    });

    $('#work_node_list tbody').on('click', '.work-node-detail-toggle', function () {
        var button = $(this);
        var tableRow = button.closest('tr')[0];
        var icon = button.find('.fa');

        if (workNodeTable.fnIsOpen(tableRow)) {
            workNodeTable.fnClose(tableRow);
            $(tableRow).removeClass('shown');
            button.attr('aria-expanded', 'false');
            icon.removeClass('fa-minus').addClass('fa-plus');
            return;
        }

        workNodeTable.fnOpen(tableRow, runInfoDetail(workNodeTable.fnGetData(tableRow)), 'run-info-row');
        $(tableRow).addClass('shown');
        button.attr('aria-expanded', 'true');
        icon.removeClass('fa-plus').addClass('fa-minus');
    });

    // job operate
    $("#work_node_list").on('click', '.delete', function () {
        var url = base_url + "/node/delete";
        var id = $(this).closest('ul').attr("data-node-id");
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
                data: {"id": id},
                dataType: "json",
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg(typeName + I18n.system_success);
                        workNodeTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || typeName + I18n.system_fail);
                    }
                }
            });
        });
    });

    // add
    $(".add").click(function () {
        openNodeForm(base_url + "/node/addModel", I18n.jobinfo_field_add);
    });

    // update
    $("#work_node_list").on('click', '.update',function() {
        var id = $(this).closest('ul').attr("data-node-id");
        openNodeForm(base_url + "/node/updateModel?workNodeId=" + id, I18n.jobinfo_field_update);
    });

    // view run node log
    $("#work_node_list").on('click', '.view-log',function() {
        var id = $(this).closest('ul').attr("data-node-id");
        layer.open({
            type: 2,
            area: getModalArea(1180, 720),
            title: '运行节点日志',
            closeBtn: 1,
            shade: 0.6,
            shadeClose: true,
            maxmin: true,
            anim: 0,
            content: base_url + "/node/viewLogModel?workNodeId=" + id
        });
    });

    // rerun node
    $("#work_node_list").on('click', '.rerun-node', function () {
        var id = $(this).closest('ul').attr("data-node-id");
        var row = tableData['key' + id];
        if (!row || !row.runNodeId) {
            layer.msg('当前节点没有可重跑的运行记录', {icon: 2});
            return;
        }
        layer.confirm('确认重跑节点【' + escapeHtml(row.nodeName) + '】？若为依赖节点，其后所有节点也将一起重跑。', {
            icon: 3,
            title: I18n.system_tips,
            btn: [I18n.system_ok, I18n.system_cancel]
        }, function (index) {
            layer.close(index);
            $.ajax({
                type: 'POST',
                url: base_url + '/node/rerun',
                data: {runNodeId: row.runNodeId},
                dataType: 'json',
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg('重跑成功', {icon: 1});
                        workNodeTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || '重跑失败', {icon: 2});
                    }
                },
                error: function () {
                    layer.msg('重跑失败', {icon: 2});
                }
            });
        });
    });

});
