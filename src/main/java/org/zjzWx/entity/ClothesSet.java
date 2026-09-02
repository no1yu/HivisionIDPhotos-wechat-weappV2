package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("clothes_set")
public class ClothesSet {

    /**
     * 换装素材表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 服装分类：1男装，2女装，3儿童装
     */
    private Integer category;

    /**
     * 分类下的服装编号
     */
    private Integer clothesId;

    /**
     * 素材图片地址
     */
    private String imageUrl;

    /**
     * 状态：1正常，2关闭
     */
    private Integer status;

}
