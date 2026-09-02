package org.zjzWx.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.zjzWx.async.AsyncCenter;
import org.zjzWx.entity.*;
import org.zjzWx.model.vo.AdminIndexVo;
import org.zjzWx.model.vo.AdminLoginVo;
import org.zjzWx.model.vo.ChartDataVo;
import org.zjzWx.service.*;
import org.zjzWx.util.PicUtil;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminServiceImpl implements AdminService {


    private static final String CLOUD_URL = "https://cloud.0po.cn/";

    @Autowired
    private WebSetService webSetService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CustomService customService;
    @Autowired
    private PhotoService photoService;
    @Autowired
    private AppSetService appSetService;
    @Autowired
    private ClothesSetService clothesSetService;
    @Autowired
    private HelpService helpService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private PayOrderService payOrderService;
    @Autowired
    private AsyncCenter asyncCenter;
    @Autowired
    private WebTaskService webTaskService;



    @Override
    public AdminLoginVo login(String type) {
        try {
            long code = System.currentTimeMillis();
            WebSet webSet = webSetService.getOne(null);
            Map<String, Object> mp = new HashMap<>();
            mp.put("scene", code);
            mp.put("page", "pages/admin/index");
            mp.put("check_path", false);
            mp.put("env_version", type);

            //获取access_token
            String url1 = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + webSet.getAppId() + "&secret=" + webSet.getAppSecret();
            RestTemplate restTemplate = new RestTemplate();

            //发起请求
            ResponseEntity<String> response = restTemplate.exchange(url1, HttpMethod.GET, null, String.class);
            JSONObject jsonopenid = JSONObject.parseObject(response.getBody());
            String accessToken = jsonopenid.getString("access_token");


            // 获取小程序码
            String url2 = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSONObject.toJSONString(mp), headers);

            //发起请求
            ResponseEntity<byte[]> byteResponse = restTemplate.exchange(url2, HttpMethod.POST, entity, byte[].class);


            if (byteResponse.getBody()!=null) {
                SaManager.getSaTokenDao().set("admin:login:code", String.valueOf(code), 300);
                SaManager.getSaTokenDao().set("admin:login:status", "0", 300);

                AdminLoginVo adminLoginVo = new AdminLoginVo();
                adminLoginVo.setPic(byteResponse.getBody());
                adminLoginVo.setCode(code);
                return adminLoginVo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public String checkLogin(String code) {
        if(code.equals(SaManager.getSaTokenDao().get("admin:login:code")) && "1".equals(SaManager.getSaTokenDao().get("admin:login:status"))){
            //网页后台使用PC设备类型登录，与小程序的WX设备互不顶号
            StpUtil.login(1,"PC");
            SaManager.getSaTokenDao().delete("admin:login:code");
            SaManager.getSaTokenDao().delete("admin:login:status");
            return StpUtil.getTokenInfo().getTokenValue();
        }
        return null;
    }


    @Override
    public String okLogin(String code1, String code2) {
        try {
            if(!code2.equals(SaManager.getSaTokenDao().get("admin:login:code"))){
                return "登录请求已失效，请重新刷新二维码";
            }
            if("1".equals(SaManager.getSaTokenDao().get("admin:login:status"))){
                return "已登录，无需重复登录";
            }

            WebSet webSet = webSetService.getById(1);
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+webSet.getAppId()
                    +"&secret="+webSet.getAppSecret()+"&js_code=" + code1 + "&grant_type=authorization_code";

            RestTemplate restTemplate = new RestTemplate();

            //发起请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonopenid = JSONObject.parseObject(response.getBody());
            if(jsonopenid==null){
                return "与微信通讯失败，请重试";
            }

            String openid = jsonopenid.getString("openid");
            // 高风险的微信用户/数据库配置错误/安全域名没有添加会存在openid没有的情况
            if (openid==null) {
                return jsonopenid.toString();
            }

            QueryWrapper<User> qwuser = new QueryWrapper<>();
            qwuser.eq("openid",openid);
            User user = userService.getOne(qwuser);
            if(user==null){
                return "您未注册，无法检查是否为管理员";
            }

            if(user.getId()==1){
                SaManager.getSaTokenDao().set("admin:login:status", "1", 300);
                return null;
            }else {
                return "您不是管理员，无法登录";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "代码报错";
        }
    }




    @Override
    public AdminIndexVo adminIndex() {
        // 获取当前日期
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate today = now.toLocalDate();
        // 当天的开始时间
        LocalDateTime startOfDay = today.atStartOfDay(); // 默认时区
        // 当天的结束时间
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX); // 使用当天的最后一刻

        AdminIndexVo adminIndexVo = new AdminIndexVo();

        // 按应用和照片去重，统计当天的照片制作次数
        Object makeNum = userRecordService.getBaseMapper().selectObjs(
                new QueryWrapper<UserRecord>()
                        .select("COUNT(DISTINCT app_id, photo_id)")
                        .ge("create_time", startOfDay)
                        .le("create_time", endOfDay)
                        .eq("status",1)
                        .ge("app_id",3)
        ).get(0);
        adminIndexVo.setMakeNum(Long.parseLong(makeNum.toString()));

        // 按应用和照片去重，统计累计照片制作次数
        Object makeTotal = userRecordService.getBaseMapper().selectObjs(
                new QueryWrapper<UserRecord>()
                        .select("COUNT(DISTINCT app_id, photo_id)")
                        .ge("app_id",3)
                        .eq("status",1)
        ).get(0);
        adminIndexVo.setMakeTotal(Long.parseLong(makeTotal.toString()));

        // 统计当天的用户数量
        QueryWrapper<User> qw2 = new QueryWrapper<>();
        qw2.ge("create_time", startOfDay)
                .le("create_time", endOfDay);
        adminIndexVo.setUserNum(userService.count(qw2));
        adminIndexVo.setUserTotal(userService.count());

        //统计当天新增的全部订单，不区分订单状态
        QueryWrapper<PayOrder> orderQw = new QueryWrapper<>();
        orderQw.ge("create_time",startOfDay)
                .le("create_time",endOfDay);
        adminIndexVo.setOrderNum(payOrderService.count(orderQw));

        // 生成最近7天的日期列表和数据统计
        List<String> timeList = new ArrayList<>();
        List<Integer> dataList = new ArrayList<>();
        LocalDate startDate = today.minusDays(6); // 最近7天的起始日期

        // 生成日期列表
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            timeList.add(date.toString());
        }

        // 查询日期范围内的记录数
        List<Map<String, Object>> counts = userRecordService.getBaseMapper().selectMaps(
                new QueryWrapper<UserRecord>()
                        .select("DATE(create_time) as date", "COUNT(DISTINCT app_id, photo_id) as count")
                        .ge("create_time", startDate.atStartOfDay())  // 从最近7天的起始日期
                        .le("create_time", endOfDay)  // 改为当前时间
                        .eq("status",1)
                        .ge("app_id",3)
                        .groupBy("DATE(create_time)")
        );

        // 将查询结果转换为日期-数量的映射
        Map<String, Integer> countMap = new HashMap<>();
        for (Map<String, Object> record : counts) {
            String date = record.get("date").toString();
            Integer count = Integer.valueOf(record.get("count").toString());
            countMap.put(date, count);
        }

        // 组装数据列表
        for (String date : timeList) {
            Integer count = countMap.getOrDefault(date, 0);
            dataList.add(count);
        }

        // 封装数据返回
        ChartDataVo chartData = new ChartDataVo();
        chartData.setTime(timeList);
        chartData.setData(dataList);
        adminIndexVo.setChartDataVo(chartData);


        return adminIndexVo;

    }


    @Override
    public IPage<Item> getItemPage(int pageNum, int pageSize,String name,int status) {
        Page<Item> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Item> qw = new QueryWrapper<>();
        if(name!=null && !"".equals(name)){
            qw.like("name",name);
        }
        if(status!=0){
            qw.eq("status",status);
        }
        qw.orderByDesc("id");
        return itemService.page(page, qw);
    }

    @Override
    public IPage<Custom> getCustomPage(int pageNum, int pageSize, int userId,String startTime,String endTime) {
        Page<Custom> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Custom> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("user_id",userId);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time", LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time", LocalDateTime.of(LocalDate.parse(endTime), LocalTime.MAX));
        }
        qw.orderByDesc("create_time").orderByDesc("id");
        return customService.page(page, qw);
    }

    @Override
    public IPage<Photo> getPhotoPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime) {
        Page<Photo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Photo> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("user_id",userId);
        }
        if(name!=null && !"".equals(name)){
            qw.like("name",name);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time",LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time",LocalDateTime.of(LocalDate.parse(endTime),LocalTime.MAX));
        }
        qw.isNotNull("n_img");
        qw.orderByDesc("create_time").orderByDesc("id");
        return photoService.page(page, qw);
    }

    @Override
    public IPage<UserRecord> getUserRecordPage(int pageNum, int pageSize,int userId,int appId,int status,String startTime,String endTime) {
        Page<UserRecord> page = new Page<>(pageNum, pageSize);
        QueryWrapper<UserRecord> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("user_id",userId);
        }
        if(appId!=0){
            qw.eq("app_id",appId);
        }
        if(status!=0){
            qw.eq("status",status);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time",LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time",LocalDateTime.of(LocalDate.parse(endTime),LocalTime.MAX));
        }
        qw.orderByDesc("create_time").orderByDesc("id");
        return userRecordService.page(page, qw);
    }


    @Override
    public List<WebTask> getWebTaskLast() {
        QueryWrapper<WebTask> firstQw = new QueryWrapper<>();
        firstQw.eq("type",1);
        firstQw.orderByDesc("id");
        firstQw.last("limit 1");
        WebTask first = webTaskService.getOne(firstQw);
        if(first==null){
            first = new WebTask();
            first.setType(1);
            first.setTaskName("未解锁照片清理");
        }

        QueryWrapper<WebTask> secondQw = new QueryWrapper<>();
        secondQw.eq("type",2).orderByDesc("id").last("limit 1");
        WebTask second = webTaskService.getOne(secondQw);
        if(second==null){
            second = new WebTask();
            second.setType(2);
            second.setTaskName("临时编辑数据清理");
        }

        List<WebTask> tasks = new ArrayList<>();
        tasks.add(first);
        tasks.add(second);
        return tasks;
    }

    @Override
    public IPage<WebTask> getWebTaskPage(int pageNum, int pageSize, int type, int status, int deleteCountType) {
        Page<WebTask> page = new Page<>(pageNum,pageSize);
        QueryWrapper<WebTask> qw = new QueryWrapper<>();
        if(type!=0){
            qw.eq("type",type);
        }
        if(status!=0){
            qw.eq("status",status);
        }
        if(deleteCountType==1){
            qw.gt("delete_count",0);
        }
        if(deleteCountType==2){
            qw.eq("delete_count",0);
        }
        qw.orderByDesc("id");
        return webTaskService.page(page,qw);
    }

    @Override
    public void deleteWebTask(Integer id) {
        webTaskService.removeById(id);
    }

    @Override
    public void clearWebTask() {
        webTaskService.truncateTable();
    }

    @Override
    public Photo getPhotoDetail(Integer id) {
        return photoService.getById(id);
    }

    @Override
    public IPage<User> getUserPage(int pageNum, int pageSize,int userId,String name,String startTime,String endTime) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("id",userId);
        }
        if(name!=null && !"".equals(name)){
            qw.like("nickname",name);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time",LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time",LocalDateTime.of(LocalDate.parse(endTime),LocalTime.MAX));
        }
        qw.orderByDesc("create_time").orderByDesc("id");
        return userService.page(page, qw);
    }

    @Override
    public Map<String, Object> getUserMap() {
        //中国省级地图使用的地区代码
        Map<String,String> regionCodes = new LinkedHashMap<>();
        regionCodes.put("北京","CN-11");
        regionCodes.put("天津","CN-12");
        regionCodes.put("河北","CN-13");
        regionCodes.put("山西","CN-14");
        regionCodes.put("内蒙古","CN-15");
        regionCodes.put("辽宁","CN-21");
        regionCodes.put("吉林","CN-22");
        regionCodes.put("黑龙江","CN-23");
        regionCodes.put("上海","CN-31");
        regionCodes.put("江苏","CN-32");
        regionCodes.put("浙江","CN-33");
        regionCodes.put("安徽","CN-34");
        regionCodes.put("福建","CN-35");
        regionCodes.put("江西","CN-36");
        regionCodes.put("山东","CN-37");
        regionCodes.put("河南","CN-41");
        regionCodes.put("湖北","CN-42");
        regionCodes.put("湖南","CN-43");
        regionCodes.put("广东","CN-44");
        regionCodes.put("广西","CN-45");
        regionCodes.put("海南","CN-46");
        regionCodes.put("重庆","CN-50");
        regionCodes.put("四川","CN-51");
        regionCodes.put("贵州","CN-52");
        regionCodes.put("云南","CN-53");
        regionCodes.put("西藏","CN-54");
        regionCodes.put("陕西","CN-61");
        regionCodes.put("甘肃","CN-62");
        regionCodes.put("青海","CN-63");
        regionCodes.put("宁夏","CN-64");
        regionCodes.put("新疆","CN-65");
        regionCodes.put("台湾","CN-71");
        regionCodes.put("香港","CN-81");
        regionCodes.put("澳门","CN-82");

        Map<String,Long> regionCounts = new LinkedHashMap<>();
        for(String regionName:regionCodes.keySet()){
            regionCounts.put(regionName,0L);
        }

        //先按照用户城市分组，再归入对应省级地区
        QueryWrapper<User> cityQw = new QueryWrapper<>();
        cityQw.select("city","COUNT(*) AS user_count");
        cityQw.groupBy("city");
        List<Map<String,Object>> cityCounts = userService.getBaseMapper().selectMaps(cityQw);
        for(Map<String,Object> cityCount:cityCounts){
            String city = (String) cityCount.get("city");
            long count = ((Number) cityCount.get("user_count")).longValue();
            if(city!=null && !city.isEmpty()){
                for(String regionName:regionCodes.keySet()){
                    if(city.contains(regionName)){
                        regionCounts.put(regionName,regionCounts.get(regionName)+count);
                        break;
                    }
                }
            }
        }

        List<Map<String,Object>> regions = new ArrayList<>();
        for(Map.Entry<String,String> regionCode:regionCodes.entrySet()){
            Map<String,Object> region = new HashMap<>();
            region.put("name",regionCode.getKey());
            region.put("code",regionCode.getValue());
            region.put("count",regionCounts.get(regionCode.getKey()));
            regions.add(region);
        }

        Map<String,Object> data = new HashMap<>();
        data.put("regions",regions);
        return data;
    }

    @Override
    public IPage<PayOrder> getPayOrderPage(int pageNum, int pageSize,int userId,String orderNo,String orderWx,int appId,int status,String startTime,String endTime) {
        return payOrderService.getPayOrderPage(pageNum,pageSize,userId,orderNo,orderWx,appId,status,startTime,endTime);
    }

    @Override
    public List<Map<String,Object>> getAppSetOptions() {
        return appSetService.listMaps(new QueryWrapper<AppSet>().select("id","name").orderByAsc("id"));
    }


    @Override
    public Map<String, Object> getPayOrderCount() {
        QueryWrapper<PayOrder> pendingQw = new QueryWrapper<>();
        pendingQw.eq("status",1);

        QueryWrapper<PayOrder> paidQw = new QueryWrapper<>();
        paidQw.eq("status",2);

        QueryWrapper<PayOrder> refundedQw = new QueryWrapper<>();
        refundedQw.eq("status",3);

        Map<String,Object> data = new HashMap<>();
        data.put("pendingCount",payOrderService.count(pendingQw));
        data.put("paidCount",payOrderService.count(paidQw));
        data.put("refundedCount",payOrderService.count(refundedQw));
        return data;
    }

    @Override
    public String refundOrder(Integer id) {
        return payOrderService.refundOrder(id);
    }

    @Override
    public void deletePayOrder(Integer id) {
        payOrderService.removeById(id);
    }

    @Override
    public WebSet getWebSet() {
        QueryWrapper<WebSet> qw = new QueryWrapper<>();
        qw.eq("id",1);
        qw.select("id","app_id","app_secret","video_unit_id","login_type","pic_domain","directory","official_switch","official_qr_code_image_url","merchant_id","merchant_serial_number",
                "api_v3_key","merchant_private_key","pay_notify_url");
        return webSetService.getOne(qw);
    }

    @Override
    public String updateWebSet(WebSet webSet,MultipartFile file) {
        webSet.setId(1);
        if(file==null){
            webSetService.updateById(webSet);
            return null;
        }
        WebSet storageSet = webSetService.getById(1);
        if(storageSet.getDirectory()==null || storageSet.getDirectory().isEmpty()
                || storageSet.getPicDomain()==null || storageSet.getPicDomain().isEmpty()){
            return "请先保存图片存储配置后重新上传";
        }
        if(file.getSize()>15*1024*1024){
            return "图片大小不能超过15M";
        }
        try {
            byte[] imageBytes = file.getBytes();
            String extension = PicUtil.getImageExtension(imageBytes);
            if(extension==null){
                return "图片不合法，仅支持jpg/png/jpeg";
            }
            String oldQrCodeImage = storageSet.getOfficialQrCodeImageUrl();
            String qrCodeImage = PicUtil.savePermanentImage("officialQrCode",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),extension);
            webSet.setOfficialQrCodeImageUrl(qrCodeImage);
            if(!webSetService.updateById(webSet)){
                return "公众号二维码图片保存失败，请重试";
            }
            PicUtil.deleteImage(oldQrCodeImage,storageSet.getDirectory());
            return null;
        } catch (Exception e) {
            return "公众号二维码图片保存失败，请重试";
        }
    }

    @Override
    public IPage<Help> getHelpPage(int pageNum, int pageSize, String title) {
        Page<Help> page = new Page<>(pageNum,pageSize);
        QueryWrapper<Help> qw = new QueryWrapper<>();
        if(title!=null && !title.isEmpty()){
            qw.like("title",title);
        }
        qw.orderByDesc("id");
        return helpService.page(page,qw);
    }

    @Override
    public void saveHelp(Help help) {
        helpService.saveOrUpdate(help);
    }

    @Override
    public void deleteHelp(Integer id) {
        helpService.removeById(id);
    }

    @Override
    public void updateHelpSort(Integer id, Integer direction) {
        Help help = helpService.getById(id);
        QueryWrapper<Help> qw = new QueryWrapper<>();
        if(direction==1){
            qw.lt("sort",help.getSort()).orderByDesc("sort");
        }else{
            qw.gt("sort",help.getSort()).orderByAsc("sort");
        }
        qw.last("limit 1");
        Help target = helpService.getOne(qw);
        if(target==null){
            return;
        }
        Integer sort = help.getSort();
        help.setSort(target.getSort());
        target.setSort(sort);
        helpService.updateBatchById(Arrays.asList(help,target));
    }

    @Override
    public String submitFunctionFeedback(String title, String content, String contact) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String,String> data = new LinkedMultiValueMap<>();
        data.add("title",title);
        data.add("content",content);
        data.add("contact",contact);
        ResponseEntity<String> response = new RestTemplate().exchange(CLOUD_URL+"functionFeedback/submit",HttpMethod.POST,new HttpEntity<>(data,headers),String.class);
        JSONObject result = JSONObject.parseObject(response.getBody());
        return result.getInteger("code")==200 ? null : result.getString("msg");
    }

    @Override
    public IPage<Feedback> getFeedbackPage(int pageNum, int pageSize, int userId, int type, String startTime, String endTime) {
        Page<Feedback> page = new Page<>(pageNum,pageSize);
        QueryWrapper<Feedback> qw = new QueryWrapper<>();
        if(userId!=0){
            qw.eq("user_id",userId);
        }
        if(type!=0){
            qw.eq("type",type);
        }
        if(!startTime.isEmpty()){
            qw.ge("create_time",LocalDate.parse(startTime).atStartOfDay());
        }
        if(!endTime.isEmpty()){
            qw.le("create_time",LocalDateTime.of(LocalDate.parse(endTime),LocalTime.MAX));
        }
        qw.orderByDesc("create_time").orderByDesc("id");
        return feedbackService.page(page,qw);
    }

    @Override
    public void deleteFeedback(Integer id) {
        Feedback feedback = feedbackService.getById(id);
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("directory");
        WebSet storageSet = webSetService.getOne(storageQw);
        PicUtil.deleteImage(feedback.getImageUrls(),storageSet.getDirectory());
        feedbackService.removeById(id);
    }

    @Override
    public WebSet getBeautySet() {
        QueryWrapper<WebSet> qw = new QueryWrapper<>();
        qw.eq("id",1);
        qw.select("id","brightness_strength","contrast_strength","sharpen_strength","saturation_strength");
        return webSetService.getOne(qw);
    }

    @Override
    public void updateBeautySet(WebSet webSet) {
        WebSet beautySet = new WebSet();
        beautySet.setId(1);
        beautySet.setBrightnessStrength(webSet.getBrightnessStrength());
        beautySet.setContrastStrength(webSet.getContrastStrength());
        beautySet.setSharpenStrength(webSet.getSharpenStrength());
        beautySet.setSaturationStrength(webSet.getSaturationStrength());
        webSetService.updateById(beautySet);
    }

    @Override
    public WebSet getModelSet() {
        QueryWrapper<WebSet> qw = new QueryWrapper<>();
        qw.eq("id",1);
        qw.select("pic_api_type","pic_api_url","pic_api_key","human_matting_model","face_detect_model","matting_model","colourize_model","cartoon_model",
                "american_human_matting_model","american_face_detect_model","template_human_matting_model","template_face_detect_model",
                "couple_human_matting_model","couple_face_detect_model","clothes_face_detect_model","clothes_parsing_model","deblur_model");
        return webSetService.getOne(qw);
    }

    @Override
    public void updateModelSet(WebSet modelSet) {
        if(modelSet.getPicApiType()==2){
            modelSet.setPicApiUrl("https://cloud.0po.cn/v1/");
        }
        modelSet.setId(1);
        webSetService.updateById(modelSet);
    }

    @Override
    public List<AppSet> getExploreSet() {
        return appSetService.list(new QueryWrapper<AppSet>().orderByAsc("sort"));
    }

    @Override
    public IPage<ClothesSet> getClothesList(int pageNum, int pageSize,int id,int category,int status) {
        Page<ClothesSet> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ClothesSet> qw = new QueryWrapper<>();
        if(id!=0){
            qw.eq("id",id);
        }
        if(category!=0){
            qw.eq("category",category);
        }
        if(status!=0){
            qw.eq("status",status);
        }
        qw.orderByDesc("id");
        return clothesSetService.page(page,qw);
    }

    @Override
    public void updateClothesStatus(Integer id, Integer status) {
        ClothesSet update = new ClothesSet();
        update.setId(id);
        update.setStatus(status);
        clothesSetService.updateById(update);
    }

    @Override
    public String startClothesImageSync() {
        return clothesSetService.startImageSync();
    }

    @Override
    public Map<String,Object> getClothesImageSyncProgress() {
        return clothesSetService.getImageSyncProgress();
    }

    @Override
    public String updateExploreSet(AppSet appSet, MultipartFile file) {
        //鉴黄阈值为色情和色情动漫两项分数相加后的拦截线
        if(appSet.getId()==14 && (appSet.getSettingValue()<0.01 || appSet.getSettingValue()>1)){
            return "鉴黄阈值必须填写0.01到1之间的数值";
        }
        //广告或付费下载启用前，先检查对应的系统配置
        if(appSet.getStatus()==2 || appSet.getStatus()==3 || appSet.getStatus()==4){
            WebSet webSet = webSetService.getById(1);
            if((appSet.getStatus()==2 || appSet.getStatus()==4)
                    && (webSet.getVideoUnitId()==null || "".equals(webSet.getVideoUnitId()))){
                return "未配置广告位ID，请先前往系统设置进行配置";
            }
            if((appSet.getStatus()==3 || appSet.getStatus()==4)
                    && (webSet.getMerchantId()==null || "".equals(webSet.getMerchantId()))){
                return "未配置微信支付，请先前往系统设置进行配置";
            }
        }
        AppSet oldAppSet = appSetService.getById(appSet.getId());

        //没有上传新封面时，继续使用原来的封面
        if(file==null || file.isEmpty()){
            appSet.setImage(oldAppSet.getImage());
            appSetService.updateById(appSet);
            return null;
        }

        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("pic_domain","directory");
        WebSet storageSet = webSetService.getOne(storageQw);
        if(storageSet==null || !StringUtils.hasText(storageSet.getPicDomain()) || !StringUtils.hasText(storageSet.getDirectory())){
            return "上传图片失败，请先配置图片存储，系统设置-图片存储配置";
        }

        //当新封面超过15MB时，停止保存
        if(file.getSize()>15*1024*1024){
            return "封面不能超过15MB";
        }
        try {
            byte[] imageBytes = file.getBytes();
            String extension = PicUtil.getImageExtension(imageBytes);

            //当上传内容不是真实的JPG或PNG图片时，拒绝保存
            if(extension==null){
                return "图片不合法，仅支持jpg/png/jpeg";
            }
            String image = PicUtil.savePermanentImage("explore",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),extension);
            appSet.setImage(image);
            appSetService.updateById(appSet);
            PicUtil.deleteImage(oldAppSet.getImage(),storageSet.getDirectory());
            return null;
        } catch (Exception e) {
            return "封面保存失败，请重试";
        }
    }

    @Override
    public String syncExploreImage(Integer id) {
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("pic_domain","directory");
        WebSet storageSet = webSetService.getOne(storageQw);
        if(storageSet==null || !StringUtils.hasText(storageSet.getPicDomain()) || !StringUtils.hasText(storageSet.getDirectory())){
            throw new IllegalStateException("上传图片失败，请先配置图片存储，系统设置-图片存储配置");
        }
        AppSet appSet = appSetService.getById(id);
        if(appSet==null || id==1 || id==2 || id==14 || id==15){
            throw new IllegalStateException("非法请求");
        }
        if(StringUtils.hasText(appSet.getImage())){
            return appSet.getImage();
        }

        JSONObject response = JSONObject.parseObject(new RestTemplate().getForObject(CLOUD_URL+"sync/applicationCover?id="+id,String.class));
        if(response==null || response.getIntValue("code")!=200 || response.getJSONObject("data")==null){
            throw new IllegalStateException(response==null ? "云平台封面读取失败" : response.getString("msg"));
        }
        byte[] imageBytes = new RestTemplate().getForObject(response.getJSONObject("data").getString("imageUrl"),byte[].class);
        if(imageBytes==null || imageBytes.length>15*1024*1024){
            throw new IllegalStateException("云平台封面下载失败");
        }
        String extension = PicUtil.getImageExtension(imageBytes);
        if(extension==null){
            throw new IllegalStateException("云平台返回的封面不是有效的JPG或PNG图片");
        }
        try {
            String image = PicUtil.savePermanentImage("explore",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),extension);
            AppSet update = new AppSet();
            update.setId(id);
            update.setImage(image);
            if(!appSetService.updateById(update)){
                PicUtil.deleteImage(image,storageSet.getDirectory());
                throw new IllegalStateException("封面保存失败，请重试");
            }
            return image;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage()==null ? "封面保存失败，请重试" : e.getMessage());
        }
    }

    @Override
    public void updateExploreSort(List<Integer> ids) {
        List<AppSet> appSets = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            AppSet appSet = new AppSet();
            appSet.setId(ids.get(i));
            appSet.setSort(i+1);
            appSets.add(appSet);
        }
        appSetService.updateBatchById(appSets);

    }

    @Override
    public String updateUserStatus(Integer userId, Integer type) {
        //type=1踢掉登录状态，2删除定制记录，3删除保存记录，4删除行为记录，5禁止登录并踢掉登录，6恢复登录，7删除支付订单
        if(type==1){
            StpUtil.kickout(userId);
            return "踢掉成功";
        }else if(type==2){
            QueryWrapper<Custom> qw = new QueryWrapper<>();
            qw.eq("user_id",userId);
            customService.remove(qw);
            return "删除成功";
        }else if(type==3 || type==4 || type==7){
            asyncCenter.clearUserData(userId,type);
            return "清理任务已提交，预计10分钟内完成，完成前请勿重复操作";
        }else if(type==5){
            User user = new User();
            user.setId(userId);
            user.setStatus(2);
            userService.updateById(user);
            StpUtil.kickout(userId);
            return "已禁止并踢掉登录";
        }else if(type==6){
            User user = new User();
            user.setId(userId);
            user.setStatus(1);
            userService.updateById(user);
            return "已恢复";
        }else {
            return "非法请求";
        }

    }

    @Override
    public Map<String,Object> getApplicationCount() {
        List<Map<String,Object>> applications = new ArrayList<>();
        long applicationTotal = 0L;
        List<AppSet> appSets = appSetService.list(new QueryWrapper<AppSet>()
                .notIn("id",1,2,14,15)
                .orderByAsc("sort"));
        for (AppSet appSet : appSets) {
            QueryWrapper<UserRecord> recordQw = new QueryWrapper<>();
            recordQw.select("COUNT(DISTINCT photo_id)").eq("app_id",appSet.getId()).eq("status",1);
            Object useCountData = userRecordService.getBaseMapper().selectObjs(recordQw).get(0);
            long useCount = Long.parseLong(useCountData.toString());
            Map<String,Object> application = new HashMap<>();
            application.put("id",appSet.getId());
            application.put("name",appSet.getName());
            application.put("useCount",useCount);
            applications.add(application);
            applicationTotal += useCount;
        }

        QueryWrapper<UserRecord> uploadQw = new QueryWrapper<>();
        uploadQw.eq("app_id",1).eq("status",1);
        long uploadCount = userRecordService.count(uploadQw);

        QueryWrapper<PayOrder> pendingOrderQw = new QueryWrapper<>();
        pendingOrderQw.eq("status",1);

        QueryWrapper<PayOrder> paidOrderQw = new QueryWrapper<>();
        paidOrderQw.eq("status",2);

        QueryWrapper<PayOrder> refundedOrderQw = new QueryWrapper<>();
        refundedOrderQw.eq("status",3);

        Map<String,Object> data = new HashMap<>();
        data.put("applications",applications);
        data.put("uploadCount",uploadCount);
        data.put("payOrderCount",payOrderService.count());
        data.put("pendingOrderCount",payOrderService.count(pendingOrderQw));
        data.put("paidOrderCount",payOrderService.count(paidOrderQw));
        data.put("refundedOrderCount",payOrderService.count(refundedOrderQw));
        data.put("totalCount",uploadCount+applicationTotal);
        return data;
    }

    @Override
    public void saveItem(Item item) {
        if(item.getId()==null){
            item.setCloudItemId(0);
        }else {
            item.setCloudItemId(null);
        }
        itemService.saveOrUpdate(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> syncItem() {
        JSONObject response = JSONObject.parseObject(new RestTemplate().getForObject(CLOUD_URL+"v1/specificationList",String.class));
        if(response==null || response.getIntValue("code")!=200 || response.getJSONArray("data")==null || response.getJSONArray("data").isEmpty()){
            throw new IllegalStateException("云平台规格读取失败");
        }
        JSONArray data = response.getJSONArray("data");
        List<Item> localList = itemService.list(new QueryWrapper<Item>().gt("cloud_item_id",0));
        Map<Integer,Item> localMap = new HashMap<>();
        for(Item item : localList){
            localMap.put(item.getCloudItemId(),item);
        }

        Set<Integer> cloudIds = new HashSet<>();
        List<Item> insertList = new ArrayList<>();
        List<Item> updateList = new ArrayList<>();
        for(int i=0;i<data.size();i++){
            JSONObject specification = data.getJSONObject(i);
            Integer cloudItemId = specification.getInteger("id");
            if(cloudItemId==null || cloudItemId<1 || !cloudIds.add(cloudItemId)){
                throw new IllegalStateException("云平台规格数据错误");
            }
            Item item = new Item();
            Item localItem = localMap.get(cloudItemId);
            if(localItem!=null){
                item.setId(localItem.getId());
            }
            item.setCloudItemId(cloudItemId);
            item.setName(specification.getString("name"));
            item.setWidthPx(specification.getInteger("width_px"));
            item.setHeightPx(specification.getInteger("height_px"));
            item.setWidthMm(specification.getInteger("width_mm"));
            item.setHeightMm(specification.getInteger("height_mm"));
            item.setIcon(specification.getInteger("icon"));
            item.setCategory(specification.getInteger("category"));
            item.setDpi(specification.getInteger("dpi"));
            item.setStatus(1);
            if(localItem==null){
                insertList.add(item);
            }else {
                updateList.add(item);
            }
        }

        if(!insertList.isEmpty() && !itemService.saveBatch(insertList)){
            throw new IllegalStateException("云平台规格新增失败");
        }
        if(!updateList.isEmpty() && !itemService.updateBatchById(updateList)){
            throw new IllegalStateException("云平台规格更新失败");
        }
        QueryWrapper<Item> deleteQw = new QueryWrapper<>();
        deleteQw.gt("cloud_item_id",0).notIn("cloud_item_id",cloudIds);
        int deleteCount = (int) itemService.count(deleteQw);
        if(deleteCount>0 && !itemService.remove(deleteQw)){
            throw new IllegalStateException("云平台下架规格删除失败");
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("insertCount",insertList.size());
        result.put("updateCount",updateList.size());
        result.put("deleteCount",deleteCount);
        result.put("customCount",itemService.count(new QueryWrapper<Item>().eq("cloud_item_id",0)));
        return result;
    }

    @Override
    public void deleteItem(Integer id) {
        itemService.removeById(id);
    }

    @Override
    public void deleteCustom(Integer id) {
        customService.removeById(id);
    }

    @Override
    public void deletePhoto(Integer id) {
        Photo photo = photoService.getById(id);
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("directory");
        WebSet storageSet = webSetService.getOne(storageQw);
        PicUtil.deleteImage(photo.getNImg(),storageSet.getDirectory());
        PicUtil.deleteTempDirectory(id,storageSet.getDirectory());
        photoService.removeById(id);
    }

    @Override
    public void deleteUserRecord(Integer id) {
        userRecordService.removeById(id);
    }

    @Override
    public Map<String, Object> getUserSummary(Integer userId) {
        Map<String, Object> data = new HashMap<>();
        User user = userService.getById(userId);
        data.put("user", user);
        if(user==null){
            return data;
        }

        QueryWrapper<Custom> customQw = new QueryWrapper<>();
        customQw.eq("user_id", userId);
        data.put("customCount", customService.count(customQw));

        QueryWrapper<Photo> photoQw = new QueryWrapper<>();
        photoQw.eq("user_id", userId).isNotNull("n_img");
        data.put("photoCount", photoService.count(photoQw));

        QueryWrapper<UserRecord> recordQw = new QueryWrapper<>();
        recordQw.eq("user_id", userId);
        data.put("recordCount", userRecordService.count(recordQw));

        QueryWrapper<UserRecord> recentRecordQw = new QueryWrapper<>();
        recentRecordQw.eq("user_id", userId).orderByDesc("create_time").orderByDesc("id");
        data.put("recentRecords", userRecordService.page(new Page<>(1, 10), recentRecordQw).getRecords());

        QueryWrapper<Photo> recentPhotoQw = new QueryWrapper<>();
        recentPhotoQw.eq("user_id", userId).isNotNull("n_img").orderByDesc("create_time").orderByDesc("id");
        data.put("recentPhotos", photoService.page(new Page<>(1, 10), recentPhotoQw).getRecords());
        return data;
    }


}
