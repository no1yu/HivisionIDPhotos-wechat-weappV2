package org.zjzWx.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeClothesDto {

    /**
     * 上传图片接口返回的照片ID
     */
    private Integer photoId;

    /**
     * 服装分类：0未选择或取消换装、1男装、2女装、3儿童装
     */
    private Integer clothesCategory;

    /**
     * 服装编号；取消换装时为0
     */
    private Integer clothesId;

    /**
     * 用户指定的输出分辨率
     */
    private Integer dpi;

    /**
     * 用户指定的图片大小，单位KB
     */
    private Integer kb;

    /**
     * 是否使用高清透明照换装：0普通预览，1高清成片
     */
    private Integer hd;

    /**
     * 当前登录用户ID，由控制器从Token中读取
     */
    private Integer userId;
}
