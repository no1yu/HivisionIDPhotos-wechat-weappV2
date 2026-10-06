package org.zjzWx.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPayVo {

    /**
     * 支付方式：1微信支付，2虚拟支付
     */
    private Integer type;

    /**
     * 虚拟支付模式
     */
    private String mode;

    /**
     * 虚拟支付用的完整JSON参数
     */
    private String signData;

    /**
     * 虚拟支付签名
     */
    private String paySig;

    /**
     * 用本次微信登录的session_key生成的签名
     */
    private String signature;

    /**
     * 微信支付时间戳
     */
    private String timeStamp;

    /**
     * 微信支付随机字符串
     */
    private String nonceStr;

    /**
     * 小程序调起支付使用的package参数
     */
    private String packageVal;

    /**
     * 微信支付签名类型
     */
    private String signType;

    /**
     * 微信支付签名
     */
    private String paySign;

    /**
     * 创建订单失败原因
     */
    private String msg;
}
