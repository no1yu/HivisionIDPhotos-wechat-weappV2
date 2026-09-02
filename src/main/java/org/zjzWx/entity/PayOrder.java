package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("pay_order")
public class PayOrder {

    /**
     * 微信支付订单表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 微信支付订单号
     */
    private String orderWx;

    /**
     * 下单用户ID
     */
    private Integer userId;

    /**
     * 订单对应应用ID
     */
    private Integer appId;

    /**
     * 订单购买下载权的照片ID
     */
    private Integer photoId;

    /**
     * 订单名称
     */
    private String name;

    /**
     * 订单金额，单位元
     */
    private BigDecimal money;

    /**
     * 订单状态：1待支付，2支付成功，3退款成功
     */
    private Integer status;

    /**
     * 商户退款单号
     */
    private String refundNo;

    /**
     * 微信退款单号
     */
    private String refundWx;

    /**
     * 订单创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 支付成功时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime;

    /**
     * 退款成功时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date refundTime;
}
