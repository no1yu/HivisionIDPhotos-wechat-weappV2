package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.HelpDao;
import org.zjzWx.entity.Help;
import org.zjzWx.service.HelpService;

import java.util.List;

@Service
public class HelpServiceImpl extends ServiceImpl<HelpDao, Help> implements HelpService {

    @Override
    public List<Help> getHelpList() {
        QueryWrapper<Help> qw = new QueryWrapper<>();
        qw.orderByAsc("sort");
        return baseMapper.selectList(qw);
    }
}
