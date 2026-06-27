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
    .flow-preview-modal {padding: 16px; background: #f8fafc;}
    .flow-preview-toolbar {display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px;}
    .flow-preview-title {font-size: 15px; font-weight: 600; color: #1f2937;}
    .flow-preview-desc {color: #6b7280; font-size: 12px; line-height: 20px;}
    .flow-preview-actions {display: flex; align-items: center; gap: 8px; flex-wrap: wrap;}
    .flow-preview-canvas {height: 560px; overflow: hidden; position: relative; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; cursor: grab;}
    .flow-preview-canvas.dragging {cursor: grabbing;}
    .flow-preview-empty {padding: 80px 16px; text-align: center; color: #999;}
    .flow-warning {padding: 8px 12px; margin-bottom: 10px; color: #9a3412; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 4px;}
    .flow-node {cursor: pointer;}
    .flow-node.dimmed, .flow-edge.dimmed {opacity: .12;}
    .flow-node.focused rect {stroke: #f97316; stroke-width: 3;}
    .flow-edge.focused {stroke: #f97316; stroke-width: 3;}
    .flow-context-menu {display: none; position: absolute; z-index: 10; width: 420px; max-height: 420px; overflow: auto; background: #fff; border: 1px solid #dbe3ef; border-radius: 8px; box-shadow: 0 12px 32px rgba(15, 23, 42, .18);}
    .flow-context-section {padding: 10px 12px; border-bottom: 1px solid #edf2f7;}
    .flow-context-section:last-child {border-bottom: 0;}
    .flow-context-title {font-weight: 600; color: #334155; margin-bottom: 6px;}
    .flow-context-item {display: block; padding: 6px 0; color: #2563eb; cursor: pointer; word-break: break-word; line-height: 20px;}
    .flow-context-empty {color: #94a3b8; font-size: 12px;}
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
                <button id="flowPreviewBtn" type="button" class="layui-btn layui-btn-normal layui-btn-sm">
                    <i class="layui-icon layui-icon-chart"></i>流程预览
                </button>
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
            nodeId: '<#if node.nodeId??>${node.nodeId?js_string}</#if>',
            nodeName: '<#if node.nodeName??>${node.nodeName?js_string}</#if>',
            nodeDesc: '<#if node.nodeDesc??>${node.nodeDesc?js_string}</#if>',
            nodeStatus: '<#if node.nodeStatus??>${node.nodeStatus}</#if>',
            nodeType: '<#if node.nodeType??>${node.nodeType?js_string}</#if>',
            dbType: '<#if node.dbType??>${node.dbType?js_string}</#if>',
            executeHandler: '<#if node.executeHandler??>${node.executeHandler?js_string}</#if>',
            executeContent: '<#if node.executeContent??>${node.executeContent?js_string}</#if>',
            executeContentParam: '<#if node.executeContentParam??>${node.executeContentParam?js_string}</#if>',
            scriptType: '<#if node.scriptType??>${node.scriptType?js_string}</#if>',
            errorStrategy: '<#if node.errorStrategy??>${node.errorStrategy?js_string}</#if>',
            retryTimes: '<#if node.retryTimes??>${node.retryTimes}</#if>'
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

        function valueOrDash(value) {
            return value === null || value === undefined || value === '' ? '-' : value;
        }

        function shortText(value, maxLength) {
            value = value === null || value === undefined ? '' : String(value);
            return value.length > maxLength ? value.substring(0, maxLength - 1) + '…' : value;
        }

        function splitTextByLength(value, lineLength, maxLines) {
            value = value === null || value === undefined ? '' : String(value);
            var lines = [];
            var maxLength = lineLength * maxLines;
            value = value.substring(0, maxLength);
            for (var index = 0; index < value.length; index += lineLength) {
                lines.push(value.substring(index, index + lineLength));
            }
            while (lines.length < maxLines) {
                lines.push('');
            }
            return lines.length > 0 ? lines : [''];
        }

        function collectDependencies() {
            var relationList = [];
            $('#dependencyList tr').each(function() {
                var nodeId1 = $(this).find('td:first').data('node-id');
                var nodeId2 = $(this).find('td:nth-child(2)').data('node-id');
                if (!nodeId1 || !nodeId2) {
                    return;
                }
                relationList.push({
                    currentNodeId: String(nodeId1),
                    dependencyNodeId: String(nodeId2)
                });
            });
            return relationList;
        }

        function buildRelationIndex(relationList) {
            var upstreamMap = {};
            var downstreamMap = {};
            nodeList.forEach(function(node) {
                upstreamMap[node.nodeId] = [];
                downstreamMap[node.nodeId] = [];
            });
            relationList.forEach(function(relation) {
                if (!upstreamMap[relation.currentNodeId]) {
                    upstreamMap[relation.currentNodeId] = [];
                }
                if (!downstreamMap[relation.dependencyNodeId]) {
                    downstreamMap[relation.dependencyNodeId] = [];
                }
                upstreamMap[relation.currentNodeId].push(relation.dependencyNodeId);
                downstreamMap[relation.dependencyNodeId].push(relation.currentNodeId);
            });
            return {
                upstreamMap: upstreamMap,
                downstreamMap: downstreamMap
            };
        }

        function calculateFlowLayout(relationList) {
            var nodeMap = {};
            var inDegree = {};
            var nextMap = {};
            var levelMap = {};
            var hasCycle = false;

            nodeList.forEach(function(node) {
                nodeMap[node.nodeId] = node;
                inDegree[node.nodeId] = 0;
                nextMap[node.nodeId] = [];
                levelMap[node.nodeId] = 0;
            });

            relationList.forEach(function(relation) {
                if (!nodeMap[relation.currentNodeId] || !nodeMap[relation.dependencyNodeId]) {
                    return;
                }
                nextMap[relation.dependencyNodeId].push(relation.currentNodeId);
                inDegree[relation.currentNodeId] = (inDegree[relation.currentNodeId] || 0) + 1;
            });

            var queue = [];
            Object.keys(inDegree).forEach(function(nodeId) {
                if (inDegree[nodeId] === 0) {
                    queue.push(nodeId);
                }
            });

            var visitedCount = 0;
            while (queue.length > 0) {
                var currentNodeId = queue.shift();
                visitedCount++;
                nextMap[currentNodeId].forEach(function(nextNodeId) {
                    levelMap[nextNodeId] = Math.max(levelMap[nextNodeId] || 0, (levelMap[currentNodeId] || 0) + 1);
                    inDegree[nextNodeId]--;
                    if (inDegree[nextNodeId] === 0) {
                        queue.push(nextNodeId);
                    }
                });
            }

            if (visitedCount < nodeList.length) {
                hasCycle = true;
            }

            var levels = {};
            nodeList.forEach(function(node) {
                var level = levelMap[node.nodeId] || 0;
                if (!levels[level]) {
                    levels[level] = [];
                }
                levels[level].push(node);
            });

            return {
                levels: levels,
                hasCycle: hasCycle
            };
        }

        function renderContextItems(nodeIds) {
            if (!nodeIds || nodeIds.length === 0) {
                return '<div class="flow-context-empty">暂无节点</div>';
            }
            return nodeIds.map(function(nodeId) {
                var node = findNode(nodeId);
                if (!node) {
                    return '';
                }
                return '<a class="flow-context-item" data-node-id="' + escapeHtml(node.nodeId) + '">' +
                    escapeHtml(node.nodeName) +
                    '<span class="node-id">' + escapeHtml(shortText(node.nodeId, 18)) + '</span></a>';
            }).join('');
        }

        function renderFlowSvg(relationList) {
            if (nodeList.length === 0) {
                return '<div class="flow-preview-empty">当前作业没有节点，无法生成流程图</div>';
            }

            var layout = calculateFlowLayout(relationList);
            var relationIndex = buildRelationIndex(relationList);
            var nodeWidth = 190;
            var nodeHeight = 100;
            var columnGap = 110;
            var rowGap = 46;
            var margin = 36;
            var maxLevel = 0;
            var maxColumnHeight = 0;
            var nodePositionMap = {};

            Object.keys(layout.levels).forEach(function(levelKey) {
                var level = parseInt(levelKey, 10);
                maxLevel = Math.max(maxLevel, level);
            });

            Object.keys(layout.levels).forEach(function(levelKey) {
                var level = parseInt(levelKey, 10);
                var nextY = margin;
                layout.levels[levelKey].forEach(function(node, index) {
                    nodePositionMap[node.nodeId] = {
                        x: margin + level * (nodeWidth + columnGap),
                        y: nextY
                    };
                    nextY += nodeHeight + rowGap;
                });
                maxColumnHeight = Math.max(maxColumnHeight, nextY - rowGap);
            });

            var width = Math.max(760, margin * 2 + (maxLevel + 1) * nodeWidth + maxLevel * columnGap);
            var height = Math.max(360, maxColumnHeight + margin);
            var edgeSvg = relationList.map(function(relation) {
                var from = nodePositionMap[relation.dependencyNodeId];
                var to = nodePositionMap[relation.currentNodeId];
                if (!from || !to) {
                    return '';
                }
                var startX = from.x + nodeWidth;
                var startY = from.y + nodeHeight / 2;
                var endX = to.x;
                var endY = to.y + nodeHeight / 2;
                var middleX = Math.max(startX + 36, startX + (endX - startX) / 2);
                var path = 'M' + startX + ',' + startY + ' C' + middleX + ',' + startY + ' ' + middleX + ',' + endY + ' ' + endX + ',' + endY;
                return '<path class="flow-edge" data-from="' + escapeHtml(relation.dependencyNodeId) + '" data-to="' + escapeHtml(relation.currentNodeId) + '" d="' + path + '" fill="none" stroke="#60a5fa" stroke-width="2" marker-end="url(#arrowHead)"></path>';
            }).join('');

            var nodeSvg = nodeList.map(function(node) {
                var position = nodePositionMap[node.nodeId] || {x: margin, y: margin};
                var titleLines = splitTextByLength(node.nodeName, 16, 3);
                var titleSvg = titleLines.map(function(line, index) {
                    return '<text class="flow-node-title" x="14" y="' + (22 + index * 17) + '" fill="#1e3a8a" font-size="13" font-weight="600">' + escapeHtml(line) + '</text>';
                }).join('');
                var detailStartY = 78;
                var subTitle = escapeHtml(shortText(node.nodeType || 'node', 18));
                var nodeId = escapeHtml(shortText(node.nodeId, 22));
                return '<g class="flow-node" data-node-id="' + escapeHtml(node.nodeId) + '" transform="translate(' + position.x + ',' + position.y + ')">' +
                    '<rect width="' + nodeWidth + '" height="' + nodeHeight + '" rx="10" fill="#eff6ff" stroke="#3b82f6" stroke-width="1.5"></rect>' +
                    titleSvg +
                    '<text class="flow-node-subtitle" x="14" y="' + detailStartY + '" fill="#475569" font-size="11">' + subTitle + '</text>' +
                    '<text class="flow-node-id" x="14" y="' + (detailStartY + 15) + '" fill="#94a3b8" font-size="10">' + nodeId + '</text>' +
                    '</g>';
            }).join('');

            var warning = layout.hasCycle
                ? '<div class="flow-warning">检测到循环依赖，流程图已按现有节点尽可能展示，请检查是否存在 A->B->A 类型关系。</div>'
                : '';
            var noRelationTip = relationList.length === 0
                ? '<div class="flow-warning">当前未配置依赖关系，所有节点将作为起始节点并行展示。</div>'
                : '';

            return warning + noRelationTip +
                '<div class="flow-preview-canvas" id="flowPreviewCanvas" data-scale="1" data-offset-x="0" data-offset-y="0">' +
                '<svg id="flowPreviewSvg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" xmlns="http://www.w3.org/2000/svg">' +
                '<defs><marker id="arrowHead" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8 Z" fill="#60a5fa"></path></marker></defs>' +
                '<g id="flowViewport">' + edgeSvg + nodeSvg + '</g>' +
                '</svg>' +
                '<div class="flow-context-menu" id="flowContextMenu" data-upstream="' + escapeHtml(JSON.stringify(relationIndex.upstreamMap)) + '" data-downstream="' + escapeHtml(JSON.stringify(relationIndex.downstreamMap)) + '"></div>' +
                '</div>';
        }

        function setFlowTransform(scale, offsetX, offsetY) {
            scale = Math.max(0.35, Math.min(2.5, scale));
            var boundedOffset = limitFlowOffset(scale, offsetX, offsetY);
            offsetX = boundedOffset.offsetX;
            offsetY = boundedOffset.offsetY;
            $('#flowPreviewCanvas').data('scale', scale).data('offset-x', offsetX).data('offset-y', offsetY);
            $('#flowViewport').attr('transform', 'translate(' + offsetX + ' ' + offsetY + ') scale(' + scale + ')');
            $('.flow-node-title').attr('font-size', Math.max(8 / scale, 13));
            $('.flow-node-subtitle').attr('font-size', Math.max(8 / scale, 11));
            $('.flow-node-id').attr('font-size', Math.max(7 / scale, 10));
            $('#flowZoomText').text(Math.round(scale * 100) + '%');
        }

        function limitFlowOffset(scale, offsetX, offsetY) {
            var canvas = $('#flowPreviewCanvas');
            var svg = $('#flowPreviewSvg');
            if (!canvas.length || !svg.length) {
                return {offsetX: offsetX, offsetY: offsetY};
            }
            var padding = 120;
            var contentWidth = Number(svg.attr('width')) * scale;
            var contentHeight = Number(svg.attr('height')) * scale;
            var canvasWidth = canvas.width();
            var canvasHeight = canvas.height();
            var minOffsetX = contentWidth > canvasWidth
                ? canvasWidth - contentWidth - padding
                : -padding;
            var maxOffsetX = contentWidth > canvasWidth
                ? padding
                : canvasWidth - contentWidth + padding;
            var minOffsetY = contentHeight > canvasHeight
                ? canvasHeight - contentHeight - padding
                : -padding;
            var maxOffsetY = contentHeight > canvasHeight
                ? padding
                : canvasHeight - contentHeight + padding;
            return {
                offsetX: Math.max(minOffsetX, Math.min(maxOffsetX, offsetX)),
                offsetY: Math.max(minOffsetY, Math.min(maxOffsetY, offsetY))
            };
        }

        function focusFlowNode(nodeId) {
            var relatedMap = {};
            relatedMap[nodeId] = true;
            $('.flow-edge').each(function() {
                var from = $(this).data('from');
                var to = $(this).data('to');
                if (from === nodeId || to === nodeId) {
                    relatedMap[from] = true;
                    relatedMap[to] = true;
                    $(this).removeClass('dimmed').addClass('focused');
                } else {
                    $(this).removeClass('focused').addClass('dimmed');
                }
            });
            $('.flow-node').each(function() {
                var currentNodeId = $(this).data('node-id');
                $(this).toggleClass('focused', currentNodeId === nodeId);
                $(this).toggleClass('dimmed', !relatedMap[currentNodeId]);
            });
        }

        function clearFlowFocus() {
            $('.flow-node,.flow-edge').removeClass('focused dimmed');
            $('#flowContextMenu').hide();
        }

        function centerFlowNode(nodeId) {
            var canvas = $('#flowPreviewCanvas');
            var svg = $('#flowPreviewSvg');
            var node = $('.flow-node[data-node-id="' + nodeId + '"]');
            if (!canvas.length || !svg.length || !node.length) {
                return;
            }
            var scale = Number(canvas.data('scale')) || 1;
            var box = node[0].getBBox();
            var offsetX = canvas.width() / 2 - (box.x + box.width / 2) * scale;
            var offsetY = canvas.height() / 2 - (box.y + box.height / 2) * scale;
            setFlowTransform(scale, offsetX, offsetY);
            focusFlowNode(nodeId);
        }

        function positionFlowContextMenu(event) {
            var canvas = $('#flowPreviewCanvas');
            var menu = $('#flowContextMenu');
            var canvasOffset = canvas.offset();
            var left = event.pageX - canvasOffset.left + 8;
            var top = event.pageY - canvasOffset.top + 8;
            var maxLeft = Math.max(8, canvas.width() - menu.outerWidth() - 8);
            var maxTop = Math.max(8, canvas.height() - menu.outerHeight() - 8);
            menu.css({
                left: Math.min(left, maxLeft),
                top: Math.min(top, maxTop)
            });
        }

        function bindFlowPreviewEvents() {
            var canvas = $('#flowPreviewCanvas');
            if (!canvas.length) {
                return;
            }
            $(document).off('.flowPreview');
            $('#flowZoomIn,#flowZoomOut,#flowZoomReset').off('click');
            setFlowTransform(1, 0, 0);

            canvas.on('wheel', function(event) {
                event.preventDefault();
                var originalEvent = event.originalEvent;
                var scale = Number(canvas.data('scale')) || 1;
                var offsetX = Number(canvas.data('offset-x')) || 0;
                var offsetY = Number(canvas.data('offset-y')) || 0;
                var delta = originalEvent.deltaY < 0 ? 1.1 : 0.9;
                setFlowTransform(scale * delta, offsetX, offsetY);
            });

            var dragging = false;
            var lastPoint = null;
            canvas.on('mousedown', function(event) {
                if ($(event.target).closest('.flow-node').length || event.which !== 1) {
                    return;
                }
                dragging = true;
                lastPoint = {x: event.pageX, y: event.pageY};
                canvas.addClass('dragging');
            });
            $(document).on('mousemove.flowPreview', function(event) {
                if (!dragging || !lastPoint) {
                    return;
                }
                var offsetX = Number(canvas.data('offset-x')) || 0;
                var offsetY = Number(canvas.data('offset-y')) || 0;
                setFlowTransform(Number(canvas.data('scale')) || 1, offsetX + event.pageX - lastPoint.x, offsetY + event.pageY - lastPoint.y);
                lastPoint = {x: event.pageX, y: event.pageY};
            }).on('mouseup.flowPreview', function() {
                dragging = false;
                lastPoint = null;
                canvas.removeClass('dragging');
            });

            canvas.on('contextmenu', '.flow-node', function(event) {
                event.preventDefault();
                event.stopPropagation();
                var nodeId = String($(this).data('node-id'));
                var upstreamMap = JSON.parse($('#flowContextMenu').attr('data-upstream') || '{}');
                var downstreamMap = JSON.parse($('#flowContextMenu').attr('data-downstream') || '{}');
                focusFlowNode(nodeId);
                $('#flowContextMenu').html(
                    '<div class="flow-context-section"><div class="flow-context-title">它依赖的节点</div>' + renderContextItems(upstreamMap[nodeId]) + '</div>' +
                    '<div class="flow-context-section"><div class="flow-context-title">依赖它的节点</div>' + renderContextItems(downstreamMap[nodeId]) + '</div>'
                ).show();
                positionFlowContextMenu(event);
            });

            canvas.on('click', '.flow-context-item', function(event) {
                event.preventDefault();
                event.stopPropagation();
                centerFlowNode(String($(this).data('node-id')));
            });

            canvas.on('click', function() {
                clearFlowFocus();
            });

            $('#flowZoomIn').on('click', function() {
                setFlowTransform((Number(canvas.data('scale')) || 1) * 1.15, Number(canvas.data('offset-x')) || 0, Number(canvas.data('offset-y')) || 0);
            });
            $('#flowZoomOut').on('click', function() {
                setFlowTransform((Number(canvas.data('scale')) || 1) * 0.85, Number(canvas.data('offset-x')) || 0, Number(canvas.data('offset-y')) || 0);
            });
            $('#flowZoomReset').on('click', function() {
                setFlowTransform(1, 0, 0);
                clearFlowFocus();
            });
        }

        window.openFlowPreview = function() {
            var relationList = collectDependencies();
            var content = '<div class="flow-preview-modal">' +
                '<div class="flow-preview-toolbar">' +
                '<div><div class="flow-preview-title">节点依赖流程预览</div>' +
                '<div class="flow-preview-desc">箭头方向：依赖节点 → 当前节点。页面展示的是当前未保存或已修改的依赖关系。</div></div>' +
                '<div class="flow-preview-actions">' +
                '<button class="layui-btn layui-btn-primary layui-btn-sm" id="flowZoomIn">放大</button>' +
                '<button class="layui-btn layui-btn-primary layui-btn-sm" id="flowZoomOut">缩小</button>' +
                '<button class="layui-btn layui-btn-primary layui-btn-sm" id="flowZoomReset">重置</button>' +
                '<span id="flowZoomText" class="node-id">100%</span>' +
                '</div>' +
                '</div><div id="flowPreviewBody">' + renderFlowSvg(relationList) + '</div></div>';
            layer.open({
                type: 1,
                title: '流程预览',
                area: ['1080px', '720px'],
                maxmin: true,
                shadeClose: true,
                content: content,
                success: function() {
                    bindFlowPreviewEvents();
                },
                end: function() {
                    $(document).off('.flowPreview');
                }
            });
        };

        $('#flowPreviewBtn').off('click').on('click', function() {
            window.openFlowPreview();
        });

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
