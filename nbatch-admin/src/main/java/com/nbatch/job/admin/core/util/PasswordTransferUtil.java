package com.nbatch.job.admin.core.util;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密码传输编码工具。
 */
@Slf4j
@Component
public class PasswordTransferUtil {

    /**
     * 解码密码：字符移位还原 -> 反转 -> Base64解码。
     */
    public ReturnT<String> decodePassword(String encodedPassword) {
        if (StrUtil.isBlank(encodedPassword)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "密码不能为空");
        }
        try {
            StringBuilder shiftedBack = new StringBuilder(encodedPassword.length());
            for (int i = 0; i < encodedPassword.length(); i++) {
                shiftedBack.append((char) (encodedPassword.charAt(i) - 1));
            }
            String base64 = shiftedBack.reverse().toString();
            byte[] passwordBytes = Base64.getDecoder().decode(base64);
            return ReturnT.success(new String(passwordBytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("解码密码失败。", e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "密码解码失败，请刷新页面后重试");
        }
    }
}
