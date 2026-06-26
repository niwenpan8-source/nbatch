package com.nbatch.job.admin.core.util;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * RSA密码工具类
 * 非对称加密，公钥加密私钥解密
 */
@Slf4j
@Component
public class RsaPasswordUtil {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String CIPHER_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int KEY_SIZE = 2048;

    private final String publicKey;
    private final PrivateKey rsaPrivateKey;

    /**
     * 在构造器当中生成额公私钥加密对
     */
    public RsaPasswordUtil() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            this.rsaPrivateKey = keyPair.getPrivate();
            this.publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("init rsa password key pair failed", e);
        }
    }

    /**
     * 获取公钥信息
     */
    public Map<String, String> publicKeyInfo() {
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", publicKey);
        result.put("algorithm", "RSA-OAEP");
        result.put("hash", HASH_ALGORITHM);
        return result;
    }

    /**
     * 解密密码
     */
    public ReturnT<String> decryptPassword(String encryptedPassword) {
        if (StrUtil.isBlank(encryptedPassword)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "密码不能为空");
        }
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaepParameterSpec());
            return ReturnT.success(new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("decrypt rsa password failed", e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "密码解密失败，请刷新页面后重试");
        }
    }

    /**
     * 获取OAEP参数
     * OAEP参数用于RSA加密解密，确保密码在传输过程中不被明文暴露
     */
    private OAEPParameterSpec oaepParameterSpec() {
        return new OAEPParameterSpec(HASH_ALGORITHM, "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }
}
