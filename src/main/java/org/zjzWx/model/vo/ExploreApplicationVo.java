package org.zjzWx.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExploreApplicationVo {

    /**
     * 应用ID
     */
    private Integer id;

    /**
     * 应用名称
     */
    private String name;

    /**
     * 应用说明
     */
    private String description;

    /**
     * 应用封面地址
     */
    private String image;

    /**
     * 累计成功使用次数
     */
    private long useCount;
}
