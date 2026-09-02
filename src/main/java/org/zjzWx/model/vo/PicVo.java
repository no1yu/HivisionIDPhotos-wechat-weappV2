package org.zjzWx.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PicVo {

    /**
     * 照片ID
     */
    private Integer photoId;

    /**
     * 当前编辑结果的公开临时预览地址
     */
    private String previewUrl;

    /**
     * 用户点击下载后生成的正式图片地址
     */
    private String picUrl;

    /**
     * 图片分辨率
     */
    private Integer dpi;

    /**
     * 规格名称
     */
    private String name;

    /**
     * 规格分类
     */
    private Integer category;

    /**
     * 规格ID
     */
    private Integer itemId;

    /**
     * 错误消息
     */
    private String msg;
}
