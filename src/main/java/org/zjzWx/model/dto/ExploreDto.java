package org.zjzWx.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExploreDto {

    /**
     * 当前登录用户ID，由控制器从Token中读取
     */
    private Integer userId;

    /**
     * 用户指定的输出分辨率
     */
    private Integer dpi;

    /**
     * 用户指定的图片大小，单位KB
     */
    private Integer kb;

    /**
     * 上传图片接口返回的照片ID
     */
    private Integer photoId;

    /**
     * 用户指定的输出高度，单位像素
     */
    private Integer height;

    /**
     * 用户指定的输出宽度，单位像素
     */
    private Integer width;

    /**
     * 排版画布预设名称
     */
    private String layoutSize;

    /**
     * 自定义排版画布高度，单位像素
     */
    private Integer layoutHeight;

    /**
     * 自定义排版画布宽度，单位像素
     */
    private Integer layoutWidth;

    /**
     * 排版裁剪线：1绘制，2不绘制
     */
    private Integer cropLine;

    /**
     * 社交媒体模板名称
     */
    private String templateName;

    /**
     * 图片格式转换的目标格式：jpg、jpeg、png或gif
     */
    private String targetFormat;

    /**
     * 模板照或水印使用的颜色
     */
    private String color;

    /**
     * 用户输入的水印文字
     */
    private String watermarkText;

    /**
     * 水印样式：striped斜纹，central居中
     */
    private String watermarkStyle;

    /**
     * 水印旋转角度
     */
    private Integer watermarkAngle;

    /**
     * 水印透明度百分比
     */
    private Integer watermarkOpacity;

    /**
     * 水印字体大小，以1000像素图片短边为基准
     */
    private Integer watermarkSize;

    /**
     * 重复水印之间的间距，以1000像素图片短边为基准
     */
    private Integer watermarkSpace;
}
