package org.zjzWx.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 微信虚拟支付签名工具类
 */
public class VirtualPayUtil {

    private VirtualPayUtil() {

    }

    //计算支付接口需要的HMAC-SHA256签名
    public static String hmac(String key,String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("虚拟支付签名失败",e);
        }
    }

    //参数先排序再拼到一起，计算微信回调需要的SHA1签名
    public static String sha1(String... values) {
        Arrays.sort(values);
        StringBuilder value = new StringBuilder();
        for(String item:values){
            value.append(item);
        }
        try {
            return toHex(MessageDigest.getInstance("SHA-1").digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("虚拟支付回调签名失败",e);
        }
    }

    //转换为微信签名使用的小写十六进制
    private static String toHex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for(byte item:bytes){
            value.append(Integer.toHexString((item&0xff)|0x100).substring(1));
        }
        return value.toString();
    }
}
