package com.geek.gatewaydemo.router;

import java.util.List;

/**
 * @Auther: song.huai
 * @Date: 2021/1/26 00:47
 * @Description:
 */
public class RouterSelector {

    private RouterRegister routerRegister = new RouterRegister();

    public String getUrl(List<String> urls){
        String name = "round_robin";//此处可作为配置
        HttpEndpointRouter router = routerRegister.getRouter(name);
        return router.route(urls);
    }
}
