<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/layui/css/layui.css">
    <style>
        html, body {height: 100%; overflow: hidden; background: #edf3f1;}
        .node-form-modal {box-sizing: border-box; height: 100vh; padding: 20px 24px 34px; overflow-y: auto;}
        .node-form-modal .form {max-width: 1280px; margin: 0 auto;}
        .node-form-modal .form-group {margin-bottom: 18px;}
        .config-section {display: none; margin: 14px 0 4px; padding: 16px 16px 6px; background: #f3f8f6; border: 1px solid #d8e4e1; border-radius: 6px;}
        .config-title {font-weight: 600; margin-bottom: 14px; color: #1e3739;}
        textarea.form-control {min-height: 150px; resize: vertical; font-family: Menlo, Monaco, Consolas, monospace; line-height: 1.5;}
        @media (max-width: 767px) {
            .node-form-modal {padding: 16px 14px 28px;}
            .node-form-modal .form-group {margin-bottom: 8px;}
            .node-form-modal .control-label {text-align: left; padding-top: 7px;}
            .config-section {padding: 14px 12px 4px;}
        }
    </style>
</head>
<body>
<div class="modal-body node-form-modal">
    <form class="form-horizontal form" role="form" id="addModel" action="${request.contextPath}/node/insert">
        <div class="form-group">
            <label class="col-sm-2 control-label"><#if fromFlow>所属作业<#else>所属作业<font color="red">*</font></#if></label>
            <div class="col-sm-4">
                <#if fromFlow>
                    <#assign currentWorkName = selectedWorkId>
                    <#list allEnableWorkList as work>
                        <#if selectedWorkId?? && selectedWorkId == work.workId><#assign currentWorkName = work.workName></#if>
                    </#list>
                    <input type="hidden" name="workId" value="${selectedWorkId}">
                    <input type="text" class="form-control" value="${currentWorkName}" readonly>
                <#else>
                <select class="form-control" name="workId" onchange="loadWorkGroups()">
                    <option value="">--请选择--</option>
                    <#list allEnableWorkList as work>
                        <option value="${work.workId}" <#if selectedWorkId?? && selectedWorkId == work.workId>selected</#if>>${work.workName}</option>
                    </#list>
                </select>
                </#if>
            </div>
            <label class="col-sm-2 control-label">节点类型<font color="red">*</font></label>
            <div class="col-sm-4">
                <select class="form-control" name="nodeType" onchange="toggleNodeTypeConfig()">
                    <option value="">--请选择--</option>
                    <#list nodeTypeEnum as nodeTypeItem>
                        <option value="${nodeTypeItem.code}">${nodeTypeItem.value}</option>
                    </#list>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">所属组</label>
            <div class="col-sm-4">
                <select class="form-control" name="groupId" disabled>
                    <option value="">选择作业后加载默认组</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">节点名称<font color="red">*</font></label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeName" placeholder="${I18n.system_please_input}" maxlength="100">
            </div>
            <label class="col-sm-2 control-label">节点说明</label>
            <div class="col-sm-4">
                <input type="text" class="form-control" name="nodeDesc" placeholder="${I18n.system_please_input}" maxlength="100">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">数据库类型</label>
            <div class="col-sm-4">
                <select class="form-control" name="dbType">
                    <option value="">--请选择--</option>
                    <#list dbTypeEnum as dbTypeItem>
                        <option value="${dbTypeItem.code}">${dbTypeItem.value}</option>
                    </#list>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-2 control-label">失败策略</label>
            <div class="col-sm-4">
                <select class="form-control" name="errorStrategy">
                    <option value="stop">停止整个流程</option>
                    <option value="skip">跳过继续</option>
                    <option value="retry">重试</option>
                </select>
            </div>
            <label class="col-sm-2 control-label">重试次数</label>
            <div class="col-sm-4">
                <input type="number" class="form-control" name="retryCount" value="0" min="-1">
                <span class="help-block">填 -1 表示无限重试，0 表示不重试，大于 0 表示最多重试次数。</span>
            </div>
        </div>

        <div class="config-section" data-section="script">
            <div class="config-title">脚本节点配置</div>
            <div class="form-group">
                <label class="col-sm-2 control-label">脚本类型</label>
                <div class="col-sm-4">
                    <select class="form-control" name="scriptType">
                        <option value="">--请选择--</option>
                        <#list scriptTypeEnum as scriptTypeItem>
                            <option value="${scriptTypeItem.code}">${scriptTypeItem.code}</option>
                        </#list>
                    </select>
                </div>
                <label class="col-sm-2 control-label">脚本参数</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="executeContentParam"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">脚本内容</label>
                <div class="col-sm-10"><textarea class="form-control" name="executeContent" maxlength="60000" spellcheck="false" placeholder="支持多行输入；保存后统一使用 LF 换行"></textarea><span class="help-block">脚本内容最多 60000 个 UTF-8 字节，换行、缩进和空行会原样保留。</span></div>
            </div>
        </div>

        <div class="config-section" data-section="bean">
            <div class="config-title">Bean 节点配置</div>
            <div class="form-group">
                <label class="col-sm-2 control-label">Bean执行器</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="executeHandler" placeholder="例如 demoJobHandler"></div>
                <label class="col-sm-2 control-label">执行参数</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="executeContentParam"></div>
            </div>
        </div>

        <div class="config-section" data-section="sql">
            <div class="config-title">SQL/存储过程节点配置</div>
            <div class="form-group">
                <label class="col-sm-2 control-label">执行内容</label>
                <div class="col-sm-10"><textarea class="form-control" name="executeContent" placeholder="SQL 或存储过程调用内容"></textarea></div>
            </div>
        </div>

        <div class="config-section" data-section="file_to_db">
            <div class="config-title">文件导入数据库配置</div>
            <div class="form-group">
                <label class="col-sm-2 control-label">导入文件名</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importFileName"></div>
                <label class="col-sm-2 control-label">导入表名</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importTableName"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">导入列</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importTableFiled" placeholder="col1,col2"></div>
                <label class="col-sm-2 control-label">导入条件</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importTableCondition"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">文件编码</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importFileCode" value="utf8"></div>
                <label class="col-sm-2 control-label">分隔符</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importSep" value=","></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">文件名参数</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="importFileNameParam"></div>
                <label class="col-sm-2 control-label">选项</label>
                <div class="col-sm-4">
                    <select class="form-control" name="importAllUpdate"><option value="0">增量</option><option value="1">全量</option></select>
                    <select class="form-control" name="importIsGzip" style="margin-top:6px;"><option value="0">不压缩</option><option value="1">gzip压缩</option></select>
                </div>
            </div>
        </div>

        <div class="config-section" data-section="db_to_file">
            <div class="config-title">数据库导出文件配置</div>
            <div class="form-group">
                <label class="col-sm-2 control-label">导出文件名</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportFileName"></div>
                <label class="col-sm-2 control-label">导出表名</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportTableName"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">导出列</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportTableFiled" placeholder="col1,col2"></div>
                <label class="col-sm-2 control-label">导出条件</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportTableCondition"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">文件编码</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportFileCode" value="utf8"></div>
                <label class="col-sm-2 control-label">分隔符</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportSep" value=","></div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">文件名参数</label>
                <div class="col-sm-4"><input type="text" class="form-control" name="exportFileNameParam"></div>
                <label class="col-sm-2 control-label">选项</label>
                <div class="col-sm-4">
                    <select class="form-control" name="exportAllUpdate"><option value="0">增量</option><option value="1">全量</option></select>
                    <select class="form-control" name="exportIsGzip" style="margin-top:6px;"><option value="0">不压缩</option><option value="1">gzip压缩</option></select>
                </div>
            </div>
        </div>
    </form>
</div>

<@netCommon.commonScript />
<script src="${request.contextPath}/static/plugins/layui/layui.js"></script>
<script>
    function loadWorkGroups(selectedGroupId) {
        var workId = document.querySelector('[name="workId"]').value;
        var groupSelect = document.querySelector('[name="groupId"]');
        groupSelect.innerHTML = '';
        if (!workId) {
            groupSelect.disabled = true;
            groupSelect.innerHTML = '<option value="">选择作业后加载默认组</option>';
            return;
        }
        groupSelect.disabled = true;
        $.post('${request.contextPath}/flow/groups', {workId: workId}, function(response) {
            var groups = response && response.code === 200 ? response.content : [];
            groups.forEach(function(group) {
                var option = document.createElement('option');
                option.value = group.groupId;
                option.textContent = group.groupName;
                option.selected = selectedGroupId && selectedGroupId === group.groupId;
                groupSelect.appendChild(option);
            });
            groupSelect.disabled = false;
        }).fail(function() {
            groupSelect.innerHTML = '<option value="">组加载失败</option>';
        });
    }

    function toggleNodeTypeConfig() {
        var nodeType = document.querySelector('[name="nodeType"]').value;
        document.querySelectorAll('.config-section').forEach(function(section) { section.style.display = 'none'; });
        if (nodeType === 'script') document.querySelector('[data-section="script"]').style.display = 'block';
        if (nodeType === 'bean') document.querySelector('[data-section="bean"]').style.display = 'block';
        if (nodeType === 'execute_sql' || nodeType === 'store_procedure' || nodeType === 'store_procedure_return_str') document.querySelector('[data-section="sql"]').style.display = 'block';
        if (nodeType === 'file_to_db') document.querySelector('[data-section="file_to_db"]').style.display = 'block';
        if (nodeType === 'db_to_file') document.querySelector('[data-section="db_to_file"]').style.display = 'block';
    }
    function beforeSubmit() {
        if (!document.querySelector('[name="workId"]').value) { parent.layer.msg('请选择所属作业'); return false; }
        if (!document.querySelector('[name="nodeType"]').value) { parent.layer.msg('请选择节点类型'); return false; }
        if (!document.querySelector('[name="nodeName"]').value) { parent.layer.msg('请输入节点名称'); return false; }
        if (document.querySelector('[name="nodeType"]').value === 'script') {
            var scriptContent = document.querySelector('[data-section="script"] textarea[name="executeContent"]');
            if (scriptContent.value.indexOf('\0') >= 0) { parent.layer.msg('脚本内容不能包含 NUL 字符'); return false; }
            if (new Blob([scriptContent.value]).size > 60000) { parent.layer.msg('脚本内容不能超过 60000 个 UTF-8 字节'); return false; }
        }
        document.querySelectorAll('.config-section input, .config-section textarea, .config-section select').forEach(function(input) { input.disabled = false; });
        document.querySelectorAll('.config-section').forEach(function(section) {
            if (section.style.display === 'none' || !section.style.display) {
                section.querySelectorAll('input, textarea, select').forEach(function(input) { input.disabled = true; });
            }
        });
        return true;
    }
    <#if selectedWorkId?? && selectedWorkId?has_content>
    loadWorkGroups();
    </#if>
</script>
</body>
</html>
