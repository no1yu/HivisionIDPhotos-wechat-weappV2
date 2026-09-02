package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName("photo")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Photo {

    /**
     * 用户保存记录表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 照片所属用户ID
     */
    private Integer userId;

    /**
     * 照片所属应用ID
     */
    private Integer appId;

    /**
     * 证件规格ID，探索中心图片允许为空
     */
    private Integer itemId;

    /**
     * 规格来源：1系统规格，2用户定制
     */
    private Integer type;

    /**
     * 规格名称或探索功能名称
     */
    private String name;

    /**
     * 用户点击下载或探索功能生成的正式图片地址
     */
    private String nImg;

    /**
     * 下载状态：1未解锁，2预览照或探索成片已解锁，3智能证件照高清已解锁
     */
    private Integer downloadStatus;

    /**
     * 图片尺寸说明
     */
    private String size;

    /**
     * 输出宽度，单位像素
     */
    private Integer width;

    /**
     * 输出高度，单位像素
     */
    private Integer height;

    /**
     * 输出分辨率
     */
    private Integer dpi;

    /**
     * 是否开启美颜
     */
    private Integer isBeautyOn;

    /**
     * 原始上传图片相对于图片根目录的路径
     */
    private String originalPath;

    /**
     * 普通透明证件照相对于图片根目录的路径
     */
    private String standardPath;

    /**
     * 高清透明证件照相对于图片根目录的路径
     */
    private String hdPath;

    /**
     * 当前背景与换装编辑结果相对于图片根目录的路径
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resultPath;

    /**
     * 当前选择的背景颜色；未选择背景时为空
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String backgroundColor;

    /**
     * 背景渲染方式：0纯色，1上下渐变，2中心渐变
     */
    private Integer backgroundRender;

    /**
     * 当前服装分类：0未选择、1男装、2女装、3儿童装
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer clothesCategory;

    /**
     * 当前服装编号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer clothesId;

    /**
     * 临时图片和编辑数据的过期时间，高清照片解锁后为空
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 照片记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
