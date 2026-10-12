<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        html, body {height: 100%; overflow: hidden; background: #edf3f1;}
        .work-form-modal {box-sizing: border-box; height: 100vh; padding: 20px 24px 34px; overflow-y: auto;}
        .work-form-modal .form {max-width: 1120px; margin: 0 auto;}
        .work-form-modal .form-group {margin-bottom: 18px;}
        .work-form-modal .control-label {color: #4b5563;}
        @media (max-width: 767px) {
            .work-form-modal {padding: 16px 14px 28px;}
            .work-form-modal .form-group {margin-bottom: 8px;}
            .work-form-modal .control-label {text-align: left; padding-top: 7px;}
        }
    </style>
</head>

<body>
<!-- job新增.模态框 -->
<div class="modal-body work-form-modal">
    <form class="form-horizontal form" role="form" id="addModel" action="/work/insert">
        <#-- 基础信息 -->
        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_name}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workName" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_desc}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workDesc" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_work_type}<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="workType">
                    <option value="" >--请选择--</option>
                    <#list workTypeEnum as type>
                        <option value="${type.code}">${type.value}</option>
                    </#list>
                </select>
            </div>

        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">执行器<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="jobGroup">
                    <option value="">--请选择--</option>
                    <#list jobGroupList as group><option value="${group.id}">${group.title}</option></#list>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_cron}</label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="cronExpression" placeholder="0 0 0 * * ? *" maxlength="128">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_timeout}</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="timeout" placeholder="0" min="0">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_notify_email}</label>
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
<script src="${request.contextPath}/static/plugins/cronGen/cronGen_core.js"></script>
<script src="${request.contextPath}/static/plugins/cronGen/cronGen<#if I18n.admin_i18n?default('')?length gt 0 >_${I18n.admin_i18n}</#if>.js"></script>
<script>
    $(function () {
        $("input[name='cronExpression']").cronGen({});
    });

</script>
</body>
</html>
