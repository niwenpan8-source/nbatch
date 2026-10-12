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
    <!-- Theme style -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/AdminLTE.min.css">
    <!-- Skin -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/skins/_all-skins.min.css">
    <style>
        html, body {min-height: 100%; background: #edf3f1; color: #203238;}
        .wrapper {background: #edf3f1;}
        .content-wrapper {background: linear-gradient(180deg, #f8fbfa 0, #edf3f1 100%);}
        .skin-blue .main-header .logo {background: #285f5d; color: #eef8f6; font-weight: 600; letter-spacing: .4px;}
        .skin-blue .main-header .logo:hover {background: #1f514f; color: #fff;}
        .skin-blue .main-header .navbar {background: #216f6b; box-shadow: 0 2px 12px rgba(18, 65, 63, .16);}
        .skin-blue .main-header .navbar .nav > li > a, .skin-blue .main-header .navbar .sidebar-toggle {color: #f5fbfa;}
        .skin-blue .main-header .navbar .nav > li > a:hover, .skin-blue .main-header .navbar .nav > li > a:focus, .skin-blue .main-header .navbar .sidebar-toggle:hover {background: rgba(0, 0, 0, .12);}
        .main-sidebar, .skin-blue .main-sidebar {background: #173f3e;}
        .skin-blue .sidebar-menu > li.header {padding: 18px 16px 8px; color: #8eadaa; background: transparent; font-size: 11px; letter-spacing: .8px;}
        .skin-blue .sidebar-menu > li > a {padding: 12px 16px; border-left: 3px solid transparent; color: #c7dcda;}
        .skin-blue .sidebar-menu > li > a:hover {border-left-color: #77b8ad; background: rgba(255, 255, 255, .07); color: #fff;}
        .skin-blue .sidebar-menu > li.active > a {border-left-color: #9bcfc5; background: #235957; color: #fff; font-weight: 600;}
        .skin-blue .sidebar-menu > li > a > .fa, .skin-blue .sidebar-menu > li > a > .glyphicon {color: #9bcfc5 !important;}
        .main-footer {border-top: 1px solid #d7e2df; background: #f8fbfa; color: #70827f;}
        .content-header {padding: 18px 20px 0;}
        .content-header h1 {margin: 0; color: #1e3739; font-size: 24px; font-weight: 700; letter-spacing: 0;}
        .content {padding: 18px 20px 28px;}
        .content .row + .row {margin-top: 12px;}
        .content .input-group {width: 100%;}
        .box {border: 1px solid #d8e4e1; border-radius: 8px; box-shadow: 0 8px 24px rgba(20, 58, 56, .08); overflow: hidden; background: #fff;}
        .box-header {padding: 18px 20px 0;}
        .box-title {font-size: 16px; font-weight: 600; color: #1e3739;}
        .box .box-body {padding: 20px; overflow-x: auto; overflow-y: visible;}
        .list-page .content > .row + .row {margin-top: 16px;}
        .list-page .box-header.with-border {display: flex; align-items: center; min-height: 64px; padding: 14px 20px; border-bottom: 1px solid #e4edeb;}
        .list-page .box-header.with-border .box-title {line-height: 32px;}
        .list-page .box-header.with-border .box-tools {position: static; float: none !important; margin-left: auto;}
        .list-page .box-header.with-border .box-tools .btn {height: 34px; min-width: 82px;}
        .list-filter-row {display: flex; flex-wrap: wrap; align-items: stretch; margin: -6px;}
        .list-filter-row > [class*="col-"] {padding: 6px;}
        .list-filter-row .input-group, .list-filter-row .form-control {height: 34px;}
        .list-filter-row #searchBtn {height: 34px; margin: 0;}
        .list-table-box .box-body {padding: 16px 20px 20px;}
        .list-table-box .dataTables_wrapper > .row:first-child {padding-bottom: 12px;}
        .list-table-box .dataTables_wrapper > .row:last-child {padding-top: 12px;}
        .box .box-body.dropdown-open {padding-bottom: 180px;}
        .table {margin-bottom: 0; table-layout: auto;}
        .table-responsive-wrap {width: 100%; overflow-x: auto; overflow-y: visible;}
        .table-fixed-wide {table-layout: fixed;}
        .table > thead > tr > th {background: #f3f8f6; color: #36504e; font-weight: 600; white-space: nowrap;}
        .table > tbody > tr:hover {background: #f7fbfa;}
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
        .btn {border-radius: 4px; font-weight: 500; transition: background-color .15s ease, border-color .15s ease, box-shadow .15s ease, color .15s ease;}
        .btn-default, .btn-default:focus {border-color: #b9d0cb; background-color: #f7fbfa; color: #285f5d;}
        .btn-default:hover, .btn-default:active, .btn-default.active, .open > .dropdown-toggle.btn-default {border-color: #83afa8; background-color: #e8f4f1; color: #175d59;}
        .btn-primary, .btn-primary:focus {border-color: #216f6b; background-color: #216f6b;}
        .btn-primary:hover, .btn-primary:active, .btn-primary.active, .open > .dropdown-toggle.btn-primary {border-color: #175d59; background-color: #175d59;}
        .btn-info, .btn-info:focus {border-color: #347d78; background-color: #347d78;}
        .btn-info:hover, .btn-info:active, .btn-info.active, .open > .dropdown-toggle.btn-info {border-color: #286b67; background-color: #286b67;}
        .btn-success, .btn-success:focus {border-color: #3f9568; background-color: #3f9568;}
        .btn-success:hover, .btn-success:active, .btn-success.active, .open > .dropdown-toggle.btn-success {border-color: #317b54; background-color: #317b54;}
        .btn-warning, .btn-warning:focus {border-color: #66815b; background-color: #66815b; color: #fff;}
        .btn-warning:hover, .btn-warning:active, .btn-warning.active, .open > .dropdown-toggle.btn-warning {border-color: #516c49; background-color: #516c49; color: #fff;}
        .btn-danger {border-radius: 4px;}
        .layui-btn:not(.layui-btn-disabled), .layui-btn-normal:not(.layui-btn-disabled) {border-color: #216f6b !important; background-color: #216f6b !important;}
        .layui-btn:not(.layui-btn-disabled):hover, .layui-btn-normal:not(.layui-btn-disabled):hover {border-color: #175d59 !important; background-color: #175d59 !important;}
        .layui-btn-primary:not(.layui-btn-disabled) {border-color: #b9d0cb !important; background-color: #f7fbfa !important; color: #285f5d !important;}
        .layui-btn-primary:not(.layui-btn-disabled):hover {border-color: #83afa8 !important; background-color: #e8f4f1 !important; color: #175d59 !important;}
        .box-body #searchBtn {min-width: 82px; height: 34px; border: 0; border-radius: 4px; background: #216f6b; font-weight: 500; box-shadow: 0 6px 14px rgba(20, 101, 96, .2);}
        .box-body #searchBtn:hover, .box-body #searchBtn:focus {background: #175d59; box-shadow: 0 8px 18px rgba(20, 101, 96, .26);}
        .box-body #searchBtn:active {background: #124d4a; box-shadow: none;}
        .box-body .input-group-addon {background: #f3f8f6; border-color: #d3e1dd; color: #52706b; font-weight: 500;}
        .form-control {border-color: #ccdcd8; color: #263b3d;}
        .form-control:focus {border-color: #3d8882; box-shadow: 0 0 0 2px rgba(33, 111, 107, .14);}
        .pagination > .active > a, .pagination > .active > span, .pagination > .active > a:hover, .pagination > .active > span:hover {border-color: #216f6b; background-color: #216f6b;}
        .dataTables_wrapper .row {margin-left: 0; margin-right: 0;}
        .dataTables_wrapper .dataTables_info {padding-top: 12px; color: #6b7280;}
        .dataTables_wrapper .dataTables_paginate {padding-top: 8px;}
        .modal-dialog {margin: 5vh auto 20px;}
        .modal-content {border: 0; border-radius: 8px; box-shadow: 0 18px 48px rgba(20, 58, 56, .22); overflow: hidden; display: flex; flex-direction: column; max-height: calc(100vh - 40px);}
        .layui-layer {border: 0; border-radius: 8px; box-shadow: 0 18px 48px rgba(20, 58, 56, .22); overflow: visible;}
        .modal-header, .layui-layer-title {background: #f3f8f6; border-bottom: 1px solid #dce8e5; color: #1e3739; font-weight: 600;}
        .modal-header {position: relative; padding: 16px 56px 16px 20px; flex: 0 0 auto;}
        .modal-header .close {position: absolute; right: 14px; top: 12px; width: 32px; height: 32px; line-height: 30px; border-radius: 16px; color: #64748b; opacity: .9;}
        .modal-header .close:hover {background: rgba(15, 23, 42, .06); color: #111827; opacity: 1;}
        .modal-title {font-size: 16px; font-weight: 600;}
        .modal-body {padding: 20px 22px; overflow-y: auto; overflow-x: hidden; flex: 1 1 auto;}
        .modal .form-horizontal > hr {margin: 22px 0 14px; border-top-color: #e4edeb;}
        .modal .form-horizontal > hr + .form-group {margin-bottom: 0;}
        .modal .form-horizontal > hr + .form-group > [class*="col-"] {display: flex; flex-wrap: wrap; gap: 8px; align-items: center;}
        .modal .form-horizontal > hr + .form-group .btn {min-width: 82px;}
        .modal-footer, .layui-layer-btn {background: #fff; border-top: 1px solid #e4edeb; padding: 14px 18px; flex: 0 0 auto;}
        .layui-layer-title {height: 48px; line-height: 48px; padding: 0 80px 0 18px; font-size: 15px;}
        .layui-layer-setwin {top: 13px; right: 14px;}
        .layui-layer-setwin .layui-layer-max {display: none;}
        .layui-layer-iframe .layui-layer-content {background: #fff; overflow: hidden; border-radius: 0 0 8px 8px;}
        .layui-layer-page .layui-layer-content {overflow: visible;}
        .layui-layer-content .modal-body {overflow-x: hidden;}
        .layui-layer-btn {text-align: right;}
        .layui-layer-btn a {min-width: 72px; height: 32px; margin-left: 8px; padding: 0 14px; border-color: #b9d0cb !important; border-radius: 4px; background: #f7fbfa !important; color: #285f5d !important; font-weight: 500; line-height: 30px; text-align: center; transition: background-color .15s ease, border-color .15s ease, color .15s ease;}
        .layui-layer-btn a:hover {border-color: #83afa8 !important; background: #e8f4f1 !important; color: #175d59 !important;}
        .layui-layer-btn .layui-layer-btn0 {border-color: #216f6b !important; background-color: #216f6b !important; color: #fff !important;}
        .layui-layer-btn .layui-layer-btn0:hover {border-color: #175d59 !important; background-color: #175d59 !important; color: #fff !important;}
        .form-horizontal .form-group {margin-bottom: 14px;}
        .form-horizontal .row, .form-horizontal .form-group {margin-left: 0; margin-right: 0;}
        .form-horizontal [class^="col-sm-"], .form-horizontal [class*=" col-sm-"] {padding-left: 12px; padding-right: 12px;}
        .form-horizontal .control-label {color: #374151; font-weight: 500; padding-top: 7px; word-break: break-word;}
        .form-horizontal .form-section-title, .form-horizontal p[style*="border-bottom"] {padding-bottom: 7px; margin-top: 6px !important; color: #6b7280 !important; font-weight: 600;}
        .form-control {border-radius: 4px; box-shadow: none;}
        textarea.form-control {resize: vertical;}
        body > .modal-body {max-height: none; min-height: 100vh; overflow-y: auto; overflow-x: hidden; padding: 20px; background: #edf3f1;}
        body > .modal-body > form {background: #fff; border: 1px solid #d8e4e1; border-radius: 8px; box-shadow: 0 10px 30px rgba(20, 58, 56, .08); padding: 22px 22px 8px;}
        body > .modal-body > form > hr {margin: 18px -22px 14px; border-top-color: #e4edeb;}
        body > .modal-body > form > .form-group:last-child {position: sticky; bottom: -20px; z-index: 3; margin: 12px -22px -8px; padding: 14px 22px; background: #fff; border-top: 1px solid #e4edeb; border-radius: 0 0 8px 8px;}
        .info-box {border-radius: 8px; box-shadow: 0 8px 22px rgba(20, 58, 56, .12); overflow: hidden;}
        .bg-aqua, .info-box.bg-aqua {background-color: #2d7e79 !important;}
        .bg-yellow, .info-box.bg-yellow {background-color: #c78738 !important;}
        .bg-green, .info-box.bg-green {background-color: #527087 !important;}
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
    <script src="${request.contextPath}/static/js/password-transfer.1.js"></script>
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
							<div class="col-sm-10"><input type="password" class="form-control" name="oldPassword" placeholder="${I18n.system_please_input} ${I18n.change_pwd_field_oldpwd}" maxlength="20" ></div>
						</div>
						<div class="form-group">
							<label for="lastname" class="col-sm-2 control-label">${I18n.change_pwd_field_newpwd}<font color="red">*</font></label>
							<div class="col-sm-10"><input type="password" class="form-control" name="password" placeholder="${I18n.system_please_input} ${I18n.change_pwd_field_newpwd}" maxlength="20" ></div>
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
                <li class="nav-click <#if pageName == "index">active</#if>" ><a href="${request.contextPath}/"><i class="fa fa-circle-o text-aqua"></i><span>${I18n.job_dashboard_name}</span></a></li>
				<#if Request["XXL_JOB_LOGIN_IDENTITY"].role == 1>
                    <li class="nav-click <#if pageName == "jobgroup">active</#if>" ><a href="${request.contextPath}/jobgroup"><i class="fa fa-circle-o text-red"></i><span>${I18n.jobgroup_name}</span></a></li>
                    <li class="nav-click <#if pageName == "user">active</#if>" ><a href="${request.contextPath}/user"><i class="fa fa-circle-o text-purple"></i><span>${I18n.user_manage}</span></a></li>
				</#if>
				<li class="nav-click <#if pageName == "work">active</#if>" >
					<a href="${request.contextPath}/work">
						<i class="fa fa-circle-o text-blue"></i><span>${I18n.job_work}</span>
					</a>
				</li>
				<li style="display:none" class="nav-click <#if pageName == "node">active</#if>" >
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
