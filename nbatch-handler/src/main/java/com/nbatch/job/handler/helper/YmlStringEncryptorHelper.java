package com.nbatch.job.handler.helper;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.util.SmUtil;
import lombok.extern.log4j.Log4j2;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;

/**
 * yml 配置文件加密解密工具
 *
 * @author: Mr.ni
 * @date: 2026/09/04
 */
@Log4j2
public class YmlStringEncryptorHelper implements StringEncryptor {


    @Override
    public String encrypt(String message) {
        if (StrUtil.isNotBlank(message)) {
            try {
                message = SmUtil.sm4Encode(message);
                log.info("配置信息加密成功!");
            } catch (Exception e) {
                log.error("配置信息加密失败!");
            }
        }
        return message;
    }

    @Override
    public String decrypt(String encryptedMessage) {
        if (StrUtil.isNotBlank(encryptedMessage)) {
            try {
                encryptedMessage = SmUtil.sm4Decode(encryptedMessage);
                log.info("配置信息解密成功!");
            } catch (Exception e) {
                log.error("配置信息解密失败!");
            }
        }
        return encryptedMessage;
    }

}
