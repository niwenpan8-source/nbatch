<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>

<!-- job新增.模态框 -->
<div class="modal-body">
    <form class="form-horizontal form" role="form" id="addModel" action="/work/insert">
        <#-- 基础信息 -->
        <div class="form-group">
            <label for="firstname" class="col-sm-2 control-label">作业名称<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workName" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>

            <label for="lastname" class="col-sm-2 control-label">作业描述<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="workDesc" placeholder="${I18n.system_please_input}" maxlength="50">
            </div>
        </div>

        <div class="form-group">
            <label for="firstname" class="col-sm-2 control-label">作业状态<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="workStatus" name="workStatus">
                    <option value="" >--请选择--</option>
                    <#list workStatusEnum as worStatusItem>
                        <option value="${worStatusItem.code}">${worStatusItem.value}</option>
                    </#list>
                </select>
            </div>

            <label for="lastname" class="col-sm-2 control-label">翻牌日期<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" id="turnTime" name="turnTime" placeholder="yyyy-MM-dd">
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
