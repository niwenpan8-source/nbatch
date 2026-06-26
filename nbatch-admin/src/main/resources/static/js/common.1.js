// DataTables shared i18n config
var dataTableI18n = {
	"sProcessing": I18n.dataTable_sProcessing,
	"sLengthMenu": I18n.dataTable_sLengthMenu,
	"sZeroRecords": I18n.dataTable_sZeroRecords,
	"sInfo": I18n.dataTable_sInfo,
	"sInfoEmpty": I18n.dataTable_sInfoEmpty,
	"sInfoFiltered": I18n.dataTable_sInfoFiltered,
	"sInfoPostFix": "",
	"sSearch": I18n.dataTable_sSearch,
	"sUrl": "",
	"sEmptyTable": I18n.dataTable_sEmptyTable,
	"sLoadingRecords": I18n.dataTable_sLoadingRecords,
	"sInfoThousands": ",",
	"oPaginate": {
		"sFirst": I18n.dataTable_sFirst,
		"sPrevious": I18n.dataTable_sPrevious,
		"sNext": I18n.dataTable_sNext,
		"sLast": I18n.dataTable_sLast
	},
	"oAria": {
		"sSortAscending": I18n.dataTable_sSortAscending,
		"sSortDescending": I18n.dataTable_sSortDescending
	}
};

// Resolve iframe popup size against the current viewport to avoid clipped layer dialogs on small screens.
function getLayerArea(width, height) {
    var viewportWidth = $(window).width();
    var viewportHeight = $(window).height();
    var resolvedWidth = Math.min(width, Math.max(320, viewportWidth - 64));
    var resolvedHeight = Math.min(height, Math.max(320, viewportHeight - 64));
    return [resolvedWidth + 'px', resolvedHeight + 'px'];
}

function fitCronPopovers() {
    $('.cron-gen-popover:visible').each(function () {
        var $popover = $(this);
        var $button = $('[aria-describedby="' + $popover.attr('id') + '"]');
        if (!$button.length || !$button.is(':visible')) {
            $popover.hide();
            return;
        }
        var $modalBody = $button.closest('.modal-body');
        var boundaryLeft = 12;
        var boundaryRight = $(window).width() - 12;
        var boundaryTop = 12;
        if ($modalBody.length) {
            var modalOffset = $modalBody.offset();
            boundaryLeft = modalOffset.left + 12;
            boundaryRight = modalOffset.left + $modalBody.outerWidth() - 24;
            boundaryTop = modalOffset.top + 12;
        }
        var availableWidth = Math.max(280, boundaryRight - boundaryLeft);
        var popoverWidth = Math.min(420, availableWidth);
        var buttonOffset = $button.offset();
        if (!buttonOffset) {
            return;
        }
        var left = Math.min(buttonOffset.left + $button.outerWidth() - popoverWidth, boundaryRight - popoverWidth);
        left = Math.max(boundaryLeft, left);
        var top = buttonOffset.top + $button.outerHeight() + 10;
        if ($modalBody.length) {
            top = Math.max(boundaryTop, Math.min(top, boundaryTop + $modalBody.outerHeight() - 80));
        }
        $popover.css({width: popoverWidth, maxWidth: popoverWidth, left: left, top: top, display: 'block'});
        $popover.find('.arrow').css({left: Math.max(24, Math.min(popoverWidth - 24, buttonOffset.left + ($button.outerWidth() / 2) - left))});
    });
}

function hideCronPopovers() {
    $('.cron-gen-trigger').popover('hide');
    $('.cron-gen-popover').remove();
}

function ensureModalCloseButtons() {
    $('.modal-header').each(function () {
        var $header = $(this);
        if ($header.find('.close').length) {
            return;
        }
        $header.append('<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>');
    });
}

function syncModalStructure() {
    $('.modal').each(function () {
        var $modal = $(this);
        var $content = $modal.find('.modal-content').first();
        if (!$content.length) {
            return;
        }
        var $header = $content.children('.modal-header').first();
        var $body = $content.children('.modal-body').first();
        var $footer = $content.children('.modal-footer').first();
        if ($header.length) {
            ensureModalCloseButtons();
        }
        if ($body.length) {
            $body.addClass('modal-body-scroll');
        }
        if ($footer.length) {
            $footer.addClass('modal-footer-fixed');
        }
    });
}

function getModalArea(width, height) {
    var viewportWidth = $(window).width();
    var viewportHeight = $(window).height();
    var resolvedWidth = Math.min(width, Math.max(360, viewportWidth - 48));
    var resolvedHeight = Math.min(height, Math.max(360, viewportHeight - 48));
    return [resolvedWidth + 'px', resolvedHeight + 'px'];
}

