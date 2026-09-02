package org.zjzWx.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePhotoDto {

    /**
     * 用户选择的系统规格ID或自定义规格ID
     */
    private Integer itemId;

    /**
     * 上传图片接口返回的照片ID
     */
    private Integer photoId;

    /**
     * 是否开启美颜：0关闭，1开启
     */
    private Integer isBeautyOn;

    /**
     * 当前登录用户ID，由控制器从Token中读取
     */
    private Integer userId;

    /**
     * 规格来源：1系统规格，2用户定制
     */
    private Integer type;
}
