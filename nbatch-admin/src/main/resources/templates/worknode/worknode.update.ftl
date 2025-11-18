<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
</head>

<!-- job新增.模态框 -->
<div class="modal-body">
    <form class="form-horizontal form" role="form" id="updateModel" action="/node/update">
        <input type="hidden" name="nodeId" value="${model.nodeId}">
        <#-- 基础信息 -->
        <div class="form-group">
            <label for="nodeName" class="col-sm-2 control-label">节点名称<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeName" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="${model.nodeName}">
            </div>

            <label for="nodeDesc" class="col-sm-2 control-label">节点描述<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeDesc" placeholder="${I18n.system_please_input}"
                       maxlength="50" value="${model.nodeDesc}">
            </div>
        </div>

        <div class="form-group">
            <label for="nodeType" class="col-sm-2 control-label">节点状态<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="nodeType" name="nodeType">
                    <option value="" >--请选择--</option>
                    <#list nodeTypeEnum as nodeTypeItem>
                        <option value="${nodeTypeItem.code}"
                                <#if model?? && model.nodeType?? && model.nodeType == nodeTypeItem.code>selected</#if>>
                            ${nodeTypeItem.value}
                        </option>
                    </#list>
                </select>
            </div>

            <label for="nodeStatus" class="col-sm-2 control-label">节点状态<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" id="nodeStatus" name="nodeStatus">
                    <option value="" >--请选择--</option>
                    <#list workStatusEnum as workStatusItem>
                        <option value="${workStatusItem.code}"
                                <#if model?? && model.workStatus?? && model.workStatus == workStatusItem.code>selected</#if>>
                            ${workStatusItem.value}
                        </option>
                    </#list>
                </select>
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
