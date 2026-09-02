package org.zjzWx.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminIndexVo {

    /**
     * 今天按应用和照片去重后的功能使用次数
     */
    private long makeNum;

    /**
     * 累计按应用和照片去重后的功能使用次数
     */
    private long makeTotal;

    /**
     * 今天新增的用户数量
     */
    private long userNum;

    /**
     * 当前用户总数
     */
    private long userTotal;

    /**
     * 今天新增的订单数量
     */
    private long orderNum;

    /**
     * 最近一段时间的操作趋势图数据
     */
    private ChartDataVo chartDataVo;
}
