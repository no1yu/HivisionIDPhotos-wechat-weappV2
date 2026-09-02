package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zjzWx.model.vo.OrderPayVo;
import org.zjzWx.model.vo.PicVo;
import org.zjzWx.service.PayOrderService;
import org.zjzWx.util.R;

/**
 * 小程序主动创建订单和下载照片的接口
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private PayOrderService payOrderService;

    //返回指定应用当前的下载模式、金额和广告位ID
    @PostMapping("/getDownloadSet")
    public R getDownloadSet(@RequestParam("appId") Integer appId) {
        return R.ok(payOrderService.getDownloadSet(appId));
    }

    //每次调用都创建新的待支付订单并返回小程序支付参数
    @PostMapping("/createOrder")
    public R createOrder(@RequestParam("photoId") Integer photoId) {
        OrderPayVo orderPayVo = payOrderService.createOrder(StpUtil.getLoginIdAsInt(),photoId);

        //创建订单失败时把具体原因返回给小程序
        if(orderPayVo.getMsg()!=null){
            return R.no(orderPayVo.getMsg());
        }
        return R.ok(orderPayVo);
    }

    //根据免费、广告、支付或已经解锁的状态发放无水印图片
    @PostMapping("/downloadPhoto")
    public R downloadPhoto(@RequestParam("photoId") Integer photoId,
                           @RequestParam("rewarded") int rewarded) {
        PicVo picVo = payOrderService.downloadPhoto(StpUtil.getLoginIdAsInt(),photoId,rewarded);

        //没有满足当前下载条件时把原因返回给小程序
        if(picVo.getMsg()!=null){
            return R.no(picVo.getMsg());
        }
        return R.ok(picVo);
    }
}
