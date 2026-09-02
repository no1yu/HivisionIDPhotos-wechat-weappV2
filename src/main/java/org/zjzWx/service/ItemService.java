package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.Item;

public interface ItemService extends IService<Item> {

    //尺寸列表
    <T> Page<T> itemList(int pageNum, int pageSize, int type, Integer userId, String name);


}
