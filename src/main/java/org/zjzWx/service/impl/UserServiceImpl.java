package org.zjzWx.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.async.AsyncCenter;
import org.zjzWx.dao.UserDao;
import org.zjzWx.entity.User;
import org.zjzWx.entity.WebSet;
import org.zjzWx.model.vo.WxLoginVo;
import org.zjzWx.service.UserService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service
public class UserServiceImpl extends ServiceImpl<UserDao,User> implements UserService {

    @Autowired
    private WebSetService webSetService;
    @Autowired
    private AsyncCenter asyncCenter;

    @Override
    public Integer getLoginType() {
        return webSetService.getById(1).getLoginType();
    }

    @Override
    public WxLoginVo wxlogin(String code,String phoneCode,String ip) {

        WxLoginVo wxlogin = new WxLoginVo();
        try {

            WebSet webSet = webSetService.getById(1);
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+webSet.getAppId()+"&secret="+webSet.getAppSecret()+"&js_code=" + code + "&grant_type=authorization_code";

            RestTemplate restTemplate = new RestTemplate();

            //发起请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonopenid = JSONObject.parseObject(response.getBody());

            //当微信没有返回响应时
            if(jsonopenid==null){
                wxlogin.setMsg("与微信通讯失败，请重试");
                return wxlogin;
            }

            String openid = jsonopenid.getString("openid");

            //当微信没有返回openid时
            if(openid==null){
                wxlogin.setMsg(jsonopenid.toString());
                return wxlogin;
            }

            String phone = null;

            //获取用户手机号
            if(webSet.getLoginType()==2){
                phone = getPhoneNumber(webSet,phoneCode,restTemplate,wxlogin);
                if(phone==null){
                    return wxlogin;
                }
            }

            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("openid",openid);
            User user = baseMapper.selectOne(qw);

            //当用户已被禁止登录时
            if(user!=null && user.getStatus()==2){
                wxlogin.setMsg("您已被禁止登录，可联系客服恢复");
                return wxlogin;
            }

            //当用户第一次登录时
            if(user==null){
                user = new User();
                user.setOpenid(openid);
                user.setPhone(phone);
                user.setStatus(1);
                user.setIp(ip);
                user.setCreateTime(new Date());
                baseMapper.insert(user);
            }else if(webSet.getLoginType()==2){
                user.setPhone(phone);
                baseMapper.updateById(user);
            }

            //小程序使用WX设备类型登录，与网页后台的PC设备互不顶号
            StpUtil.login(user.getId(),"WX");

            //登录成功，返回token
            wxlogin.setToken(StpUtil.getTokenInfo().getTokenValue());

            //异步处理IP定位和后台登录地区更新
            asyncCenter.updateUserLoginRegion(user.getId(),ip);
        } catch (Exception e) {
            wxlogin.setMsg("代码报错");
            e.printStackTrace();
        }
        return wxlogin;
    }

    //使用手机号动态令牌读取手机号
    private String getPhoneNumber(WebSet webSet,String phoneCode,RestTemplate restTemplate,WxLoginVo wxlogin){
        String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + webSet.getAppId() + "&secret=" + webSet.getAppSecret();
        JSONObject tokenJson = JSONObject.parseObject(restTemplate.getForEntity(tokenUrl,String.class).getBody());
        if(tokenJson==null){
            wxlogin.setMsg("与微信通讯失败，请重试");
            return null;
        }
        if(tokenJson.getString("access_token")==null){
            wxlogin.setMsg(tokenJson.toString());
            return null;
        }


        JSONObject requestData = new JSONObject();
        requestData.put("code",phoneCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestData.toJSONString(),headers);
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + tokenJson.getString("access_token");

        JSONObject phoneJson = JSONObject.parseObject(restTemplate.postForEntity(url,entity,String.class).getBody());
        if(phoneJson==null){
            wxlogin.setMsg("与微信通讯失败，请重试");
            return null;
        }

        if(phoneJson.getIntValue("errcode")!=0){
            String errmsg = phoneJson.getString("errmsg");

            if(phoneJson.getIntValue("errcode")==48001 || (errmsg!=null && (errmsg.contains("api unauthorized") || errmsg.contains("no permission")))){
                wxlogin.setMsg("无权限，请在后台选择不获取手机号");
                return null;
            }

            wxlogin.setMsg(phoneJson.toString());
            return null;
        }


        return phoneJson.getJSONObject("phone_info").getString("phoneNumber");
    }

    @Override
    public String updateUserInfo(MultipartFile file,String nickname,Integer userId) {

        User user = new User();
        user.setId(userId);
        String oldAvatarUrl = null;

        //当用户上传新头像时
        if(file!=null){
            oldAvatarUrl = baseMapper.selectById(userId).getAvatarUrl();
            Map<String, Object> mp = this.updateAvatar(file);
            if((int)mp.get("type")==0){
                return (String) mp.get("msg");
            }
            user.setAvatarUrl((String) mp.get("msg"));
        }

        //当用户填写新昵称时
        if(nickname!=null && !nickname.trim().isEmpty()){
            user.setNickname(nickname);
        }

        //当头像或昵称发生变化时
        if(user.getAvatarUrl()!=null || user.getNickname()!=null){
            int updateCount = baseMapper.updateById(user);
            if(updateCount>0 && oldAvatarUrl!=null){
                asyncCenter.deleteImage(oldAvatarUrl);
            }
        }
        return null;
    }

    //上传用户头像
    private Map<String,Object> updateAvatar(MultipartFile file){

        Map<String,Object> mp = new HashMap<>();
        String originalFilename = file.getOriginalFilename();

        //当文件扩展名不受支持时
        if(!StringUtils.hasText(originalFilename) || (!originalFilename.toLowerCase().endsWith(".png")
                && !originalFilename.toLowerCase().endsWith(".jpg")) && !originalFilename.toLowerCase().endsWith(".jpeg")) {
            mp.put("type",0);
            mp.put("msg","图片类型不合法，仅支持jpg/png/jpeg的图片");
            return mp;
        }

        //当头像超过1MB时
        if(file.getSize()>1024*1024){
            mp.put("type",0);
            mp.put("msg","头像太大啦");
            return mp;
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory");
            WebSet storageSet = webSetService.getOne(storageQw);

            byte[] imageBytes = file.getBytes();
            String extension = PicUtil.getImageExtension(imageBytes);

            //当文件内容不是PNG或JPEG图片时
            if(extension==null){
                mp.put("type",0);
                mp.put("msg","图片不合法，仅支持jpg/png/jpeg");
                return mp;
            }

            //保存头像
            String filename = PicUtil.filesCopy("avatar",storageSet.getDirectory(),extension,imageBytes);
            String imagePath = storageSet.getPicDomain() + "avatar" + "/" + filename;
            mp.put("type",1);
            mp.put("msg",imagePath);
            return mp;
        } catch (Exception e) {
            e.printStackTrace();
            mp.put("type",0);
            mp.put("msg","头像保存失败，请重试");
            return mp;
        }
    }
}
