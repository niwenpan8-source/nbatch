<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>运行节点日志</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        body {background: #f5f7fa;}
        .log-page {padding: 14px;}
        .log-meta {padding: 12px 15px; background: #fff; border-radius: 4px; margin-bottom: 12px; color: #4b5563;}
        .log-meta span {display: inline-block; margin-right: 24px; line-height: 24px;}
        .log-filter {display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px;}
        .log-filter .layui-input {width: 260px; height: 30px;}
        .layui-tab {background: #fff; margin: 0; border-radius: 4px; overflow: hidden;}
        .layui-tab-content {padding: 12px;}
        .log-detail-cell {white-space: pre-line; line-height: 20px;}
    </style>
</head>
<body>
<div class="log-page">
    <input type="hidden" id="workId" value="${workNode.workId}">
    <input type="hidden" id="nodeId" value="${workNode.nodeId}">
    <div class="log-meta">
        <span>节点：${workNode.nodeName}</span>
        <span>节点ID：${workNode.nodeId}</span>
        <div class="log-filter">
            <label>创建时间：</label>
            <input type="text" class="layui-input" id="createTimeRange" placeholder="开始日期 - 结束日期">
            <button class="layui-btn layui-btn-normal layui-btn-sm" id="searchLogBtn">查询</button>
            <button class="layui-btn layui-btn-primary layui-btn-sm" id="resetLogBtn">重置</button>
        </div>
    </div>
    <div class="layui-tab layui-tab-brief" lay-filter="logTabs">
        <ul class="layui-tab-title">
            <li class="layui-this" lay-id="execute">执行日志</li>
            <li lay-id="detail">详细日志</li>
        </ul>
        <div class="layui-tab-content">
            <div class="layui-tab-item layui-show">
                <table class="layui-table" id="work_node_log_list" lay-filter="work_node_log_list"></table>
            </div>
            <div class="layui-tab-item">
                <table class="layui-table" id="work_node_log_detail_list" lay-filter="work_node_log_detail_list"></table>
            </div>
        </div>
    </div>
</div>

<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>
<script>
    layui.use(['table', 'element', 'laydate'], function(){
        var table = layui.table;
        var element = layui.element;
        var laydate = layui.laydate;
        var $ = layui.$;
        var base_url = '${request.contextPath}';
        var workId = $('#workId').val();
        var nodeId = $('#nodeId').val();
        var detailLoaded = false;

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

        // Layui table endpoints share the same admin ReturnT<IPage<?>> response wrapper.
        function parsePageData(res) {
            var content = res && res.content ? res.content : {};
            return {
                code: 0,
                msg: '',
                count: content.total || 0,
                data: content.records || []
            };
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
            return {
                nodeId: nodeId,
                workId: workId,
                startTime: range[0] || '',
                endTime: range[1] || ''
            };
        }

        laydate.render({elem: '#createTimeRange', type: 'date', range: true});

        function renderExecuteLog() {
            table.render({
                elem: '#work_node_log_list',
                url: base_url + '/node/logPageList',
                method: 'post',
                where: logWhere(),
                request: {pageName: 'start', limitName: 'length'},
                parseData: parsePageData,
                cols: [[
                    {field: 'runWorkId', title: '运行作业ID', width: 180},
                    {field: 'runNodeId', title: '运行节点ID', width: 180},
                {field: 'turnDate', title: '运行日期', width: 120, templet: function(row) { return dateOrDash(row.turnDate); }},
                    {field: 'handleCode', title: '执行状态', width: 100},
                    {field: 'handleMsg', title: '执行信息', minWidth: 260, templet: function(row) { return shortText(row.handleMsg); }},
                    {field: 'createTime', title: '创建时间', width: 170},
                    {field: 'callBackTime', title: '回调时间', width: 170}
                ]],
                page: true,
                limit: 10,
                limits: [10, 20, 30, 50],
                loading: true
            });
        }

        // Render detail logs lazily so the initial popup opens quickly.
        function renderDetailLog() {
            detailLoaded = true;
            table.render({
                elem: '#work_node_log_detail_list',
                url: base_url + '/node/logDetailPageList',
                method: 'post',
                where: logWhere(),
                request: {pageName: 'start', limitName: 'length'},
                parseData: parsePageData,
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
        }

        renderExecuteLog();

        element.on('tab(logTabs)', function(data){
            if (data.index === 1 && !detailLoaded) {
                renderDetailLog();
            }
        });

        $('#searchLogBtn').on('click', function(){
            table.reload('work_node_log_list', {where: logWhere(), page: {curr: 1}});
            if (detailLoaded) {
                table.reload('work_node_log_detail_list', {where: logWhere(), page: {curr: 1}});
            }
        });

        $('#resetLogBtn').on('click', function(){
            $('#createTimeRange').val('');
            $('#searchLogBtn').click();
        });

        $(document).on('click', '.show-full-log', function(){
            layer.open({
                type: 1,
                title: '日志详情',
                area: ['860px', '560px'],
                content: '<pre style="white-space:pre-wrap;word-break:break-word;padding:16px;max-height:500px;overflow:auto;">' + $(this).attr('data-log') + '</pre>'
            });
        });
    });
</script>
</body>
</html>
