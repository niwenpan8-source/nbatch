<!DOCTYPE html>
<html>
<div class="layui-fluid">
	<div class="layui-row layui-col-space15">
		<div class="layui-col-md1">
			<button class="layui-btn layui-btn-normal" id="searchLogBtn">搜索</button>
		</div>
	</div>
	<!-- Main content -->
	<section class="layui-row layui-col-space15">
		<input type="hidden" id="workId" value="${workNode.workId}">
		<input type="hidden" id="nodeId" value="${workNode.nodeId}">
		<div class="layui-col-md12">
			<div class="layui-card">
				<div class="layui-card-body">
					<table class="layui-table" id="work_node_log_list" lay-filter="work_node_log_list"></table>
				</div>
			</div>
		</div>
	</section>
</div>


<!-- Layui CSS -->
<link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
<!-- jQuery -->
<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
<!-- Layui JS -->
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>

<script>
	layui.use(['table', 'form'], function(){
		var table = layui.table;
		var $ = layui.$;
		var form = layui.form;
		var base_url = '${request.contextPath}';
		var workId = $('#workId').val()
		var nodeId = $('#nodeId').val()
		// 渲染表格
		var workNodeLogTable = table.render({
			elem: '#work_node_log_list',
			url: base_url + '/node/logPageList',
			method: 'post',
			where: {
				nodeId: nodeId,
				workId: workId
			},
			request: {
				pageName: 'start', // 页码的参数名称
				limitName: 'length' // 每页数据条数的参数名称
			},
			parseData: function(res){ // 数据预处理
				console.log(res);
				// 参数验证
				if (!res) {
					return {
						"code": 500,
						"msg": "响应数据为空",
						"count": 0,
						"data": []
					};
				}
				return {
					"code": 0, // 解析接口状态码
					"msg": "", // 解析提示文本
					"count": res.content.total || 0, // 解析总数据量，提供默认值
					"data": res. content.records || [] // 解析数据列表，提供默认值
				};
			},
			cols: [[
				{field: 'handleCode', title: '执行状态', width: '10%'},
				{field: 'handleMsg', title: '执行信息', width: '30%'},
				{field: 'createTime', title: '创建时间', width: '15%'},
				{field: 'callBackTime', title: '回调时间', width: '15%'},
				{field: 'logDetail', title: '日志详情', width: '30%', templet: function(row){
						if(row.logDetail) {
							return '<div style="white-space: pre-line;">' +
										row.logDetail +
									'</div>';
						}
						return '';
					}}
			]],
			page: true, // 开启分页
			limit: 10, // 每页显示数量
			limits: [10, 20, 30, 40, 50], // 每页显示数量选择
			loading: true // 开启加载条
		});

		// 搜索按钮事件
		$('#searchLogBtn').on('click', function(){
			table.reload('work_node_log_list', {
				where: {
					nodeId: nodeId,
					workId: workId
				}
			});
		});
	});
</script>
</body>
</html>
