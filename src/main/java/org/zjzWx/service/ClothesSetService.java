package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.ClothesSet;

import java.util.List;
import java.util.Map;

public interface ClothesSetService extends IService<ClothesSet> {

    //读取小程序可用的换装素材
    List<ClothesSet> getClothesList(Integer category);

    //检查指定换装素材状态是否正常
    boolean isClothesAvailable(Integer category,Integer clothesId);

    //开始同步换装素材图片，成功时返回null
    String startImageSync();

    //读取换装素材同步进度
    Map<String,Object> getImageSyncProgress();

}
