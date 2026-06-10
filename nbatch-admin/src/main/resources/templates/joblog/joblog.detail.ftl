<!DOCTYPE html>
<html>
<head>
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <title>${I18n.admin_name}</title>
</head>
<body class="hold-transition skin-blue layout-top-nav">

<div class="wrapper">

    <header class="main-header">
        <nav class="navbar navbar-static-top">
            <div class="container">
                <#-- icon -->
                <div class="navbar-header">
                    <a class="navbar-brand"><b>${I18n.joblog_rolling_log}</b> Console</a>
                    <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse">
                        <i class="fa fa-bars"></i>
                    </button>
                </div>

                <#-- left nav -->
                <div class="collapse navbar-collapse pull-left" id="navbar-collapse">
                    <ul class="nav navbar-nav">
                        <#--<li class="active" ><a href="javascript:;">任务：<span class="sr-only">(current)</span></a></li>-->
                    </ul>
                </div>

                <#-- right nav -->
                <div class="navbar-custom-menu">
                    <ul class="nav navbar-nav">
                        <li>
                            <a href="javascript:window.location.reload();" >
                                <i class="fa fa-fw fa-refresh" ></i>
                                ${I18n.joblog_rolling_log_refresh}
                            </a>
                        </li>
                    </ul>
                </div>

            </div>
        </nav>
    </header>

    <div class="content-wrapper">
        <section class="content">
            <div class="box">
                <div class="box-header with-border">
                    <h3 class="box-title">${I18n.joblog_rolling_log}</h3>
                    <div class="box-tools pull-right">
                        <a class="btn btn-primary btn-sm" href="javascript:window.location.reload();"><i class="fa fa-refresh"></i> ${I18n.joblog_rolling_log_refresh}</a>
                    </div>
                </div>
                <div class="box-body">
                    <div id="logConsole" style="font-size:12px;white-space: pre-wrap; word-break: break-word;"></div>
                    <li class="fa fa-refresh fa-spin" style="font-size: 20px;display:none;" id="logConsoleRunning"></li>
                </div>
            </div>
        </section>
    </div>

    <!-- footer -->
    <@netCommon.commonFooter />

</div>

<@netCommon.commonScript />
<script>
    // 参数
    var triggerCode = '${triggerCode}';
    var handleCode = '${handleCode}';
    var logId = '${logId}';
</script>
<script src="${request.contextPath}/static/js/joblog.detail.1.js"></script>

</body>
</html>
