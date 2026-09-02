package org.zjzWx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.zjzWx.dao.ClothesSetDao;
import org.zjzWx.entity.ClothesSet;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.AppSetService;
import org.zjzWx.service.ClothesSetService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
public class ClothesSetServiceImpl extends ServiceImpl<ClothesSetDao, ClothesSet> implements ClothesSetService {

    private static final String CLOUD_URL = "https://cloud.0po.cn/";

    @Autowired
    @Qualifier("bizExecutor")
    private Executor bizExecutor;
    //同步任务状态：1未开始，2同步中，3同步成功，4同步失败
    private volatile int status = 1;
    //当前正在处理的素材名称
    private volatile String currentName = "";
    //同步结果或失败原因
    private volatile String message = "";
    //当前已经完成的素材数量
    private volatile int current = 0;
    //需要同步的素材总数
    private volatile int total = 43;

    @Autowired
    private AppSetService appSetService;
    @Autowired
    private WebSetService webSetService;

    @Override
    public List<ClothesSet> getClothesList(Integer category) {
        if(appSetService.getClothesSwitch()==0){
            return null;
        }
        QueryWrapper<ClothesSet> qw = new QueryWrapper<>();
        qw.eq("status",1);
        qw.eq("category",category);
        return baseMapper.selectList(qw);
    }

    @Override
    public boolean isClothesAvailable(Integer category, Integer clothesId) {
        QueryWrapper<ClothesSet> qw = new QueryWrapper<>();
        qw.eq("category",category);
        qw.eq("clothes_id",clothesId);
        qw.eq("status",1);
        return baseMapper.selectCount(qw)>0;
    }

    @Override
    public synchronized String startImageSync() {
        if(status==2){
            return "换装素材正在同步中";
        }
        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("pic_domain","directory");
        WebSet storageSet = webSetService.getOne(storageQw);
        if(storageSet==null || !StringUtils.hasText(storageSet.getPicDomain()) || !StringUtils.hasText(storageSet.getDirectory())){
            return "上传图片失败，请先配置图片存储，系统设置-图片存储配置";
        }
        status = 2;
        currentName = "正在读取云平台素材清单";
        message = "";
        current = 0;
        total = 43;
        bizExecutor.execute(() -> sync(storageSet.getDirectory(),storageSet.getPicDomain()));
        return null;
    }

    @Override
    public Map<String,Object> getImageSyncProgress() {
        Map<String,Object> progress = new LinkedHashMap<>();
        progress.put("status",status);
        progress.put("currentName",currentName);
        progress.put("current",current);
        progress.put("total",total);
        progress.put("message",message);
        return progress;
    }

    private void sync(String directory,String picDomain) {
        Path stagingPath = PicUtil.getFile("clothesSync",directory);
        Path clothesPath = PicUtil.getFile("clothes",directory);
        Path backupPath = PicUtil.getFile("clothesBackup",directory);
        try {
            PicUtil.deleteDirectory(stagingPath);
            PicUtil.deleteDirectory(backupPath);
            Files.createDirectories(stagingPath);

            JSONObject response = JSONObject.parseObject(new RestTemplate().getForObject(CLOUD_URL+"sync/clothesImageList",String.class));
            if(response==null || response.getIntValue("code")!=200 || response.getJSONArray("data")==null || response.getJSONArray("data").isEmpty()){
                throw new IllegalStateException("云平台换装素材读取失败");
            }
            JSONArray data = response.getJSONArray("data");
            total = data.size();

            List<ClothesSet> localList = list();
            Map<String,ClothesSet> localMap = new HashMap<>();
            for(ClothesSet clothesSet : localList){
                localMap.put(clothesSet.getCategory()+"-"+clothesSet.getClothesId(),clothesSet);
            }
            if(localMap.size()!=total){
                throw new IllegalStateException("本地换装素材数量与云平台不一致");
            }

            RestTemplate restTemplate = new RestTemplate();
            List<ClothesSet> updates = new ArrayList<>();
            for(int i=0;i<data.size();i++){
                JSONObject imageData = data.getJSONObject(i);
                int category = imageData.getIntValue("category");
                int clothesId = imageData.getIntValue("clothesId");
                String filename = imageData.getString("filename");
                ClothesSet clothesSet = localMap.get(category+"-"+clothesId);
                if(clothesSet==null){
                    throw new IllegalStateException("本地缺少换装素材 "+category+"/"+filename);
                }

                currentName = "正在下载 "+category+"/"+filename;
                byte[] imageBytes = restTemplate.getForObject(imageData.getString("imageUrl"),byte[].class);
                if(imageBytes==null || !"png".equals(PicUtil.getImageExtension(imageBytes)) || !PicUtil.sha256(imageBytes).equalsIgnoreCase(imageData.getString("sha256"))){
                    throw new IllegalStateException("换装素材校验失败 "+category+"/"+filename);
                }
                Path imagePath = stagingPath.resolve(String.valueOf(category)).resolve(filename).normalize();
                if(!imagePath.startsWith(stagingPath)){
                    throw new IllegalStateException("换装素材路径非法");
                }
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath,imageBytes);

                ClothesSet update = new ClothesSet();
                update.setId(clothesSet.getId());
                update.setImageUrl(PicUtil.getPublicUrl("clothes/"+category+"/"+filename,picDomain));
                updates.add(update);
                current = i+1;
            }

            if(Files.exists(clothesPath)){
                PicUtil.moveDirectory(clothesPath,backupPath);
            }
            try {
                PicUtil.moveDirectory(stagingPath,clothesPath);
            } catch (Exception e) {
                if(Files.exists(backupPath)){
                    PicUtil.moveDirectory(backupPath,clothesPath);
                }
                throw e;
            }
            try {
                if(!updateBatchById(updates)){
                    throw new IllegalStateException("换装素材地址更新失败");
                }
            } catch (Exception e) {
                PicUtil.deleteDirectory(clothesPath);
                if(Files.exists(backupPath)){
                    PicUtil.moveDirectory(backupPath,clothesPath);
                }
                throw e;
            }
            PicUtil.deleteDirectory(backupPath);
            currentName = "全部素材同步完成";
            message = "换装素材同步成功";
            status = 3;
        } catch (Exception e) {
            currentName = "同步失败";
            message = e.getMessage()==null ? "换装素材同步失败" : e.getMessage();
            status = 4;
        }
    }

}
