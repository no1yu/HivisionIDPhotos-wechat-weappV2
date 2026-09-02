package org.zjzWx.util;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import org.zjzWx.entity.WebSet;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * 微信支付工具类
 */
public class WeChatPayUtil {

    private WeChatPayUtil() {

    }

    //创建微信支付SDK配置
    public static RSAAutoCertificateConfig createConfig(WebSet webSet) {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(webSet.getMerchantId())
                .privateKey(webSet.getMerchantPrivateKey())
                .merchantSerialNumber(webSet.getMerchantSerialNumber())
                .apiV3Key(webSet.getApiV3Key())
                .build();
    }

    //微信回调请求体和请求头创建验签参数
    public static RequestParam createRequestParam(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder requestBody = new StringBuilder();
        char[] buffer = new char[2048];
        int length;
        while((length=reader.read(buffer))!=-1){
            requestBody.append(buffer,0,length);
        }

        return new RequestParam.Builder()
                .serialNumber(request.getHeader("Wechatpay-Serial"))
                .nonce(request.getHeader("Wechatpay-Nonce"))
                .signature(request.getHeader("Wechatpay-Signature"))
                .timestamp(request.getHeader("Wechatpay-Timestamp"))
                .signType(request.getHeader("Wechatpay-Signature-Type"))
                .body(requestBody.toString())
                .build();
    }
}
