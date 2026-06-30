<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>运行节点详细日志</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        body {background: #f3f6fb;}
        .layui-fluid {padding: 16px;}
        .layui-card {border-radius: 12px; box-shadow: 0 8px 24px rgba(15, 23, 42, .08); overflow: hidden;}
        .layui-card-header {font-weight: 600;}
        .log-toolbar {margin-bottom: 12px; color: #4b5563;}
        .log-filter {display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px;}
        .log-filter .layui-input {width: 260px; height: 30px;}
        .log-detail-cell {white-space: pre-line; line-height: 20px;}
    </style>
</head>
<body>
<div class="layui-fluid">
    <div class="layui-card">
        <div class="layui-card-header">运行节点详细日志</div>
        <div class="layui-card-body">
            <input type="hidden" id="workId" value="${workNode.workId}">
            <input type="hidden" id="nodeId" value="${workNode.nodeId}">
            <div class="layui-row layui-col-space10 log-toolbar">
                <div class="layui-col-md3">节点：${workNode.nodeName}</div>
                <div class="layui-col-md4">节点ID：${workNode.nodeId}</div>
                <div class="layui-col-md5 log-filter">
                    <label>创建时间：</label>
                    <input type="text" class="layui-input" id="createTimeRange" placeholder="开始日期 - 结束日期">
                    <button class="layui-btn layui-btn-normal layui-btn-sm" id="searchLogBtn">查询</button>
                    <button class="layui-btn layui-btn-primary layui-btn-sm" id="resetLogBtn">重置</button>
                </div>
            </div>
            <table class="layui-table" id="work_node_log_detail_list" lay-filter="work_node_log_detail_list"></table>
        </div>
    </div>
</div>

<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>

<script>
    layui.use(['table', 'laydate'], function(){
        var table = layui.table;
        var laydate = layui.laydate;
        var $ = layui.$;
        var base_url = '${request.contextPath}';
        var workId = $('#workId').val();
        var nodeId = $('#nodeId').val();

        function escapeHtml(value) {
            if (value === null || value === undefined) {
                return '';
            }
            return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
        }

        function shortText(value) {
            var text = value || '';
            var escaped = escapeHtml(text);
            if (text.length <= 120) {
                return '<span>' + escaped + '</span>';
            }
            return '<span>' + escapeHtml(text.substring(0, 120)) + '...</span> ' +
                '<a href="javascript:void(0);" class="show-full-log" data-log="' + escaped + '">详情</a>';
        }
        function dateOrDash(value) {
            if (value === null || value === undefined || value === '') {
                return '-';
            }
            if (typeof value === 'string') {
                return value.length > 10 ? value.substring(0, 10) : value;
            }
            var date = new Date(value);
            if (isNaN(date.getTime())) {
                return escapeHtml(value);
            }
            var month = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            return date.getFullYear() + '-' + month + '-' + day;
        }
        function logWhere() {
            var range = $('#createTimeRange').val().split(' - ');
            return {nodeId: nodeId, workId: workId, startTime: range[0] || '', endTime: range[1] || ''};
        }

        laydate.render({elem: '#createTimeRange', type: 'date', range: true});

        table.render({
            elem: '#work_node_log_detail_list',
            url: base_url + '/node/logDetailPageList',
            method: 'post',
            where: logWhere(),
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
                {field: 'turnDate', title: '运行日期', width: 120, templet: function(row) { return dateOrDash(row.turnDate); }},
                {field: 'handleMsg', title: '详细日志', minWidth: 420, templet: function(row) {
                    return '<div class="log-detail-cell">' + shortText(row.handleMsg) + '</div>';
                }},
                {field: 'createTime', title: '创建时间', width: 170},
                {field: 'executeTime', title: '执行时间', width: 170},
                {field: 'callBackTime', title: '回调时间', width: 170}
            ]],
            page: true,
            limit: 10,
            limits: [10, 20, 30, 50],
            loading: true
        });

        $('#searchLogBtn').on('click', function(){
            table.reload('work_node_log_detail_list', {where: logWhere(), page: {curr: 1}});
        });
        $('#resetLogBtn').on('click', function(){
            $('#createTimeRange').val('');
            $('#searchLogBtn').click();
        });
        $(document).on('click', '.show-full-log', function(){
            layer.open({type: 1, title: '日志详情', area: ['860px', '560px'], content: '<pre style="white-space:pre-wrap;word-break:break-word;padding:16px;max-height:500px;overflow:auto;">' + $(this).attr('data-log') + '</pre>'});
        });
    });
</script>
</body>
</html>
