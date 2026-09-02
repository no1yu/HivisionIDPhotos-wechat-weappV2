package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("feedback")
public class Feedback {

    /**
     * 意见反馈记录表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 反馈类型：1功能建议，2使用体验，3投诉，4其他
     */
    private Integer type;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 反馈图片地址
     */
    private String imageUrls;

    /**
     * 联系方式
     */
    private String contact;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
