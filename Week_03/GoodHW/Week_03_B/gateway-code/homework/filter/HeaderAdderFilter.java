package daiwei.geekbang.homework.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by Daiwei on 2021/1/29
 */
public class HeaderAdderFilter implements HttpRequestFilter {

    @Override
    public boolean filter(FullHttpRequest request, ChannelHandlerContext ctx) {
        request.headers().set("name", "daiwei");
        request.headers().set("age", 25);
        request.headers().set("addr", "shanghai");
        return true;
    }
}
