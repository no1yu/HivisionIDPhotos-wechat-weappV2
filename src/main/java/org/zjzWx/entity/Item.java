package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("item")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {

    /**
     * 证件规格表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 对应云端返回的ID
     */
    private Integer cloudItemId;

    /**
     * 名称
     */
    private String name;

    /**
     * 像素-宽
     */
    private Integer widthPx;

    /**
     * 像素-高
     */
    private Integer heightPx;

    /**
     * 尺寸-宽
     */
    private Integer widthMm;

    /**
     * 尺寸-高
     */
    private Integer heightMm;

    /**
     * 图标：1-6
     */
    private Integer icon;

    /**
     * 1=常用寸照，2=各类签证，3=各类证件
     */
    private Integer category;

    /**
     * 分辨率
     */
    private Integer dpi;

    /**
     * 状态：1开启，2关闭
     */
    private Integer status;
}
