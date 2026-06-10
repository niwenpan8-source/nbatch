<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <!-- DataTables -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/css/dataTables.bootstrap.min.css">
    <title>${I18n.admin_name}</title>
    <style>
        #work_node_list {min-width: 1120px; table-layout: fixed;}
        #work_node_list th, #work_node_list td {vertical-align: middle; box-sizing: border-box;}
        #work_node_list th {white-space: nowrap;}
        #work_node_list td {white-space: normal; word-break: break-word;}
        #work_node_list .details-control {width: 44px; min-width: 44px; text-align: center;}
        #work_node_list th:nth-child(1), #work_node_list td:nth-child(1) {width: 48px;}
        #work_node_list th:nth-child(2), #work_node_list td:nth-child(2) {width: 150px;}
        #work_node_list th:nth-child(3), #work_node_list td:nth-child(3) {width: 140px;}
        #work_node_list th:nth-child(4), #work_node_list td:nth-child(4) {width: 120px;}
        #work_node_list th:nth-child(5), #work_node_list td:nth-child(5) {width: 120px;}
        #work_node_list th:nth-child(6), #work_node_list td:nth-child(6) {width: 90px;}
        #work_node_list th:nth-child(7), #work_node_list td:nth-child(7) {width: 80px;}
        #work_node_list th:nth-child(8), #work_node_list td:nth-child(8) {width: 90px;}
        #work_node_list th:nth-child(9), #work_node_list td:nth-child(9) {width: 100px;}
        #work_node_list th:nth-child(10), #work_node_list td:nth-child(10) {width: 90px;}
        #work_node_list th:nth-child(11), #work_node_list td:nth-child(11) {width: 120px;}
        #work_node_list th:nth-child(12), #work_node_list td:nth-child(12) {width: 120px;}
        #work_node_list th:nth-child(13), #work_node_list td:nth-child(13) {width: 220px;}
        #work_node_list .node-main-text {display: inline-block; max-width: 220px; word-break: break-word;}
        #work_node_list tr.run-info-row > td, #work_node_list td.run-info-row {padding: 0; background: #f8fafc;}
        .work-node-detail-toggle {width: 28px; height: 24px; padding: 1px 0; line-height: 20px;}
        .work-node-run-detail {display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 10px 18px; padding: 12px 16px; border-left: 3px solid #3c8dbc; background: #f8fafc;}
        .work-node-run-item {display: flex; align-items: flex-start; gap: 8px; min-width: 0;}
        .work-node-run-label {flex: 0 0 86px; color: #6b7280; font-weight: 600;}
        .node-info-id {font-family: Menlo, Monaco, Consolas, monospace; color: #374151; word-break: break-all;}
        @media (max-width: 1199px) {
            #work_node_list {min-width: 1040px;}
        }
        @media (max-width: 767px) {
            .box .box-body {padding: 12px;}
            #work_node_list {min-width: 1020px;}
            .work-node-run-detail {grid-template-columns: 1fr; padding: 10px 12px;}
        }
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
                <div class="col-xs-12">
                    <div class="box">
                        <div class="box-header with-border">
                            <h3 class="box-title">作业节点管理</h3>
                            <div class="box-tools pull-right">
                                <button class="btn btn-success add" type="button">${I18n.jobinfo_field_add}</button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div class="row">
                                <div class="col-xs-12 col-md-3">
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
                                <div class="col-xs-12 col-md-3">
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
                                <div class="col-xs-12 col-md-1">
                                    <button class="btn btn-block btn-info" id="searchBtn">${I18n.system_search}</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-xs-12">
                    <div class="box">
                        <div class="box-body">
                            <table id="work_node_list" class="table table-bordered table-striped" width="100%">
                                <thead>
                                <tr>
                                    <th></th>
                                    <th>节点名称</th>
                                    <th>节点ID</th>
                                    <th>作业</th>
                                    <th>类型</th>
                                    <th>数据库</th>
                                    <th>状态</th>
                                    <th>运行状态</th>
                                    <th>翻牌时间</th>
                                    <th>剩余重试</th>
                                    <th>开始时间</th>
                                    <th>结束时间</th>
                                    <th class="table-action-cell">${I18n.system_opt}</th>
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
