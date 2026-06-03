<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>

<!-- 节点新增.模态框 -->
<div class="modal-body">
    <form class="form-horizontal form" role="form" id="addModel" action="/node/insert">
        <#-- 基础信息 -->
        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_node_field_name}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeName" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_node_field_desc}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeDesc" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_node_field_type}<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="nodeType">
                    <option value="" >--请选择--</option>
                    <#list nodeTypeEnum as nodeTypeItem>
                        <option value="${nodeTypeItem.code}">${nodeTypeItem.value}</option>
                    </#list>
                </select>
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_node_field_db_type}</label>
            <div class="col-sm-4">
                <select class="form-control" name="dbType">
                    <option value="" >--请选择--</option>
                    <option value="mysql">MySQL</option>
                    <option value="oracle">Oracle</option>
                    <option value="sqlserver">SQL Server</option>
                    <option value="postgresql">PostgreSQL</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_node_field_status}<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="nodeStatus">
                    <option value="" >--请选择--</option>
                    <#list workStatusEnum as worStatusItem>
                        <option value="${worStatusItem.code}">${worStatusItem.value}</option>
                    </#list>
                </select>
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_node_field_error_strategy}</label>
            <div class="col-sm-4">
                <select class="form-control" name="errorStrategy">
                    <option value="stop">停止整个流程</option>
                    <option value="skip">跳过继续</option>
                    <option value="retry">重试</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_node_field_node_timeout}</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="timeout" placeholder="0" min="0">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_node_field_retry_count}</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="retryCount" placeholder="0" min="0">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_node_field_retry_interval}</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="retryInterval" placeholder="0" min="0">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_node_field_notify_email}</label>
            <div class="col-sm-4">
                <input type="email" class="form-control" name="notifyEmail" placeholder="xxx@example.com" maxlength="255">
            </div>
        </div>

    </form>
</div>

<@netCommon.commonScript />
<!-- DataTables -->
<script src="${request.contextPath}/static/adminlte/bower_components/datatables.net/js/jquery.dataTables.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/js/dataTables.bootstrap.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.js"></script>
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>
</body>
</html>
