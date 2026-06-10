<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>运行节点执行日志</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        body {background: #f3f6fb;}
        .layui-fluid {padding: 16px;}
        .layui-card {border-radius: 12px; box-shadow: 0 8px 24px rgba(15, 23, 42, .08); overflow: hidden;}
        .layui-card-header {font-weight: 600;}
        .log-toolbar {margin-bottom: 12px; color: #4b5563;}
    </style>
</head>
<body>
<div class="layui-fluid">
    <div class="layui-card">
        <div class="layui-card-header">运行节点执行日志</div>
        <div class="layui-card-body">
            <input type="hidden" id="workId" value="${workNode.workId}">
            <input type="hidden" id="nodeId" value="${workNode.nodeId}">
            <div class="layui-row layui-col-space10 log-toolbar">
                <div class="layui-col-md3">节点：${workNode.nodeName}</div>
                <div class="layui-col-md4">节点ID：${workNode.nodeId}</div>
                <div class="layui-col-md2"><button class="layui-btn layui-btn-normal layui-btn-sm" id="searchLogBtn">刷新</button></div>
            </div>
            <table class="layui-table" id="work_node_log_list" lay-filter="work_node_log_list"></table>
        </div>
    </div>
</div>

<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>

<script>
    layui.use(['table'], function(){
        var table = layui.table;
        var $ = layui.$;
        var base_url = '${request.contextPath}';
        var workId = $('#workId').val();
        var nodeId = $('#nodeId').val();

        table.render({
            elem: '#work_node_log_list',
            url: base_url + '/node/logPageList',
            method: 'post',
            where: {nodeId: nodeId, workId: workId},
            request: {pageName: 'start', limitName: 'length'},
            parseData: function(res){
                var content = res && res.content ? res.content : {};
                return {
                    code: 0,
                    msg: '',
                    count: content.total || 0,
                    data: content.records || []
                };
            },
            cols: [[
                {field: 'runWorkId', title: '运行作业ID', width: 180},
                {field: 'runNodeId', title: '运行节点ID', width: 180},
                {field: 'handleCode', title: '执行状态', width: 100},
                {field: 'handleMsg', title: '执行信息', minWidth: 260},
                {field: 'createTime', title: '创建时间', width: 170},
                {field: 'callBackTime', title: '回调时间', width: 170}
            ]],
            page: true,
            limit: 10,
            limits: [10, 20, 30, 50],
            loading: true
        });

        $('#searchLogBtn').on('click', function(){
            table.reload('work_node_log_list', {where: {nodeId: nodeId, workId: workId}});
        });
    });
</script>
</body>
</html>
