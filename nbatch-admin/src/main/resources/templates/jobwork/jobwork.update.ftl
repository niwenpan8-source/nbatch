<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>

<!-- job编辑.模态框 -->
<div class="modal-body">
    <form class="form-horizontal form" role="form" id="updateModel" action="/work/update">
        <input type="hidden" name="workId" value="${model.workId}">
        <#-- 基础信息 -->
        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_name}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workName" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="<#if model?? && model.workName??>${model.workName}</#if>">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_desc}<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workDesc" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="<#if model?? && model.workDesc??>${model.workDesc}</#if>">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_work_type}<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="workType">
                    <option value="" >--请选择--</option>
                    <#list workTypeEnum as type>
                        <option value="${type.code}"
                        <#if model?? && model.workType?? && model.workType == type.code>selected</#if>>${type.value}</option>
                    </#list>
                </select>
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_status}<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="workStatus" name="workStatus">
                     <option value="" >--请选择--</option>
                    <#list workStatusEnum as worStatusItem>
                        <option value="${worStatusItem.code}"
                        <#if model?? && model.workStatus?? && model.workStatus == worStatusItem.code>selected</#if>>
                            ${worStatusItem.value}
                        </option>
                    </#list>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_cron}</label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="cronExpression" placeholder="0 0 0 * * ? *" maxlength="128"
                       value="<#if model?? && model.cronExpression??>${model.cronExpression}</#if>">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_turn_time}</label>
            <div class="col-sm-4">
                <input type="text" class="form-control" id="turnTime" name="turnTime" placeholder="yyyy-MM-dd"
                       value="<#if model?? && model.turnTime??>${model.turnTime?string('yyyy-MM-dd')}</#if>">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">${I18n.job_work_field_timeout}</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="timeout" placeholder="0" min="0"
                       value="<#if model?? && model.timeout??>${model.timeout}</#if>">
            </div>

            <label class="col-sm-2 control-label">${I18n.job_work_field_notify_email}</label>
            <div class="col-sm-4">
                <input type="email" class="form-control" name="notifyEmail" placeholder="xxx@example.com" maxlength="255"
                       value="<#if model?? && model.notifyEmail??>${model.notifyEmail}</#if>">
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
<script>
    layui.use(function(){
        var laydate = layui.laydate;
        // 渲染
        laydate.render({
            elem: '#turnTime'
        });
    });

</script>
</body>
</html>
