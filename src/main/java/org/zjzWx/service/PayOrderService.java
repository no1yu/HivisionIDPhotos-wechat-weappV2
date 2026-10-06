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
    IPage<PayOrder> getPayOrderPage(int pageNum,int pageSize,int userId,String orderNo,String orderWx,int appId,int type,int status,String startTime,String endTime);

    //按照当前支付方式创建订单
    OrderPayVo createOrder(Integer userId,Integer photoId,String code);

    //解锁照片并返回无水印图片
    PicVo downloadPhoto(Integer userId,Integer photoId,int rewarded);

    //按照订单支付方式发起全额退款
    String refundOrder(Integer id);

    //删除没有处于退款中的订单
    String deleteOrder(Integer id);

    //微信支付回调
    JSONObject wxNotify(HttpServletRequest request,HttpServletResponse response);

    //微信退款回调
    JSONObject wxRefundNotify(HttpServletRequest request,HttpServletResponse response);

    //验证虚拟支付消息推送地址
    String verifyVirtualNotify(String signature,String timestamp,String nonce,String echostr);

    //处理虚拟支付消息通知
    String virtualNotify(HttpServletRequest request,JSONObject body);
}
