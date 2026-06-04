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
    .custom-select {width: 100%; height: 38px; line-height: 38px; padding: 0 10px; border: 1px solid #dcdfe6; border-radius: 4px; background-color: #fff;}
    .custom-select:focus {border-color: #1E9FFF; outline: none; box-shadow: 0 0 0 2px rgba(30,159,255,.1);}
    .node-tip {margin-top: 6px; color: #999; font-size: 12px;}
    .dependency-table {background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden;}
    .table-header {display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #eef0f5;}
    .table-title {font-size: 15px; font-weight: 600;}
    .empty-tip {text-align: center; color: #999; padding: 26px;}
    .node-id {font-family: Menlo, Monaco, Consolas, monospace; color: #999; font-size: 12px; margin-left: 6px;}
    .button-group {text-align: right; padding: 14px 0 0;}
    .layui-table {margin: 0;}
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
                <select name="currentNodeId" class="custom-select" onchange="updateDependencyOptions(this.value)">
                    <option value="">请选择当前节点</option>
                    <#list list as node>
                        <option value="${node.nodeId}">${node.nodeName}</option>
                    </#list>
                </select>
                <div class="node-tip">需要等待依赖节点完成后再执行的节点</div>
            </div>
            <div>
                <div class="config-label">依赖节点</div>
                <select name="dependencyIds" class="custom-select">
                    <option value="">请选择依赖节点</option>
                    <#list list as node>
                        <option value="${node.nodeId}">${node.nodeName}</option>
                    </#list>
                </select>
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
            <div>
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
    layui.use(['form', 'layer', 'jquery'], function () {
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

        window.updateDependencyOptions = function (selectedNodeId) {
            var dependencySelect = $('select[name="dependencyIds"]');
            dependencySelect.empty();
            dependencySelect.append('<option value="">请选择依赖节点</option>');

            nodeList.forEach(function(node) {
                if (node.nodeId !== selectedNodeId) {
                    dependencySelect.append('<option value="' + node.nodeId + '">' + escapeHtml(node.nodeName) + '</option>');
                }
            });
        };

        window.addDependencies = function (event) {
            if (event) {
                event.preventDefault();
            }

            var currentNodeSelect = $('select[name="currentNodeId"]');
            var currentNodeId = currentNodeSelect.val();
            var currentNodeName = currentNodeSelect.find('option:selected').text();
            var dependencyNodeSelect = $('select[name="dependencyIds"]');
            var dependencyNodeId = dependencyNodeSelect.val();
            var dependencyNodeName = dependencyNodeSelect.find('option:selected').text();

            if (!currentNodeId || !dependencyNodeId) {
                layer.msg('请选择完整的节点信息');
                return;
            }
            if (currentNodeId === dependencyNodeId) {
                layer.msg('当前节点不能依赖自身');
                return;
            }

            updateDependencyTable(currentNodeId, currentNodeName, dependencyNodeId, dependencyNodeName);
        };

        window.updateDependencyTable = function (currentNodeId, currentNodeName, dependencyNodeId, dependencyNodeName) {
            var relationKey = currentNodeId + '_' + dependencyNodeId;
            if ($('#dependencyList tr[data-relation-key="' + relationKey + '"]').length > 0) {
                layer.msg('该依赖关系已存在');
                return;
            }
            $('#dependencyList .empty-row').remove();

            var newRow = '<tr data-relation-key="' + relationKey + '">' +
                '<td data-node-id="' + currentNodeId + '">' + escapeHtml(currentNodeName) + '<span class="node-id">' + currentNodeId + '</span></td>' +
                '<td data-node-id="' + dependencyNodeId + '">' + escapeHtml(dependencyNodeName) + '<span class="node-id">' + dependencyNodeId + '</span></td>' +
                '<td><button class="layui-btn layui-btn-danger layui-btn-sm" onclick="deleteDependency(this)"><i class="layui-icon layui-icon-delete"></i>删除</button></td>' +
                '</tr>';

            $('#dependencyList').append(newRow);
            layer.msg('依赖关系已添加');
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
            $('select[name="currentNodeId"]').val('');
            updateDependencyOptions('');
            layer.msg('依赖关系列表已清空');
        };

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
