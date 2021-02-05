package com.geek.gatewaydemo.router;

import java.util.List;

/**
 * @Auther: song.huai
 * @Date: 2021/1/26 00:30
 * @Description:
 */
public class RoundRobinRouter implements HttpEndpointRouter {

    private static int i = 0;

    @Override
    public String name() {
        return "round_robin";
    }

    @Override
    public String route(List<String> endpoints) {
        int j = (RoundRobinRouter.i + 1) % endpoints.size();
        i = j;
        return endpoints.get(j);
    }
}
