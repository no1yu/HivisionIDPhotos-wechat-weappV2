package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.WebSetDao;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.WebSetService;

import java.util.HashMap;
import java.util.Map;


@Service
public class WebSetServiceImpl extends ServiceImpl<WebSetDao,WebSet> implements WebSetService {

    @Override
    public Map<String,Object> getMineSet() {
        QueryWrapper<WebSet> qw = new QueryWrapper<>();
        qw.eq("id",1);
        qw.select("official_switch","official_qr_code_image_url");
        WebSet webSet = baseMapper.selectOne(qw);
        Map<String,Object> mineSet = new HashMap<>();
        mineSet.put("officialSwitch",webSet.getOfficialSwitch());
        mineSet.put("officialQrCodeImageUrl",null);
        if(webSet.getOfficialSwitch()==1){
            mineSet.put("officialQrCodeImageUrl",webSet.getOfficialQrCodeImageUrl());
        }
        return mineSet;
    }
}
