package org.zjzWx.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("custom")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Custom {

    /**
     * 自定义证件规格表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户id
     */
    private Integer userId;

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
     * 分辨率
     */
    private Integer dpi;

    /**
     * 尺寸说明
     */
    @TableField(exist = false)
    private String size;

    /**
     * 图标，1-6
     */
    private Integer icon;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
