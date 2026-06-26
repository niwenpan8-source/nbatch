(function (window, $) {
    var cachedPublicKeyInfo = null;

    /**
     * 将Base64字符串转换为ArrayBuffer
     */
    function base64ToArrayBuffer(base64) {
        var binary = window.atob(base64);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }

    /**
     * 将ArrayBuffer转换为Base64字符串
     */
    function arrayBufferToBase64(buffer) {
        var bytes = new Uint8Array(buffer);
        var binary = '';
        for (var i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

    /**
     * 获取浏览器的加密对象
     */
    function getCrypto() {
        var cryptoObj = window.crypto || window.msCrypto;
        return cryptoObj && cryptoObj.subtle ? cryptoObj : null;
    }

    /**
     * 获取加密公钥信息
     */
    function getPublicKeyInfo() {
        if (cachedPublicKeyInfo) {
            return Promise.resolve(cachedPublicKeyInfo);
        }
        return new Promise(function (resolve, reject) {
            $.ajax({
                type: 'GET',
                url: base_url + '/rsaPublicKey',
                dataType: 'json',
                cache: false,
                success: function (data) {
                    if (!data || data.code !== 200 || !data.content || !data.content.publicKey) {
                        reject(data && data.msg ? data.msg : '获取加密公钥失败');
                        return;
                    }
                    cachedPublicKeyInfo = data.content;
                    resolve(cachedPublicKeyInfo);
                },
                error: function () {
                    reject('获取加密公钥失败');
                }
            });
        });
    }

    /**
     *  编码文本为Uint8Array
     */
    function encodeText(value) {
        if (window.TextEncoder) {
            return new TextEncoder().encode(value || '');
        }
        var encoded = unescape(encodeURIComponent(value || ''));
        var bytes = new Uint8Array(encoded.length);
        for (var i = 0; i < encoded.length; i++) {
            bytes[i] = encoded.charCodeAt(i);
        }
        return bytes;
    }

    /**
     * 加密
     */
    function encryptValue(value) {
        var cryptoObj = getCrypto();
        if (!cryptoObj) {
            return $.Deferred().reject('当前浏览器不支持密码加密，请升级浏览器').promise();
        }
        return getPublicKeyInfo().then(function (keyInfo) {
            return cryptoObj.subtle.importKey(
                'spki',
                base64ToArrayBuffer(keyInfo.publicKey),
                {name: 'RSA-OAEP', hash: {name: keyInfo.hash || 'SHA-256'}},
                false,
                ['encrypt']
            ).then(function (publicKey) {
                return cryptoObj.subtle.encrypt(
                    {name: 'RSA-OAEP'},
                    publicKey,
                    encodeText(value)
                );
            }).then(arrayBufferToBase64);
        });
    }

    /**
     * 对数据中的指定字段进行加密
     */
    function encryptFields(data, fieldNames) {
        var promise = Promise.resolve();
        $.each(fieldNames, function (_, fieldName) {
            if (data[fieldName]) {
                promise = promise.then(function () {
                    return encryptValue(data[fieldName]).then(function (encryptedValue) {
                        data[fieldName] = encryptedValue;
                    });
                });
            }
        });
        return promise.then(function () {
            return data;
        });
    }

    function encryptForm($form, fieldNames) {
        var data = {};
        $.each($form.serializeArray(), function (_, item) {
            data[item.name] = item.value;
        });
        return encryptFields(data, fieldNames || ['password']);
    }

    window.RsaPassword = {
        encryptValue: encryptValue,
        encryptFields: encryptFields,
        encryptForm: encryptForm
    };
})(window, jQuery);
