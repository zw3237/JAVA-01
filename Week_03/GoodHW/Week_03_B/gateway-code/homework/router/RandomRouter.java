package daiwei.geekbang.homework.router;

import java.util.List;
import java.util.Random;

/**
 * Created by Daiwei on 2021/1/29
 */
public class RandomRouter extends HttpRouter {

    private final Random rdn;

    public RandomRouter(List<String> proxyServers) {
        super(proxyServers);
        this.rdn = new Random();
    }

    @Override
    public String route() {
        return this.proxyServers.get(this.rdn.nextInt(3));
    }
}
