<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <!-- DataTables -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/datatables.net-bs/css/dataTables.bootstrap.min.css">
    <title>${I18n.admin_name}</title>
    <style>
        #job_work_list {min-width: 1480px;}
        #job_work_list th, #job_work_list td {vertical-align: middle;}
        #job_work_list th, #job_work_list td {box-sizing: border-box;}
        #job_work_list .table-action-cell {width: 120px; min-width: 120px;}
        @media (max-width: 767px) {
            .box .box-body {padding: 12px;}
            #job_work_list {min-width: 1120px;}
        }
    </style>
</head>
<body class="hold-transition skin-blue sidebar-mini list-page <#if cookieMap?exists && cookieMap["nbatch_job_adminlte_settings"]?exists && "off" == cookieMap["nbatch_job_adminlte_settings"].value >sidebar-collapse</#if>">
<div class="wrapper">
    <!-- header -->
    <@netCommon.commonHeader />
    <!-- left -->
    <@netCommon.commonLeft "work" />

    <!-- Content Wrapper. Contains page content -->
    <div class="content-wrapper">
        <!-- Content Header (Page header) -->
        <section class="content-header">
            <h1>作业管理</h1>
        </section>

        <!-- Main content -->
        <section class="content">
            <div class="row">
                <div class="col-xs-12">
                    <div class="box">
                        <div class="box-header with-border">
                            <h3 class="box-title">作业管理</h3>
                            <div class="box-tools pull-right">
                                <button class="btn btn-success add" type="button">${I18n.jobinfo_field_add}</button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div class="row list-filter-row">
                                <div class="col-xs-12 col-md-3">
                                    <div class="input-group">
                                        <span class="input-group-addon">作业名称</span>
                                        <input type="text" class="form-control" id="workName" placeholder="请输入作业名称">
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
                    <div class="box list-table-box">
                        <div class="box-body">
                            <div class="table-responsive-wrap">
                            <table id="job_work_list" class="table table-bordered table-striped table-fixed-wide" width="100%">
                                <thead>
                                <tr>
                                    <th>作业名称</th>
                                    <th>类型</th>
                                    <th>运行状态</th>
                                    <th>翻牌时间</th>
                                    <th>创建时间</th>
                                    <th>执行器</th>
                                    <th>Cron 表达式</th>
                                    <th>定时状态</th>
                                    <th>下次触发</th>
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
<script src="${request.contextPath}/static/plugins/cronGen/cronGen_core.js"></script>
<script src="${request.contextPath}/static/plugins/cronGen/cronGen<#if I18n.admin_i18n?default('')?length gt 0 >_${I18n.admin_i18n}</#if>.js"></script>
<script src="${request.contextPath}/static/js/jobwork.index.1.js?v=2026082701"></script>
</body>
</html>
