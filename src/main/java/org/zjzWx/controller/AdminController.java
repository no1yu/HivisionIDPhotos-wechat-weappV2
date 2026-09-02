package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.entity.AppSet;
import org.zjzWx.entity.Help;
import org.zjzWx.entity.Item;
import org.zjzWx.entity.WebSet;
import org.zjzWx.model.vo.AdminLoginVo;
import org.zjzWx.service.AdminService;
import org.zjzWx.util.R;
import org.zjzWx.util.WebSocketHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private WebSocketHandler webSocketHandler;


    //直接返回微信生成的二维码图片，登录code放在响应头中
    @GetMapping(value = "/login", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> login(String type) {
        AdminLoginVo login = adminService.login(type);
        if(login==null){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok()
                .header("X-Admin-Login-Code",String.valueOf(login.getCode()))
                .contentType(MediaType.IMAGE_PNG)
                .body(login.getPic());
    }

    //WebSocket收到授权通知后换取token
    @GetMapping("/checkLogin")
    public R checkLogin(String code) {
        if(code==null || code.trim().isEmpty()){
           return R.no("无效的请求登录");
        }

        String msg = adminService.checkLogin(code);
        if(msg==null){
            return R.no();
        }
        return R.ok(msg);
    }

    //小程序请求修改登录
    @GetMapping("/okLogin")
    public R okLogin(String code1,String code2) {
        if(code1==null || code1.trim().isEmpty() || code2==null || code2.trim().isEmpty()){
            return R.no("无效的请求登录");
        }
        String msg = adminService.okLogin(code1, code2);
        if(msg!=null){
            return R.no(msg);
        }
        webSocketHandler.authorized(code2);
        return R.ok();
    }


    //首页数据
    @PostMapping("/adminIndex")
    public R adminIndex(){
        return R.ok(adminService.adminIndex());
    }



    //规格列表
    @PostMapping("/getItemPage")
    public R getItemPage(int pageNum, int pageSize, String name,int status){
        return R.ok(adminService.getItemPage(pageNum,pageSize,name,status));
    }


    //定制列表
    @PostMapping("/getCustomPage")
    public R getCustomPage(int pageNum, int pageSize, int userId,String startTime,String endTime){
        return R.ok(adminService.getCustomPage(pageNum, pageSize, userId,startTime,endTime));
    }


    //保存列表
    @PostMapping("/getPhotoPage")
    public R getPhotoPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime){
        return R.ok(adminService.getPhotoPage(pageNum,pageSize,userId,name,startTime,endTime));

    }

    //用户操作记录
    @PostMapping("/getUserRecordPage")
    public R getUserRecordPage(int pageNum, int pageSize,int userId,int appId,int status,String startTime,String endTime){
        return R.ok(adminService.getUserRecordPage(pageNum,pageSize,userId,appId,status,startTime,endTime));
    }

    //定时器最后一次执行日志
    @PostMapping("/getWebTaskLast")
    public R getWebTaskLast(){
        return R.ok(adminService.getWebTaskLast());
    }

    //定时器执行日志
    @PostMapping("/getWebTaskPage")
    public R getWebTaskPage(int pageNum,int pageSize,int type,int status,int deleteCountType){
        return R.ok(adminService.getWebTaskPage(pageNum,pageSize,type,status,deleteCountType));
    }

    //删除单条定时器日志
    @PostMapping("/deleteWebTask")
    public R deleteWebTask(Integer id){
        adminService.deleteWebTask(id);
        return R.ok("删除成功");
    }

    //清空全部定时器日志
    @PostMapping("/clearWebTask")
    public R clearWebTask(){
        adminService.clearWebTask();
        return R.ok("日志已清空");
    }

    //行为记录关联的照片详情
    @PostMapping("/getPhotoDetail")
    public R getPhotoDetail(Integer id){
        return R.ok(adminService.getPhotoDetail(id));
    }

    //用户列表
    @PostMapping("/getUserPage")
    public R getUserPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime){
        return R.ok(adminService.getUserPage(pageNum,pageSize,userId,name,startTime,endTime));
    }

    //用户地区分布
    @PostMapping("/getUserMap")
    public R getUserMap(){
        return R.ok(adminService.getUserMap());
    }

    //支付订单列表
    @PostMapping("/getPayOrderPage")
    public R getPayOrderPage(int pageNum,int pageSize,int userId,String orderNo,String orderWx,int appId,int status,String startTime,String endTime){
        return R.ok(adminService.getPayOrderPage(pageNum,pageSize,userId,orderNo,orderWx,appId,status,startTime,endTime));
    }

    //应用筛选下拉
    @PostMapping("/getAppSetOptions")
    public R getAppSetOptions(){
        return R.ok(adminService.getAppSetOptions());
    }

    //支付订单状态总数
    @PostMapping("/getPayOrderCount")
    public R getPayOrderCount(){
        return R.ok(adminService.getPayOrderCount());
    }

    //管理员发起微信全额退款
    @PostMapping("/refundPayOrder")
    public R refundPayOrder(Integer id){
        String msg = adminService.refundOrder(id);
        if(msg!=null){
            return R.no(msg);
        }
        return R.ok("退款成功");
    }

    //删除支付订单
    @PostMapping("/deletePayOrder")
    public R deletePayOrder(Integer id){
        adminService.deletePayOrder(id);
        return R.ok("删除成功");
    }

    //读取系统设置
    @PostMapping("/getWebSet")
    public R getWebSet(){
        return R.ok(adminService.getWebSet());

    }

    //修改系统设置
    @PostMapping(value = "/updateWebSet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R updateWebSet(@ModelAttribute WebSet webSet,@RequestParam(value = "file",required = false) MultipartFile file){
        String msg = adminService.updateWebSet(webSet,file);
        if(msg!=null){
            return R.no(msg);
        }
        if(file!=null){
            return R.ok(adminService.getWebSet().getOfficialQrCodeImageUrl());
        }
        return R.ok();
    }

    //问题列表
    @PostMapping("/getHelpPage")
    public R getHelpPage(int pageNum,int pageSize,String title){
        return R.ok(adminService.getHelpPage(pageNum,pageSize,title));
    }

    //保存问题
    @PostMapping("/saveHelp")
    public R saveHelp(@RequestBody Help help){
        adminService.saveHelp(help);
        return R.ok();
    }

    //删除问题
    @PostMapping("/deleteHelp")
    public R deleteHelp(Integer id){
        adminService.deleteHelp(id);
        return R.ok();
    }

    //修改问题排序
    @PostMapping("/updateHelpSort")
    public R updateHelpSort(Integer id,Integer direction){
        adminService.updateHelpSort(id,direction);
        return R.ok();
    }

    //提交功能反馈
    @PostMapping("/submitFunctionFeedback")
    public R submitFunctionFeedback(@RequestBody Map<String,String> feedback){
        String title = feedback.get("title");
        String content = feedback.get("content");
        String contact = feedback.get("contact");
        if(title==null||title.trim().isEmpty()||content==null||content.trim().isEmpty()||contact==null||contact.trim().isEmpty()){
            return R.no("请完整填写标题、内容和联系方式");
        }
        if(title.length()>255||content.length()>5000||contact.length()>255){
            return R.no("提交内容超过长度限制");
        }
        String msg = adminService.submitFunctionFeedback(title.trim(),content.trim(),contact.trim());
        if(msg!=null){
            return R.no(msg);
        }
        return R.ok();
    }

    //意见反馈列表
    @PostMapping("/getFeedbackPage")
    public R getFeedbackPage(int pageNum,int pageSize,int userId,int type,String startTime,String endTime){
        return R.ok(adminService.getFeedbackPage(pageNum,pageSize,userId,type,startTime,endTime));
    }

    //删除意见反馈
    @PostMapping("/deleteFeedback")
    public R deleteFeedback(Integer id){
        adminService.deleteFeedback(id);
        return R.ok();
    }


    //读取美颜设置
    @PostMapping("/getBeautySet")
    public R getBeautySet(){
        return R.ok(adminService.getBeautySet());

    }

    //修改美颜设置
    @PostMapping("/updateBeautySet")
    public R updateBeautySet(@RequestBody WebSet webSet){
        adminService.updateBeautySet(webSet);
        return R.ok();

    }

    //读取模型配置
    @PostMapping("/getModelSet")
    public R getModelSet(){
        return R.ok(adminService.getModelSet());
    }

    //修改模型配置
    @PostMapping("/updateModelSet")
    public R updateModelSet(@RequestBody WebSet modelSet){
        if(modelSet.getPicApiType()==1&&(modelSet.getPicApiUrl()==null||!modelSet.getPicApiUrl().startsWith("h")||!modelSet.getPicApiUrl().endsWith("/"))){
            return R.no("自建API地址错误，请输入完整地址，如http://你的ip:你的端口/ 或 http://你的域名/");
        }
        adminService.updateModelSet(modelSet);
        return R.ok();
    }

    //换装素材列表
    @PostMapping("/getClothesList")
    public R getClothesList(int pageNum, int pageSize,int id,int category,int status){
        return R.ok(adminService.getClothesList(pageNum,pageSize,id,category,status));
    }

    //修改换装素材状态
    @PostMapping("/updateClothesStatus")
    public R updateClothesStatus(Integer id,Integer status){
        adminService.updateClothesStatus(id,status);
        return R.ok();
    }

    //开始同步换装素材图片
    @PostMapping("/startClothesImageSync")
    public R startClothesImageSync(){
        String msg = adminService.startClothesImageSync();
        if(msg!=null){
            return R.no(msg);
        }
        return R.ok();
    }

    //读取换装素材图片同步进度
    @PostMapping("/getClothesImageSyncProgress")
    public R getClothesImageSyncProgress(){
        return R.ok(adminService.getClothesImageSyncProgress());
    }

    //读取探索中心设置
    @PostMapping("/getExploreSet")
    public R getExploreSet(){
        return R.ok(adminService.getExploreSet());

    }

    //修改探索中心设置
    @PostMapping(value = "/updateExploreSet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R updateExploreSet(@RequestParam("id") Integer id,
                              @RequestParam("name") String name,
                              @RequestParam("description") String description,
                              @RequestParam("status") Integer status,
                              @RequestParam("settingValue") Double settingValue,
                              @RequestParam("downloadPrice") BigDecimal downloadPrice,
                              @RequestParam(value = "file",required = false) MultipartFile file){
        AppSet appSet = new AppSet();
        appSet.setId(id);
        appSet.setName(name);
        appSet.setDescription(description);
        appSet.setStatus(status);
        appSet.setSettingValue(settingValue);
        appSet.setDownloadPrice(downloadPrice.setScale(2,RoundingMode.DOWN));
        String msg = adminService.updateExploreSet(appSet,file);
        if(msg!=null){
            return R.no(msg);
        }
        return R.ok();
    }

    //从云平台同步探索应用封面
    @PostMapping("/syncExploreImage")
    public R syncExploreImage(Integer id){
        try {
            return R.ok(adminService.syncExploreImage(id));
        } catch (Exception e) {
            return R.no(e.getMessage());
        }
    }

    //修改探索中心卡片排序
    @PostMapping("/updateExploreSort")
    public R updateExploreSort(@RequestBody List<Integer> ids){
        adminService.updateExploreSort(ids);
        return R.ok();
    }


    //用户列表面板：type=1踢掉登录状态，2删除定制记录，3删除保存记录，4删除行为记录，5禁止登录并踢掉登录，6恢复登录，7删除支付订单
    @PostMapping("/updateUserStatus")
    public R updateUserStatus(Integer userId,Integer type){
        if(userId==1 && type==1){
            return R.no("您不能踢掉自已的登录状态");
        }
        if(userId==1 && type==5){
            return R.no("您不能禁止自已登录");
        }
        return R.ok(adminService.updateUserStatus(userId,type));
    }


    //使用量统计
    @PostMapping("/getApplicationCount")
    public R getApplicationCount(){
        return R.ok(adminService.getApplicationCount());
    }

    //退出登录
    @PostMapping("/logout")
    public R logout(){
        StpUtil.logout();
        return R.ok();
    }

    //保存规格
    @PostMapping("/saveItem")
    public R saveItem(@RequestBody Item item){
        adminService.saveItem(item);
        return R.ok();
    }

    //从云平台同步证件规格
    @PostMapping("/syncItem")
    public R syncItem(){
        try {
            return R.ok(adminService.syncItem());
        } catch (Exception e) {
            return R.no(e.getMessage());
        }
    }

    //删除规格
    @PostMapping("/deleteItem")
    public R deleteItem(Integer id){
        adminService.deleteItem(id);
        return R.ok();
    }

    //删除定制规格
    @PostMapping("/deleteCustom")
    public R deleteCustom(Integer id){
        adminService.deleteCustom(id);
        return R.ok();
    }

    //删除保存图片
    @PostMapping("/deletePhoto")
    public R deletePhoto(Integer id){
        adminService.deletePhoto(id);
        return R.ok();
    }

    //删除用户操作记录
    @PostMapping("/deleteUserRecord")
    public R deleteUserRecord(Integer id){
        adminService.deleteUserRecord(id);
        return R.ok();
    }

    //用户详情统计
    @PostMapping("/getUserSummary")
    public R getUserSummary(Integer userId){
        return R.ok(adminService.getUserSummary(userId));
    }



}
