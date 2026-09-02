package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zjzWx.entity.Custom;
import org.zjzWx.entity.Photo;
import org.zjzWx.service.CustomService;
import org.zjzWx.service.ItemService;
import org.zjzWx.service.PhotoService;
import org.zjzWx.util.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/item")
public class ItemController {


    @Autowired
    private ItemService itemService;
    @Autowired
    private CustomService customService;
    @Autowired
    private PhotoService photoService;


    //保存用户自定义
    @PostMapping("/saveCustom")
    public R saveCustom(@RequestBody Custom custom){
        custom.setId(null);
        custom.setUserId(StpUtil.getLoginIdAsInt());
        custom.setIcon(new Random().nextInt(6) + 1);
        custom.setCreateTime(new Date());
        customService.save(custom);
        return R.ok(custom.getId());
    }

    //证件列表
    @GetMapping("/itemList")
    public R itemList(int pageNum, int pageSize, int type,String name){
        Integer userId = 0;
        if(type==4){
            userId = StpUtil.getLoginIdAsInt();
        }
        Page<?> page = itemService.itemList(pageNum, pageSize, type,userId,name);
        Map<String,Object> data = new HashMap<>();
        data.put("records",page.getRecords());
        data.put("pages",page.getPages());
        return R.ok(data);
    }


    //用户作品列表
    @GetMapping("/photoList")
    public R photoList(int pageNum, int pageSize){
        Page<Photo> page = photoService.photoList(pageNum, pageSize, StpUtil.getLoginIdAsInt());
        Map<String,Object> data = new HashMap<>();
        data.put("records",page.getRecords());
        data.put("pages",page.getPages());
        return R.ok(data);
    }

    //删除作品
    @GetMapping("/deletePhotoId")
    public R deletePhotoId(int id){
        photoService.deletePhotoId(id,StpUtil.getLoginIdAsInt());
        return R.ok();
    }

}
