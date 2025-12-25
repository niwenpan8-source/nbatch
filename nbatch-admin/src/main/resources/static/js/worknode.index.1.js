$(function () {
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
                "data": 'nodeId',
                "bSortable": false,
                "visible": true,
                "width": '12%'
            },
            {
                "data": 'nodeName',
                "visible": true,
                "width": '12%'
            },
            {
                "data": 'workId',
                "visible": true,
                "width": '12%'
            },
            {
                "data": 'workName',
                "visible": true,
                "width": '12%'
            },
            {
                "data": 'nodeDesc',
                "visible": true,
                "width": '15%'
            },
            {
                "data": 'nodeStatus',
                "visible": true,
                "width": '12%',
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
                "data": 'nodeTypeName',
                "visible": true,
                "width": '15%'
            },
            {
                "data": I18n.system_opt,
                "width": '15%',
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
                            '     <ul class="dropdown-menu" role="menu" _id="' + row.nodeId + '" >\n' +
                            '       <li><a href="javascript:void(0);" class="update">' + I18n.system_opt_edit + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="delete">' + I18n.system_opt_del + '</a></li>\n' +
                            '       <li><a href="javascript:void(0);" class="viewLog">' + I18n.system_opt_view_log + '</a></li>\n' +
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
        workNodeTable.fnDraw();
    });

    // job operate
    $("#work_node_list").on('click', '.delete', function () {

        var url = base_url + "/node/delete";
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
        var url = base_url + "/node/addModel";
        layer.open({
            type: 2,
            area: ['800px', '500px'],
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
                        url: base_url + '/node/insert',  // 替换为实际的提交地址
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg('添加成功', {icon: 1});
                                layer.close(index);
                                workNodeTable.fnDraw();  // 刷新表格
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

    // update
    $("#work_node_list").on('click', '.update',function() {
        var id = $(this).parents('ul').attr("_id");
        console.log(id)
        var url = base_url + "/node/updateModel?workNodeId=" + id;
        layer.open({
            type: 2,
            area: ['800px', '500px'],
            title: '修改',
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
                        url: base_url + '/node/update',  // 替换为实际的提交地址
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg('添加成功', {icon: 1});
                                layer.close(index);
                                workNodeTable.fnDraw();  // 刷新表格
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

    // 查看日志
    $("#work_node_list").on('click', '.viewLog',function() {
        var id = $(this).parents('ul').attr("_id");
        var url = base_url + "/node/viewLogModel?workNodeId=" + id;
        layer.open({
            type: 2,
            area: ['1000px', '650px'],
            title: '查看日志',
            shade: 0.6,
            shadeClose: false,
            maxmin: true,
            anim: 0,
            content: url,
            btn: ['确认', '取消']
        });
    });


});
