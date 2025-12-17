$(function () {
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
                "data": 'workId',
                "bSortable": false,
                "visible": true,
                "width": '15%'
            },
            {
                "data": 'workName',
                "visible": true,
                "width": '15%'
            },
            {
                "data": 'workStatus',
                "visible": true,
                "width": '10%',
                "render": function (data, type, row) {
                    // status
                    if (1 === data) {
                        return '<div style="text-align: left;"><small class="label label-success">启用</small></div>';
                    } else {
                        return '<div style="text-align: left;"><small class="label label-default">停用</small></div>';
                    }
                }
            },
            {
                "data": 'runWorkStatus',
                "visible": true,
                "width": '10%',
                "render": function (data, type, row) {
                    // status
                    if (2 === data) {
                        return '<div style="text-align: left;"><small class="label label-info">执行完毕</small></div>';
                    } else if (1 === data) {
                        return '<div style="text-align: left;"><small class="label label-info">进行中</small></div>';
                    } else {
                        return '<div style="text-align: left;"><small class="label label-default">待执行</small></div>';
                    }
                }
            },
            {
                "data": 'turnDate',
                "visible": true,
                "width": '15%',
                "render": function (data, type, row) {
                    return data ? moment(new Date(data)).format("YYYY-MM-DD") : "";
                }
            },
            {
                "data": I18n.system_opt,
                "width": '10%',
                "render": function (data, type, row) {
                    return function () {

                        // data
                        tableData['key' + row.id] = row;

                        // opt
                        return '<div class="btn-group">\n' +
                            '     <button type="button" class="btn btn-primary btn-sm">' + I18n.system_opt + '</button>\n' +
                            '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                            '       <span class="caret"></span>\n' +
                            '       <span class="sr-only">Toggle Dropdown</span>\n' +
                            '     </button>\n' +
                            '     <ul class="dropdown-menu" role="menu" _id="' + row.workId + '" >\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="edit">' + I18n.system_opt_edit + '</a></li>\n' +
                            '     </ul>\n' +
                            '   </div>';
                    };
                }
            }
        ],
        "language": {
            "sProcessing": I18n.dataTable_sProcessing,
            "sLengthMenu": I18n.dataTable_sLengthMenu,
            "sZeroRecords": I18n.dataTable_sZeroRecords,
            "sInfo": I18n.dataTable_sInfo,
            "sInfoEmpty": I18n.dataTable_sInfoEmpty,
            "sInfoFiltered": I18n.dataTable_sInfoFiltered,
            "sInfoPostFix": "",
            "sSearch": I18n.dataTable_sSearch,
            "sUrl": "",
            "sEmptyTable": I18n.dataTable_sEmptyTable,
            "sLoadingRecords": I18n.dataTable_sLoadingRecords,
            "sInfoThousands": ",",
            "oPaginate": {
                "sFirst": I18n.dataTable_sFirst,
                "sPrevious": I18n.dataTable_sPrevious,
                "sNext": I18n.dataTable_sNext,
                "sLast": I18n.dataTable_sLast
            },
            "oAria": {
                "sSortAscending": I18n.dataTable_sSortAscending,
                "sSortDescending": I18n.dataTable_sSortDescending
            }
        }
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
        console.log(id);
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
            area: ['500px', '400px'],
            title: '添加',
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: ['确认', '取消'],
            yes: function(index, layero) {
                // 获取iframe内的表单数据并提交
                var iframeWin = layero.find('iframe')[0].contentWindow;
                var form = iframeWin.document.getElementById('addModel');

                if (form) {
                    // 创建FormData对象收集表单数据
                    var formData = new FormData(form);

                    // 使用jQuery.ajax提交表单
                    $.ajax({
                        url: base_url + '/work/insert',  // 替换为实际的提交地址
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg('添加成功', {icon: 1});
                                layer.close(index);
                                jobWorkTable.fnDraw();  // 刷新表格
                            } else {
                                layer.msg(response.msg || '添加失败', {icon: 2});
                            }
                        },
                        error: function() {
                            layer.msg('请求失败', {icon: 2});
                        }
                    });
                } else {
                    layer.msg('未找到表单', {icon: 2});
                }
            },
            btn2: function(index) {
                layer.close(index);
            }
        });
    });

    // add
    $("#job_work_list").on('click', '.update',function() {
        var id = $(this).parents('ul').attr("_id");
        console.log(id)
        var url = base_url + "/work/updateModel?workId=" + id;
        layer.open({
            type: 2,
            area: ['500px', '400px'],
            title: '编辑',
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: ['确认', '取消'],
            yes: function(index, layero) {
                // 获取iframe内的表单数据并提交
                var iframeWin = layero.find('iframe')[0].contentWindow;
                var form = iframeWin.document.getElementById('updateModel');

                if (form) {
                    // 创建FormData对象收集表单数据
                    var formData = new FormData(form);

                    // 使用jQuery.ajax提交表单
                    $.ajax({
                        url: base_url + '/work/update',  // 替换为实际的提交地址
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg('添加成功', {icon: 1});
                                layer.close(index);
                                jobWorkTable.fnDraw();  // 刷新表格
                            } else {
                                layer.msg(response.msg || '添加失败', {icon: 2});
                            }
                        },
                        error: function() {
                            layer.msg('请求失败', {icon: 2});
                        }
                    });
                } else {
                    layer.msg('未找到表单', {icon: 2});
                }
            },
            btn2: function(index) {
                layer.close(index);
            }
        });
    });

    // add
    $("#job_work_list").on('click', '.edit',function() {
        var id = $(this).parents('ul').attr("_id");
        var url = base_url + "/work/editModel?workId=" + id;
        // 打开新页签
        window.open(url, '_blank');
    });


});
