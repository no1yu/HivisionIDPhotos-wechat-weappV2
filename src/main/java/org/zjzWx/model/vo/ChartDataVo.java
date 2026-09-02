package org.zjzWx.model.vo;

import java.util.List;

public class ChartDataVo {

    /**
     * 图表横轴显示的日期
     */
    private List<String> time;

    /**
     * 每个日期按应用和照片去重后的功能使用次数
     */
    private List<Integer> data;

    public List<String> getTime() {
        return time;
    }

    public void setTime(List<String> time) {
        this.time = time;
    }

    public List<Integer> getData() {
        return data;
    }

    public void setData(List<Integer> data) {
        this.data = data;
    }
}