$(function(){

    $(document).on('show.bs.dropdown', '.box-body .dropdown, .box-body .btn-group', function () {
        $(this).closest('.box-body').addClass('dropdown-open');
    });

    $(document).on('hidden.bs.dropdown', '.box-body .dropdown, .box-body .btn-group', function () {
        $(this).closest('.box-body').removeClass('dropdown-open');
    });

    $(document).on('shown.bs.popover', fitCronPopovers);
    $(document).on('hidden.bs.modal', hideCronPopovers);
    $(document).on('shown.bs.modal', syncModalStructure);
    $(window).on('resize', fitCronPopovers);
    $(document).on('scroll', '.modal-body, .layui-layer-content', fitCronPopovers);
    ensureModalCloseButtons();
    syncModalStructure();

	// logout
	$("#logoutBtn").click(function(){
		layer.confirm( I18n.logout_confirm , {
			icon: 3,
			title: I18n.system_tips ,
            btn: [ I18n.system_ok, I18n.system_cancel ]
		}, function(index){
			layer.close(index);

			$.post(base_url + "/logout", function(data, status) {
				if (data.code == "200") {
                    layer.msg( I18n.logout_success );
                    setTimeout(function(){
                        window.location.href = base_url + "/";
                    }, 500);
				} else {
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: (data.msg || I18n.logout_fail),
						icon: '2'
					});
				}
			});
		});

	});

	// slideToTop
	var slideToTop = $("<div />");
	slideToTop.html('<i class="fa fa-chevron-up"></i>');
	slideToTop.css({
		position: 'fixed',
		bottom: '20px',
		right: '25px',
		width: '40px',
		height: '40px',
		color: '#eee',
		'font-size': '',
		'line-height': '40px',
		'text-align': 'center',
		'background-color': '#222d32',
		cursor: 'pointer',
		'border-radius': '5px',
		'z-index': '99999',
		opacity: '.7',
		'display': 'none'
	});
	slideToTop.on('mouseenter', function () {
		$(this).css('opacity', '1');
	});
	slideToTop.on('mouseout', function () {
		$(this).css('opacity', '.7');
	});
	$('.wrapper').append(slideToTop);
	$(window).scroll(function () {
		if ($(window).scrollTop() >= 150) {
			if (!$(slideToTop).is(':visible')) {
				$(slideToTop).fadeIn(500);
			}
		} else {
			$(slideToTop).fadeOut(500);
		}
	});
	$(slideToTop).click(function () {
		$("html,body").animate({		// firefox ie not support body, chrome support body. but found that new version chrome not support body too.
			scrollTop: 0
		}, 100);
	});

	// left menu status v: js + server + cookie
	$('.sidebar-toggle').click(function(){
		var xxljob_adminlte_settings = $.cookie('xxljob_adminlte_settings');	// on=open，off=close
		if ('off' == xxljob_adminlte_settings) {
            xxljob_adminlte_settings = 'on';
		} else {
            xxljob_adminlte_settings = 'off';
		}
		$.cookie('xxljob_adminlte_settings', xxljob_adminlte_settings, { expires: 7 });	//$.cookie('the_cookie', '', { expires: -1 });
	});

	// left menu status v1: js + cookie
	/*
	 var xxljob_adminlte_settings = $.cookie('xxljob_adminlte_settings');
	 if (xxljob_adminlte_settings == 'off') {
	 	$('body').addClass('sidebar-collapse');
	 }
	 */


    // update pwd
    $('#updatePwd').on('click', function(){
        $('#updatePwdModal').modal({backdrop: false, keyboard: false}).modal('show');
    });
    var updatePwdModalValidate = $("#updatePwdModal .form").validate({
        errorElement : 'span',
        errorClass : 'help-block',
        focusInvalid : true,
        rules : {
            oldPassword : {
                required : true ,
                rangelength:[4,20]
            },
            password : {
                required : true ,
                rangelength:[4,20]
            }
        },
        messages : {
            oldPassword : {
                required : I18n.system_please_input +I18n.change_pwd_field_oldpwd,
                rangelength : I18n.system_lengh_limit + "[4-20]"
            },
            password : {
                required : I18n.system_please_input +I18n.change_pwd_field_newpwd,
                rangelength : I18n.system_lengh_limit + "[4-20]"
            }
        },
        highlight : function(element) {
            $(element).closest('.form-group').addClass('has-error');
        },
        success : function(label) {
            label.closest('.form-group').removeClass('has-error');
            label.remove();
        },
        errorPlacement : function(error, element) {
            element.parent('div').append(error);
        },
        submitHandler : function(form) {
            RsaPassword.encryptForm($("#updatePwdModal .form"), ['oldPassword', 'password']).then(function (paramData) {
                $.post(base_url + "/user/updatePwd",  paramData, function(data, status) {
                if (data.code == 200) {
                    $('#updatePwdModal').modal('hide');

                    layer.msg( I18n.change_pwd_suc_to_logout );
                    setTimeout(function(){
                        $.post(base_url + "/logout", function(data, status) {
                            if (data.code == 200) {
                                window.location.href = base_url + "/";
                            } else {
                                layer.open({
                                    icon: '2',
                                    content: (data.msg|| I18n.logout_fail)
                                });
                            }
                        });
                    }, 500);
                } else {
                    layer.open({
                        icon: '2',
                        content: (data.msg|| I18n.change_pwd + I18n.system_fail )
                    });
                }
                });
            }, function (msg) {
                layer.msg(msg || I18n.change_pwd + I18n.system_fail, {icon: 2});
            });
        }
    });
    $("#updatePwdModal").on('hide.bs.modal', function () {
        $("#updatePwdModal .form")[0].reset();
        updatePwdModalValidate.resetForm();
        $("#updatePwdModal .form .form-group").removeClass("has-error");
    });
	
});
