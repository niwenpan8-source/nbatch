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
                "width": '15%'
            },
            {
                "data": 'nodeName',
                "visible": true,
                "width": '15%'
            },
            {
                "data": 'nodeDesc',
                "visible": true,
                "width": '20%'
            },
            {
                "data": 'nodeStatus',
                "visible": true,
                "width": '15%',
                "render": function (data, type, row) {
                    // status
                    if (1 === data) {
                        return '<div style="text-align: left;"><small class="label label-success">' + I18n.jobinfo_opt_start + '</small></div>';
                    } else {
                        return '<div style="text-align: left;"><small class="label label-default">' + I18n.jobinfo_opt_stop + '</small></div>';
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
            area: ['700px', '520px'],
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
                        url: base_url + '/node/insert',
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg(I18n.system_add_suc, {icon: 1});
                                layer.close(index);
                                workNodeTable.fnDraw();
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

    // add
    $("#work_node_list").on('click', '.update',function() {
        var id = $(this).parents('ul').attr("_id");
        var url = base_url + "/node/updateModel?workNodeId=" + id;
        layer.open({
            type: 2,
            area: ['700px', '520px'],
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
                        url: base_url + '/node/update',
                        type: 'POST',
                        data: formData,
                        processData: false,
                        contentType: false,
                        success: function(response) {
                            if (response.code === 200) {
                                layer.msg(I18n.system_update_suc, {icon: 1});
                                layer.close(index);
                                workNodeTable.fnDraw();
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


});
