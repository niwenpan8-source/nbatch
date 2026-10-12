<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>运行节点日志</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        body {background: #edf3f1; color: #263b3d;}
        .log-page {padding: 16px;}
        .log-meta {padding: 16px 18px; margin-bottom: 14px; border: 1px solid #d8e4e1; border-radius: 8px; background: #fff; box-shadow: 0 8px 24px rgba(20, 58, 56, .08); color: #4b5563;}
        .log-meta span {display: inline-block; margin-right: 24px; line-height: 24px;}
        .log-filter {display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px;}
        .log-filter .layui-input {width: 260px; height: 32px; border-color: #ccdcd8; border-radius: 4px;}
        .log-scope-picker {position: relative; width: 300px;}
        .log-scope-picker .layui-input {width: 100%;}
        .log-scope-value {display: none;}
        .log-scope-results {position: absolute; z-index: 10; top: calc(100% + 4px); right: 0; left: 0; max-height: 248px; overflow: auto; border: 1px solid #bdd3ce; border-radius: 4px; background: #fff; box-shadow: 0 8px 20px rgba(25, 49, 46, .16);}
        .log-scope-results:empty {display: none;}
        .log-scope-result {display: flex; align-items: center; width: 100%; min-height: 34px; padding: 6px 9px; border: 0; border-bottom: 1px solid #edf0f3; background: #fff; color: #36504e; font-size: 12px; text-align: left;}
        .log-scope-result:hover, .log-scope-result.is-selected {background: #eef8f5; color: #175d59;}
        .log-scope-result:last-child {border-bottom: 0;}
        .log-scope-empty {padding: 12px 9px; color: #8a96a3; font-size: 12px; text-align: center;}
        .log-filter .layui-input:focus {border-color: #3d8882 !important; box-shadow: 0 0 0 2px rgba(33, 111, 107, .14);}
        .log-filter .layui-btn {height: 32px; border-color: #216f6b; border-radius: 4px; background: #216f6b; line-height: 32px;}
        .log-filter .layui-btn:hover {border-color: #175d59; background: #175d59;}
        .log-filter .layui-btn-primary {border-color: #b9d0cb; background: #f7fbfa; color: #285f5d;}
        .log-filter .layui-btn-primary:hover {border-color: #83afa8; background: #e8f4f1; color: #175d59;}
        .layui-tab {margin: 0; border: 1px solid #d8e4e1; border-radius: 8px; background: #fff; box-shadow: 0 8px 24px rgba(20, 58, 56, .08); overflow: hidden;}
        .layui-tab-title {border-bottom-color: #dce8e5; background: #f3f8f6;}
        .layui-tab-title .layui-this {color: #216f6b;}
        .layui-tab-title .layui-this:after {border-color: #dce8e5; border-bottom-color: #fff;}
        .layui-tab-content {padding: 16px;}
        .layui-table {border-color: #d8e4e1;}
        .layui-table thead tr {background: #f3f8f6;}
        .layui-table th {color: #36504e; font-weight: 600;}
    </style>
</head>
<body>
<div class="log-page">
    <input type="hidden" id="workId" value="${workId!''}">
    <div class="log-meta">
        <div class="log-filter">
            <label>日志范围：</label>
            <div class="log-scope-picker">
                <input type="search" class="layui-input" id="logScopeSearch" placeholder="搜索节点或组名称、ID" autocomplete="off">
                <select class="log-scope-value" id="logScope" data-default-scope="${(logScopeValue!'')?html}">
                <option value="WORK" <#if logScopeValue == "WORK">selected</#if>>当前作业</option>
                <#if flowGroups?has_content>
                    <optgroup label="流程组">
                        <#list flowGroups as group>
                            <option value="GROUP:${group.groupId?html}" <#if logScopeValue == "GROUP:" + group.groupId>selected</#if>>组：${group.groupName?html}（${group.groupId?html}）</option>
                        </#list>
                    </optgroup>
                </#if>
                <#if workNodes?has_content>
                    <optgroup label="节点">
                        <#list workNodes as node>
                            <option value="NODE:${node.nodeId?html}" <#if logScopeValue == "NODE:" + node.nodeId>selected</#if>>节点：${node.nodeName?html}（${node.nodeId?html}）</option>
                        </#list>
                    </optgroup>
                </#if>
                </select>
                <div class="log-scope-results" id="logScopeResults"></div>
            </div>
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
        var detailLoaded = false;
        var maxLogScopeResults = 30;

        function escapeHtml(value) {
            if (value === null || value === undefined) {
                return '';
            }
            return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
        }

        function shortText(value) {
            var text = value || '';
            if (!text) {
                return '-';
            }
            var escaped = escapeHtml(text);
            var preview = text.length > 160 ? text.substring(0, 160) + '...' : text;
            return '<span title="' + escaped + '">' + escapeHtml(preview) + '</span>';
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
        function logScopeOptions() {
            return $('#logScope option').map(function() {
                return {value: this.value, label: $(this).text()};
            }).get();
        }
        function syncLogScopeInput() {
            var selected = $('#logScope option:selected');
            $('#logScopeSearch').val(selected.length ? selected.text() : '当前作业');
        }
        function renderLogScopeResults(keywordOverride) {
            var input = $('#logScopeSearch');
            var keyword = keywordOverride === undefined ? $.trim(String(input.val() || '')).toLowerCase() : keywordOverride;
            var currentScope = $('#logScope').val();
            var matches = logScopeOptions().filter(function(item) {
                return !keyword || item.label.toLowerCase().indexOf(keyword) >= 0 || item.value.toLowerCase().indexOf(keyword) >= 0;
            });
            var results = matches.slice(0, maxLogScopeResults);
            var html = results.map(function(item) {
                return '<button type="button" class="log-scope-result' + (item.value === currentScope ? ' is-selected' : '')
                    + '" data-scope="' + escapeHtml(item.value) + '" title="' + escapeHtml(item.label) + '">' + escapeHtml(item.label) + '</button>';
            }).join('');
            if (!results.length) {
                html = '<div class="log-scope-empty">未找到匹配的日志范围</div>';
            } else if (matches.length > maxLogScopeResults) {
                html += '<div class="log-scope-empty">匹配结果较多，仅显示前 ' + maxLogScopeResults + ' 项</div>';
            }
            $('#logScopeResults').html(html);
        }
        function selectLogScope(scope, reloadAfterSelect) {
            $('#logScope').val(scope);
            syncLogScopeInput();
            $('#logScopeResults').empty();
            if (reloadAfterSelect) {
                reloadLogs();
            }
        }
        function logWhere() {
            var range = $('#createTimeRange').val().split(' - ');
            var scope = $('#logScope').val() || 'WORK';
            var where = {
                workId: workId,
                startTime: range[0] || '',
                endTime: range[1] || ''
            };
            if (scope.indexOf('GROUP:') === 0) {
                where.groupId = scope.substring('GROUP:'.length);
            } else if (scope.indexOf('NODE:') === 0) {
                where.nodeId = scope.substring('NODE:'.length);
            }
            return where;
        }

        laydate.render({elem: '#createTimeRange', type: 'date', range: true});
        syncLogScopeInput();

        function renderExecuteLog() {
            table.render({
                elem: '#work_node_log_list',
                url: base_url + '/node/logPageList',
                method: 'post',
                where: logWhere(),
                request: {pageName: 'start', limitName: 'length'},
                parseData: parsePageData,
                cols: [[
                    {field: 'nodeName', title: '节点名称', minWidth: 160},
                    {field: 'nodeId', title: '节点 ID', width: 180},
                    {field: 'turnDate', title: '运行日期', width: 120, templet: function(row) { return dateOrDash(row.turnDate); }},
                    {field: 'handleCode', title: '执行状态', width: 100},
                    {field: 'handleMsg', title: '执行信息', minWidth: 260, templet: function(row) { return shortText(row.handleMsg); }},
                    {field: 'executeStartTime', title: '实际开始时间', width: 170},
                    {field: 'executeEndTime', title: '实际结束时间', width: 170}
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
                    {field: 'nodeName', title: '节点名称', minWidth: 160},
                    {field: 'nodeId', title: '节点 ID', width: 180},
                    {field: 'turnDate', title: '运行日期', width: 120, templet: function(row) { return dateOrDash(row.turnDate); }},
                    {field: 'handleMsg', title: '详细日志', minWidth: 260, templet: function(row) { return shortText(row.handleMsg); }},
                    {field: 'executeTime', title: '执行时间', width: 170}
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

        function reloadLogs() {
            table.reload('work_node_log_list', {where: logWhere(), page: {curr: 1}});
            if (detailLoaded) {
                table.reload('work_node_log_detail_list', {where: logWhere(), page: {curr: 1}});
            }
        }

        $('#searchLogBtn').on('click', function(){
            reloadLogs();
        });

        $('#logScopeSearch').on('focus', function(){
            this.select();
            renderLogScopeResults('');
        });

        $('#logScopeSearch').on('input', function(){
            if (!$.trim(this.value)) {
                if ($('#logScope').val() !== 'WORK') {
                    $('#logScope').val('WORK');
                    reloadLogs();
                }
                renderLogScopeResults('');
                return;
            }
            renderLogScopeResults();
        });

        $('#logScopeSearch').on('keydown', function(event){
            if (event.key === 'Enter') {
                var firstResult = $('#logScopeResults .log-scope-result').first();
                if (firstResult.length) {
                    event.preventDefault();
                    selectLogScope(firstResult.data('scope'), true);
                }
            } else if (event.key === 'Escape') {
                $('#logScopeResults').empty();
                syncLogScopeInput();
            }
        });

        $(document).on('click', '.log-scope-result', function(){
            selectLogScope($(this).data('scope'), true);
        });

        $(document).on('click', function(event){
            if (!$(event.target).closest('.log-scope-picker').length) {
                $('#logScopeResults').empty();
            }
        });

        $('#resetLogBtn').on('click', function(){
            $('#createTimeRange').val('');
            selectLogScope('WORK', false);
            reloadLogs();
        });

    });
</script>
</body>
</html>
