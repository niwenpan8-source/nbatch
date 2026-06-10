<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Error</title>
    <style type="text/css">
        html, body {height: 100%;}
        body {margin: 0; background: linear-gradient(180deg, #f8fafc 0, #eef2f7 100%); color: #1f2937; font-family: Arial, sans-serif; display: flex; align-items: center; justify-content: center;}
        .dialog {width: min(92vw, 720px); padding: 32px; background: #fff; border: 1px solid #e5e7eb; border-radius: 20px; box-shadow: 0 18px 48px rgba(15, 23, 42, .12); text-align: left;}
        h1 {margin: 0 0 12px; font-size: 28px; color: #dc2626; line-height: 1.2;}
        p {margin: 0 0 18px; font-size: 15px; color: #475569; word-break: break-word;}
        a {display: inline-block; padding: 10px 16px; background: #3c8dbc; color: #fff; border-radius: 8px; text-decoration: none;}
    </style>
</head>
<body>
	<div class="dialog">
	    <h1>System Error</h1>
	    <p>${exceptionMsg}</p>
		<a href="javascript:window.location.href='${request.contextPath}/'">Back</a>
	</div>

</body>
</html>
