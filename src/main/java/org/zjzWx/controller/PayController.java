package org.zjzWx.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zjzWx.service.PayOrderService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayOrderService payOrderService;

    //微信支付回调
    @PostMapping("/wxNotify")
    public JSONObject wxNotify(HttpServletRequest request, HttpServletResponse response) {
        return payOrderService.wxNotify(request,response);
    }

    //微信退款回调
    @PostMapping("/wxRefundNotify")
    public JSONObject wxRefundNotify(HttpServletRequest request, HttpServletResponse response) {
        return payOrderService.wxRefundNotify(request,response);
    }
}
