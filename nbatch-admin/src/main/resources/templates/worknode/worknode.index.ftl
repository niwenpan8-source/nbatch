<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <!-- DataTables -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/css/dataTables.bootstrap.min.css">
    <title>${I18n.admin_name}</title>
    <style>
        .node-info-title {font-weight: 600; font-size: 14px; margin-bottom: 6px;}
        .node-info-line {color: #666; line-height: 22px;}
        .node-info-line span {display: inline-block; margin-right: 16px;}
        .node-info-id {font-family: Menlo, Monaco, Consolas, monospace; color: #555;}
        .run-info-empty {color: #999; line-height: 54px;}
    </style>
</head>
<body class="hold-transition skin-blue sidebar-mini <#if cookieMap?exists && cookieMap["xxljob_adminlte_settings"]?exists && "off" == cookieMap["xxljob_adminlte_settings"].value >sidebar-collapse</#if>">
<div class="wrapper">
    <!-- header -->
    <@netCommon.commonHeader />
    <!-- left -->
    <@netCommon.commonLeft "node" />

    <!-- Content Wrapper. Contains page content -->
    <div class="content-wrapper">
        <!-- Content Header (Page header) -->
        <section class="content-header">
            <h1>作业节点管理</h1>
        </section>

        <!-- Main content -->
        <section class="content">
            <div class="row">
                <div class="col-xs-3">
                    <div class="input-group">
                        <span class="input-group-addon">${I18n.job_work_node_field_type}</span>
                        <select class="form-control" id="nodeType">
                            <option value="">--请选择--</option>
                            <#list nodeTypeEnum as type>
                                <option value="${type.code}">${type.value}</option>
                            </#list>
                        </select>
                    </div>
                </div>
                <div class="col-xs-3">
                    <div class="input-group">
                        <span class="input-group-addon">${I18n.job_work_node_field_work}</span>
                        <select class="form-control" id="workId">
                            <option value="">--请选择--</option>
                            <#list allEnableWorkList as type>
                                <option value="${type.workId}">${type.workName}</option>
                            </#list>
                        </select>
                    </div>
                </div>
                <div class="col-xs-1">
                    <button class="btn btn-block btn-info" id="searchBtn">${I18n.system_search}</button>
                </div>
                <div class="col-xs-1">
                    <button class="btn btn-block btn-success add" type="button">${I18n.jobinfo_field_add}</button>
                </div>
            </div>
            <div class="row">
                <div class="col-xs-12">
                    <div class="box">
                        <div class="box-body">
                            <table id="work_node_list" class="table table-bordered table-striped" width="100%">
                                <thead>
                                <tr>
                                    <th>节点信息</th>
                                    <th>最近运行节点信息</th>
                                    <th>${I18n.system_opt}</th>
                                </tr>
                                </thead>
                                <tbody></tbody>
                                <tfoot></tfoot>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>

    <!-- footer -->
    <@netCommon.commonFooter />
</div>

<@netCommon.commonScript />
<!-- DataTables -->
<script src="${request.contextPath}/static/adminlte/bower_components/datatables.net/js/jquery.dataTables.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/js/dataTables.bootstrap.min.js"></script>
<!-- moment -->
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<#-- cronGen -->
<script src="${request.contextPath}/static/plugins/cronGen/cronGen<#if I18n.admin_i18n?default('')?length gt 0 >_${I18n.admin_i18n}</#if>.js"></script>
<script src="${request.contextPath}/static/js/worknode.index.1.js"></script>
</body>
</html>
