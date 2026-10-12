package com.nbatch.job.core.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * @author Mr.ni
 */
@Slf4j
public class JobRemotingUtil {

    public static final String BATCH_JOB_ACCESS_TOKEN = "BATCH-JOB-ACCESS-TOKEN";


    // 信任 HTTPS 的开始标记
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();

            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier((hostname, session) -> true);
    }
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }};
    // 信任 HTTPS 的结束标记


    /**
     * 发送 POST 请求
     *
     * @param url  url
     * @param accessToken       accessToken
     * @param timeout           by second
     * @param requestObj requestObj
     * @param returnTargClassOfT returnTargClassOfT
     */
    public static<T> ReturnT<T> postBody(String url, String accessToken, int timeout, Object requestObj, Class<T> returnTargClassOfT) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // 创建连接
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // 配置 HTTPS 信任策略
            boolean useHttps = url.startsWith("https");
            if (useHttps) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }

            // 设置连接参数
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(timeout * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            if(StrUtil.isNotBlank(accessToken)){
                connection.setRequestProperty(BATCH_JOB_ACCESS_TOKEN, accessToken);
            }

            // 建立连接
            connection.connect();

            // 写入请求体
            if (requestObj != null) {
                String requestBody = GsonTool.toJson(requestObj);

                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // 校验响应状态码
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业远程调用失败，响应状态码无效：" + statusCode + "，地址：" + url);
            }

            // 读取响应结果
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String resultJson = result.toString();
            // 解析返回结果
            try {
                return GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
            } catch (Exception e) {
            log.error("作业远程调用（url={}）响应内容无效（{}）。", url, resultJson, e);
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业远程调用响应内容无效，地址：" + url + "，响应：" + resultJson);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof SocketTimeoutException) {
                // 读取超时可能是请求已到达但响应未返回，交给管理端按“结果未知”处理。
                String rootCauseMessage = ExceptionUtil.getRootCauseMessage(e);
                String timeoutType = rootCauseMessage != null
                        && rootCauseMessage.toLowerCase().contains("read timed out")
                        ? "read timeout" : "timeout";
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_TIMEOUT, "作业远程调用" + timeoutType + "，原因：" + rootCauseMessage + "，地址：" + url);
            }
            if (e instanceof ConnectException) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_TIMEOUT, "作业远程调用超时，原因：" + ExceptionUtil.getRootCauseMessage(e) + "，地址：" + url);
            }
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业远程调用异常，原因：" + ExceptionUtil.getRootCauseMessage(e) + "，地址：" + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                log.error(e2.getMessage(), e2);
            }
        }
    }

}
