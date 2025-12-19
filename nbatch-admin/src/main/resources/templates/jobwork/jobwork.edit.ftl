<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>节点依赖关系设置</title>
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>
<body>
<style>

    .button-group {
        text-align: center;
        margin-top: 15px;
    }

    .button-group .layui-btn {
        margin-right: 10px;
    }

    .dependency-table {
        margin-top: 20px;
    }

    .empty-tip {
        text-align: center;
        color: #999;
        padding: 20px;
    }

    .action-buttons .layui-btn {
        margin-right: 5px;
    }

    .custom-select {
        width: 100%;
        height: 38px;
        line-height: 38px;
        padding: 0 10px;
        border: 1px solid #e6e6e6;
        border-radius: 4px;
        background-color: #fff;
        font-size: 14px;
        transition: border-color 0.3s ease;
    }

    .custom-select:focus {
        border-color: #1E9FFF;
        outline: none;
        box-shadow: 0 0 0 2px rgba(30, 159, 255, 0.1);
    }

    .custom-select:hover {
        border-color: #c9c9c9;
    }

    .node-config-section {
        margin-bottom: 20px;
        padding: 20px;
        background-color: #f9f9f9;
        border-radius: 6px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }

    .config-item {
        display: flex;
        align-items: center;
        margin-bottom: 20px;
        padding: 10px 0;
    }

    .config-label {
        width: 100px;
        margin-right: 20px;
        font-weight: bold;
        color: #333;
        font-size: 14px;
    }

    .config-input {
        flex: 1;
        max-width: 220px;
    }


</style>

<div class="layui-container" style="padding: 20px;">
    <fieldset class="layui-elem-field layui-field-title">
        <legend>节点依赖关系配置</legend>
    </fieldset>

    <div class="node-config-section">
        <div class="config-item">
            <label class="config-label">当前节点</label>
            <div class="config-input">
                <select name="currentNodeId" class="custom-select" onchange="updateDependencyOptions(this.value)">
                    <option value="">请选择节点</option>
                    <#list list as node>
                        <option value="${node.nodeId}">${node.nodeName}</option>
                    </#list>
                </select>
            </div>
        </div>

        <div class="config-item">
            <label class="config-label">依赖节点</label>
            <div class="config-input">
                <select name="dependencyIds" class="custom-select">
                    <option value="">请选择节点</option>
                    <#list list as node>
                        <option value="${node.nodeId}">${node.nodeName}</option>
                    </#list>
                </select>
            </div>
        </div>
    </div>

    <div class="button-group">
        <button class="layui-btn" onclick="addDependencies()">
            <i class="layui-icon layui-icon-add-1"></i>添加依赖关系
        </button>
    </div>


    <!-- 依赖关系展示区域 -->
    <div class="layui-card dependency-table">
        <div class="layui-card-header">已配置的依赖关系</div>
        <div class="layui-card-body">
            <table class="layui-table" lay-skin="line">
                <thead>
                <tr>
                    <th>节点名称</th>
                    <th>依赖节点</th>
                    <th width="150">操作</th>
                </tr>
                </thead>
                <tbody id="dependencyList">
                <#if relationList?? && relationList?size gt 0>
                    <#list relationList as relation>
                        <tr>
                            <td data-node-id="${relation.nodeId1}">${relation.nodeName1}</td>
                            <td data-node-id="${relation.nodeId2}">${relation.nodeName2}</td>
                            <td>
                                <button class="layui-btn layui-btn-sm" onclick="deleteDependency(this)">
                                    <i class="layui-icon layui-icon-delete"></i>删除
                                </button>
                            </td>
                        </tr>
                    </#list>
                <#else>
                    <tr>
                        <td colspan="3">
                            <div class="empty-tip">暂无配置的依赖关系</div>
                        </td>
                    </tr>
                </#if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="button-group">
        <button class="layui-btn" onclick="submitDependencies()">
            <i class="layui-icon layui-icon-ok"></i>提交依赖关系
        </button>
        <button class="layui-btn layui-btn-primary" onclick="resetDependencyList()">
            <i class="layui-icon layui-icon-refresh"></i>重置
        </button>
    </div>
</div>


