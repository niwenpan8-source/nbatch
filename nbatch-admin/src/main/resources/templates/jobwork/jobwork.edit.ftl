<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>作业流程设计工具</title>
<#--    <script src="https://cdn.tailwindcss.com"></script>-->
    <link href="${request.contextPath}/static/plugins/layui/css/font-awesome.css" rel="stylesheet">
    <link href="${request.contextPath}/static/plugins/layui/css/layui.css" rel="stylesheet">
    <!-- jQuery -->
	<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
	<script src="${request.contextPath}/static/plugins/tailwindcss/tailwindcss.js"></script>
    <script src="${request.contextPath}/static/plugins/layui/layui.js"></script>

    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#3B82F6',
                        success: '#10B981',
                        warning: '#F59E0B',
                        danger: '#EF4444',
                        dark: '#1F2937'
                    }
                }
            }
        }
    </script>

    <style type="text/tailwindcss">
        @layer utilities {
            .node-shadow {
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            }
            .connection-line {
                stroke: #9CA3AF;
                stroke-width: 2;
                stroke-dasharray: 5,5;
            }
            .connection-line-active {
                stroke: #3B82F6;
                stroke-dasharray: none;
            }
            .node-hover {
                transition: all 0.2s ease;
            }
            .node-hover:hover {
                transform: translateY(-2px);
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
            }
        }
        .layui-form-label {
            width: 100px;
        }
        .layui-form select {
            display: block;
        }
    </style>
