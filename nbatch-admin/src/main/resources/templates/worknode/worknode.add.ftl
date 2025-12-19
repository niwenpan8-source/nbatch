<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>

<!-- job新增.模态框 -->
<div class="modal-body">
    <form class="form-horizontal form" role="form" id="addModel" action="/node/insert">

        <div class="form-group">
            <label for="workId" class="col-sm-2 control-label">作业名称<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="workId" name="workId">
                    <option value="" >--请选择--</option>
                    <#list allEnableWorkList as item>
                        <option value="${item.workId}">
                            ${item.workName}</option>
                    </#list>
                </select>
            </div>
            <label for="nodeName" class="col-sm-2 control-label">节点名称<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeName" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="">
            </div>
        </div>
        <div class="form-group">
            <label for="nodeDesc" class="col-sm-2 control-label">节点描述<font color="red">*</font></label>
            <div class="col-sm-10">
                <textarea class="textarea form-control" name="nodeDesc" placeholder="${I18n.system_please_input}" maxlength="512" style="height: 63px; line-height: 1.2;"></textarea>
            </div>
        </div>
        <div class="form-group">
            <label for="nodeStatus" class="col-sm-2 control-label">节点状态<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="nodeStatus" name="nodeStatus">
                    <option value="" >--请选择--</option>
                    <#list workStatusEnum as item>
                        <option value="${item.code}">
                            ${item.value}
                        </option>
                    </#list>
                </select>
            </div>
            <label for="nodeType" class="col-sm-2 control-label">节点类型<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="nodeType" name="nodeType">
                    <option value="" >--请选择--</option>
                    <#list nodeTypeEnum as item>
                        <option value="${item.code}">
                            ${item.value}
                        </option>
                    </#list>
                </select>
            </div>
        </div>
        <div class="form-group">
            <label for="dbType" class="col-sm-2 control-label">数据库类型<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="dbType" name="dbType">
                    <option value="" >--请选择--</option>
                    <#list dbTypeEnum as item>
                        <option value="${item.code}">
                            ${item.value}
                        </option>
                    </#list>
                </select>
            </div>
            <label for="executeHandler" class="col-sm-2 control-label">执行器<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="executeHandler" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="">
            </div>
        </div>
        <div class="form-group">

            <label for="scriptType" class="col-sm-2 control-label">脚本类型<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="scriptType" name="scriptType">
                    <option value="" >--请选择--</option>
                    <#list scriptTypeEnum as item>
                        <option value="${item.code}">
                            ${item.code}
                        </option>
                    </#list>
                </select>
            </div>
        </div>
        <div class="form-group">
            <label for="executeContent" class="col-sm-2 control-label">执行内容<font color="red">*</font></label>
            <div class="col-sm-10">
                <textarea class="textarea form-control" name="executeContent" placeholder="${I18n.system_please_input}"
                          maxlength="512" style="height: 63px; line-height: 1.2;"></textarea>
            </div>
        </div>

        <div class="form-group">
            <label for="executeContentParam" class="col-sm-2 control-label">执行内容参数<font color="red">*</font></label>
            <div class="col-sm-10">
                <textarea class="textarea form-control" name="executeContentParam" placeholder="${I18n.system_please_input}"
                          maxlength="512" style="height: 63px; line-height: 1.2;"></textarea>
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
