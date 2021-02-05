package daiwei.geekbang.homework.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Daiwei on 2021/1/29
 */
public class RoundRobinRouter extends HttpRouter {

    private final AtomicInteger cnt = new AtomicInteger();

    public RoundRobinRouter(List<String> proxyServers) {
        super(proxyServers);
    }

    @Override
    public String route() {
        return this.proxyServers.get(cnt.getAndIncrement() % this.proxyServers.size());
    }
}
