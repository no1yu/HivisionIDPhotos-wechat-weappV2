package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.CustomDao;
import org.zjzWx.entity.Custom;
import org.zjzWx.service.CustomService;


@Service
public class CustomServiceImpl extends ServiceImpl<CustomDao,Custom> implements CustomService {
}
