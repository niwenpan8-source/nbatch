<!DOCTYPE html>
<html>
<head>
  	<#import "./common/common.macro.ftl" as netCommon>
	<@netCommon.commonStyle />
    <!-- daterangepicker -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.css">
    <title>${I18n.admin_name}</title>
    <style>
        .dashboard-heading {display: flex; align-items: baseline; gap: 10px;}
        .dashboard-heading h1 {margin: 0;}
        .dashboard-heading .dashboard-subtitle {font-size: 13px; color: #70817c;}
        .dashboard-summary {margin-bottom: 14px;}
        .overview-card {position: relative; min-height: 112px; padding: 18px 18px 16px; background: #fff; border: 1px solid #dce7e4; border-top: 3px solid #267a73; box-shadow: 0 4px 12px rgba(20, 67, 62, .08);}
        .overview-card.is-danger {border-top-color: #c94b4b;}
        .overview-card.is-success {border-top-color: #2d9b75;}
        .overview-card.is-info {border-top-color: #4e7690;}
        .overview-card .overview-label {display: block; padding-right: 48px; color: #61736e; font-size: 13px; font-weight: 600;}
        .overview-card .overview-value {display: block; margin-top: 6px; color: #213c38; font-size: 28px; font-weight: 600; line-height: 1;}
        .overview-card .overview-description {display: block; margin-top: 10px; color: #82918d; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
        .overview-card .overview-icon {position: absolute; top: 18px; right: 18px; color: #267a73; font-size: 24px; opacity: .85;}
        .overview-card.is-danger .overview-icon {color: #c94b4b;}
        .overview-card.is-success .overview-icon {color: #2d9b75;}
        .overview-card.is-info .overview-icon {color: #4e7690;}
        .dashboard-box {border: 1px solid #dce7e4; border-radius: 6px; box-shadow: 0 4px 12px rgba(20, 67, 62, .08);}
        .dashboard-box .box-header {min-height: 58px; padding: 16px 18px 12px; border-bottom: 1px solid #edf2f0;}
        .dashboard-box .box-title {color: #213c38; font-size: 16px; font-weight: 600;}
        .dashboard-box .box-tools {margin-top: -3px;}
        .dashboard-box .daterange {min-width: 126px; color: #276f69; background: #f3f8f7; border-color: #cfe0dc;}
        .dashboard-box .daterange:hover {color: #fff; background: #267a73; border-color: #267a73;}
        .dashboard-box .box-body {padding: 12px 18px 18px;}
        .dashboard-failure-list {margin: 0; padding: 0; list-style: none;}
        .dashboard-failure-item {display: block; padding: 12px 4px; border-bottom: 1px solid #edf2f0; color: inherit;}
        .dashboard-failure-item:last-child {border-bottom: 0;}
        .dashboard-failure-item:hover {background: #f7fbfa; color: inherit;}
        .dashboard-failure-title {display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #29423e; font-size: 13px; font-weight: 600;}
        .dashboard-failure-title strong {overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}
        .dashboard-failure-time {flex: 0 0 auto; color: #8a9995; font-size: 12px; font-weight: 400;}
        .dashboard-failure-message {margin-top: 5px; color: #7a4a4a; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;}
        .dashboard-empty {padding: 48px 12px; color: #82918d; text-align: center; font-size: 13px;}
        @media (max-width: 991px) {.overview-card {margin-bottom: 12px;}.dashboard-heading {display: block;}.dashboard-heading .dashboard-subtitle {display: block; margin-top: 5px;}}
    </style>
</head>
<body class="hold-transition skin-blue sidebar-mini <#if cookieMap?exists && cookieMap["xxljob_adminlte_settings"]?exists && "off" == cookieMap["xxljob_adminlte_settings"].value >sidebar-collapse</#if> ">
<div class="wrapper">
	<!-- header -->
	<@netCommon.commonHeader />
	<!-- left -->
	<@netCommon.commonLeft "index" />
	
	<!-- Content Wrapper. Contains page content -->
	<div class="content-wrapper">
		<!-- Content Header (Page header) -->
		<section class="content-header">
			<div class="dashboard-heading">
                <h1>${I18n.job_dashboard_name}</h1>
                <span class="dashboard-subtitle">${I18n.job_dashboard_subtitle}</span>
            </div>
		</section>

		<!-- Main content -->
		<section class="content">

            <div class="row dashboard-summary">
                <div class="col-lg-3 col-sm-6 col-xs-12">
                    <div class="overview-card">
                        <i class="fa fa-spinner overview-icon"></i>
                        <span class="overview-label">${I18n.job_dashboard_running_node}</span>
                        <span class="overview-value">${runningNodeCount}</span>
                        <span class="overview-description">${I18n.job_dashboard_running_node_tip}</span>
                    </div>
                </div>
                <div class="col-lg-3 col-sm-6 col-xs-12">
                    <div class="overview-card is-danger">
                        <i class="fa fa-exclamation-triangle overview-icon"></i>
                        <span class="overview-label">${I18n.job_dashboard_recent_failure}</span>
                        <span class="overview-value">${recentFailureCount}</span>
                        <span class="overview-description">${I18n.job_dashboard_recent_failure_tip}</span>
                    </div>
                </div>
                <div class="col-lg-3 col-sm-6 col-xs-12">
                    <div class="overview-card is-success">
                        <i class="fa fa-check-circle overview-icon"></i>
                        <span class="overview-label">${I18n.job_dashboard_today_success}</span>
                        <span class="overview-value">${todaySuccessCount}</span>
                        <span class="overview-description">${I18n.job_dashboard_today_success_tip}</span>
                    </div>
                </div>
                <div class="col-lg-3 col-sm-6 col-xs-12">
                    <div class="overview-card is-info">
                        <i class="fa fa-server overview-icon"></i>
                        <span class="overview-label">${I18n.job_dashboard_jobgroup_num}</span>
                        <span class="overview-value">${executorCount}</span>
                        <span class="overview-description">${I18n.job_dashboard_jobgroup_num_tip}</span>
                    </div>
                </div>
            </div>

            <div class="row">
                <div class="col-md-8">
                    <div class="box dashboard-box">
                        <div class="box-header with-border">
                            <h3 class="box-title">${I18n.job_dashboard_date_report}</h3>
                            <div class="pull-right box-tools">
                                <button type="button" class="btn btn-primary btn-sm daterange pull-right" data-toggle="tooltip" id="filterTime" >
                                    <i class="fa fa-calendar"></i> <span id="filterTimeText">${I18n.job_dashboard_recent_week}</span>
                                </button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div id="lineChart" style="height: 330px;"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="box dashboard-box">
                        <div class="box-header with-border">
                            <h3 class="box-title">${I18n.job_dashboard_recent_failure_list}</h3>
                        </div>
                        <div class="box-body">
                            <ul class="dashboard-failure-list" id="recentFailureList">
                                <#list recentFailures as failure>
                                    <li>
                                        <a class="dashboard-failure-item" href="${request.contextPath}/work/editModel?workId=${failure.workId?url('UTF-8')}">
                                            <div class="dashboard-failure-title"><strong>${failure.workName?html} / ${failure.nodeName?html}</strong><span class="dashboard-failure-time">${failure.occurredAt?html}</span></div>
                                            <div class="dashboard-failure-message" title="${failure.handleMsg?html}">${failure.handleMsg?html}</div>
                                        </a>
                                    </li>
                                <#else>
                                    <li class="dashboard-empty">${I18n.job_dashboard_no_failure}</li>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

		</section>
		<!-- /.content -->
	</div>
	<!-- /.content-wrapper -->
	
	<!-- footer -->
	<@netCommon.commonFooter />
</div>
<@netCommon.commonScript />
<!-- daterangepicker -->
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.js"></script>
<#-- echarts -->
<script src="${request.contextPath}/static/plugins/echarts/echarts.common.min.js"></script>
<script src="${request.contextPath}/static/js/index.js"></script>
</body>
</html>
