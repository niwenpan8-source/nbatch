<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <!-- DataTables -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/css/dataTables.bootstrap.min.css">
    <title>${I18n.admin_name}</title>
    <style>
        .run-info-id {font-family: Menlo, Monaco, Consolas, monospace; color: #555;}
        #job_work_list th, #job_work_list td {vertical-align: middle;}
    </style>
</head>
<body class="hold-transition skin-blue sidebar-mini <#if cookieMap?exists && cookieMap["xxljob_adminlte_settings"]?exists && "off" == cookieMap["xxljob_adminlte_settings"].value >sidebar-collapse</#if>">
<div class="wrapper">
    <!-- header -->
    <@netCommon.commonHeader />
    <!-- left -->
    <@netCommon.commonLeft "work" />

    <!-- Content Wrapper. Contains page content -->
    <div class="content-wrapper">
        <!-- Content Header (Page header) -->
        <section class="content-header">
            <h1>批次流程管理</h1>
        </section>

        <!-- Main content -->
        <section class="content">
            <div class="row">
                <div class="col-xs-12">
                    <div class="box">
                        <div class="box-header with-border">
                            <h3 class="box-title">批次流程管理</h3>
                            <div class="box-tools pull-right">
                                <button class="btn btn-success add" type="button">${I18n.jobinfo_field_add}</button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div class="row">
                                <div class="col-xs-12 col-md-3">
                                    <div class="input-group">
                                        <span class="input-group-addon">${I18n.job_work_field_status}</span>
                                        <select class="form-control" id="workStatus">
                                            <option value="">全部</option>
                                            <#list workStatusEnum as status>
                                                <option value="${status.code}">${status.value}</option>
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
                            <table id="job_work_list" class="table table-bordered table-striped" width="100%">
                                <thead>
                                <tr>
                                    <th>作业名称</th>
                                    <th>作业ID</th>
                                    <th>类型</th>
                                    <th>状态</th>
                                    <th>描述</th>
                                    <th>版本</th>
                                    <th>运行ID</th>
                                    <th>运行状态</th>
                                    <th>翻牌时间</th>
                                    <th>创建时间</th>
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
<script src="${request.contextPath}/static/js/jobwork.index.1.js"></script>
</body>
</html>
