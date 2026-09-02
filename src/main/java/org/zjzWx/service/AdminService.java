package org.zjzWx.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.zjzWx.entity.*;
import org.zjzWx.model.vo.AdminIndexVo;
import org.zjzWx.model.vo.AdminLoginVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AdminService {

    //登录二维码生成
    AdminLoginVo login(String type);

    //检查登录
    String checkLogin(String code);

    //登录成功code1=微信code，code2=系统code
    String okLogin(String code1,String code2);

    //管理员首页数据
    AdminIndexVo adminIndex();

    //规格列表
    IPage<Item> getItemPage(int pageNum, int pageSize,String name,int status);

    //用户自定义分页
    IPage<Custom> getCustomPage(int pageNum, int pageSize, int userId,String startTime,String endTime);

    //保存列表
    IPage<Photo> getPhotoPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime);

    //用户操作记录
    IPage<UserRecord> getUserRecordPage(int pageNum, int pageSize,int userId,int appId,int status,String startTime,String endTime);

    //两个定时器最后一次执行日志
    List<WebTask> getWebTaskLast();

    //定时器执行日志
    IPage<WebTask> getWebTaskPage(int pageNum,int pageSize,int type,int status,int deleteCountType);

    //删除单条定时器日志
    void deleteWebTask(Integer id);

    //清空全部定时器日志
    void clearWebTask();

    //读取行为记录关联的照片
    Photo getPhotoDetail(Integer id);

    //用户列表
    IPage<User> getUserPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime);

    //用户地区分布
    Map<String,Object> getUserMap();

    //支付订单列表
    IPage<PayOrder> getPayOrderPage(int pageNum,int pageSize,int userId,String orderNo,String orderWx,int appId,int status,String startTime,String endTime);

    //应用筛选下拉
    List<Map<String,Object>> getAppSetOptions();

    //支付订单状态总数
    Map<String,Object> getPayOrderCount();

    //管理员发起微信全额退款
    String refundOrder(Integer id);

    //删除支付订单
    void deletePayOrder(Integer id);

    //读取系统设置
    WebSet getWebSet();

    //修改系统设置，返回失败原因，成功时返回null
    String updateWebSet(WebSet webSet,MultipartFile file);

    //问题列表
    IPage<Help> getHelpPage(int pageNum,int pageSize,String title);

    //保存问题
    void saveHelp(Help help);

    //删除问题
    void deleteHelp(Integer id);

    //修改问题排序
    void updateHelpSort(Integer id,Integer direction);

    //提交功能反馈到云中心，返回失败原因，成功时返回null
    String submitFunctionFeedback(String title,String content,String contact);

    //意见反馈列表
    IPage<Feedback> getFeedbackPage(int pageNum,int pageSize,int userId,int type,String startTime,String endTime);

    //删除意见反馈
    void deleteFeedback(Integer id);

    //读取美颜设置
    WebSet getBeautySet();

    //修改美颜设置
    void updateBeautySet(WebSet webSet);

    //读取模型配置
    WebSet getModelSet();

    //修改模型配置
    void updateModelSet(WebSet modelSet);

    //换装素材列表
    IPage<ClothesSet> getClothesList(int pageNum, int pageSize,int id,int category,int status);

    //修改换装素材状态
    void updateClothesStatus(Integer id,Integer status);

    //开始同步换装素材图片，成功时返回null
    String startClothesImageSync();

    //读取换装素材同步进度
    Map<String,Object> getClothesImageSyncProgress();

    //读取探索中心设置
    List<AppSet> getExploreSet();

    //修改探索中心设置和封面，返回失败原因，成功时返回null
    String updateExploreSet(AppSet appSet, MultipartFile file);

    //从云平台同步探索应用封面
    String syncExploreImage(Integer id);

    //保存探索中心卡片顺序
    void updateExploreSort(List<Integer> ids);

    //用户列表面板：type=1踢掉登录状态，2删除定制记录，3删除保存记录，4删除行为记录，5禁止登录并踢掉登录，6恢复登录，7删除支付订单
    String updateUserStatus(Integer userId,Integer type);

    //使用量统计
    Map<String,Object> getApplicationCount();

    //保存规格
    void saveItem(Item item);

    //从云平台同步证件规格
    Map<String,Object> syncItem();

    //删除规格
    void deleteItem(Integer id);

    //删除定制规格
    void deleteCustom(Integer id);

    //删除保存图片
    void deletePhoto(Integer id);

    //删除用户操作记录
    void deleteUserRecord(Integer id);

    //用户详情统计
    Map<String,Object> getUserSummary(Integer userId);


}
