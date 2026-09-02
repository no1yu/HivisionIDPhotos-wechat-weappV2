package org.zjzWx.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePhotoDto {

    /**
     * 上传图片接口返回的照片ID
     */
    private Integer photoId;

    /**
     * 用户选择的背景颜色
     */
    private String colors;

    /**
     * 用户指定的输出分辨率
     */
    private Integer dpi;

    /**
     * 换色方式：0纯色，1上下渐变，2中心渐变
     */
    private Integer render;

    /**
     * 用户指定的图片大小，单位KB
     */
    private Integer kb;

    /**
     * 是否使用高清透明照换底：0普通预览，1高清成片
     */
    private Integer hd;

    /**
     * 当前登录用户ID，由控制器从Token中读取
     */
    private Integer userId;
}
