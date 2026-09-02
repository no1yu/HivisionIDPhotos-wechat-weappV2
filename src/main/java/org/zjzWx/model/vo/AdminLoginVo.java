package org.zjzWx.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginVo {

    /**
     * 微信登录二维码图片二进制数据
     */
    private byte[] pic;

    /**
     * 本次网页登录使用的临时识别码
     */
    private long code;
}
