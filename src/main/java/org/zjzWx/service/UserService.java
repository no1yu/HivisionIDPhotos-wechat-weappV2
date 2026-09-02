package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.entity.User;
import org.zjzWx.model.vo.WxLoginVo;

public interface UserService extends IService<User> {

    //读取登录方式
    Integer getLoginType();

    //微信登录
    WxLoginVo wxlogin(String code,String phoneCode,String ip);

    //修改用户信息
    String updateUserInfo(MultipartFile file,String nickname,Integer userId);
}
