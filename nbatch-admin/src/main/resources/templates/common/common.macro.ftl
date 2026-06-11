	<#macro commonStyle>

	<#-- favicon -->
	<link rel="icon" href="${request.contextPath}/static/favicon.ico" />

	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <!-- Tell the browser to be responsive to screen width -->
    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport">
    <!-- Bootstrap -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/bootstrap/css/bootstrap.min.css">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/font-awesome/css/font-awesome.min.css">
    <!-- Ionicons -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/Ionicons/css/ionicons.min.css">
    <!-- Theme style -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/AdminLTE.min.css">
    <!-- Skin -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/skins/_all-skins.min.css">
    <style>
        html, body {min-height: 100%; background: #f3f6fb; color: #1f2937;}
        .content-wrapper {background: linear-gradient(180deg, #f8fafc 0, #f3f6fb 100%);}
        .content-header {padding: 18px 20px 0;}
        .content-header h1 {margin: 0; color: #111827; font-size: 24px; font-weight: 700; letter-spacing: -.02em;}
        .content {padding: 18px 20px 28px;}
        .content .row + .row {margin-top: 12px;}
        .content .input-group {width: 100%;}
        .box {border: 1px solid #e5e7eb; border-radius: 14px; box-shadow: 0 8px 24px rgba(15, 23, 42, .08); overflow: hidden; background: #fff;}
        .box-header {padding: 18px 20px 0;}
        .box-title {font-size: 16px; font-weight: 600; color: #111827;}
        .box .box-body {padding: 20px; overflow-x: auto; overflow-y: visible;}
        .box .box-body.dropdown-open {padding-bottom: 180px;}
        .table {margin-bottom: 0; table-layout: auto;}
        .table-responsive-wrap {width: 100%; overflow-x: auto; overflow-y: visible;}
        .table-fixed-wide {table-layout: fixed;}
        .table > thead > tr > th {background: #f8fafc; color: #374151; font-weight: 600; white-space: nowrap;}
        .table > thead > tr > th, .table > tbody > tr > td {vertical-align: middle;}
        .table > tbody > tr > td {word-break: break-word;}
        .table .label {display: inline-block; min-width: 48px; padding: 4px 7px;}
        .table-action-cell {width: 120px; min-width: 110px; white-space: nowrap; text-align: left;}
        .table-action-cell .btn {margin: 0 4px 6px 0;}
        .table-action-cell .btn-group .btn {margin: 0;}
        .table-action-cell .btn-group .dropdown-toggle {padding-left: 10px; padding-right: 10px;}
        .table-action-buttons {display: flex; flex-wrap: wrap; gap: 6px; align-items: center;}
        .table-action-buttons .btn {margin: 0;}
        .table .dropdown-menu {z-index: 2147483647; min-width: 128px;}
        .table td .btn-group, .table td .dropdown {position: relative;}
        .box-header.with-border {padding-bottom: 14px;}
        .box-header .box-tools .btn {min-width: 82px; border-radius: 4px; font-weight: 500;}
        .box-body #searchBtn {min-width: 82px; height: 34px; border: 0; border-radius: 4px; background: #2563eb; font-weight: 500; box-shadow: 0 6px 14px rgba(37, 99, 235, .22);}
        .box-body #searchBtn:hover, .box-body #searchBtn:focus {background: #1d4ed8; box-shadow: 0 8px 18px rgba(37, 99, 235, .28);}
        .box-body #searchBtn:active {background: #1e40af; box-shadow: none;}
        .box-body .input-group-addon {background: #f8fafc; border-color: #dbe3ef; color: #475569; font-weight: 500;}
        .dataTables_wrapper .row {margin-left: 0; margin-right: 0;}
        .dataTables_wrapper .dataTables_info {padding-top: 12px; color: #6b7280;}
        .dataTables_wrapper .dataTables_paginate {padding-top: 8px;}
        .modal-dialog {margin: 5vh auto 20px;}
        .modal-content {border: 0; border-radius: 16px; box-shadow: 0 18px 48px rgba(15, 23, 42, .22); overflow: hidden; display: flex; flex-direction: column; max-height: calc(100vh - 40px);}
        .layui-layer {border: 0; border-radius: 16px; box-shadow: 0 18px 48px rgba(15, 23, 42, .22); overflow: visible;}
        .modal-header, .layui-layer-title {background: #f8fafc; border-bottom: 1px solid #e5e7eb; color: #1f2937; font-weight: 600;}
        .modal-header {position: relative; padding: 16px 56px 16px 20px; flex: 0 0 auto;}
        .modal-header .close {position: absolute; right: 14px; top: 12px; width: 32px; height: 32px; line-height: 30px; border-radius: 16px; color: #64748b; opacity: .9;}
        .modal-header .close:hover {background: rgba(15, 23, 42, .06); color: #111827; opacity: 1;}
        .modal-title {font-size: 16px; font-weight: 600;}
        .modal-body {padding: 20px 22px; overflow-y: auto; overflow-x: hidden; flex: 1 1 auto;}
        .modal-footer, .layui-layer-btn {background: #fff; border-top: 1px solid #edf2f7; padding: 14px 18px; flex: 0 0 auto;}
        .layui-layer-title {height: 48px; line-height: 48px; padding: 0 80px 0 18px; font-size: 15px;}
        .layui-layer-setwin {top: 13px; right: 14px;}
        .layui-layer-iframe .layui-layer-content {background: #fff; overflow: hidden; border-radius: 0 0 16px 16px;}
        .layui-layer-page .layui-layer-content {overflow: visible;}
        .layui-layer-content .modal-body {overflow-x: hidden;}
        .layui-layer-btn {text-align: right;}
        .layui-layer-btn .layui-layer-btn0 {border-color: #3c8dbc; background-color: #3c8dbc;}
        .form-horizontal .form-group {margin-bottom: 14px;}
        .form-horizontal .row, .form-horizontal .form-group {margin-left: 0; margin-right: 0;}
        .form-horizontal [class^="col-sm-"], .form-horizontal [class*=" col-sm-"] {padding-left: 12px; padding-right: 12px;}
        .form-horizontal .control-label {color: #374151; font-weight: 500; padding-top: 7px; word-break: break-word;}
        .form-horizontal .form-section-title, .form-horizontal p[style*="border-bottom"] {padding-bottom: 7px; margin-top: 6px !important; color: #6b7280 !important; font-weight: 600;}
        .form-control {border-radius: 4px; box-shadow: none;}
        .form-control:focus {border-color: #3c8dbc; box-shadow: 0 0 0 2px rgba(60, 141, 188, .12);}
        textarea.form-control {resize: vertical;}
        body > .modal-body {max-height: none; min-height: 100vh; overflow-y: auto; overflow-x: hidden; padding: 20px; background: #f3f6fb;}
        body > .modal-body > form {background: #fff; border: 1px solid #e5e7eb; border-radius: 16px; box-shadow: 0 10px 30px rgba(15, 23, 42, .08); padding: 22px 22px 8px;}
        body > .modal-body > form > hr {margin: 18px -22px 14px; border-top-color: #edf2f7;}
        body > .modal-body > form > .form-group:last-child {position: sticky; bottom: -20px; z-index: 3; margin: 12px -22px -8px; padding: 14px 22px; background: #fff; border-top: 1px solid #edf2f7; border-radius: 0 0 16px 16px;}
        .cronGen, .cronGen-panel, .cronGen-popover, .popover {z-index: 2147483647 !important;}
        .cron-gen-popover {max-width: calc(100vw - 48px) !important; margin: 0; border-radius: 6px; box-shadow: 0 10px 30px rgba(15, 23, 42, .22);}
        .cron-gen-popover .popover-content {max-height: 56vh; overflow-y: auto; overflow-x: hidden; padding: 14px 16px;}
        .cron-gen-popover #CronGenMainDiv {width: 100%; min-width: 0;}
        .cron-gen-popover #CronGenTabs {display: flex; flex-wrap: wrap; border-bottom: 1px solid #ddd;}
        .cron-gen-popover #CronGenTabs > li > a {padding: 8px 12px;}
        .cron-gen-popover .tab-content {overflow-x: hidden;}
        .cron-gen-popover .line, .cron-gen-popover .imp {white-space: normal; line-height: 1.9;}
        .cron-gen-popover input[type="checkbox"], .cron-gen-popover input[type="radio"] {margin-right: 3px; vertical-align: middle;}
        .cron-gen-popover textarea#runTime {width: 100% !important; min-height: 88px;}
        .cron-gen-trigger {min-width: 42px;}
        @media (max-width: 767px) {
            .content {padding: 14px 14px 20px;}
            .content-header {padding: 14px 14px 0;}
            .content .row > [class^="col-xs-"], .content .row > [class*=" col-xs-"] {width: 100%; margin-bottom: 8px;}
            .table-action-cell {min-width: 200px;}
            .modal-dialog {width: auto; margin: 10px;}
            .modal-content {max-height: calc(100vh - 20px);}
            .modal-body {padding: 16px;}
            body > .modal-body {max-height: none;}
            .form-horizontal .control-label {text-align: left; padding-top: 0;}
            .form-horizontal [class^="col-sm-"], .form-horizontal [class*=" col-sm-"] {padding-left: 0; padding-right: 0;}
        }
    </style>

	<#-- i18n -->
	<#global I18n = I18nUtil.getMultString()?eval />

</#macro>

<#macro commonScript>
	<!-- jQuery -->
	<script src="${request.contextPath}/static/adminlte/bower_components/jquery/jquery.min.js"></script>
	<!-- Bootstrap -->
	<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap/js/bootstrap.min.js"></script>
	<!-- FastClick -->
	<script src="${request.contextPath}/static/adminlte/bower_components/fastclick/fastclick.js"></script>
	<!-- AdminLTE App -->
	<script src="${request.contextPath}/static/adminlte/dist/js/adminlte.min.js"></script>
	<!-- jquery.slimscroll -->
	<script src="${request.contextPath}/static/adminlte/bower_components/jquery-slimscroll/jquery.slimscroll.min.js"></script>

    <#-- jquery cookie -->
	<script src="${request.contextPath}/static/plugins/jquery/jquery.cookie.js"></script>
	<#-- jquery.validate -->
	<script src="${request.contextPath}/static/plugins/jquery/jquery.validate.min.js"></script>

	<#-- layer -->
	<script src="${request.contextPath}/static/plugins/layer/layer.js"></script>

    <script>
		var base_url = '${request.contextPath}';
        var I18n = ${I18nUtil.getMultString()};
	</script>
	<#-- common -->
    <script src="${request.contextPath}/static/js/common.1.js"></script>

</#macro>

<#macro commonHeader>
	<header class="main-header">
		<a href="${request.contextPath}/" class="logo">
			<span class="logo-mini"><b>NB</b></span>
			<span class="logo-lg"><b>${I18n.admin_name}</b></span>
		</a>
		<nav class="navbar navbar-static-top" role="navigation">

			<a href="#" class="sidebar-toggle" data-toggle="push-menu" role="button">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </a>

          	<div class="navbar-custom-menu">
				<ul class="nav navbar-nav">
					<#-- login user -->
                    <li class="dropdown">
                        <a href="javascript:" class="dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
                            ${I18n.system_welcome} ${Request["XXL_JOB_LOGIN_IDENTITY"].username}
                            <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu" role="menu">
                            <li id="updatePwd" ><a href="javascript:">${I18n.change_pwd}</a></li>
                            <li id="logoutBtn" ><a href="javascript:">${I18n.logout_btn}</a></li>
                        </ul>
                    </li>
				</ul>
			</div>

		</nav>
	</header>

	<!-- 修改密码.模态框 -->
	<div class="modal fade" id="updatePwdModal" tabindex="-1" role="dialog"  aria-hidden="true">
		<div class="modal-dialog ">
			<div class="modal-content">
				<div class="modal-header">
					<h4 class="modal-title" >${I18n.change_pwd}</h4>
				</div>
				<div class="modal-body">
					<form class="form-horizontal form" role="form" >
						<div class="form-group">
							<label for="lastname" class="col-sm-2 control-label">${I18n.change_pwd_field_oldpwd}<font color="red">*</font></label>
							<div class="col-sm-10"><input type="text" class="form-control" name="oldPassword" placeholder="${I18n.system_please_input} ${I18n.change_pwd_field_oldpwd}" maxlength="20" ></div>
						</div>
						<div class="form-group">
							<label for="lastname" class="col-sm-2 control-label">${I18n.change_pwd_field_newpwd}<font color="red">*</font></label>
							<div class="col-sm-10"><input type="text" class="form-control" name="password" placeholder="${I18n.system_please_input} ${I18n.change_pwd_field_newpwd}" maxlength="20" ></div>
						</div>
						<hr>
						<div class="form-group">
							<div class="col-sm-offset-3 col-sm-6">
								<button type="submit" class="btn btn-primary"  >${I18n.system_save}</button>
								<button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
	</div>

</#macro>

<#macro commonLeft pageName >
	<!-- Left side column. contains the logo and sidebar -->
	<aside class="main-sidebar">
		<!-- sidebar: style can be found in sidebar.less -->
		<section class="sidebar">
			<!-- sidebar menu: : style can be found in sidebar.less -->
			<ul class="sidebar-menu">
                <li class="header">${I18n.system_nav}</li>
                <li class="nav-click <#if pageName == "index">active</#if>" ><a href="${request.contextPath}/"><i class="fa fa-circle-o text-aqua"></i><span>${I18n.job_dashboard_name}</span></a></li>
				<li class="nav-click <#if pageName == "jobinfo">active</#if>" ><a href="${request.contextPath}/jobinfo"><i class="fa fa-circle-o text-yellow"></i><span>${I18n.jobinfo_name}</span></a></li>
				<li class="nav-click <#if pageName == "joblog">active</#if>" ><a href="${request.contextPath}/joblog"><i class="fa fa-circle-o text-green"></i><span>${I18n.joblog_name}</span></a></li>
				<#if Request["XXL_JOB_LOGIN_IDENTITY"].role == 1>
                    <li class="nav-click <#if pageName == "jobgroup">active</#if>" ><a href="${request.contextPath}/jobgroup"><i class="fa fa-circle-o text-red"></i><span>${I18n.jobgroup_name}</span></a></li>
                    <li class="nav-click <#if pageName == "user">active</#if>" ><a href="${request.contextPath}/user"><i class="fa fa-circle-o text-purple"></i><span>${I18n.user_manage}</span></a></li>
				</#if>
				<li class="nav-click <#if pageName == "work">active</#if>" >
					<a href="${request.contextPath}/work">
						<i class="fa fa-circle-o text-blue"></i><span>${I18n.job_work}</span>
					</a>
				</li>
				<li class="nav-click <#if pageName == "node">active</#if>" >
					<a href="${request.contextPath}/node">
						<i class="fa fa-circle-o text-aqua"></i><span>${I18n.job_work_node}</span>
					</a>
				</li>
			</ul>
		</section>
		<!-- /.sidebar -->
	</aside>
</#macro>



<#macro commonFooter >
	<footer class="main-footer">
        Powered by <b>NBATCH</b> ${I18n.admin_version}
	</footer>
</#macro>
