package org.zjzWx.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 [Sa-Token全局过滤器]
 */
@Configuration
public class SaTokenConfigure {

    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()

                // 指定 拦截路由 与 放行路由
                .addInclude("/**").addExclude("/favicon.ico")

                // 认证函数: 每次请求执行
                .setAuth(obj -> {
                    System.out.println("---------- 进入Sa-Token全局认证 -----------");
                    // 登录认证 -- 拦截所有路由,特别url放行
                    SaRouter.match("/**").notMatch(
                            "/user/login",
                            "/user/getLoginType",
                            "/user/getMineSet",
                            "/user/getHelpList",
                            "/item/itemList",
                            "/admin/login",
                            "/admin/checkLogin",
                            "/admin/okLogin",
                            "/admin/login/ws",
                            "/otherApi/exploreIndex",
                            "/api/getBeautySwitch",
                            "/api/getClothesList",
                            //下载设置不需要登录，微信回调使用微信签名验证
                            "/order/getDownloadSet",
                            "/pay/**",
                            "/imgTest/**").check(r -> StpUtil.checkLogin());

                    // 管理员接口只允许用户ID为1的登录账号访问
                    SaRouter.match("/admin/**").notMatch("/admin/login",
                            "/admin/checkLogin",
                            "/admin/okLogin",
                            "/admin/login/ws").check(r -> {
                        if(StpUtil.getLoginIdAsInt()!=1){
                            SaHolder.getResponse().setStatus(500);
                            SaRouter.back(SaResult.error("非法请求"));
                        }
                    });
                })

                // 异常处理函数：每次认证函数发生异常时执行此函数
                .setError(e -> {
                    System.out.println("---------- 进入Sa-Token异常处理 -----------");
                    SaHolder.getResponse().setStatus(500);
                    return SaResult.error(e.getMessage());
                })

                // 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
                .setBeforeAuth(r -> {
                    // ---------- 设置一些安全响应头 ----------
                    SaHolder.getResponse()
                    // 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以
                    .setHeader("X-Frame-Options", "DENY")
                    // 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
                    .setHeader("X-XSS-Protection", "1; mode=block")
                    // 禁用浏览器内容嗅探
                    .setHeader("X-Content-Type-Options", "nosniff");
                });
    }


}
