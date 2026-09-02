package org.zjzWx.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.PayOrder;
import org.zjzWx.model.vo.DownloadSetVo;
import org.zjzWx.model.vo.OrderPayVo;
import org.zjzWx.model.vo.PicVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//订单业务
public interface PayOrderService extends IService<PayOrder> {

    //读取应用下载设置
    DownloadSetVo getDownloadSet(Integer appId);

    //管理员后台分页读取支付订单
    IPage<PayOrder> getPayOrderPage(int pageNum,int pageSize,int userId,String orderNo,String orderWx,int appId,int status,String startTime,String endTime);

    //发起微信支付
    OrderPayVo createOrder(Integer userId,Integer photoId);

    //解锁照片并返回无水印图片
    PicVo downloadPhoto(Integer userId,Integer photoId,int rewarded);

    //发起微信退款
    String refundOrder(Integer id);

    //微信支付回调
    JSONObject wxNotify(HttpServletRequest request,HttpServletResponse response);

    //微信退款回调
    JSONObject wxRefundNotify(HttpServletRequest request,HttpServletResponse response);
}
