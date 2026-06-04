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

    function openNodeForm(url, title) {
        layer.open({
            type: 2,
            area: ['900px', '720px'],
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
                "width": '48%',
                "render": function (data, type, row) {
                    return '<div class="node-info-title">' + escapeHtml(row.nodeName) + '</div>' +
                        '<div class="node-info-line"><span>节点ID：<span class="node-info-id">' + escapeHtml(row.nodeId) + '</span></span><span>作业：' + escapeHtml(row.workName || '-') + '</span></div>' +
                        '<div class="node-info-line"><span>类型：' + escapeHtml(row.nodeTypeName || '-') + '</span><span>数据库：' + escapeHtml(row.dbType || '-') + '</span><span>状态：' + statusLabel(row.nodeStatus, row.nodeStatus === 1 ? I18n.jobinfo_opt_start : I18n.jobinfo_opt_stop) + '</span></div>' +
                        '<div class="node-info-line"><span>描述：' + escapeHtml(row.nodeDesc || '-') + '</span></div>';
                }
            },
            {
                "data": 'runNodeId',
                "visible": true,
                "width": '38%',
                "render": function (data, type, row) {
                    if (!row.runNodeId) {
                        return '<div class="run-info-empty">暂无运行节点记录</div>';
                    }
                    return '<div class="node-info-title">' + statusLabel(row.nodeRunStatus, row.nodeRunStatusName) + '</div>' +
                        '<div class="node-info-line"><span>运行节点ID：<span class="node-info-id">' + escapeHtml(row.runNodeId) + '</span></span></div>' +
                        '<div class="node-info-line"><span>运行作业ID：<span class="node-info-id">' + escapeHtml(row.runWorkId || '-') + '</span></span></div>' +
                        '<div class="node-info-line"><span>翻牌时间：' + escapeHtml(row.turnDateText || '-') + '</span><span>剩余重试：' + escapeHtml(row.retryTimes == null ? '-' : row.retryTimes) + '</span></div>' +
                        '<div class="node-info-line"><span>开始：' + escapeHtml(row.startTimeText || '-') + '</span><span>结束：' + escapeHtml(row.endTimeText || '-') + '</span></div>';
                }
            },
            {
                "data": I18n.system_opt,
                "width": '14%',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.nodeId] = row;

                        return '<div class="btn-group">\n' +
                            '     <button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>\n' +
                            '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                            '       <span class="caret"></span>\n' +
                            '       <span class="sr-only">Toggle Dropdown</span>\n' +
                            '     </button>\n' +
                            '     <ul class="dropdown-menu" role="menu" _id="' + row.nodeId + '" >\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="view-log">查看运行节点执行日志</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="view-log-detail">查看运行节点详细日志</a></li>\n' +
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

    // job operate
    $("#work_node_list").on('click', '.delete', function () {
        var url = base_url + "/node/delete";
        var id = $(this).parents('ul').attr("_id");
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
        var id = $(this).parents('ul').attr("_id");
        openNodeForm(base_url + "/node/updateModel?workNodeId=" + id, I18n.jobinfo_field_update);
    });

    // view run node log
    $("#work_node_list").on('click', '.view-log',function() {
        var id = $(this).parents('ul').attr("_id");
        layer.open({
            type: 2,
            area: ['92%', '82%'],
            title: '运行节点执行日志',
            shade: 0.6,
            shadeClose: true,
            maxmin: true,
            anim: 0,
            content: base_url + "/node/viewLogModel?workNodeId=" + id
        });
    });

    // view run node log detail
    $("#work_node_list").on('click', '.view-log-detail',function() {
        var id = $(this).parents('ul').attr("_id");
        layer.open({
            type: 2,
            area: ['92%', '82%'],
            title: '运行节点详细日志',
            shade: 0.6,
            shadeClose: true,
            maxmin: true,
            anim: 0,
            content: base_url + "/node/viewLogDetailModel?workNodeId=" + id
        });
    });
});
