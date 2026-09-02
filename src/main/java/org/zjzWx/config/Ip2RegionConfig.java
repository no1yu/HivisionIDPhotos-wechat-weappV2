package org.zjzWx.config;

import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class Ip2RegionConfig {

    /**
     * 启动时把IPv4和IPv6离线数据库加载到内存，登录查询时直接复用
     */
    @Bean(destroyMethod = "close")
    public Ip2Region ip2Region() throws Exception {
        try(InputStream v4InputStream = new ClassPathResource("ip2region/ip2region_v4.xdb").getInputStream();
            InputStream v6InputStream = new ClassPathResource("ip2region/ip2region_v6.xdb").getInputStream()){
            Config v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(v4InputStream)
                    .asV4();
            Config v6Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(v6InputStream)
                    .asV6();
            return Ip2Region.create(v4Config,v6Config);
        }
    }
}
