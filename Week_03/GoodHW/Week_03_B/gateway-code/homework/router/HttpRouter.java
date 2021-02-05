package daiwei.geekbang.homework.router;

import java.util.List;

/**
 * Created by Daiwei on 2021/1/29
 */
public abstract class HttpRouter {

    protected final List<String> proxyServers;

    public HttpRouter(List<String> proxyServers) {
        this.proxyServers = proxyServers;
    }

    public abstract String route();
}
