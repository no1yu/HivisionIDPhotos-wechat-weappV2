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
@TableName("web_task")
public class WebTask {

    /**
     * 定时任务执行日志表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 类型：1未解锁照片清理，2临时编辑数据清理
     */
    private Integer type;

    /**
     * 定时任务名称
     */
    private String taskName;

    /**
     * 本次删除的数据数量
     */
    private Integer deleteCount;

    /**
     * 执行状态：1成功，2失败
     */
    private Integer status;

    /**
     * 执行耗时，单位毫秒
     */
    private Integer durationMs;

    /**
     * 错误日志，执行成功时为空
     */
    private String errorLog;

    /**
     * 开始执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
}
