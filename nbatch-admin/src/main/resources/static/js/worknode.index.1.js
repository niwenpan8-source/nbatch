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

    function openNodeForm(url, title) {
        layer.open({
            type: 2,
            area: getLayerArea(980, 720),
            title: title,
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
        "columns": [
            {
                "data": 'nodeName',
                "bSortable": false,
                "visible": true,
                "width": '160px',
                "render": function (data) { return textOrDash(data); }
            },
            {"data": 'nodeId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'workName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeTypeName', "width": '140px', "render": function (data) { return textOrDash(data); }},
            {"data": 'dbType', "width": '100px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeStatus', "width": '80px', "render": function (data, type, row) { return statusLabel(row.nodeStatus, row.nodeStatus === 1 ? I18n.jobinfo_opt_start : I18n.jobinfo_opt_stop); }},
            {"data": 'nodeDesc', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'runNodeId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'runWorkId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'nodeRunStatusName', "width": '90px', "render": function (data, type, row) { return row.runNodeId ? statusLabel(row.nodeRunStatus, data) : '-'; }},
            {"data": 'turnDateText', "width": '110px', "render": function (data) { return textOrDash(data); }},
            {"data": 'retryTimes', "width": '80px', "render": function (data) { return escapeHtml(data == null ? '-' : data); }},
            {"data": 'startTimeText', "width": '160px', "render": function (data) { return textOrDash(data); }},
            {"data": 'endTimeText', "width": '160px', "render": function (data) { return textOrDash(data); }},
            {
                "data": I18n.system_opt,
                "width": '190px',
                "className": 'table-action-cell',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.nodeId] = row;
                        // A single log entry opens a tabbed page that contains execution and detail logs.
                        var logButtons = row.runNodeId
                            ? '<button type="button" class="btn btn-default btn-xs view-log">日志</button>'
                            : '';
                        return '<div data-node-id="' + escapeHtml(row.nodeId) + '">' +
                            '<button type="button" class="btn btn-primary btn-xs update">' + I18n.system_opt_edit + '</button>' +
                            '<button type="button" class="btn btn-danger btn-xs delete">' + I18n.system_opt_del + '</button>' +
                            logButtons +
                            '</div>';
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
            area: getLayerArea(1180, 720),
            title: '运行节点日志',
            shade: 0.6,
            shadeClose: true,
            maxmin: true,
            anim: 0,
            content: base_url + "/node/viewLogModel?workNodeId=" + id
        });
    });

});
