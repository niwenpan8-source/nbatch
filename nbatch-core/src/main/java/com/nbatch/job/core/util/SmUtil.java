package com.nbatch.job.core.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.BCUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

/**
 * @description:
 * @author: Mr.ni
 * @date: 2025-06-18
 */
public class SmUtil {

    private static final String SM4Key = "0d31e5458a5eb12bc52c988f92e99aa7";
    private static final Logger log = LogManager.getLogger(SmUtil.class);

    /**
     * SM2 生成密钥 16进制
     */
    public static SM2KeyPair generateSM2Key() {
        SM2 sm2 = new SM2();
        String privateKey = HexUtil.encodeHexStr(BCUtil.encodeECPrivateKey(sm2.getPrivateKey()));
        log.info("SM2 私钥：{}", privateKey);
        String publicKey = HexUtil.encodeHexStr(((BCECPublicKey) sm2.getPublicKey()).getQ().getEncoded(false));
        log.info("SM2 公钥：{}", publicKey);
        return new SM2KeyPair(publicKey, privateKey);
    }

    public static class SM2KeyPair {
        private String publicKey;
        private String privateKey;

        SM2KeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }

    /**
     * SM2 私钥解密
     */
    public static String sm2Decode(String privateKey, String encryptedText) {
        SM2 sm2 = cn.hutool.crypto.SmUtil.sm2(privateKey, null);
        byte[] decryptBytes = sm2.decrypt(encryptedText, KeyType.PrivateKey);
        return new String(decryptBytes);
    }

    /**
     * SM2 私钥签名
     */
    public static String sm2PrivateSign(String privateKey, String text) {
        SM2 sm2 = cn.hutool.crypto.SmUtil.sm2(privateKey, null);
        return sm2.signHex(HexUtil.encodeHexStr(text));
    }

    /**
     * SM2 公钥验签
     */
    public static boolean sm2PublicVerify(String publicKey, String signStr, String text) {
        SM2 sm2 = cn.hutool.crypto.SmUtil.sm2(null, publicKey);
        return sm2.verifyHex(HexUtil.encodeHexStr(text), signStr);

    }

    /**
     * SM2 公钥加密
     */
    public static String sm2Encode(String publicKey, String text) {
        SM2 sm2 = cn.hutool.crypto.SmUtil.sm2(null, publicKey);
        byte[] encryptBytes = sm2.encrypt(text.getBytes(), KeyType.PublicKey);
        return HexUtil.encodeHexStr(encryptBytes);
    }

    /**
     * SM4 解密
     */
    public static String sm4Decode(String encryptedText) {
        if (StrUtil.isEmpty(encryptedText)) {
            return "";
        }
        SM4 sm4 = cn.hutool.crypto.SmUtil.sm4(HexUtil.decodeHex(SM4Key));
        Base64 base64 = new Base64();
        return new String(sm4.decrypt(encryptedText));
    }

    /**
     * SM4 加密
     */
    public static String sm4Encode(String text) {
        SM4 sm4 = cn.hutool.crypto.SmUtil.sm4(HexUtil.decodeHex(SM4Key));
        return HexUtil.encodeHexStr(sm4.encrypt(text));
    }

    /**
     * SM4 生成密钥
     */
    public void generateSM4Key() {
        byte[] sm4Key = SecureUtil.generateKey("SM4").getEncoded();
        System.out.println("SM4密钥：" + HexUtil.encodeHexStr(sm4Key));
    }

    public static void main(String[] args) {
        String text = "aca2048d7e6b7c88289a48f5a8770e8c";
//        generateSM2Key();
//        byte[] sm4Key = SecureUtil.generateKey("SM4").getEncoded();
//        //SM4Key = HexUtil.encodeHexStr(sm4Key);
//        System.out.println("SM4密钥：" + HexUtil.encodeHexStr(sm4Key));
//        System.out.println("SM4加密：" + sm4Encode(text));
        System.out.println("SM4解密：" + sm4Decode("220e353584c0c9244f66aa2b3d095997"));
//
//        String publicKey = "046e22141086c610a5b0cdd1827588199863eb845b78633e4d34a3f9114974beff5dec1e959cd19b8cd0cbc2693602e25ad8e3d09235d7898c0ffb00d0d2aef5ca";
//        String s = Base64Encoder.encode(sm2Encode(publicKey, "encryptPublicKey"));
//        System.out.println("SM2加密：" + s);
//
//        String s1 = sm2EncodeChinaBank(publicKey, "encryptPublicKey");
//        System.out.println("SM2加密china：" + s1);
    }
}

