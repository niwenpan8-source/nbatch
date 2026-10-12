<!DOCTYPE html>
<html>
<head>
  	<#import "./common/common.macro.ftl" as netCommon>
	<@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/plugins/iCheck/square/blue.css">
	<style>
		html,
		body.login-page {
			width: 100%;
			height: 100%;
			min-height: 100vh;
		}
		body.login-page {
			position: fixed;
			inset: 0;
			display: flex;
			align-items: center;
			justify-content: center;
			height: 100vh !important;
			min-height: 100vh;
			margin: 0;
			padding: 40px;
			overflow: auto;
			background: #536772 url("${request.contextPath}/static/img/login-lake.jpg") center center / cover no-repeat;
		}
		body.login-page::before {
			position: fixed;
			inset: 0;
			z-index: 0;
			content: "";
			background: rgba(12, 28, 36, .5);
			pointer-events: none;
		}
		.login-shell {
			position: relative;
			z-index: 1;
			display: flex;
			align-items: center;
			justify-content: center;
			width: min(100%, 410px);
			margin: 0;
		}
		.login-box {
			width: 100%;
			padding: 34px;
			border: 1px solid rgba(255, 255, 255, .7);
			border-radius: 8px;
			background: rgba(250, 253, 252, .93);
			box-shadow: 0 24px 64px rgba(4, 18, 24, .32);
		}
		.login-logo {
			margin: 0 0 28px;
			color: #1a3238;
			text-align: center;
			text-shadow: none;
		}
		.login-logo .login-brand {
			display: inline-flex;
			align-items: center;
			gap: 8px;
			color: #28736e;
			font-size: 12px;
			font-weight: 700;
			letter-spacing: 1.4px;
		}
		.login-logo h1 {
			margin: 11px 0 8px;
			font-size: 28px;
			font-weight: 600;
			letter-spacing: 0;
		}
		.login-logo p {
			margin: 0;
			color: #65767a;
			font-size: 13px;
			font-weight: 400;
			opacity: 1;
		}
		.login-box-body {
			padding: 0;
			background: transparent;
		}
		.login-box .form-group {
			position: relative;
			margin-bottom: 16px;
		}
		.login-box .input-icon {
			position: absolute;
			top: 0;
			left: 16px;
			z-index: 2;
			display: flex;
			align-items: center;
			height: 48px;
			color: #7f9194;
			font-size: 15px;
			pointer-events: none;
		}
		.login-box .form-control {
			height: 48px;
			padding: 0 18px 0 44px;
			border: 1px solid #d5dfdf;
			border-radius: 5px;
			background: #fff;
			box-shadow: none;
			color: #20313a;
			font-size: 15px;
		}
		.login-box .form-control:focus {
			border-color: #347d78;
			box-shadow: 0 0 0 3px rgba(52, 125, 120, .14);
		}
		.login-box .form-control-feedback {
			display: none;
		}
		.login-options {
			display: flex;
			align-items: center;
			min-height: 22px;
			margin: 2px 0 24px;
			color: #65767a;
			font-size: 13px;
			text-shadow: none;
		}
		.login-options .icheck {
			margin: 0;
		}
		.login-options label {
			font-weight: 400;
		}
		.login-box .btn-login {
			height: 48px;
			border: 0;
			border-radius: 5px;
			background: #216f6b;
			box-shadow: 0 8px 16px rgba(16, 83, 79, .22);
			font-size: 15px;
			font-weight: 600;
			letter-spacing: 0;
		}
		.login-box .btn-login:hover,
		.login-box .btn-login:focus {
			background: #175d59;
			box-shadow: 0 10px 20px rgba(16, 83, 79, .3);
		}
		.login-box .btn-login:active {
			background: #0d4f4c;
			box-shadow: none;
		}
		.login-box .help-block {
			margin: 7px 0 0;
			color: #c34545;
			font-size: 12px;
			text-shadow: none;
		}
		@media (max-width: 820px) {
			body.login-page { padding: 24px 18px; }
			.login-box { padding: 28px 24px; }
		}
	</style>
	<title>${I18n.admin_name}</title>
</head>

<body class="hold-transition login-page">
	<main class="login-shell">
		<section class="login-box" aria-label="${I18n.login_btn}">
			<div class="login-logo">
				<div class="login-brand"><i class="fa fa-sitemap" aria-hidden="true"></i> NBATCH</div>
				<h1>${I18n.login_btn}</h1>
				<p>${I18n.admin_name}</p>
			</div>
			<form id="loginForm" method="post" >
				<div class="login-box-body">
					<div class="form-group has-feedback">
						<span class="input-icon fa fa-user" aria-hidden="true"></span>
						<input type="text" name="userName" class="form-control" placeholder="${I18n.login_username_placeholder}" maxlength="20" autocomplete="username" >
						<span class="glyphicon glyphicon-envelope form-control-feedback"></span>
					</div>
					<div class="form-group has-feedback">
						<span class="input-icon fa fa-lock" aria-hidden="true"></span>
						<input type="password" name="password" class="form-control" placeholder="${I18n.login_password_placeholder}" maxlength="20" autocomplete="current-password" >
						<span class="glyphicon glyphicon-lock form-control-feedback"></span>
					</div>
					<div class="login-options">
						<div class="checkbox icheck">
							<label>
								<input type="checkbox" name="ifRemember" > &nbsp; ${I18n.login_remember_me}
							</label>
						</div>
					</div>
					<button type="submit" class="btn btn-primary btn-block btn-login">${I18n.login_btn}</button>
				</div>
			</form>
		</section>
	</main>
<@netCommon.commonScript />
<script src="${request.contextPath}/static/adminlte/plugins/iCheck/icheck.min.js"></script>
<script src="${request.contextPath}/static/js/login.1.js"></script>

</body>
</html>
