package daiwei.geekbang.homework;

import daiwei.geekbang.homework.server.NettyGateWayServer;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Daiwei on 2021/1/29
 */
public class NettyServerApplication {

    public static void main(String[] args) {
        int port = 8888;
        List<String> serverList = Arrays.asList("http://127.0.0.1:8801", "http://127.0.0.1:8802", "http://127.0.0.1:8803");
        NettyGateWayServer server = new NettyGateWayServer(port, serverList);
        System.out.println("my gateway is listening at http:127.0.0.1:"+ port + " for " + server.toString());
        try {
            server.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
