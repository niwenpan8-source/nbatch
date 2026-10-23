(function (window, $) {
    var BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';

    function resolvedPromise(value) {
        if (window.Promise) {
            return Promise.resolve(value);
        }
        var deferred = $.Deferred();
        deferred.resolve(value);
        return deferred.promise();
    }

    function utf8Encode(value) {
        return unescape(encodeURIComponent(value || ''));
    }

    function base64Encode(value) {
        var input = utf8Encode(value);
        var output = '';
        var i = 0;

        while (i < input.length) {
            var chr1 = input.charCodeAt(i++);
            var chr2 = input.charCodeAt(i++);
            var chr3 = input.charCodeAt(i++);

            var enc1 = chr1 >> 2;
            var enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            var enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            var enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output += BASE64_CHARS.charAt(enc1) + BASE64_CHARS.charAt(enc2) +
                BASE64_CHARS.charAt(enc3) + BASE64_CHARS.charAt(enc4);
        }

        return output;
    }

    function encodeValue(value) {
        var base64 = base64Encode(value);
        var reversed = base64.split('').reverse().join('');
        var shifted = '';
        for (var i = 0; i < reversed.length; i++) {
            shifted += String.fromCharCode(reversed.charCodeAt(i) + 1);
        }
        return shifted;
    }

    function encodeFields(data, fieldNames) {
        var promise = resolvedPromise();
        $.each(fieldNames, function (_, fieldName) {
            if (data[fieldName]) {
                promise = promise.then(function () {
                    return resolvedPromise(encodeValue(data[fieldName])).then(function (encodedValue) {
                        data[fieldName] = encodedValue;
                    });
                });
            }
        });
        return promise.then(function () {
            return data;
        });
    }

    function encodeForm($form, fieldNames) {
        var data = {};
        $.each($form.serializeArray(), function (_, item) {
            data[item.name] = item.value;
        });
        return encodeFields(data, fieldNames || ['password']);
    }

    window.PasswordTransfer = {
        encodeValue: encodeValue,
        encodeFields: encodeFields,
        encodeForm: encodeForm
    };
})(window, jQuery);
