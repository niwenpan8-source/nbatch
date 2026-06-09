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
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/skins/skin-blue.min.css">
    <style>
        .content .row + .row {margin-top: 12px;}
        .box .box-body {overflow-x: auto;}
        .table > thead > tr > th, .table > tbody > tr > td {vertical-align: middle;}
        .table-action-cell {width: 180px; min-width: 160px; white-space: normal;}
        .table-action-cell .btn {margin: 0 4px 6px 0;}
        .dataTables_wrapper .row {margin-left: 0; margin-right: 0;}
        .modal-dialog {margin-top: 7vh;}
        .modal-content, .layui-layer {border: 0; border-radius: 8px; box-shadow: 0 14px 40px rgba(15, 23, 42, .22); overflow: hidden;}
        .modal-header, .layui-layer-title {background: #f8fafc; border-bottom: 1px solid #e5e7eb; color: #1f2937; font-weight: 600;}
        .modal-header {padding: 14px 18px;}
        .modal-title {font-size: 16px; font-weight: 600;}
        .modal-body {padding: 18px 20px; max-height: 70vh; overflow-y: auto;}
        body > .modal-body {max-height: none; min-height: 100vh; overflow: visible;}
        .modal-footer, .layui-layer-btn {background: #fff; border-top: 1px solid #edf2f7; padding: 12px 18px;}
        .layui-layer-title {height: 46px; line-height: 46px; padding: 0 80px 0 18px; font-size: 15px;}
        .layui-layer-setwin {top: 13px; right: 14px;}
        .layui-layer-iframe .layui-layer-content {background: #fff;}
        .layui-layer-btn {text-align: right;}
        .layui-layer-btn .layui-layer-btn0 {border-color: #3c8dbc; background-color: #3c8dbc;}
        .form-horizontal .form-group {margin-bottom: 14px;}
        .form-horizontal .control-label {color: #374151; font-weight: 500;}
        .form-control {border-radius: 4px; box-shadow: none;}
        .form-control:focus {border-color: #3c8dbc; box-shadow: 0 0 0 2px rgba(60, 141, 188, .12);}
        @media (max-width: 767px) {
            .content .row > [class^="col-xs-"], .content .row > [class*=" col-xs-"] {width: 100%; margin-bottom: 8px;}
            .table-action-cell {min-width: 180px;}
            .modal-dialog {width: auto; margin: 10px;}
            .modal-body {max-height: 76vh; padding: 14px;}
            body > .modal-body {max-height: none;}
            .form-horizontal .control-label {text-align: left; padding-top: 0;}
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
