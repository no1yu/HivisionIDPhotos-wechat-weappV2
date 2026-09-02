package org.zjzWx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("user")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    /**
     * 用户表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * openid
     */
    private String openid;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户名字
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 用户状态：1正常，2禁止登录
     */
    private Integer status;

    /**
     * 用户最新登录IP
     */
    private String ip;

    /**
     * 用户最新登录城市
     */
    private String city;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
