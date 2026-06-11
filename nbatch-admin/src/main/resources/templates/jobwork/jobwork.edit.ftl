<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>设置节点依赖关系</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>
<body>
<style>
    body {background: #f5f7fb;}
    .page-wrap {padding: 18px;}
    .summary-card {background: #fff; border-radius: 8px; padding: 16px 18px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06);}
    .summary-title {font-size: 18px; font-weight: 600; margin-bottom: 8px;}
    .summary-desc {color: #666; line-height: 24px;}
    .config-card {background: #fff; border-radius: 8px; padding: 18px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06);}
    .config-grid {display: grid; grid-template-columns: 1fr 1fr auto; gap: 16px; align-items: end;}
    .config-label {font-weight: 600; margin-bottom: 8px; color: #333;}
    .node-picker {height: 38px; line-height: 38px; padding: 0 12px; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; cursor: pointer; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;}
    .node-picker:hover {border-color: #1E9FFF;}
    .node-tip {margin-top: 6px; color: #999; font-size: 12px;}
    .dependency-table {background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden;}
    .table-header {display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #eef0f5;}
    .table-title {font-size: 15px; font-weight: 600;}
    .relation-search {display: flex; align-items: center; gap: 8px;}
    .relation-search .layui-input {width: 220px; height: 30px;}
    .empty-tip {text-align: center; color: #999; padding: 26px;}
    .node-id {font-family: Menlo, Monaco, Consolas, monospace; color: #999; font-size: 12px; margin-left: 6px;}
    .button-group {text-align: right; padding: 14px 0 0;}
    .layui-table {margin: 0;}
    .node-picker-modal {padding: 14px;}
    .node-picker-toolbar {display: flex; gap: 8px; margin-bottom: 10px;}
    .node-picker-toolbar .layui-input {height: 32px;}
    .node-picker-table-wrap {max-height: 430px; overflow: auto; border: 1px solid #eef0f5;}
    .node-picker-table-wrap .layui-table td, .node-picker-table-wrap .layui-table th {font-size: 13px;}
    .node-picker-table-wrap tr.selected {background: #f0f9ff;}
    .node-picker-table-wrap tbody tr {cursor: pointer;}
    @media (max-width: 768px) {.config-grid {grid-template-columns: 1fr;} .button-group {text-align: left;}}
</style>

<div class="page-wrap">
    <div class="summary-card">
        <div class="summary-title">设置节点依赖关系</div>
        <div class="summary-desc">选择“当前节点”和它依赖的“前置节点”。当前节点只有在所有已配置的前置节点执行完成后，才会进入可执行队列。</div>
        <div class="summary-desc">作业ID：${workId}，节点数量：<#if list??>${list?size}<#else>0</#if>，已配置依赖：<#if relationList??>${relationList?size}<#else>0</#if></div>
    </div>

    <div class="config-card">
        <div class="config-grid">
            <div>
                <div class="config-label">当前节点</div>
                <div class="node-picker" id="currentNodePicker" onclick="openNodePicker('current')">请选择当前节点</div>
                <div class="node-tip">需要等待依赖节点完成后再执行的节点</div>
            </div>
            <div>
                <div class="config-label">依赖节点</div>
                <div class="node-picker" id="dependencyNodePicker" onclick="openNodePicker('dependency')">请选择依赖节点，可多选</div>
                <div class="node-tip">当前节点的前置节点，不能选择自身</div>
            </div>
            <div>
                <button class="layui-btn" onclick="addDependencies()">
                    <i class="layui-icon layui-icon-add-1"></i>添加依赖
                </button>
            </div>
        </div>
    </div>

    <div class="dependency-table">
        <div class="table-header">
            <div class="table-title">已配置的依赖关系</div>
            <div class="relation-search">
                <input type="text" class="layui-input" id="relationNodeName" placeholder="按节点名称查询">
                <button class="layui-btn layui-btn-primary layui-btn-sm" onclick="filterDependencyList()">查询</button>
                <button class="layui-btn layui-btn-primary layui-btn-sm" onclick="clearDependencyFilter()">重置</button>
                <button class="layui-btn layui-btn-sm" onclick="submitDependencies()">
                    <i class="layui-icon layui-icon-ok"></i>保存
                </button>
                <button class="layui-btn layui-btn-primary layui-btn-sm" onclick="resetDependencyList()">
                    <i class="layui-icon layui-icon-refresh"></i>清空
                </button>
            </div>
        </div>
        <table class="layui-table" lay-skin="line">
            <thead>
            <tr>
                <th width="40%">当前节点</th>
                <th width="40%">依赖节点</th>
                <th width="120">操作</th>
            </tr>
            </thead>
            <tbody id="dependencyList">
            <#if relationList?? && relationList?size gt 0>
                <#list relationList as relation>
                    <tr data-relation-key="${relation.nodeId1}_${relation.nodeId2}">
                        <td data-node-id="${relation.nodeId1}">${relation.nodeName1}<span class="node-id">${relation.nodeId1}</span></td>
                        <td data-node-id="${relation.nodeId2}">${relation.nodeName2}<span class="node-id">${relation.nodeId2}</span></td>
                        <td>
                            <button class="layui-btn layui-btn-danger layui-btn-sm" onclick="deleteDependency(this)">
                                <i class="layui-icon layui-icon-delete"></i>删除
                            </button>
                        </td>
                    </tr>
                </#list>
            <#else>
                <tr class="empty-row">
                    <td colspan="3"><div class="empty-tip">暂无配置的依赖关系</div></td>
                </tr>
            </#if>
            </tbody>
        </table>
    </div>
</div>

<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>
<script>
    layui.use(['layer', 'jquery'], function () {
        var layer = layui.layer;
        var $ = layui.jquery;
        var nodeList = [];

        <#list list as node>
        nodeList.push({
            nodeId: '${node.nodeId}',
            nodeName: '${node.nodeName}'
        });
        </#list>

        var workId = '${workId}';
        var selectedCurrentNode = null;
        var selectedDependencyNodes = [];

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

        function emptyRow() {
            return '<tr class="empty-row"><td colspan="3"><div class="empty-tip">暂无配置的依赖关系</div></td></tr>';
        }

        function nodeDisplay(node) {
            return node ? node.nodeName + ' ' + node.nodeId : '';
        }

        function renderPickerText() {
            $('#currentNodePicker').text(selectedCurrentNode ? nodeDisplay(selectedCurrentNode) : '请选择当前节点');
            if (selectedDependencyNodes.length === 0) {
                $('#dependencyNodePicker').text('请选择依赖节点，可多选');
            } else if (selectedDependencyNodes.length === 1) {
                $('#dependencyNodePicker').text(nodeDisplay(selectedDependencyNodes[0]));
            } else {
                $('#dependencyNodePicker').text('已选择 ' + selectedDependencyNodes.length + ' 个依赖节点');
            }
        }

        function filterNodes(keyword) {
            var value = $.trim(keyword).toLowerCase();
            return nodeList.filter(function(node) {
                return !value || node.nodeName.toLowerCase().indexOf(value) >= 0 || node.nodeId.toLowerCase().indexOf(value) >= 0;
            });
        }

        function renderNodeRows(type, keyword) {
            var nodes = filterNodes(keyword);
            if (selectedCurrentNode && type === 'dependency') {
                nodes = nodes.filter(function(node) { return node.nodeId !== selectedCurrentNode.nodeId; });
            }
            var selectedMap = {};
            selectedDependencyNodes.forEach(function(node) { selectedMap[node.nodeId] = true; });
            if (nodes.length === 0) {
                return '<tr><td colspan="3"><div class="empty-tip">没有匹配的节点</div></td></tr>';
            }
            return nodes.map(function(node) {
                var checked = type === 'dependency' && selectedMap[node.nodeId] ? ' checked' : '';
                var currentChecked = type === 'current' && selectedCurrentNode && selectedCurrentNode.nodeId === node.nodeId ? ' checked' : '';
                var selectedClass = checked || currentChecked ? ' class="selected"' : '';
                var selector = type === 'current'
                    ? '<input type="radio" name="nodePick" value="' + node.nodeId + '"' + currentChecked + '>'
                    : '<input type="checkbox" class="dependency-pick" value="' + node.nodeId + '"' + checked + '>';
                return '<tr data-node-id="' + node.nodeId + '"' + selectedClass + '><td width="48">' + selector + '</td><td>' + escapeHtml(node.nodeName) + '</td><td><span class="node-id">' + node.nodeId + '</span></td></tr>';
            }).join('');
        }

        function syncSelectedDependenciesFromTable() {
            selectedDependencyNodes = [];
            if (!selectedCurrentNode) {
                renderPickerText();
                return;
            }
            $('#dependencyList tr').each(function() {
                var currentNodeId = $(this).find('td:first').data('node-id');
                var dependencyNodeId = $(this).find('td:nth-child(2)').data('node-id');
                if (currentNodeId === selectedCurrentNode.nodeId && dependencyNodeId) {
                    var dependencyNode = findNode(dependencyNodeId);
                    if (dependencyNode) {
                        selectedDependencyNodes.push(dependencyNode);
                    }
                }
            });
            renderPickerText();
        }

        window.openNodePicker = function(type) {
            var title = type === 'current' ? '选择当前节点' : '选择依赖节点';
            var content = '<div class="node-picker-modal">' +
                '<div class="node-picker-toolbar"><input type="text" class="layui-input" id="nodePickerKeyword" placeholder="输入节点名称或ID搜索"><button class="layui-btn layui-btn-sm" id="nodePickerSearch">查询</button></div>' +
                '<div class="node-picker-table-wrap"><table class="layui-table" lay-skin="line"><thead><tr><th width="48">选择</th><th>节点名称</th><th>节点ID</th></tr></thead><tbody id="nodePickerBody">' + renderNodeRows(type, '') + '</tbody></table></div>' +
                '</div>';
            layer.open({
                type: 1,
                title: title,
                area: ['760px', '600px'],
                content: content,
                btn: ['确定', '取消'],
                success: function(layero) {
                    layero.on('click', '#nodePickerSearch', function() {
                        $('#nodePickerBody').html(renderNodeRows(type, $('#nodePickerKeyword').val()));
                    });
                    layero.on('keyup', '#nodePickerKeyword', function(event) {
                        if (event.keyCode === 13) {
                            $('#nodePickerBody').html(renderNodeRows(type, $('#nodePickerKeyword').val()));
                        }
                    });
                    layero.on('click', 'tbody tr[data-node-id]', function(event) {
                        if ($(event.target).is('input')) {
                            $(this).toggleClass('selected', $(event.target).is(':checked'));
                            return;
                        }
                        var input = $(this).find('input');
                        if (type === 'current') {
                            layero.find('tbody tr').removeClass('selected');
                            input.prop('checked', true);
                            $(this).addClass('selected');
                        } else {
                            input.prop('checked', !input.prop('checked'));
                            $(this).toggleClass('selected', input.prop('checked'));
                        }
                    });
                    layero.on('dblclick', 'tbody tr[data-node-id]', function() {
                        if (type === 'current') {
                            selectedCurrentNode = findNode($(this).data('node-id'));
                            selectedDependencyNodes = selectedDependencyNodes.filter(function(node) { return node.nodeId !== selectedCurrentNode.nodeId; });
                            syncSelectedDependenciesFromTable();
                            renderPickerText();
                            layer.closeAll('page');
                        }
                    });
                },
                yes: function(index, layero) {
                    if (type === 'current') {
                        var currentNodeId = layero.find('input[name="nodePick"]:checked').val();
                        if (!currentNodeId) {
                            layer.msg('请选择当前节点');
                            return;
                        }
                        selectedCurrentNode = findNode(currentNodeId);
                        selectedDependencyNodes = selectedDependencyNodes.filter(function(node) { return node.nodeId !== selectedCurrentNode.nodeId; });
                        syncSelectedDependenciesFromTable();
                    } else {
                        var selected = [];
                        layero.find('.dependency-pick:checked').each(function() {
                            selected.push(findNode($(this).val()));
                        });
                        selectedDependencyNodes = selected.filter(Boolean);
                    }
                    renderPickerText();
                    layer.close(index);
                }
            });
        };

        function findNode(nodeId) {
            for (var index = 0; index < nodeList.length; index++) {
                if (nodeList[index].nodeId === String(nodeId)) {
                    return nodeList[index];
                }
            }
            return null;
        }

        window.addDependencies = function (event) {
            if (event) {
                event.preventDefault();
            }

            if (!selectedCurrentNode || selectedDependencyNodes.length === 0) {
                layer.msg('请选择完整的节点信息');
                return;
            }
            var addedCount = 0;
            selectedDependencyNodes.forEach(function(dependencyNode) {
                if (selectedCurrentNode.nodeId !== dependencyNode.nodeId && updateDependencyTable(selectedCurrentNode.nodeId, selectedCurrentNode.nodeName, dependencyNode.nodeId, dependencyNode.nodeName, false)) {
                    addedCount++;
                }
            });
            layer.msg(addedCount > 0 ? '已添加 ' + addedCount + ' 个依赖关系' : '没有新增依赖关系');
        };

        window.updateDependencyTable = function (currentNodeId, currentNodeName, dependencyNodeId, dependencyNodeName, showMessage) {
            var relationKey = currentNodeId + '_' + dependencyNodeId;
            if ($('#dependencyList tr[data-relation-key="' + relationKey + '"]').length > 0) {
                if (showMessage !== false) {
                    layer.msg('该依赖关系已存在');
                }
                return false;
            }
            $('#dependencyList .empty-row').remove();

            var newRow = '<tr data-relation-key="' + relationKey + '">' +
                '<td data-node-id="' + currentNodeId + '">' + escapeHtml(currentNodeName) + '<span class="node-id">' + currentNodeId + '</span></td>' +
                '<td data-node-id="' + dependencyNodeId + '">' + escapeHtml(dependencyNodeName) + '<span class="node-id">' + dependencyNodeId + '</span></td>' +
                '<td><button class="layui-btn layui-btn-danger layui-btn-sm" onclick="deleteDependency(this)"><i class="layui-icon layui-icon-delete"></i>删除</button></td>' +
                '</tr>';

            $('#dependencyList').append(newRow);
            filterDependencyList();
            if (showMessage !== false) {
                layer.msg('依赖关系已添加');
            }
            return true;
        };

        window.deleteDependency = function (button) {
            $(button).closest('tr').remove();
            if ($('#dependencyList tr').length === 0) {
                $('#dependencyList').html(emptyRow());
            }
            layer.msg('依赖关系已删除');
        };

        window.resetDependencyList = function() {
            $('#dependencyList').html(emptyRow());
            selectedCurrentNode = null;
            selectedDependencyNodes = [];
            renderPickerText();
            layer.msg('依赖关系列表已清空');
        };

        window.filterDependencyList = function() {
            var keyword = $.trim($('#relationNodeName').val()).toLowerCase();
            $('#dependencyList tr').each(function() {
                var row = $(this);
                if (row.hasClass('empty-row')) {
                    return;
                }
                var text = row.text().toLowerCase();
                row.toggle(!keyword || text.indexOf(keyword) >= 0);
            });
        };

        window.clearDependencyFilter = function() {
            $('#relationNodeName').val('');
            filterDependencyList();
        };

        $('#relationNodeName').on('keyup', function(event) {
            if (event.keyCode === 13) {
                filterDependencyList();
            }
        });

        renderPickerText();

        window.submitDependencies = function() {
            var nodeRelationList = [];
            $('#dependencyList tr').each(function() {
                var nodeId1 = $(this).find('td:first').data('node-id');
                var nodeId2 = $(this).find('td:nth-child(2)').data('node-id');
                if (!nodeId1 || !nodeId2) {
                    return;
                }
                nodeRelationList.push({
                    nodeId1: nodeId1,
                    nodeId2: nodeId2
                });
            });

            $.ajax({
                url: '${request.contextPath}/work/edit',
                type: 'POST',
                data: JSON.stringify({
                    workId: workId,
                    nodeRelationList: nodeRelationList
                }),
                contentType: 'application/json',
                success: function(response) {
                    if (response.code === 200) {
                        layer.msg('依赖关系保存成功', {icon: 1});
                    } else {
                        layer.msg(response.msg || '依赖关系保存失败', {icon: 2});
                    }
                },
                error: function() {
                    layer.msg('依赖关系保存失败', {icon: 2});
                }
            });
        };
    });
</script>
</body>
</html>
