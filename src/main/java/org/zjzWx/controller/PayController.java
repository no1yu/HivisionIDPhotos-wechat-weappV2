package org.zjzWx.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    //验证虚拟支付消息推送地址
    @GetMapping("/virtualNotify")
    public String verifyVirtualNotify(@RequestParam String signature,@RequestParam String timestamp,@RequestParam String nonce,@RequestParam String echostr,HttpServletResponse response) {
        try {
            return payOrderService.verifyVirtualNotify(signature,timestamp,nonce,echostr);
        } catch (Exception e) {
            response.setStatus(400);
            return "fail";
        }
    }

    //虚拟支付消息通知，微信后台选择JSON安全模式
    @PostMapping("/virtualNotify")
    public String virtualNotify(@RequestBody JSONObject body,HttpServletRequest request,HttpServletResponse response) {
        try {
            return payOrderService.virtualNotify(request,body);
        } catch (Exception e) {
            response.setStatus(500);
            return "fail";
        }
    }
}
