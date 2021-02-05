package com.geek.gatewaydemo.router;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @Auther: song.huai
 * @Date: 2021/1/26 00:40
 * @Description:
 */
public class RouterRegister {

    private static Map<String, HttpEndpointRouter> map = Maps.newHashMap();

    static {
        HttpEndpointRouter randomRouter = new RandomHttpEndpointRouter();
        HttpEndpointRouter roundRobinRouter = new RoundRobinRouter();
        map.put(randomRouter.name(), randomRouter);
        map.put(roundRobinRouter.name(), roundRobinRouter);
    }

    public HttpEndpointRouter getRouter(String routerConf){
        return map.get(routerConf);
    }
}
