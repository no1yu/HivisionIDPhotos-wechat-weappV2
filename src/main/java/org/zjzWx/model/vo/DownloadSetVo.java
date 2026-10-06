package org.zjzWx.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadSetVo {

    /**
     * 下载模式：0关闭功能，1免费，2广告，3付费，4广告或付费
     */
    private Integer status;

    /**
     * 支付方式：1微信支付，2虚拟支付
     */
    private Integer payType;

    /**
     * 付费下载金额，单位元
     */
    private BigDecimal downloadPrice;

    /**
     * 微信激励视频广告位ID
     */
    private String videoUnitId;
}
