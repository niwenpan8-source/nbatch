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

    function detailItem(label, value, extraClass) {
        return '<div class="work-node-run-item"><span class="work-node-run-label">' + escapeHtml(label) + '</span>' +
            '<span class="' + (extraClass || '') + '">' + value + '</span></div>';
    }

    function renderNodeDetail(data) {
        var runRows = (data.runList || []).map(function (item, index) {
            return '<tr>' +
                '<td>' + (index + 1) + '</td>' +
                '<td class="node-info-id">' + textOrDash(item.runNodeId) + '</td>' +
                '<td class="node-info-id">' + textOrDash(item.runWorkId) + '</td>' +
                '<td>' + dateOrDash(item.turnDate) + '</td>' +
                '<td>' + textOrDash(item.nodeRunStatusName) + '</td>' +
                '<td>' + escapeHtml(item.retryTimes == null ? '-' : item.retryTimes) + '</td>' +
                '<td>' + dateTimeOrDash(item.createTime) + '</td>' +
                '<td>' + dateTimeOrDash(item.startTime) + '</td>' +
                '<td>' + dateTimeOrDash(item.endTime) + '</td>' +
                '</tr>';
        }).join('');
        if (!runRows) {
            runRows = '<tr><td colspan="9" class="text-center">暂无运行记录</td></tr>';
        }
        return '<div class="work-node-run-detail">' +
            detailItem('节点ID', textOrDash(data.nodeId), 'node-info-id') +
            detailItem('节点名称', textOrDash(data.nodeName)) +
            detailItem('所属作业', textOrDash(data.workName)) +
            detailItem('类型', textOrDash(data.nodeTypeName)) +
            detailItem('数据库', textOrDash(data.dbType)) +
            detailItem('状态', textOrDash(data.nodeStatusName)) +
            detailItem('运行次数', escapeHtml(data.runCount == null ? 0 : data.runCount)) +
            detailItem('默认重试', escapeHtml(data.retryTimes == null ? '-' : data.retryTimes)) +
            detailItem('执行器', textOrDash(data.executeHandler)) +
            detailItem('脚本类型', textOrDash(data.scriptType)) +
            detailItem('失败策略', textOrDash(data.errorStrategy)) +
            detailItem('节点描述', textOrDash(data.nodeDesc)) +
            detailItem('执行参数', textOrDash(data.executeContentParam)) +
            detailItem('执行内容', textOrDash(data.executeContent)) +
            '</div>' +
            '<div class="detail-section-title">运行历史</div>' +
            '<table class="table table-bordered table-striped detail-history-table"><thead><tr>' +
            '<th>#</th><th>运行节点ID</th><th>运行作业ID</th><th>翻牌日期</th><th>状态</th><th>剩余重试</th><th>创建时间</th><th>开始时间</th><th>结束时间</th>' +
            '</tr></thead><tbody>' + runRows + '</tbody></table>';
    }

    function openNodeDetail(nodeId) {
        $.ajax({
            type: 'POST',
            url: base_url + '/node/detail',
            data: {nodeId: nodeId},
            dataType: 'json',
            success: function (result) {
                if (result.code !== 200) {
                    layer.msg(result.msg || '获取详情失败', {icon: 2});
                    return;
                }
                layer.open({
                    type: 1,
                    title: '作业节点详情',
                    area: getModalArea(1080, 720),
                    content: '<div class="work-node-detail-modal">' + renderNodeDetail(result.content || {}) + '</div>'
                });
            },
            error: function () {
                layer.msg('获取详情失败', {icon: 2});
            }
        });
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
                obj.nodeName = $('#nodeName').val();
                obj.nodeType = $('#nodeType').val();
                obj.workId = $('#workId').val();
                obj.nodeRunStatus = $('#nodeRunStatus').val();
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
                "data": 'nodeName',
                "bSortable": false,
                "visible": true,
                "width": '180px',
                "render": function (data) { return '<a href="javascript:void(0);" class="work-node-detail-toggle node-main-text" aria-expanded="false">' + textOrDash(data) + '</a>'; }
            },
            {"data": 'workName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeTypeName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'dbType', "width": '100px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeStatus', "width": '80px', "render": function (data, type, row) { return statusLabel(row.nodeStatus, row.nodeStatus === 1 ? I18n.jobinfo_opt_start : I18n.jobinfo_opt_stop); }},
            {"data": 'nodeRunStatusName', "width": '90px', "render": function (data, type, row) { return row.runNodeId ? statusLabel(row.nodeRunStatus, data) : '-'; }},
            {"data": 'turnDateText', "width": '120px', "render": function (data) { return dateOrDash(data); }},
            {
                "data": I18n.system_opt,
                "width": '120px',
                "className": 'table-action-cell',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.nodeId] = row;
                        // A single log entry opens a tabbed page that contains execution and detail logs.
                        var logButtons = row.runNodeId
                            ? '<li><a href="javascript:void(0);" class="view-log">日志</a></li>'
                            : '';
                        var rerunButton = row.runNodeId
                            ? '<li><a href="javascript:void(0);" class="rerun-node">重跑</a></li>'
                            : '';
                        var rerunInitButton = row.runNodeId
                            ? '<li><a href="javascript:void(0);" class="rerun-node-init-turn">初始化重跑</a></li>'
                            : '';
                        return '<div class="btn-group" data-node-id="' + escapeHtml(row.nodeId) + '">' +
                            '<button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>' +
                            '<button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">' +
                            '<span class="caret"></span><span class="sr-only">Toggle Dropdown</span>' +
                            '</button>' +
                            '<ul class="dropdown-menu" role="menu">' +
                            '<li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>' +
                            '<li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>' +
                            rerunButton +
                            rerunInitButton +
                            logButtons +
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
        workNodeTable.fnDraw();
    });

    $('#work_node_list tbody').on('click', '.work-node-detail-toggle', function () {
        var row = workNodeTable.fnGetData($(this).closest('tr')[0]);
        if (row && row.nodeId) {
            openNodeDetail(row.nodeId);
        }
    });

    // job operate
    $("#work_node_list").on('click', '.delete', function () {
        var url = base_url + "/node/delete";
        var id = $(this).closest('[data-node-id]').attr("data-node-id");
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
        var id = $(this).closest('[data-node-id]').attr("data-node-id");
        openNodeForm(base_url + "/node/updateModel?workNodeId=" + id, I18n.jobinfo_field_update);
    });

    // view run node log
    $("#work_node_list").on('click', '.view-log',function() {
        var id = $(this).closest('[data-node-id]').attr("data-node-id");
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
        var id = $(this).closest('[data-node-id]').attr("data-node-id");
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

    // rerun node from init turn date
    $("#work_node_list").on('click', '.rerun-node-init-turn', function () {
        var id = $(this).closest('[data-node-id]').attr("data-node-id");
        var row = tableData['key' + id];
        if (!row || !row.runNodeId) {
            layer.msg('当前节点没有可重跑的运行记录', {icon: 2});
            return;
        }
        layer.confirm('确认按初始化翻牌日期重跑节点【' + escapeHtml(row.nodeName) + '】？该节点及其后继节点将复位，之后的运行记录会被删除。', {
            icon: 3,
            title: I18n.system_tips,
            btn: [I18n.system_ok, I18n.system_cancel]
        }, function (index) {
            layer.close(index);
            $.ajax({
                type: 'POST',
                url: base_url + '/node/rerunFromInitTurnDate',
                data: {runNodeId: row.runNodeId},
                dataType: 'json',
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg('初始化重跑成功', {icon: 1});
                        workNodeTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || '初始化重跑失败', {icon: 2});
                    }
                },
                error: function () {
                    layer.msg('初始化重跑失败', {icon: 2});
                }
            });
        });
    });

});
