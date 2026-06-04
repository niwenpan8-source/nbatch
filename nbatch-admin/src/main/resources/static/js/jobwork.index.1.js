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
                "width": '48%',
                "render": function (data, type, row) {
                    return '<div class="work-info-title">' + escapeHtml(row.workName) + '</div>' +
                        '<div class="work-info-line"><span>作业ID：' + escapeHtml(row.workId) + '</span><span>类型：' + escapeHtml(row.workTypeName || '-') + '</span><span>版本：' + escapeHtml(row.version || 0) + '</span></div>' +
                        '<div class="work-info-line"><span>状态：' + statusLabel(row.workStatus, row.workStatusName) + '</span><span>描述：' + escapeHtml(row.workDesc || '-') + '</span></div>';
                }
            },
            {
                "data": 'runWorkId',
                "visible": true,
                "width": '38%',
                "render": function (data, type, row) {
                    if (!row.runWorkId) {
                        return '<div class="run-info-empty">暂无运行作业记录</div>';
                    }
                    return '<div class="work-info-title">' + statusLabel(row.runWorkStatus, row.runWorkStatusName) + '</div>' +
                        '<div class="work-info-line"><span>运行ID：<span class="run-info-id">' + escapeHtml(row.runWorkId) + '</span></span></div>' +
                        '<div class="work-info-line"><span>翻牌时间：' + escapeHtml(row.turnDate || '-') + '</span><span>创建时间：' + escapeHtml(row.runWorkCreateTime || '-') + '</span></div>';
                }
            },
            {
                "data": I18n.system_opt,
                "width": '14%',
                "render": function (data, type, row) {
                    return function () {
                        tableData['key' + row.workId] = row;

                        return '<div class="btn-group">\n' +
                            '     <button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>\n' +
                            '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                            '       <span class="caret"></span>\n' +
                            '       <span class="sr-only">Toggle Dropdown</span>\n' +
                            '     </button>\n' +
                            '     <ul class="dropdown-menu" role="menu" _id="' + row.workId + '" >\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="edit">设置节点依赖关系</a></li>\n' +
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
            area: ['700px', '480px'],
            title: I18n.jobinfo_field_add,
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
        var id = $(this).parents('ul').attr("_id");
        var url = base_url + "/work/updateModel?workId=" + id;
        layer.open({
            type: 2,
            area: ['700px', '480px'],
            title: I18n.jobinfo_field_update,
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
        var id = $(this).parents('ul').attr("_id");
        var url = base_url + "/work/editModel?workId=" + id;
        layer.open({
            type: 2,
            area: ['92%', '86%'],
            title: '设置节点依赖关系',
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url
        });
    });
});