<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>
<script>

    layui.use(['form', 'layer', 'jquery'], function () {
        var layer = layui.layer;
        var $ = layui.jquery;
        var nodeList = []; // 存储节点列表

        // 初始化节点列表
        <#list list as node>
            nodeList.push({
                nodeId: '${node.nodeId}',
                nodeName: '${node.nodeName}'
            });
        </#list>

        var workId = '${workId}';

        // 更新依赖节点选项
        window.updateDependencyOptions = function (selectedNodeId) {
            let dependencySelect = $('select[name="dependencyIds"]');
            dependencySelect.empty(); // 清空现有选项

            // 重新填充选项，排除已选中的当前节点
            nodeList.forEach(function(node) {
                if (node.nodeId !== selectedNodeId) {
                    dependencySelect.append(`<option value="` + node.nodeId + `">` + node.nodeName + `</option>`);
                }
            });
            // 重新渲染表单
            layui.form.render('select');
        }

        // 将普通函数定义改为全局属性赋值
        window.addDependencies = function (event) {
            if (event) {
                event.preventDefault();
            }

            let currentNodeSelect = $('select[name="currentNodeId"]');
            let currentNodeId = currentNodeSelect.val();
            let currentNodeName = currentNodeSelect.find("option:selected").text();

            let dependencyNodeSelect = $('select[name="dependencyIds"]');
            let dependencyNodeId = dependencyNodeSelect.val();
            let dependencyNodeName = dependencyNodeSelect.find("option:selected").text();

            if (!currentNodeId || !dependencyNodeId) {
                layer.msg('请选择完整的节点信息');
                return;
            }

            updateDependencyTable(currentNodeId, currentNodeName, dependencyNodeId, dependencyNodeName);
        };

        window.updateDependencyTable = function (currentNodeId, currentNodeName, dependencyNodeId, dependencyNodeName) {
            // 移除空状态提示行（如果存在）
            if ($('#dependencyList .empty-tip').length > 0) {
                $('#dependencyList').empty();
            }

            let newRow = `
            <tr>
                <td  data-node-id="` + currentNodeId + `">` + currentNodeName + `</td>
                <td  data-node-id="` + dependencyNodeId + `">` + dependencyNodeName + `</td>
                <td>
                    <button class="layui-btn layui-btn-sm" onclick="deleteDependency(this)">
                        <i class="layui-icon layui-icon-delete"></i>删除
                    </button>
                </td>
            </tr>`;

            let exists = false;
            $('#dependencyList tr').each(function () {
                let nodeName = $(this).find('td:first').text();
                if (nodeName === currentNodeName) {
                    $(this).replaceWith(newRow);
                    exists = true;
                    return false;
                }
            });

            if (!exists) {
                $('#dependencyList').append(newRow);
            }

            layer.msg('依赖关系已添加到表格');
        };

        // 删除依赖关系
        window.deleteDependency = function (button) {
            $(button).closest('tr').remove();
            layer.msg('依赖关系已删除');
        };

        // 在 resetDependencyList 函数中添加空状态恢复
        window.resetDependencyList = function() {
            // 清空依赖关系列表
            $('#dependencyList').html('<tr><td colspan="3"><div class="empty-tip">暂无配置的依赖关系</div></td></tr>');

            // 重置选择框
            $('select[name="currentNodeId"]').val('');
            $('select[name="dependencyIds"]').val('');

            // 重新初始化依赖节点选项
            updateDependencyOptions('');

            // 显示提示信息
            layer.msg('依赖关系列表已重置');
        };

        // 提交依赖关系
        window.submitDependencies = function() {
            // 或者在遍历表格行时获取
            var nodeRelationList = []; // 存储节点列表
            $('#dependencyList tr').each(function() {
                let nodeId1 = $(this).find('td:first').data('node-id');
                let nodeId2 = $(this).find('td:nth-child(2)').data('node-id');
                if (!nodeId1 || !nodeId2) {
                    return;
                }
                nodeRelationList.push({
                    nodeId1: nodeId1,
                    nodeId2: nodeId2,
                })
            });
            var workNodeRelationParam = {
                workId: workId,
                nodeRelationList: nodeRelationList
            };
            $.ajax({
                url: '${request.contextPath}/work/edit',
                type: 'POST',
                data: JSON.stringify(workNodeRelationParam),
                contentType: 'application/json',
                success: function(response) {
                    if (response.code === 200) {
                        layer.msg('依赖关系提交成功');
                    } else {
                        layer.msg('依赖关系提交失败：' + response.message);
                    }
                },
                error: function(xhr, status, error) {}
            })
        };
    });

</script>

</body>
</html>
