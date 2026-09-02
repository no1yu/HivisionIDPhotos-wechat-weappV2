package org.zjzWx.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WxLoginVo {

    /**
     * 登录成功后返回给小程序的Token
     */
    private String token;

    /**
     * 登录失败时返回给小程序的原因
     */
    private String msg;
}