</head>
<body class="bg-gray-50 h-screen flex flex-col overflow-hidden">

    <!-- 顶部标题区域 (10%) -->
    <header class="h-[10%] bg-white border-b border-gray-200 flex items-center px-6 shadow-sm z-10">
        <div class="flex-1">
            <h1 class="text-[clamp(1.25rem,3vw,1.75rem)] font-bold text-dark flex items-center">
                <i class="fa fa-sitemap text-primary mr-3"></i>作业流程设计
            </h1>
            <p class="text-gray-500 text-sm mt-1">拖拽左侧节点到右侧工作区，构建您的作业流程</p>
        </div>
        <div class="flex items-center gap-4">
            <button id="save-flow" class="px-4 py-2 bg-primary text-white rounded-md hover:bg-primary/90 transition-colors flex items-center">
                <i class="fa fa-save mr-2"></i>保存流程
            </button>
            <button id="clear-flow" class="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors flex items-center">
                <i class="fa fa-trash mr-2"></i>清空
            </button>
        </div>
    </header>

    <!-- 主内容区域 -->
    <div class="flex flex-1 overflow-hidden">
        <!-- 左侧节点列表 (15%) -->
        <aside class="w-[15%] bg-white border-r border-gray-200 overflow-y-auto" id="nodes-panel">
            <div class="p-4 border-b border-gray-100">
                <h2 class="font-semibold text-gray-800">作业节点库</h2>
                <p class="text-xs text-gray-500 mt-1">拖拽节点到右侧工作区</p>
            </div>
                        <!-- 数据处理类节点 -->
            <div class="p-3">
                <!-- 在 HTML 元素中添加数据属性 -->
                <div id="jobWorkNodeTypeJsonStr" data-node-types="${jobWorkNodeTypeJsonStr}"></div>

                <#list list as jobWorkNodeTypeVo>
                    <h3 class="text-sm font-medium text-gray-600 mb-2 flex items-center">
                        <i class="fa fa-database text-success mr-2"></i>${jobWorkNodeTypeVo.nodeTypeName}
                    </h3>
                    <div class="space-y-2">
                        <#list jobWorkNodeTypeVo.jobWorkNodeList as jobWorkNode>
                            <div class="node-item p-2 bg-gray-100 rounded-md text-sm cursor-move node-hover" draggable="true" data-type="${jobWorkNode.nodeId}">
                                <i class="fa fa-download text-success mr-2"></i>${jobWorkNode.nodeName}
                            </div>
                        </#list>
                    </div>
                </#list>
            </div>

        </aside>

        <!-- 右侧工作区 (85%) -->
        <main class="flex-1 bg-gray-100 overflow-hidden flex flex-col" id="workspace-container">
            <!-- 工作区工具栏 -->
            <div class="bg-white p-2 border-b border-gray-200 flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <span class="text-sm text-gray-600">工作区</span>
                    <div class="h-4 w-px bg-gray-300"></div>
                    <button id="toggle-dependency" class="px-3 py-1 text-xs bg-primary/10 text-primary rounded hover:bg-primary/20 transition-colors">
                        <i class="fa fa-link mr-1"></i>设置依赖关系
                    </button>
                </div>
                <div class="flex items-center gap-2">
                    <span class="text-xs text-gray-500" id="nodes-count">0个节点</span>
                </div>
            </div>

            <!-- 节点关系设置面板 (默认隐藏) -->
            <div id="dependency-panel" class="hidden bg-white p-3 border-b border-gray-200 text-sm">
                <div class="flex items-center gap-4">
                    <p class="text-gray-700"><i class="fa fa-info-circle text-primary mr-1"></i>选择第一个节点，然后选择第二个节点设置关系</p>
                    <div class="flex gap-2 ml-4">
                        <button data-relation="depend" class="relation-btn px-3 py-1 bg-primary text-white rounded text-xs">依赖</button>
                        <button data-relation="mutex" class="relation-btn px-3 py-1 bg-danger text-white rounded text-xs">互斥</button>
                    </div>
                    <button id="cancel-relation" class="ml-auto px-3 py-1 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300">
                        取消
                    </button>
                </div>
            </div>

            <!-- 主要工作区 -->
            <div id="workspace" class="flex-1 overflow-auto p-6 relative">
                <!-- 节点容器 - 确保4个节点一行 -->
                <div id="nodes-container" class="min-h-full grid grid-cols-4 gap-6">
                    <!-- 节点将通过JS动态添加到这里 -->
                </div>

                <!-- 连接线将通过JS动态绘制到这个SVG上 -->
                <svg id="connections" class="absolute top-0 left-0 w-full h-full pointer-events-none"></svg>
            </div>
        </main>
    </div>

    <script>
        $(document).ready(function() {
            // 全局变量
            let nodeIdCounter = 1;
            let nodes = {}; // 存储所有节点信息
            let connections = []; // 存储所有连接关系
            let isSettingDependency = false;
            let firstNodeId = null;

            <!-- 在 JavaScript 中读取 -->
            console.log(123)
            // 使用方式
            const base64Data = $('#jobWorkNodeTypeJsonStr').data('node-types');
            const decodedData = base64DecodeUnicode(base64Data);
            const jobWorkNodeTypeList = JSON.parse(decodedData);

            // 节点类型配置
            // 将数组 jobWorkNodeTypeList 转换为 nodeTypes 对象格式
            const nodeTypes = {};

            // 遍历 jobWorkNodeTypeList 数组
            jobWorkNodeTypeList.forEach(category => {
                // 遍历每个分类中的节点列表
                category.jobWorkNodeList.forEach(node => {
                    // 将节点信息添加到 nodeTypes 对象中
                    nodeTypes[node.nodeId] = {
                        name: node.nodeName,
                        icon: 'fa-exchange', // 根据节点ID获取图标
                        color: 'bg-success' // 根据分类名称获取颜色
                    };
                });
            });

            // 初始化拖拽功能
            initDragAndDrop();

            // 初始化事件监听
            initEventListeners();

            // 更新节点计数显示
            updateNodesCount();

            // 初始化拖拽功能
            function initDragAndDrop() {
                // 左侧节点拖拽
                $('.node-item').on('dragstart', function(e) {
                    const nodeType = $(this).data('type');
                    e.originalEvent.dataTransfer.setData('node-type', nodeType);
                    $(this).addClass('opacity-50');
                });

                $('.node-item').on('dragend', function() {
                    $(this).removeClass('opacity-50');
                });

                // 工作区接收拖拽
                $('#workspace').on('dragover', function(e) {
                    e.preventDefault();
                    $(this).addClass('bg-gray-200/50');
                });

                $('#workspace').on('dragleave', function() {
                    $(this).removeClass('bg-gray-200/50');
                });

                $('#workspace').on('drop', function(e) {
                    e.preventDefault();
                    $(this).removeClass('bg-gray-200/50');

                    const nodeType = e.originalEvent.dataTransfer.getData('node-type');
                    if (nodeType) {
                        // 添加新节点（位置由grid布局自动管理）
                        addNode(nodeType);
                    }
                });
            }

            // 初始化事件监听
            function initEventListeners() {
                // 设置依赖关系按钮
                $('#toggle-dependency').on('click', function() {
                    isSettingDependency = !isSettingDependency;
                    $('#dependency-panel').toggle();
                    $(this).toggleClass('bg-primary text-white', isSettingDependency)
                           .toggleClass('bg-primary/10 text-primary', !isSettingDependency);

                    if (!isSettingDependency) {
                        resetDependencySetting();
                    }
                });

                // 取消依赖关系设置
                $('#cancel-relation').on('click', function() {
                    isSettingDependency = false;
                    $('#dependency-panel').hide();
                    $('#toggle-dependency').removeClass('bg-primary text-white')
                                           .addClass('bg-primary/10 text-primary');
                    resetDependencySetting();
                });

                // 依赖关系类型选择
                $('.relation-btn').on('click', function() {
                    if (firstNodeId) {
                        const relationType = $(this).data('relation');
                        setNodeRelation(relationType);
                    }
                });

                // 清空工作区
                $('#clear-flow').on('click', function() {
                    if (confirm('确定要清空所有节点和关系吗？')) {
                        $('#nodes-container').empty();
                        $('#connections').empty();
                        nodes = {};
                        connections = [];
                        nodeIdCounter = 1;
                        updateNodesCount();
                        resetDependencySetting();
                    }
                });

                // 保存流程
                $('#save-flow').on('click', function() {
                    const flowData = {
                        nodes: nodes,
                        connections: connections
                    };

                    console.log('保存流程数据:', flowData);
                    alert('流程已保存！（数据已输出到控制台）');
                });

                // 节点点击事件委托
                $('#nodes-container').on('click', '.work-node', function(e) {
                    const nodeId = $(this).data('id');

                    if (isSettingDependency) {
                        handleNodeSelectionForDependency(nodeId);
                        e.stopPropagation();
                    }
                });

                // 删除节点按钮事件委托
                $('#nodes-container').on('click', '.delete-node', function(e) {
                    const nodeId = $(this).closest('.work-node').data('id');
                    removeNode(nodeId);
                    e.stopPropagation();
                });

                // 点击工作区空白处取消节点选择
                $('#workspace').on('click', function() {
                    if (isSettingDependency) {
                        resetDependencySetting();
                    }
                });
            }

            // 添加新节点到工作区（由grid布局自动控制每行4个节点）
            function addNode(nodeType) {
                console.log('添加节点:', nodeType);
                const nodeId = 'node-' + nodeIdCounter++;
                const nodeConfig = nodeTypes[nodeType];

                // 创建节点元素
                const nodeElement = $(`
                    <div class="work-node w-[150px] h-[200px] rounded-lg shadow node-shadow node-hover transition-all"
                         data-id="` + nodeId + `">
                        <div class="h-1/3 ` + nodeConfig.color + ` text-white rounded-t-lg flex items-center justify-center">
                            <i class="fa ` + nodeConfig.icon + ` text-2xl"></i>
                        </div>
                        <div class="h-2/3 bg-white p-3 flex flex-col">
                            <h4 class="font-medium text-center text-gray-800 mb-2">` + nodeConfig.name + `</h4>
                            <p class="text-xs text-gray-500 text-center flex-1">ID: ` + nodeId + `</p>
                            <div class="flex justify-between mt-2">
                                <div class="setting-node text-primary text-center text-xs cursor-pointer hover:underline">
                                    <i class="fa fa-cog mr-1"></i>设置
                                </div>
                                <div class="delete-node text-danger text-center text-xs cursor-pointer hover:underline">
                                    <i class="fa fa-trash-o mr-1"></i>删除
                                </div>
                            </div>
                        </div>
                    </div>
                `);

                // 添加到工作区
                $('#nodes-container').append(nodeElement);

                // 获取节点位置信息（用于连接线）
                const position = nodeElement.position();

                // 存储节点信息
                nodes[nodeId] = {
                    id: nodeId,
                    type: nodeType,
                    x: position.left,
                    y: position.top,
                    width: 150,
                    height: 200
                };

                // 更新计数
                updateNodesCount();

                // 重新计算位置并绘制连接线（确保位置准确）
                setTimeout(() => {
                    const newPosition = nodeElement.position();
                    nodes[nodeId].x = newPosition.left;
                    nodes[nodeId].y = newPosition.top;
                    redrawConnections();
                }, 0);
            }

            // 在事件监听初始化函数中添加以下代码
            // 设置节点按钮事件委托
            $('#nodes-container').on('click', '.setting-node', function(e) {
                const nodeId = $(this).closest('.work-node').data('id');
                openNodeSettings(nodeId);
                e.stopPropagation();
            });

            // 打开节点设置面板的函数 - 使用layui弹窗
            function openNodeSettings(nodeId) {
                const node = nodes[nodeId];
                if (!node) {
                    return;
                }
                console.log(node);
                // 创建弹窗内容
                const content = `
                    <div class="p-4">
                        <form class="layui-form" lay-filter="node-setting-form">
                            <input type="hidden" name="nodeId" value="` + nodeId + `">

                            <div class="layui-form-item">
                                <label class="layui-form-label">节点ID</label>
                                <div class="layui-input-block">
                                    <input type="text" name="nodeIdDisplay" value="` + nodeId + `" disabled class="layui-input">
                                </div>
                            </div>

                            <div class="layui-form-item">
                                <label class="layui-form-label">节点类型</label>
                                <div class="layui-input-block">
                                    <select name="nodeType" lay-verify="required">
                                    <option value="">请选择节点类型</option>
                                        <option value="data-import" ` + (node.type === 'data-import' ? 'selected' : '') + `>数据导入</option>
                                    </select>
                                </div>
                            </div>

                            <div class="layui-form-item">
                                <label class="layui-form-label">执行脚本</label>
                                <div class="layui-input-block">
                                    <textarea name="scriptContent" placeholder="请输入节点执行脚本" class="layui-textarea" style="height: 150px;">` + (node.scriptContent || '') + `</textarea>
                                </div>
                            </div>

                            <div class="layui-form-item">
                                <label class="layui-form-label">参数配置</label>
                                <div class="layui-input-block">
                                    <input type="text" name="parameters" value="` + (node.parameters || '') + `" placeholder="请输入参数配置，以JSON格式" class="layui-input">
                                </div>
                            </div>
                        </form>
                    </div>
                `;

                // 使用layui弹窗
                layer.open({
                    type: 1,
                    title: '节点设置-' + nodeId,
                    area: ['700px', '500px'],
                    shade: 0.6,
                    shadeClose: false,
                    maxmin: true,
                    anim: 0,
                    content: content,
                    btn: ['保存', '取消'],
                    success: function(layero, index) {
                        // 弹窗打开后的回调
                        layui.form.render(); // 渲染layui表单元素
                    },
                    btn1: function(index, form) {
                        // 保存按钮回调
                        const formData = {};

                        // 获取表单数据
                        formData.nodeId = form.find('input[name="nodeId"]').val();
                        formData.nodeType = form.find('select[name="nodeType"]').val();
                        formData.scriptContent = form.find('textarea[name="scriptContent"]').val();
                        formData.parameters = form.find('input[name="parameters"]').val();

                        // 保存节点配置
                        saveNodeSettings(formData);

                        // 关闭弹窗
                        layer.close(index);
                    },
                    btn2: function(index, layero) {
                        // 取消按钮回调
                        layer.close(index);
                    }
                });
            }

            // 保存节点设置
            function saveNodeSettings(formData) {
                console.log('保存节点设置:', formData);
                // 更新节点数据
                const node = nodes[formData.nodeId];
                if (node) {
                    node.type = formData.nodeType;
                    node.scriptContent = formData.scriptContent;
                    node.parameters = formData.parameters;

                    // 更新节点显示名称（如果类型改变）
                    if (nodeTypes[formData.nodeType]) {
                        const nodeConfig = nodeTypes[formData.nodeType];
                        $(`.work-node[data-id="` + formData.nodeId + `"] .font-medium`).text(nodeConfig.name);
                    }

                    console.log('节点设置已保存:', formData);
                    layer.msg('节点设置保存成功', {icon: 1});
                }
            }

            // 移除节点
            function removeNode(nodeId) {
                // 从DOM中移除节点
                $(`.work-node[data-id="` + nodeId + `"]`).remove();

                // 从数据中移除节点
                delete nodes[nodeId];

                // 移除与该节点相关的连接
                connections = connections.filter(conn =>
                    conn.source !== nodeId && conn.target !== nodeId
                );

                // 重新绘制连接线
                redrawConnections();

                // 更新计数
                updateNodesCount();

                // 如果正在设置依赖关系，重置
                if (isSettingDependency && (firstNodeId === nodeId)) {
                    resetDependencySetting();
                }
            }

            // 处理节点选择用于设置依赖关系
            function handleNodeSelectionForDependency(nodeId) {
                // 重置所有节点的选择状态
                $('.work-node').removeClass('ring-2 ring-primary ring-offset-2');

                if (!firstNodeId) {
                    // 选择第一个节点
                    firstNodeId = nodeId;
                    $(`.work-node[data-id="` + nodeId + `"]`).addClass('ring-2 ring-primary ring-offset-2');
                } else if (firstNodeId === nodeId) {
                    // 取消选择
                    firstNodeId = null;
                } else {
                    // 选择第二个节点，等待用户选择关系类型
                    $(`.work-node[data-id="` + firstNodeId + `"]`).addClass('ring-2 ring-primary ring-offset-2');
                    $(`.work-node[data-id="` + nodeId + `"]`).addClass('ring-2 ring-primary ring-offset-2');
                }
            }

            // 设置节点关系
            function setNodeRelation(relationType) {
                if (!firstNodeId) return;

                // 获取第二个节点（所有选中的节点中排除第一个）
                const selectedNodes = $('.work-node.ring-2').not(`[data-id="` + firstNodeId + `"]`);
                if (selectedNodes.length === 0) return;

                const secondNodeId = selectedNodes.data('id');

                // 检查连接是否已存在
                const existingConnection = connections.find(conn =>
                    (conn.source === firstNodeId && conn.target === secondNodeId) ||
                    (conn.source === secondNodeId && conn.target === firstNodeId)
                );

                if (existingConnection) {
                    // 更新现有连接
                    existingConnection.type = relationType;
                } else {
                    // 添加新连接
                    connections.push({
                        source: firstNodeId,
                        target: secondNodeId,
                        type: relationType
                    });
                }

                // 重新绘制连接线
                redrawConnections();

                // 重置选择状态
                resetDependencySetting();
            }

            // 重置依赖关系设置
            function resetDependencySetting() {
                firstNodeId = null;
                $('.work-node').removeClass('ring-2 ring-primary ring-offset-2');
            }

            // 重新绘制所有连接线
            function redrawConnections() {
                const svg = $('#connections');
                svg.empty();

                // 获取工作区位置，用于计算相对坐标
                const workspaceOffset = $('#workspace').offset();

                connections.forEach(conn => {
                    const sourceNode = nodes[conn.source];
                    const targetNode = nodes[conn.target];

                    if (!sourceNode || !targetNode) return;

                    // 计算节点中心位置（相对于SVG）
                    const sourceX = sourceNode.x + sourceNode.width / 2;
                    const sourceY = sourceNode.y + sourceNode.height / 2;
                    const targetX = targetNode.x + targetNode.width / 2;
                    const targetY = targetNode.y + targetNode.height / 2;

                    // 创建连接线
                    const line = $(`<line x1="` + sourceX + `" y1="` + sourceY + `" x2="` + targetX + `" y2="` + targetY + `" />`);

                    // 根据关系类型设置样式
                    line.addClass('connection-line');
                    if (conn.type === 'depend') {
                        line.addClass('connection-line-active');
                    }

                    // 添加箭头标记
                    const markerId = `arrow-` + conn.source + `-` + conn.target + ``;
                    svg.append(`
                        <marker id="` + markerId + `" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                            <polygon points="0 0, 10 3.5, 0 7" fill="` + (conn.type === 'depend' ? '#3B82F6' : '#9CA3AF') + `" />
                        </marker>
                    `);
                    svg.append(`
                        <marker id="` +safeMarkerId + `" markerWidth="` + MARKER_WIDTH + `" markerHeight="` + MARKER_HEIGHT + `" refX="` + REF_X + `" refY="` + REF_Y + `" orient="auto">
                            <polygon points="0 0, ` + MARKER_WIDTH + ` ` + REF_Y + `, 0 ` + MARKER_HEIGHT + `" fill="` + arrowColor + `" />
                        </marker>
                    `);
                    line.attr('marker-end', `url(#` + markerId + `)`);

                    // 添加关系标签
                    const labelX = (sourceX + targetX) / 2;
                    const labelY = (sourceY + targetY) / 2 - 10;
                    const labelText = conn.type === 'depend' ? '依赖' : '互斥';
                    const labelColor = conn.type === 'depend' ? '#3B82F6' : '#EF4444';

                    const label = $(`<text x="` + labelX + `" y="` + labelY + `" text-anchor="middle" fill="` + labelColor + `" font-size="12">` + labelText + `</text>`);

                    svg.append(line);
                    svg.append(label);
                });
            }

            // 更新节点计数显示
            function updateNodesCount() {
                const count = Object.keys(nodes).length;
                $('#nodes-count').text(count + `个节点`);
            }

            // 当窗口大小改变时重新计算节点位置并更新连接线
            $(window).on('resize', function() {
                // 更新所有节点的位置信息
                $('.work-node').each(function() {
                    const nodeId = $(this).data('id');
                    const position = $(this).position();
                    if (nodes[nodeId]) {
                        nodes[nodeId].x = position.left;
                        nodes[nodeId].y = position.top;
                    }
                });

                // 重新绘制连接线
                redrawConnections();
            });

            // UTF-8 安全的 Base64 解码函数
            function base64DecodeUnicode(str) {
                // 将 Base64 字符串转换为二进制字符串
                const binaryString = atob(str);

                // 将二进制字符串转换为字节数组
                const bytes = new Uint8Array(binaryString.length);
                for (let i = 0; i < binaryString.length; i++) {
                    bytes[i] = binaryString.charCodeAt(i);
                }

                // 使用 TextDecoder 解码 UTF-8 字符
                const decoder = new TextDecoder('utf-8');
                return decoder.decode(bytes);
            }

        });
    </script>
</body>
</html>