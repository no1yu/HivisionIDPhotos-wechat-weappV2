package org.zjzWx.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("app_set")
public class AppSet {

    /**
     * 应用表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 名字
     */
    private String name;

    /**
     * 功能说明
     */
    private String description;

    /**
     * 探索中心封面地址
     */
    private String image;

    /**
     * 下载模式：0关闭，1免费下载，2看广告下载，3付费下载，4看广告或付费下载
     * ID为1时控制图片上传，ID为2时控制美颜，ID为14时控制图片鉴黄，ID为15时控制换装
     */
    private Integer status;

    /**
     * 鉴黄拦截阈值，仅ID为14时使用，范围0.01到1
     */
    private Double settingValue;

    /**
     * 下载金额，单位元，状态为3或4时使用
     */
    private BigDecimal downloadPrice;

    /**
     * 探索应用显示顺序，数值最小的为置顶应用
     */
    private Integer sort;
}
