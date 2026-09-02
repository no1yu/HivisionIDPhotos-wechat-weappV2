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
@TableName("user_record")
public class UserRecord {

    /**
     * 用户操作记录表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 关联的功能ID，图片上传固定为1
     */
    private Integer appId;

    /**
     * 本次操作名称
     */
    private String name;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 关联的照片ID，上传失败时允许为空
     */
    private Integer photoId;

    /**
     * 执行结果：1成功，2失败
     */
    private Integer status;

    /**
     * 接口处理耗时，单位毫秒
     */
    private Long durationMs;

    /**
     * 失败原因，成功记录为空
     */
    private String errorMessage;

    /**
     * 接口开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
