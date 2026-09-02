package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.entity.User;
import org.zjzWx.entity.Feedback;
import org.zjzWx.model.vo.WxLoginVo;
import org.zjzWx.service.UserService;
import org.zjzWx.service.FeedbackService;
import org.zjzWx.service.HelpService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.R;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private WebSetService webSetService;
    @Autowired
    private HelpService helpService;


    //读取登录方式
    @PostMapping("/getLoginType")
    public R getLoginType(){
        return R.ok(userService.getLoginType());
    }

    //登录
    @GetMapping("/login")
    public R login(String code,String phoneCode,HttpServletRequest request){
        //优先读取反向代理写入的真实IP，避免线上只能取得Nginx的内网IP
        String ip = request.getHeader("X-Real-IP");

        //当反向代理没有传递X-Real-IP时，继续读取转发链中的第一个IP
        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("X-Forwarded-For");
        }

        //当请求经过多层代理时，只保存最前面的用户IP
        if(StringUtils.hasText(ip) && ip.contains(",")){
            ip = ip.split(",")[0].trim();
        }

        //当代理请求头中没有IP时，使用当前连接的来源IP
        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip) || ip.length()>45){
            ip = request.getRemoteAddr();
        }

        //当本地开发环境使用IPv6回环地址时，统一保存为IPv4回环地址
        if("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)){
            ip = "127.0.0.1";
        }

        WxLoginVo wxlogin = userService.wxlogin(code,phoneCode,ip);
        if(wxlogin.getToken()!=null){
            return R.ok(wxlogin.getToken());
        }
        return R.no(wxlogin.getMsg());
    }

    //获取用户信息
    @GetMapping("/userInfo")
    public R userInfo(){
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.select("nickname","avatar_url","create_time");
        qw.eq("id",StpUtil.getLoginIdAsInt());
        User user = userService.getOne(qw);
        if(user==null){
            return R.no();
        }
        return R.ok(user);
    }

    //保存用户信息
    @PostMapping("/updateUserInfo")
    public R updateUserInfo(@RequestParam(name = "file", required = false) MultipartFile file,
                            @RequestParam(name = "nickname", required = false) String nickname){

        //当本次请求带有超长昵称时，无论是否同时上传头像都拒绝保存
        if(nickname!=null && nickname.length()>20){
            return R.no("名字太长啦~");
        }
        String msg = userService.updateUserInfo(file,nickname,StpUtil.getLoginIdAsInt());
        if(msg!=null){
            return R.no(msg);
        }
        return R.ok();
    }

    //提交意见反馈
    @PostMapping("/submitFeedback")
    public R submitFeedback(Integer type,String content,String contact,@RequestParam(value = "file",required = false) MultipartFile file){
        Feedback feedback = new Feedback();
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setContact(contact);
        return feedbackService.submitFeedback(feedback,StpUtil.getLoginIdAsInt(),file);
    }

    //读取个人中心公众号配置
    @PostMapping("/getMineSet")
    public R getMineSet(){
        return R.ok(webSetService.getMineSet());
    }

    //读取常见问题
    @PostMapping("/getHelpList")
    public R getHelpList(){
        return R.ok(helpService.getHelpList());
    }









}
