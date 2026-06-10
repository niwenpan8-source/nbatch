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
                obj.workStatus = $('#workStatus').val();
                obj.start = d.start;
                obj.length = d.length;
                return obj;
            }
        },
        "searching": false,
        "ordering": false,
        "columns": [
            {
                "data": 'workName',
                "bSortable": false,
                "visible": true,
                "width": '160px',
                "render": function (data) { return textOrDash(data); }
            },
            {"data": 'workId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'workTypeName', "width": '100px', "render": function (data) { return textOrDash(data); }},
            {"data": 'workStatusName', "width": '90px', "render": function (data, type, row) { return statusLabel(row.workStatus, data); }},
            {"data": 'workDesc', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'version', "width": '70px', "render": function (data) { return escapeHtml(data == null ? 0 : data); }},
            {"data": 'runWorkId', "width": '180px', "render": function (data) { return textOrDash(data); }},
            {"data": 'runWorkStatusName', "width": '90px', "render": function (data, type, row) { return row.runWorkId ? statusLabel(row.runWorkStatus, data) : '-'; }},
            {"data": 'turnDate', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {"data": 'runWorkCreateTime', "width": '160px', "render": function (data) { return dateTimeOrDash(data); }},
            {
                "data": I18n.system_opt,
                "width": '120px',
                "className": 'table-action-cell',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.workId] = row;
                        var recoverBtn = '';
                        if (row.runWorkId && (row.runWorkStatus === 1 || row.runWorkStatus === 3)) {
                            recoverBtn = '<li><a href="javascript:void(0);" class="recover" data-run-work-id="' + escapeHtml(row.runWorkId) + '">恢复重跑</a></li>\n';
                        }
                        // Latest-run rerun is only meaningful when the workflow has at least one run record.
                        var rerunLatestBtn = row.runWorkId
                            ? '<li><a href="javascript:void(0);" class="rerun-latest">一键重跑</a></li>\n'
                            : '';
                        return '<div class="btn-group">\n' +
                            '     <button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>\n' +
                            '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                            '       <span class="caret"></span>\n' +
                            '       <span class="sr-only">Toggle Dropdown</span>\n' +
                            '     </button>\n' +
                            '     <ul class="dropdown-menu" role="menu" data-work-id="' + escapeHtml(row.workId) + '">\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            rerunLatestBtn +
                            '       <li><a href="javascript:void(0);" class="edit">依赖关系</a></li>\n' +
                            recoverBtn +
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
        jobWorkTable.fnDraw();
    });

    // job operate
    $("#job_work_list").on('click', '.delete', function () {
        var url = base_url + "/work/delete";
        var id = $(this).closest('ul').attr("data-work-id");
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
            area: getModalArea(860, 560),
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
        var id = $(this).closest('ul').attr("data-work-id");
        var url = base_url + "/work/updateModel?workId=" + id;
        layer.open({
            type: 2,
            area: getModalArea(860, 560),
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
        var id = $(this).closest('ul').attr("data-work-id");
        var url = base_url + "/work/editModel?workId=" + id;
        layer.open({
            type: 2,
            area: getModalArea(1180, 760),
            title: '设置节点依赖关系',
            closeBtn: 1,
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url
        });
    });

    // Rerun the latest run work from the beginning.
    $("#job_work_list").on('click', '.rerun-latest', function () {
        var workId = $(this).closest('ul').attr('data-work-id');
        layer.confirm('确认完整重跑该流程最新一次运行作业？所有节点都会重新执行。', {
            icon: 3,
            title: I18n.system_tips,
            btn: [I18n.system_ok, I18n.system_cancel]
        }, function (index) {
            layer.close(index);
            $.ajax({
                type: 'POST',
                url: base_url + '/work/rerunLatestRunWork',
                data: {workId: workId},
                dataType: 'json',
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg('重跑设置成功', {icon: 1});
                        jobWorkTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || '重跑设置失败', {icon: 2});
                    }
                },
                error: function () {
                    layer.msg(I18n.system_fail, {icon: 2});
                }
            });
        });
    });

    // recover only failed/exception nodes in a run work
    $("#job_work_list").on('click', '.recover', function () {
        var runWorkId = $(this).attr('data-run-work-id');
        layer.confirm('确认只恢复该运行作业中异常或失败的节点，并继续重跑？已完成节点不会重跑。', {
            icon: 3,
            title: I18n.system_tips,
            btn: [I18n.system_ok, I18n.system_cancel]
        }, function (index) {
            layer.close(index);
            $.ajax({
                type: 'POST',
                url: base_url + '/work/recoverRunWork',
                data: {runWorkId: runWorkId},
                dataType: 'json',
                success: function (data) {
                    if (data.code === 200) {
                        layer.msg('恢复成功', {icon: 1});
                        jobWorkTable.fnDraw(false);
                    } else {
                        layer.msg(data.msg || '恢复失败', {icon: 2});
                    }
                },
                error: function () {
                    layer.msg(I18n.system_fail, {icon: 2});
                }
            });
        });
    });
});
