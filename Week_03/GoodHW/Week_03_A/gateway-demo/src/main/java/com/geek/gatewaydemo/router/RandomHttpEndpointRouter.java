package com.geek.gatewaydemo.router;

import java.util.List;
import java.util.Random;

public class RandomHttpEndpointRouter implements HttpEndpointRouter {
    @Override
    public String name() {
        return "random";
    }

    @Override
    public String route(List<String> urls) {
        int size = urls.size();
        Random random = new Random(System.currentTimeMillis());
        return urls.get(random.nextInt(size));
    }
}
