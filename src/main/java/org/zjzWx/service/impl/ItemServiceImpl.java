package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.ItemDao;
import org.zjzWx.entity.Custom;
import org.zjzWx.entity.Item;
import org.zjzWx.service.CustomService;
import org.zjzWx.service.ItemService;


@Service
public class ItemServiceImpl extends ServiceImpl<ItemDao,Item> implements ItemService {

    @Autowired
    private CustomService customService;

    @Override
    public <T> Page<T> itemList(int pageNum, int pageSize, int type, Integer userId,String name) {

        //当查询用户定制规格时
        if(type==4){
            Page<Custom> page = new Page<>(pageNum, pageSize);
            QueryWrapper<Custom> qw = new QueryWrapper<>();
            qw.eq("user_id",userId);
            qw.select("id","name","width_px","height_px","width_mm","height_mm","dpi","icon");
            qw.orderByDesc("create_time");
            return (Page<T>) customService.page(page, qw);
        }

        //查询系统规格
        Page<Item> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Item> qw = new QueryWrapper<>();
        qw.eq("status",1);

        //当用户填写了规格名称时
        if(name!=null && !name.equals("")){
            qw.like("name",name);
        }else {
            qw.eq("category",type);
        }

        qw.select("id","name","width_px","height_px","width_mm","height_mm","icon","dpi");
        return (Page<T>) baseMapper.selectPage(page, qw);
    }
}
